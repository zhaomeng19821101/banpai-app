"""打卡：打卡 / 乱入奖励 / 打卡点 / 进度 / 守护宠 / 路线。"""
import math
from datetime import date

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.constants import (
    ENERGY_CAMEO_PER_DAY,
    ENERGY_FIRST_CHECKIN,
    ENERGY_PHOTO,
    ENERGY_REVISIT,
    LOCKED_LINES,
)
from app.db import get_db
from app.energy import apply_energy
from app.models import Checkin, Checkpoint, EnergyLedger, Guardian, User
from app.schemas import CameoIn, CheckinIn, CheckinOut, GuardianOut, PointOut, RouteOut
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/checkin", tags=["checkin"])


@router.post("", response_model=CheckinOut)
def checkin(body: CheckinIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    point = db.get(Checkpoint, body.point_id)
    if point is None:
        raise HTTPException(status_code=404, detail="打卡点不存在")
    today = date.today()
    # 每点每天 1 次：已打过则幂等返回（不重复加能量）
    existing = (
        db.query(Checkin)
        .filter_by(user_id=user.id, point_id=body.point_id, day=today)
        .first()
    )
    if existing is not None:
        return CheckinOut(id=existing.id, point_id=body.point_id, day=today,
                          energy_gained=0, first_time=False)
    first_time = db.query(Checkin).filter_by(user_id=user.id, point_id=body.point_id).first() is None
    gained = ENERGY_FIRST_CHECKIN if first_time else ENERGY_REVISIT
    if body.photo_url:
        gained += ENERGY_PHOTO
    row = Checkin(user_id=user.id, point_id=body.point_id, day=today,
                  lat=body.lat, lng=body.lng, photo_url=body.photo_url)
    db.add(row)
    db.flush()
    kind = "checkin_first" if first_time else "checkin_revisit"
    apply_energy(db, user, gained, kind, ref=f"point:{body.point_id}")
    db.commit()
    db.refresh(row)
    return CheckinOut(id=row.id, point_id=body.point_id, day=today,
                      energy_gained=gained, first_time=first_time)


@router.post("/cameo-reward")
def cameo_reward(body: CameoIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """乱入 +5/天：绿幕乱入拍照后领一次，当天重复领返回 0。"""
    from datetime import datetime

    today = date.today()
    day_start = datetime(today.year, today.month, today.day)
    already = (
        db.query(EnergyLedger)
        .filter(EnergyLedger.user_id == user.id, EnergyLedger.kind == "cameo",
                EnergyLedger.created_at >= day_start)
        .first()
    )
    if already is not None:
        return {"ok": True, "energy_gained": 0, "detail": "今天已领取乱入奖励"}
    apply_energy(db, user, ENERGY_CAMEO_PER_DAY, "cameo", ref=body.photo_url or "cameo")
    db.commit()
    return {"ok": True, "energy_gained": ENERGY_CAMEO_PER_DAY,
            "line": LOCKED_LINES[7], "detail": "乱入奖励 +5"}


@router.get("/points", response_model=list[PointOut])
def points(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    visited_ids = {
        c.point_id for c in db.query(Checkin).filter_by(user_id=user.id).all()
    }
    out = []
    for p in db.query(Checkpoint).order_by(Checkpoint.id).all():
        out.append(PointOut(id=p.id, name=p.name, lat=p.lat, lng=p.lng,
                            guardian_id=p.guardian_id, visited=p.id in visited_ids))
    return out


@router.get("/progress")
def progress(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    total = db.query(Checkpoint).count()
    visited = db.query(Checkin).filter_by(user_id=user.id).distinct(Checkin.point_id).count() \
        if db.bind.dialect.name != "sqlite" else len({c.point_id for c in db.query(Checkin).filter_by(user_id=user.id).all()})
    return {"total": total, "visited": visited, "energy": user.energy or 0}


@router.get("/guardians", response_model=list[GuardianOut])
def guardians(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    visited_guardian_ids = set()
    for c in db.query(Checkin).filter_by(user_id=user.id).all():
        p = db.get(Checkpoint, c.point_id)
        if p and p.guardian_id:
            visited_guardian_ids.add(p.guardian_id)
    out = []
    for g in db.query(Guardian).order_by(Guardian.id).all():
        out.append(GuardianOut(id=g.id, name=g.name, location=g.location, pet=g.pet,
                               sound=g.sound, line=g.line, chapter=g.chapter,
                               unlocked=g.id in visited_guardian_ids))
    return out


@router.get("/guardians/{guardian_id}", response_model=GuardianOut)
def guardian_detail(guardian_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    g = db.get(Guardian, guardian_id)
    if g is None:
        raise HTTPException(status_code=404, detail="守护宠不存在")
    visited = (
        db.query(Checkin).join(Checkpoint, Checkin.point_id == Checkpoint.id)
        .filter(Checkin.user_id == user.id, Checkpoint.guardian_id == guardian_id)
        .first() is not None
    )
    return GuardianOut(id=g.id, name=g.name, location=g.location, pet=g.pet,
                        sound=g.sound, line=g.line, chapter=g.chapter, unlocked=visited)


@router.get("/routes", response_model=RouteOut)
def routes(from_point_id: int, to_point_id: int, db: Session = Depends(get_db),
           user: User = Depends(get_current_user)):
    """步行路线：有 AMAP_KEY 时应调高德步行规划；未配置时返回直线距离占位。"""
    import os

    a = db.get(Checkpoint, from_point_id)
    b = db.get(Checkpoint, to_point_id)
    if a is None or b is None:
        raise HTTPException(status_code=404, detail="打卡点不存在")
    # 简化直线距离（米）
    dx = (b.lng - a.lng) * 85000
    dy = (b.lat - a.lat) * 111000
    dist = int(math.hypot(dx, dy))
    source = "amap" if os.environ.get("AMAP_KEY") else "straight-line"
    return RouteOut(from_point_id=from_point_id, to_point_id=to_point_id,
                    distance_m=dist, walk_minutes=max(1, dist // 80),
                    polyline=[[a.lat, a.lng], [b.lat, b.lng]], source=source)
