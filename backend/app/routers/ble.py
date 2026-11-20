"""吧唧 BLE：表盘 / 设备 / OTA / 同步 / 小游戏记分。"""
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.constants import BADGE_GAMES
from app.db import get_db
from app.energy import apply_energy
from app.models import Device, DeviceEvent, Dial, GameScore, User, UserDial
from app.schemas import DeviceEventIn, DeviceIn, DeviceOut, DialOut, GameScoreIn
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/ble", tags=["ble"])

LATEST_FIRMWARE = "1.0.0"  # 占位版本号；OTA 包走 /assets 分发说明


# ---- 表盘（30 款） ----
@router.get("/dials", response_model=list[DialOut])
def list_dials(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    owned = {r.dial_id: r for r in db.query(UserDial).filter_by(user_id=user.id).all()}
    return [DialOut(id=d.id, name=d.name, style=d.style, price=d.price,
                    owned=d.id in owned, equipped=owned[d.id].equipped if d.id in owned else False)
            for d in db.query(Dial).order_by(Dial.id).all()]


@router.post("/dials/{dial_id}/redeem")
def redeem_dial(dial_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    dial = db.get(Dial, dial_id)
    if dial is None:
        raise HTTPException(status_code=404, detail="表盘不存在")
    if dial.is_hidden:
        raise HTTPException(status_code=403, detail="隐藏表盘不可直接兑换")
    if db.query(UserDial).filter_by(user_id=user.id, dial_id=dial_id).first():
        return {"ok": True, "detail": "已拥有"}
    if dial.price > 0:
        try:
            apply_energy(db, user, -dial.price, "unlock", ref=f"dial:{dial_id}")
        except ValueError:
            raise HTTPException(status_code=400, detail="能量不足")
    db.add(UserDial(user_id=user.id, dial_id=dial_id))
    db.commit()
    return {"ok": True, "detail": f"已兑换 {dial.name}"}


@router.post("/dials/{dial_id}/equip")
def equip_dial(dial_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    row = db.query(UserDial).filter_by(user_id=user.id, dial_id=dial_id).first()
    if row is None:
        raise HTTPException(status_code=400, detail="未拥有该表盘，请先兑换")
    db.query(UserDial).filter_by(user_id=user.id, equipped=True).update({"equipped": False})
    row.equipped = True
    db.commit()
    return {"ok": True, "detail": "已装备", "dial_id": dial_id}


@router.get("/ota/latest")
def ota_latest():
    return {"version": LATEST_FIRMWARE, "url": "/api/v1/assets/firmware/banpai.bin",
            "detail": "OTA 包占位：未配置真实固件包"}


# ---- 设备 ----
@router.get("/devices", response_model=list[DeviceOut])
def list_devices(user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    return db.query(Device).filter_by(user_id=user.id).all()


@router.post("/devices", response_model=DeviceOut)
def create_device(body: DeviceIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if db.query(Device).filter_by(serial=body.serial).first():
        raise HTTPException(status_code=400, detail="该设备已登记")
    dev = Device(serial=body.serial, name=body.name, user_id=user.id)
    db.add(dev)
    db.commit()
    db.refresh(dev)
    return dev


@router.post("/devices/bind", response_model=DeviceOut)
def bind_device(body: DeviceIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    dev = db.query(Device).filter_by(serial=body.serial).first()
    if dev is None:
        dev = Device(serial=body.serial, name=body.name)
        db.add(dev)
        db.flush()
    dev.user_id = user.id
    db.commit()
    db.refresh(dev)
    return dev


@router.get("/devices/{device_id}", response_model=DeviceOut)
def get_device(device_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    dev = db.get(Device, device_id)
    if dev is None or dev.user_id != user.id:
        raise HTTPException(status_code=404, detail="设备不存在")
    return dev


@router.get("/devices/{device_id}/state")
def device_state(device_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    dev = db.get(Device, device_id)
    if dev is None or dev.user_id != user.id:
        raise HTTPException(status_code=404, detail="设备不存在")
    equipped = db.query(UserDial).filter_by(user_id=user.id, equipped=True).first()
    return {"device_id": dev.id, "serial": dev.serial, "battery": dev.battery,
            "firmware_version": dev.firmware_version,
            "equipped_dial_id": equipped.dial_id if equipped else None,
            "last_seen": dev.last_seen.isoformat() if dev.last_seen else None}


@router.post("/devices/{device_id}/events")
def device_event(device_id: int, body: DeviceEventIn, user: User = Depends(get_current_user),
                 db: Session = Depends(get_db)):
    dev = db.get(Device, device_id)
    if dev is None or dev.user_id != user.id:
        raise HTTPException(status_code=404, detail="设备不存在")
    db.add(DeviceEvent(device_id=device_id, kind=body.kind, payload=body.payload))
    dev.last_seen = datetime.now(timezone.utc).replace(tzinfo=None)
    db.commit()
    return {"ok": True, "detail": "事件已记录"}


@router.get("/devices/{device_id}/ota-report")
def ota_report(device_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    dev = db.get(Device, device_id)
    if dev is None or dev.user_id != user.id:
        raise HTTPException(status_code=404, detail="设备不存在")
    return {"device_id": dev.id, "firmware_version": dev.firmware_version,
            "latest": LATEST_FIRMWARE, "up_to_date": dev.firmware_version == LATEST_FIRMWARE}


@router.get("/devices/{device_id}/ota-status")
def ota_status(device_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    dev = db.get(Device, device_id)
    if dev is None or dev.user_id != user.id:
        raise HTTPException(status_code=404, detail="设备不存在")
    return {"device_id": dev.id, "status": "idle", "progress": 0}


@router.post("/devices/{device_id}/sync")
def device_sync(device_id: int, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    dev = db.get(Device, device_id)
    if dev is None or dev.user_id != user.id:
        raise HTTPException(status_code=404, detail="设备不存在")
    dev.last_seen = datetime.now(timezone.utc).replace(tzinfo=None)
    db.commit()
    equipped = db.query(UserDial).filter_by(user_id=user.id, equipped=True).first()
    return {"ok": True, "energy": user.energy or 0,
            "equipped_dial_id": equipped.dial_id if equipped else None,
            "time": datetime.now(timezone.utc).replace(tzinfo=None).isoformat()}


# ---- 小游戏记分（只在吧唧上，敲吧唧记分） ----
@router.post("/game-scores")
def submit_score(body: GameScoreIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    if body.game not in BADGE_GAMES:
        raise HTTPException(status_code=400, detail=f"未知小游戏，可选：{'/'.join(BADGE_GAMES)}")
    row = GameScore(user_id=user.id, device_id=body.device_id, game=body.game, score=body.score)
    db.add(row)
    db.commit()
    db.refresh(row)
    return {"ok": True, "id": row.id, "game": body.game, "score": body.score}


@router.get("/game-scores")
def list_scores(game: str | None = None, user: User = Depends(get_current_user),
                db: Session = Depends(get_db)):
    q = db.query(GameScore).filter_by(user_id=user.id)
    if game:
        q = q.filter_by(game=game)
    rows = q.order_by(GameScore.score.desc()).limit(20).all()
    return [{"game": r.game, "score": r.score, "created_at": r.created_at.isoformat()} for r in rows]
