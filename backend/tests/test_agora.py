"""声网 token：好友对讲控件取 token + 派对房间 token（占位 / 真实两种情况）。

占位：未配置 AGORA_APP_ID / 证书时返回 placeholder token，客户端不要真连。
真实：配置 32 位 dummy 证书后，TokenBuilder 签发的 RTC token 为 006 标准格式
（006 + App ID + 签名体，与旧 67 提交版的约定一致）。
"""
import uuid

from app.db import SessionLocal
from app.models import Friend


def _register(client, nickname=None):
    phone = "139" + uuid.uuid4().hex[:8]
    r = client.post("/api/v1/auth/register",
                    json={"phone": phone, "password": "password123",
                          "nickname": nickname or f"用户{phone[-4:]}"})
    assert r.status_code == 200, r.text
    return r.json()


def _add_friend(a_id, b_id):
    db = SessionLocal()
    try:
        db.add(Friend(user_id=a_id, friend_id=b_id, status="accepted"))
        db.commit()
    finally:
        db.close()


def _make_room(auth_client):
    r = auth_client.post("/api/v1/party/rooms", json={"title": "测试房"})
    assert r.status_code == 200, r.text
    return r.json()["id"]


def test_party_token_placeholder_when_unconfigured(auth_client):
    room_id = _make_room(auth_client)
    r = auth_client.get(f"/api/v1/party/rooms/{room_id}/agora-token")
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["placeholder"] is True
    assert body["token"].startswith("placeholder-")
    assert body["channel"] == f"banpai-room-{room_id}"


def test_party_token_real_when_configured(auth_client, monkeypatch):
    monkeypatch.setenv("AGORA_APP_ID", "a" * 32)
    monkeypatch.setenv("AGORA_APP_CERTIFICATE", "b" * 32)
    room_id = _make_room(auth_client)
    r = auth_client.get(f"/api/v1/party/rooms/{room_id}/agora-token")
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["placeholder"] is False
    assert body["token"].startswith("006" + "a" * 32)
    assert len(body["token"]) > 100
    assert body["app_id"] == "a" * 32


def test_friend_space_token_requires_friendship(auth_client, client):
    other = _register(client, "路人")
    r = auth_client.get(f"/api/v1/friends/{other['user_id']}/space-token")
    assert r.status_code == 403


def test_friend_space_token_placeholder(auth_client, client):
    me = auth_client.get("/api/v1/users/me").json()
    other = _register(client, "好友B")
    _add_friend(me["id"], other["user_id"])
    r = auth_client.get(f"/api/v1/friends/{other['user_id']}/space-token")
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["placeholder"] is True
    lo, hi = sorted((me["id"], other["user_id"]))
    assert body["channel"] == f"banpai-friend-{lo}-{hi}"
    assert body["uid"] == me["id"]
    assert body["token"].startswith("placeholder-")


def test_friend_space_token_real_when_configured(auth_client, client, monkeypatch):
    monkeypatch.setenv("AGORA_APP_ID", "a" * 32)
    monkeypatch.setenv("AGORA_APP_CERTIFICATE", "b" * 32)
    me = auth_client.get("/api/v1/users/me").json()
    other = _register(client, "好友C")
    _add_friend(me["id"], other["user_id"])
    r = auth_client.get(f"/api/v1/friends/{other['user_id']}/space-token")
    assert r.status_code == 200, r.text
    body = r.json()
    assert body["placeholder"] is False
    assert body["token"].startswith("006" + "a" * 32)
    assert len(body["token"]) > 100
    assert body["channel"].startswith("banpai-friend-")
