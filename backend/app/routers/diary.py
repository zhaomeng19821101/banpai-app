"""日记：纯文字，不读出声。

种子数据：2026-11-01 起每天一篇到 11-19，共 19 篇，
每篇结尾锁定台词 #14「晚安，明天见。」（见 app/seed.py）。
"""
from datetime import date

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.constants import LOCKED_LINES
from app.db import get_db
from app.models import Diary, User
from app.schemas import DiaryGenerateIn, DiaryOut
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/diary", tags=["diary"])


@router.get("", response_model=list[DiaryOut])
def list_diaries(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return (
        db.query(Diary).filter_by(user_id=user.id)
        .order_by(Diary.day.desc()).all()
    )


@router.get("/today", response_model=DiaryOut)
def today_diary(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = db.query(Diary).filter_by(user_id=user.id, day=date.today()).first()
    if row is None:
        raise HTTPException(status_code=404, detail="今天的日记还没有")
    return row


@router.post("/generate", response_model=DiaryOut)
def generate_diary(body: DiaryGenerateIn, user: User = Depends(get_current_user),
                   db: Session = Depends(get_db)):
    """生成一篇日记（纯文字；不读出声，不做 TTS/口型）。

    占位实现：按心情拼一段模板文字；后续可接小模型生成。
    每篇结尾固定锁定台词 #14。
    """
    day = body.day or date.today()
    if db.query(Diary).filter_by(user_id=user.id, day=day).first():
        raise HTTPException(status_code=400, detail="这天的日记已经有了")
    mood = body.mood or "平静"
    text = (
        f"今天的心情是{mood}。我在哈尔滨慢慢走了一天，"
        f"冰、雪、乐、花，四片花瓣都在。第五瓣还在路上，我不着急。\n{LOCKED_LINES[14]}"
    )
    row = Diary(user_id=user.id, day=day, text=text)
    db.add(row)
    db.commit()
    db.refresh(row)
    return row
