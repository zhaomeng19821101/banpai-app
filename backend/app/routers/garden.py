"""花园 + 雪夜传信。

花园：动态流 / 今日 / 发帖 / 帖子详情 / 评论 / 点赞。
雪夜传信：信放进飘雪，100m 内可捡，24h 化掉，一封只被一人捡。
"""
import math
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.constants import LETTER_PICKUP_RADIUS_M, LETTER_TTL_HOURS
from app.db import get_db
from app.models import GardenComment, GardenLike, GardenPost, Letter, User
from app.schemas import CommentIn, LetterIn, LetterOut, PostIn, PostOut
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/garden", tags=["garden"])


def _post_out(post: GardenPost, db: Session) -> PostOut:
    u = db.get(User, post.user_id)
    likes = db.query(GardenLike).filter_by(post_id=post.id).count()
    comments = db.query(GardenComment).filter_by(post_id=post.id).count()
    return PostOut(id=post.id, user_id=post.user_id, nickname=u.nickname if u else "",
                   text=post.text, image_url=post.image_url or "", likes=likes,
                   comments=comments, created_at=post.created_at)


@router.get("/feed", response_model=list[PostOut])
def feed(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    posts = db.query(GardenPost).order_by(GardenPost.id.desc()).limit(50).all()
    return [_post_out(p, db) for p in posts]


@router.get("/today", response_model=list[PostOut])
def today(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    start = datetime.now(timezone.utc).replace(tzinfo=None).replace(hour=0, minute=0, second=0, microsecond=0)
    posts = (db.query(GardenPost).filter(GardenPost.created_at >= start)
             .order_by(GardenPost.id.desc()).all())
    return [_post_out(p, db) for p in posts]


@router.post("/posts", response_model=PostOut)
def create_post(body: PostIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = GardenPost(user_id=user.id, text=body.text, image_url=body.image_url)
    db.add(row)
    db.commit()
    db.refresh(row)
    return _post_out(row, db)


@router.get("/posts/{post_id}", response_model=PostOut)
def get_post(post_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    post = db.get(GardenPost, post_id)
    if post is None:
        raise HTTPException(status_code=404, detail="帖子不存在")
    return _post_out(post, db)


@router.get("/posts/{post_id}/comments")
def list_comments(post_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = (db.query(GardenComment).filter_by(post_id=post_id)
            .order_by(GardenComment.id).all())
    out = []
    for r in rows:
        u = db.get(User, r.user_id)
        out.append({"id": r.id, "user_id": r.user_id, "nickname": u.nickname if u else "",
                    "text": r.text, "created_at": r.created_at.isoformat()})
    return out


@router.post("/posts/{post_id}/comments")
def add_comment(post_id: int, body: CommentIn, user: User = Depends(get_current_user),
                db: Session = Depends(get_db)):
    if db.get(GardenPost, post_id) is None:
        raise HTTPException(status_code=404, detail="帖子不存在")
    row = GardenComment(post_id=post_id, user_id=user.id, text=body.text)
    db.add(row)
    db.commit()
    db.refresh(row)
    return {"ok": True, "id": row.id, "text": row.text}


@router.post("/posts/{post_id}/like")
def like_post(post_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if db.get(GardenPost, post_id) is None:
        raise HTTPException(status_code=404, detail="帖子不存在")
    if db.query(GardenLike).filter_by(post_id=post_id, user_id=user.id).first():
        return {"ok": True, "detail": "已点赞过"}
    db.add(GardenLike(post_id=post_id, user_id=user.id))
    db.commit()
    return {"ok": True, "detail": "已点赞"}


@router.delete("/posts/{post_id}/like")
def unlike_post(post_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = db.query(GardenLike).filter_by(post_id=post_id, user_id=user.id).first()
    if row is None:
        return {"ok": True, "detail": "本来就没点赞"}
    db.delete(row)
    db.commit()
    return {"ok": True, "detail": "已取消点赞"}


# ---- 雪夜传信 ----
@router.post("/letters", response_model=LetterOut)
def send_letter(body: LetterIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    row = Letter(sender_id=user.id, lat=body.lat, lng=body.lng, text=body.text,
                 created_at=now, expires_at=now + timedelta(hours=LETTER_TTL_HOURS))
    db.add(row)
    db.commit()
    db.refresh(row)
    return LetterOut(id=row.id, sender_id=row.sender_id, lat=row.lat, lng=row.lng,
                     text=row.text, expires_at=row.expires_at, picked=False)


@router.get("/letters/nearby", response_model=list[LetterOut])
def nearby_letters(lat: float, lng: float, user: User = Depends(get_current_user),
                   db: Session = Depends(get_db)):
    """100m 内未被捡、未过期的信（24h 化掉）。只返回位置，不返回全文——捡到才看全文。"""
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    out = []
    for row in db.query(Letter).filter(Letter.picker_id.is_(None), Letter.expires_at > now).all():
        dx = (row.lng - lng) * 85000
        dy = (row.lat - lat) * 111000
        if math.hypot(dx, dy) <= LETTER_PICKUP_RADIUS_M:
            out.append(LetterOut(id=row.id, sender_id=row.sender_id, lat=row.lat, lng=row.lng,
                                 text="", expires_at=row.expires_at, picked=False))
    return out


@router.post("/letters/{letter_id}/pick", response_model=LetterOut)
def pick_letter(letter_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = db.get(Letter, letter_id)
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    if row is None:
        raise HTTPException(status_code=404, detail="这封信不存在")
    if row.picker_id is not None:
        raise HTTPException(status_code=409, detail="这封信已经被别人捡走了")
    if row.expires_at <= now:
        raise HTTPException(status_code=410, detail="这封信已经化掉了")
    row.picker_id = user.id
    row.picked_at = now
    db.commit()
    db.refresh(row)
    return LetterOut(id=row.id, sender_id=row.sender_id, lat=row.lat, lng=row.lng,
                     text=row.text, expires_at=row.expires_at, picked=True)
