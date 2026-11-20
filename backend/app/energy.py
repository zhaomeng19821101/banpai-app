"""半拍 Banpai 后端 — 能量账本：所有能量变动的唯一入口。"""
from sqlalchemy.orm import Session

from app.models import EnergyLedger, User


def apply_energy(db: Session, user: User, amount: int, kind: str, ref: str = "") -> EnergyLedger:
    """能量变动统一入口：改 users.energy 缓存 + 写 energy_ledger 一条记录。

    kind 取值：checkin_first / checkin_revisit / photo / cameo / postcard /
    redeem / unlock / month_card / admin。
    消耗时 amount 为负；余额不足时抛 ValueError，由路由转 400。
    """
    new_balance = (user.energy or 0) + amount
    if new_balance < 0:
        raise ValueError("能量不足")
    user.energy = new_balance
    entry = EnergyLedger(
        user_id=user.id, amount=amount, kind=kind, ref=ref, balance_after=new_balance
    )
    db.add(entry)
    db.flush()
    return entry
