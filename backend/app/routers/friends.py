"""好友：好友列表 / 语音留言 + 文字（异步语音消息）。

语音留言走 WebSocket/HTTP 文件存储，不用 Agora 实时：
客户端先把语音文件上传到文件存储，拿到 voice_url 再调本接口。
"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

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
