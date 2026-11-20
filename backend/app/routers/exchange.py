"""兑换：能量余额 / 兑换码 / 月卡。"""
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db import get_db
from app.energy import apply_energy
from app.models import ExchangeCode, MonthCard, User
from app.schemas import MonthCardOut, RedeemCodeIn
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/exchange", tags=["exchange"])


@router.get("/balance")
def balance(user: User = Depends(get_current_user)):
    return {"energy": user.energy or 0, "level": user.level}


@router.post("/redeem-code")
def redeem_code(body: RedeemCodeIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    code = db.query(ExchangeCode).filter_by(code=body.code.strip()).first()
    if code is None:
        raise HTTPException(status_code=404, detail="兑换码不存在")
    if code.redeemed_by is not None:
        raise HTTPException(status_code=409, detail="兑换码已被使用")
    code.redeemed_by = user.id
    code.redeemed_at = datetime.now(timezone.utc).replace(tzinfo=None)
    apply_energy(db, user, code.reward_energy, "redeem", ref=f"code:{code.code}")
    db.commit()
    return {"ok": True, "energy_gained": code.reward_energy}


@router.get("/month-card", response_model=MonthCardOut)
def get_month_card(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    row = (db.query(MonthCard).filter_by(user_id=user.id, active=True)
           .order_by(MonthCard.ends_at.desc()).first())
    if row is None or row.ends_at <= now:
        return MonthCardOut(active=False, ends_at=row.ends_at if row else None)
    return MonthCardOut(active=True, ends_at=row.ends_at)


@router.post("/month-card", response_model=MonthCardOut)
def buy_month_card(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """月卡占位：30 天有效；支付对接前不扣费、不扣能量，只发卡记录。"""
    now = datetime.now(timezone.utc).replace(tzinfo=None)
    existing = (db.query(MonthCard).filter_by(user_id=user.id, active=True)
                .order_by(MonthCard.ends_at.desc()).first())
    if existing and existing.ends_at > now:
        return MonthCardOut(active=True, ends_at=existing.ends_at)
    row = MonthCard(user_id=user.id, started_at=now, ends_at=now + timedelta(days=30), active=True)
    db.add(row)
    db.commit()
    db.refresh(row)
    return MonthCardOut(active=True, ends_at=row.ends_at)
