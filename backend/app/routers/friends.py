"""好友：好友列表 / 语音留言 + 文字（异步语音消息）+ 好友对讲控件取 token。

语音留言走 WebSocket/HTTP 文件存储，不用 Agora 实时：
客户端先把语音文件上传到文件存储，拿到 voice_url 再调本接口。
实时对讲只在好友 1:1 里用（好友对讲控件）：/space-token 用证书签发
真实 RTC token；公共区域（派对房间 / 公共花园）不用声网实时语音。
"""
import hashlib
import os

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.agora_tokens import build_rtc_token, friend_channel
from app.db import get_db
from app.models import Friend, FriendMessage, User
from app.schemas import FriendMessageIn, FriendMessageOut, UserOut
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/friends", tags=["friends"])


@router.get("", response_model=list[UserOut])
def list_friends(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = db.query(Friend).filter_by(user_id=user.id, status="accepted").all()
    out = []
    for r in rows:
        u = db.get(User, r.friend_id)
        if u:
            out.append(u)
    return out


@router.get("/{friend_id}/space-token")
def friend_space_token(friend_id: int, user: User = Depends(get_current_user),
                       db: Session = Depends(get_db)):
    """好友对讲控件取 token：好友 1:1 实时对讲专用。

    频道名由双方 id 排序拼出，两边算出同一个频道。已配置证书时签发真实 token；
    未配置时返回占位 token 并写明原因，客户端不要拿它真连。
    """
    friend = db.get(User, friend_id)
    if friend is None:
        raise HTTPException(status_code=404, detail="好友不存在")
    is_friend = db.query(Friend).filter(
        Friend.status == "accepted",
        ((Friend.user_id == user.id) & (Friend.friend_id == friend_id)) |
        ((Friend.user_id == friend_id) & (Friend.friend_id == user.id))
    ).first() is not None
    if not is_friend:
        raise HTTPException(status_code=403, detail="还不是好友")
    channel = friend_channel(user.id, friend_id)
    real = build_rtc_token(channel, user.id)
    if real is not None:
        return real
    digest = hashlib.sha256(f"placeholder:friend:{user.id}:{friend_id}".encode()).hexdigest()[:32]
    return {"app_id": os.environ.get("AGORA_APP_ID", "") or "placeholder",
            "token": f"placeholder-{digest}",
            "channel": channel, "uid": user.id,
            "expires_in": 3600, "placeholder": True,
            "detail": "未配置声网证书，返回占位 token，客户端不要真连"}


@router.post("/{friend_id}/messages", response_model=FriendMessageOut)
def send_friend_message(friend_id: int, body: FriendMessageIn,
                        user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    friend = db.get(User, friend_id)
    if friend is None:
        raise HTTPException(status_code=404, detail="好友不存在")
    if not body.text and not body.voice_url:
        raise HTTPException(status_code=400, detail="文字或语音至少发一种")
    row = FriendMessage(sender_id=user.id, receiver_id=friend_id, text=body.text,
                        voice_url=body.voice_url, duration_sec=body.duration_sec)
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


@router.get("/{friend_id}/messages", response_model=list[FriendMessageOut])
def list_friend_messages(friend_id: int, user: User = Depends(get_current_user),
                         db: Session = Depends(get_db)):
    rows = (
        db.query(FriendMessage)
        .filter(
            ((FriendMessage.sender_id == user.id) & (FriendMessage.receiver_id == friend_id))
            | ((FriendMessage.sender_id == friend_id) & (FriendMessage.receiver_id == user.id))
        )
        .order_by(FriendMessage.id.desc()).limit(100).all()
    )
    return list(reversed(rows))
