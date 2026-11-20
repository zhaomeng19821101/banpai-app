"""装扮：16 套，只有 3 个槽 —— 服装/套装、背景、特效（配饰已去掉）。"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db import get_db
from app.energy import apply_energy
from app.models import Outfit, User, UserOutfit
from app.schemas import OutfitOut
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/outfits", tags=["outfits"])


@router.get("", response_model=list[OutfitOut])
def list_outfits(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    owned = {r.outfit_id: r for r in db.query(UserOutfit).filter_by(user_id=user.id).all()}
    return [OutfitOut(id=o.id, name=o.name, slot=o.slot, price=o.price,
                      owned=o.id in owned, equipped=owned[o.id].equipped if o.id in owned else False)
            for o in db.query(Outfit).order_by(Outfit.id).all()]


@router.post("/{outfit_id}/unlock")
def unlock_outfit(outfit_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    outfit = db.get(Outfit, outfit_id)
    if outfit is None:
        raise HTTPException(status_code=404, detail="装扮不存在")
    if db.query(UserOutfit).filter_by(user_id=user.id, outfit_id=outfit_id).first():
        return {"ok": True, "detail": "已拥有"}
    if outfit.price < 0:
        # price -1：种芽限定，不售卖（2026-11-20 Jackson 定：开花后在首页领取）
        raise HTTPException(status_code=400, detail="种芽限定装扮不售卖，开花后在首页领取")
    if outfit.price > 0:
        try:
            apply_energy(db, user, -outfit.price, "unlock", ref=f"outfit:{outfit_id}")
        except ValueError:
            raise HTTPException(status_code=400, detail="能量不足")
    db.add(UserOutfit(user_id=user.id, outfit_id=outfit_id))
    db.commit()
    return {"ok": True, "detail": f"已解锁 {outfit.name}"}


@router.post("/{outfit_id}/equip")
def equip_outfit(outfit_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = db.query(UserOutfit).filter_by(user_id=user.id, outfit_id=outfit_id).first()
    if row is None:
        raise HTTPException(status_code=400, detail="未拥有该装扮，请先解锁")
    outfit = db.get(Outfit, outfit_id)
    # 同槽只能装备一件
    same_slot_ids = [o.id for o in db.query(Outfit).filter_by(slot=outfit.slot).all()]
    db.query(UserOutfit).filter(UserOutfit.user_id == user.id,
                                UserOutfit.outfit_id.in_(same_slot_ids)).update({"equipped": False})
    row.equipped = True
    db.commit()
    return {"ok": True, "detail": "已装备", "slot": outfit.slot, "outfit_id": outfit_id}
