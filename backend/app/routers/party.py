"""派对房间：房间管理 + WebSocket IM（IM 走房间消息接口）。

语音：派对房间只用异步语音消息，不用声网实时语音——公共区域陌生人
实时语音有合规风险（2026-11-20 用户锁定）。/agora-token 接口仍保留，
已配置证书时签发真实 token（兼容旧客户端），但 App 不再从派对房间调用它；
实时对讲只在好友 1:1（好友对讲控件）里用，走 /friends/{id}/space-token。
Agora 证书只留服务端：token 接口只下发 token，不下发证书。
"""
import hashlib
import os
import time

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.agora_tokens import build_rtc_token
from app.db import get_db
from app.models import Room, RoomMember, RoomMessage, User
from app.schemas import RoomIn, RoomMessageIn, RoomMessageOut, RoomOut
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/party", tags=["party"])


def _room_out(room: Room, db: Session) -> RoomOut:
    n = db.query(RoomMember).filter_by(room_id=room.id).count()
    return RoomOut(id=room.id, owner_id=room.owner_id, title=room.title,
                   topic=room.topic, lat=room.lat, lng=room.lng, members=n, is_live=room.is_live)


@router.post("/rooms", response_model=RoomOut)
def create_room(body: RoomIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    room = Room(owner_id=user.id, title=body.title, topic=body.topic,
                lat=body.lat, lng=body.lng, max_members=body.max_members)
    db.add(room)
    db.flush()
    db.add(RoomMember(room_id=room.id, user_id=user.id, role="owner"))
    db.commit()
    db.refresh(room)
    return _room_out(room, db)


@router.get("/rooms", response_model=list[RoomOut])
def list_rooms(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rooms = db.query(Room).filter_by(is_live=True).order_by(Room.id.desc()).all()
    return [_room_out(r, db) for r in rooms]


@router.get("/rooms/{room_id}", response_model=RoomOut)
def get_room(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    room = db.get(Room, room_id)
    if room is None:
        raise HTTPException(status_code=404, detail="房间不存在")
    return _room_out(room, db)


@router.post("/rooms/{room_id}/join")
def join_room(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    room = db.get(Room, room_id)
    if room is None or not room.is_live:
        raise HTTPException(status_code=404, detail="房间不存在或已结束")
    if db.query(RoomMember).filter_by(room_id=room_id, user_id=user.id).first():
        return {"ok": True, "detail": "已在房间里"}
    if db.query(RoomMember).filter_by(room_id=room_id).count() >= room.max_members:
        raise HTTPException(status_code=400, detail="房间已满")
    db.add(RoomMember(room_id=room_id, user_id=user.id, role="listener"))
    db.commit()
    return {"ok": True, "detail": "已加入"}


@router.post("/rooms/{room_id}/leave")
def leave_room(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    member = db.query(RoomMember).filter_by(room_id=room_id, user_id=user.id).first()
    if member is None:
        raise HTTPException(status_code=400, detail="不在房间里")
    room = db.get(Room, room_id)
    db.delete(member)
    if room and room.owner_id == user.id:
        room.is_live = False  # 房主离开，房间结束
    db.commit()
    return {"ok": True, "detail": "已离开"}


@router.get("/rooms/{room_id}/agora-token")
def agora_token(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """下发 Agora token：已配置证书时用 TokenBuilder 签发真实 token。

    未配置证书时返回确定性占位串，仅供联调识别，客户端不要拿它真连 Agora。
    注意：App 不再从派对房间调用本接口（公共区域不用声网实时语音）；
    实时对讲只在好友 1:1 里用，走 /friends/{id}/space-token。
    """
    room = db.get(Room, room_id)
    if room is None:
        raise HTTPException(status_code=404, detail="房间不存在")
    if db.query(RoomMember).filter_by(room_id=room_id, user_id=user.id).first() is None:
        raise HTTPException(status_code=403, detail="请先加入房间")
    real = build_rtc_token(f"banpai-room-{room_id}", user.id)
    if real is not None:
        return real
    digest = hashlib.sha256(f"placeholder:{room_id}:{user.id}".encode()).hexdigest()[:32]
    return {"app_id": os.environ.get("AGORA_APP_ID", "") or "placeholder",
            "token": f"placeholder-{digest}",
            "channel": f"banpai-room-{room_id}", "uid": user.id,
            "expires_in": 3600, "placeholder": True,
            "detail": "未配置声网证书，返回占位 token，客户端不要真连"}


@router.post("/rooms/{room_id}/raise-hand")
def raise_hand(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    member = db.query(RoomMember).filter_by(room_id=room_id, user_id=user.id).first()
    if member is None:
        raise HTTPException(status_code=400, detail="不在房间里")
    member.hand_raised = not member.hand_raised
    db.commit()
    return {"ok": True, "hand_raised": member.hand_raised}


@router.get("/rooms/{room_id}/speakers")
def speakers(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = db.query(RoomMember).filter_by(room_id=room_id).filter(
        RoomMember.role.in_(["owner", "speaker"])).all()
    return [{"user_id": r.user_id, "role": r.role, "muted": r.muted} for r in rows]


@router.post("/rooms/{room_id}/speakers")
def set_speaker(room_id: int, user_id: int, user: User = Depends(get_current_user),
                db: Session = Depends(get_db)):
    room = db.get(Room, room_id)
    if room is None or room.owner_id != user.id:
        raise HTTPException(status_code=403, detail="只有房主可以指定发言者")
    member = db.query(RoomMember).filter_by(room_id=room_id, user_id=user_id).first()
    if member is None:
        raise HTTPException(status_code=404, detail="该用户不在房间里")
    member.role = "speaker"
    member.hand_raised = False
    db.commit()
    return {"ok": True, "detail": "已设为发言者"}


@router.post("/rooms/{room_id}/mute")
def mute(room_id: int, user_id: int, muted: bool = True, user: User = Depends(get_current_user),
         db: Session = Depends(get_db)):
    room = db.get(Room, room_id)
    if room is None or room.owner_id != user.id:
        raise HTTPException(status_code=403, detail="只有房主可以禁言")
    member = db.query(RoomMember).filter_by(room_id=room_id, user_id=user_id).first()
    if member is None:
        raise HTTPException(status_code=404, detail="该用户不在房间里")
    member.muted = muted
    db.commit()
    return {"ok": True, "muted": muted}


@router.post("/rooms/{room_id}/kick")
def kick(room_id: int, user_id: int, user: User = Depends(get_current_user),
         db: Session = Depends(get_db)):
    room = db.get(Room, room_id)
    if room is None or room.owner_id != user.id:
        raise HTTPException(status_code=403, detail="只有房主可以踢人")
    member = db.query(RoomMember).filter_by(room_id=room_id, user_id=user_id).first()
    if member is None:
        raise HTTPException(status_code=404, detail="该用户不在房间里")
    db.delete(member)
    db.commit()
    return {"ok": True, "detail": "已移出房间"}


@router.get("/rooms/{room_id}/location")
def room_location(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    room = db.get(Room, room_id)
    if room is None:
        raise HTTPException(status_code=404, detail="房间不存在")
    return {"room_id": room.id, "lat": room.lat, "lng": room.lng}


@router.get("/rooms/{room_id}/messages", response_model=list[RoomMessageOut])
def room_messages(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = (db.query(RoomMessage).filter_by(room_id=room_id)
            .order_by(RoomMessage.id.desc()).limit(100).all())
    out = []
    for r in rows:
        u = db.get(User, r.user_id)
        out.append(RoomMessageOut(id=r.id, room_id=r.room_id, user_id=r.user_id,
                                  nickname=u.nickname if u else "", text=r.text,
                                  created_at=r.created_at))
    return list(reversed(out))


@router.post("/rooms/{room_id}/messages", response_model=RoomMessageOut)
def send_room_message(room_id: int, body: RoomMessageIn, user: User = Depends(get_current_user),
                      db: Session = Depends(get_db)):
    if db.query(RoomMember).filter_by(room_id=room_id, user_id=user.id).first() is None:
        raise HTTPException(status_code=403, detail="请先加入房间")
    row = RoomMessage(room_id=room_id, user_id=user.id, text=body.text)
    db.add(row)
    db.commit()
    db.refresh(row)
    return RoomMessageOut(id=row.id, room_id=room.id, user_id=user.id,
                          nickname=user.nickname, text=row.text, created_at=row.created_at)


@router.get("/rooms/{room_id}/online")
def room_online(room_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = db.query(RoomMember).filter_by(room_id=room_id).all()
    return {"room_id": room_id, "online": len(rows),
            "members": [{"user_id": r.user_id, "role": r.role} for r in rows]}
