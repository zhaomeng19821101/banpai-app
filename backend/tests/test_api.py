"""半拍后端测试：健康、认证、打卡幂等、能量账本、日记种子、花语口径等。"""
from app.constants import LOCKED_LINES


def test_health(client):
    r = client.get("/api/v1/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"


def test_register_and_login(client):
    r = client.post("/api/v1/auth/register",
                    json={"phone": "13900001111", "password": "password123", "nickname": "小测"})
    assert r.status_code == 200
    assert r.json()["access_token"]
    r2 = client.post("/api/v1/auth/login",
                     json={"phone": "13900001111", "password": "password123"})
    assert r2.status_code == 200
    assert r2.json()["nickname"] == "小测"


def test_auth_required(client):
    r = client.get("/api/v1/users/me")
    assert r.status_code == 401


def test_checkin_first_time_gives_80(auth_client):
    r = auth_client.post("/api/v1/checkin", json={"point_id": 1})
    assert r.status_code == 200, r.text
    assert r.json()["energy_gained"] == 80
    assert r.json()["first_time"] is True


def test_checkin_idempotent_same_day(auth_client):
    auth_client.post("/api/v1/checkin", json={"point_id": 2})
    r = auth_client.post("/api/v1/checkin", json={"point_id": 2})
    assert r.status_code == 200
    assert r.json()["energy_gained"] == 0  # 同点同天不重复加能量


def test_energy_ledger_records_checkin(auth_client):
    auth_client.post("/api/v1/checkin", json={"point_id": 3})
    r = auth_client.get("/api/v1/users/me/energy")
    assert r.status_code == 200
    kinds = [row["kind"] for row in r.json()["ledger"]]
    assert "checkin_first" in kinds
    assert r.json()["energy"] >= 80


def test_diary_seed_19_entries(demo_client):
    r = demo_client.get("/api/v1/diary")
    assert r.status_code == 200
    diaries = r.json()
    assert len(diaries) == 19
    days = sorted(d["day"] for d in diaries)
    assert days[0] == "2026-11-01"
    assert days[-1] == "2026-11-19"
    for d in diaries:
        assert d["text"].rstrip().endswith(LOCKED_LINES[14])  # 每篇结尾锁定台词 #14


def test_chat_out_of_scope_returns_daqing(demo_client):
    r = demo_client.post("/api/v1/chat/messages", json={"text": "帮我写个 Python 排序算法"})
    assert r.status_code == 200
    assert r.json()["reply"] == "我还小，帮你去打听"
    assert r.json()["source"] == "out_of_scope"


def test_chat_in_scope_not_out_of_scope(demo_client):
    r = demo_client.post("/api/v1/chat/messages", json={"text": "你好呀"})
    assert r.status_code == 200
    assert r.json()["reply"] != "我还小，帮你去打听"


def test_dials_list_30(demo_client):
    r = demo_client.get("/api/v1/ble/dials")
    assert r.status_code == 200
    assert len(r.json()) == 30


def test_outfits_only_three_slots(demo_client):
    r = demo_client.get("/api/v1/outfits")
    assert r.status_code == 200
    assert len(r.json()) == 17  # 16 套 + 1 种芽限定「芽开·丁香」
    slots = {o["slot"] for o in r.json()}
    assert slots == {"服装/套装", "背景", "特效"}  # 配饰已去掉
    limited = [o for o in r.json() if o["name"] == "芽开·丁香"]
    assert len(limited) == 1 and limited[0]["price"] == -1  # 不售卖，只能开花领


def test_guardians_24(demo_client):
    r = demo_client.get("/api/v1/checkin/guardians")
    assert r.status_code == 200
    assert len(r.json()) == 24


def test_cameo_reward_once_per_day(auth_client):
    r1 = auth_client.post("/api/v1/checkin/cameo-reward", json={})
    assert r1.status_code == 200
    assert r1.json()["energy_gained"] == 5
    r2 = auth_client.post("/api/v1/checkin/cameo-reward", json={})
    assert r2.json()["energy_gained"] == 0


def test_letter_pick_only_once(auth_client):
    r = auth_client.post("/api/v1/garden/letters",
                         json={"text": "雪夜的一封信", "lat": 45.7712, "lng": 126.6175})
    assert r.status_code == 200
    letter_id = r.json()["id"]
    r1 = auth_client.post(f"/api/v1/garden/letters/{letter_id}/pick")
    assert r1.status_code == 200
    assert r1.json()["picked"] is True
    r2 = auth_client.post(f"/api/v1/garden/letters/{letter_id}/pick")
    assert r2.status_code == 409  # 一封只被一人捡
