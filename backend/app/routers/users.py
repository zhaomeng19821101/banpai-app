"""用户：我的资料、能量与账本。"""
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import EnergyLedger, User
from app.schemas import EnergyOut, UserOut, UserPatchIn
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/users", tags=["users"])


@router.get("/me", response_model=UserOut)
def me(user: User = Depends(get_current_user)):
    return user


@router.patch("/me", response_model=UserOut)
def patch_me(body: UserPatchIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if body.nickname is not None:
        user.nickname = body.nickname
    if body.avatar_url is not None:
        user.avatar_url = body.avatar_url
    db.commit()
    db.refresh(user)
    return user


@router.get("/me/energy", response_model=EnergyOut)
def my_energy(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    rows = (
        db.query(EnergyLedger).filter_by(user_id=user.id)
        .order_by(EnergyLedger.id.desc()).limit(50).all()
    )
    ledger = [
        {"amount": r.amount, "kind": r.kind, "ref": r.ref,
         "balance_after": r.balance_after, "created_at": r.created_at.isoformat()}
        for r in rows
    ]
    return EnergyOut(energy=user.energy or 0, level=user.level, ledger=ledger)
