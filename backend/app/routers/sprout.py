"""一起种芽：浇水 → 开花 → 领限定装扮（奖励型，2026-11-20 Jackson 定）。

规则：每天浇水 1 次，累计浇 7 次开花（不要求连续 7 天）；
开花后领一次限定装扮「芽开·丁香」（种芽限定，不售卖，outfits.unlock 会拒绝）。
奖励不是守护宠（守护宠只靠打卡解锁），也不是表盘（金瓣 24 号只认第五瓣信件）。
"""
from datetime import date

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.constants import SPROUT_REWARD_OUTFIT, SPROUT_WATERS_TO_BLOOM
from app.db import get_db
from app.models import Outfit, Sprout, User, UserOutfit
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/sprout", tags=["sprout"])


def _get_or_create(db: Session, user: User) -> Sprout:
    row = db.query(Sprout).filter_by(user_id=user.id).first()
    if row is None:
        row = Sprout(user_id=user.id)
        db.add(row)
        db.commit()
        db.refresh(row)
    return row


def _status(row: Sprout) -> dict:
    return {
        "waters": row.waters,
        "waters_needed": SPROUT_WATERS_TO_BLOOM,
        "watered_today": row.last_watered == date.today(),
        "bloomed": row.bloomed,
        "claimed": row.claimed,
        "reward_outfit": SPROUT_REWARD_OUTFIT,
    }


@router.get("")
def get_sprout(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return _status(_get_or_create(db, user))


@router.post("/water")
def water_sprout(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = _get_or_create(db, user)
    today = date.today()
    if row.last_watered == today:
        raise HTTPException(status_code=400, detail="今天已经浇过水了，明天再来")
    row.waters += 1
    row.last_watered = today
    newly_bloomed = False
    if row.waters >= SPROUT_WATERS_TO_BLOOM and not row.bloomed:
        row.bloomed = True
        newly_bloomed = True
    db.commit()
    result = _status(row)
    result["detail"] = "开花了！去领限定装扮吧" if newly_bloomed else f"已浇水 {row.waters}/{SPROUT_WATERS_TO_BLOOM}"
    return result


@router.post("/claim")
def claim_reward(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = _get_or_create(db, user)
    if not row.bloomed:
        raise HTTPException(status_code=400, detail="还没开花，再浇浇水")
    if row.claimed:
        return {"ok": True, "detail": "已经领过啦，去装扮页看看"}
    outfit = db.query(Outfit).filter_by(name=SPROUT_REWARD_OUTFIT).first()
    if outfit is None:
        raise HTTPException(status_code=500, detail="限定装扮未配置，请先跑 seed")
    if db.query(UserOutfit).filter_by(user_id=user.id, outfit_id=outfit.id).first() is None:
        db.add(UserOutfit(user_id=user.id, outfit_id=outfit.id))
    row.claimed = True
    db.commit()
    return {"ok": True, "detail": f"已领取限定装扮「{outfit.name}」",
            "outfit_id": outfit.id, "outfit_name": outfit.name}
