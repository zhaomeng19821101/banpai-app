"""pytest 配置：用独立临时 SQLite 库跑测试，不碰开发库。"""
import os
import tempfile

import pytest

TMP = tempfile.NamedTemporaryFile(suffix=".db", delete=False)
TMP.close()
os.environ["DATABASE_URL"] = f"sqlite:///{TMP.name}"
os.environ["JWT_SECRET"] = "test-secret"

from fastapi.testclient import TestClient  # noqa: E402

from app.db import init_db  # noqa: E402
from app.main import app  # noqa: E402
from app.seed import seed  # noqa: E402


@pytest.fixture(scope="session", autouse=True)
def _seed():
    init_db()
    seed()
    yield


@pytest.fixture()
def client():
    return TestClient(app)


@pytest.fixture()
def auth_client(client):
    """注册一个全新用户并返回带 token 的 client 与用户信息。"""
    import uuid

    phone = "139" + uuid.uuid4().hex[:8]
    r = client.post("/api/v1/auth/register",
                    json={"phone": phone, "password": "password123", "nickname": "测试用户"})
    assert r.status_code == 200, r.text
    token = r.json()["access_token"]
    client.headers.update({"Authorization": f"Bearer {token}"})
    return client


@pytest.fixture()
def demo_client(client):
    """演示用户 Larry（种子数据：能量 1280，日记 19 篇）。"""
    r = client.post("/api/v1/auth/login",
                    json={"phone": "13800000000", "password": "demo1234"})
    assert r.status_code == 200, r.text
    client.headers.update({"Authorization": f"Bearer {r.json()['access_token']}"})
    return client
