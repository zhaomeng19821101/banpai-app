"""一起种芽测试：浇水 1 次/天、累计 7 次开花、开花领限定装扮（奖励型）。"""
from datetime import date, timedelta

from app.db import SessionLocal
from app.models import Sprout


def _move_last_watered_back(user_id: int, days: int = 1):
    """把最近浇水日期往回拨 days 天（模拟第二天），只用于测试。"""
    db = SessionLocal()
    try:
        row = db.query(Sprout).filter_by(user_id=user_id).first()
        assert row is not None
        row.last_watered = date.today() - timedelta(days=days)
        db.commit()
    finally:
        db.close()


def _me_id(client) -> int:
    r = client.get("/api/v1/users/me")
    assert r.status_code == 200, r.text
    return r.json()["id"]


def test_sprout_status_starts_empty(auth_client):
    r = auth_client.get("/api/v1/sprout")
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["waters"] == 0
    assert body["waters_needed"] == 7
    assert body["watered_today"] is False
    assert body["bloomed"] is False
    assert body["claimed"] is False
    assert body["reward_outfit"] == "芽开·丁香"


def test_water_once_per_day(auth_client):
    r = auth_client.post("/api/v1/sprout/water")
    assert r.status_code == 200, r.text
    assert r.json()["waters"] == 1
    assert r.json()["watered_today"] is True
    r2 = auth_client.post("/api/v1/sprout/water")
    assert r2.status_code == 400  # 同一天不能浇两次


def test_claim_before_bloom_fails(auth_client):
    auth_client.post("/api/v1/sprout/water")
    r = auth_client.post("/api/v1/sprout/claim")
    assert r.status_code == 400  # 还没开花


def test_bloom_after_seven_waters_and_claim(auth_client):
    user_id = _me_id(auth_client)
    for i in range(7):
        r = auth_client.post("/api/v1/sprout/water")
        assert r.status_code == 200, r.text
        assert r.json()["waters"] == i + 1
        if i < 6:
            _move_last_watered_back(user_id)
    r = auth_client.get("/api/v1/sprout")
    assert r.json()["bloomed"] is True
    r = auth_client.post("/api/v1/sprout/claim")
    assert r.status_code == 200, r.text
    assert r.json()["outfit_name"] == "芽开·丁香"
    # 装扮页里已拥有
    r = auth_client.get("/api/v1/outfits")
    mine = [o for o in r.json() if o["name"] == "芽开·丁香"]
    assert len(mine) == 1 and mine[0]["owned"] is True
    # 再领一次幂等返回 ok，不重复发
    r = auth_client.post("/api/v1/sprout/claim")
    assert r.status_code == 200


def test_limited_outfit_not_for_sale(auth_client):
    r = auth_client.get("/api/v1/outfits")
    mine = [o for o in r.json() if o["name"] == "芽开·丁香"]
    assert len(mine) == 1
    r = auth_client.post(f"/api/v1/outfits/{mine[0]['id']}/unlock")
    assert r.status_code == 400  # 种芽限定不售卖
