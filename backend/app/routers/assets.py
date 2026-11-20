"""素材分发：占位实现，不复制大文件。

素材源在 ~/workspace/banpai/assets/（本仓库外），本服务只提供清单与
使用说明，不直接托管 518 MB 大文件。生产部署时把 ASSETS_DIR 指到
对象存储或 CDN，再由本接口下发签名 URL。
"""
import os

from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse

from app.models import User
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/assets", tags=["assets"])

ASSETS_DIR = os.environ.get("ASSETS_DIR", "")

MANIFEST = {
    "version": "v3-fresh-build",
    "kinds": {
        "guardians": {"count": 24, "detail": "守护宠卡片与视频"},
        "green-screen": {"count": 33, "detail": "绿幕视频（上半身/下半身）"},
        "dials": {"count": 30, "detail": "表盘图片"},
        "outfits": {"count": 16, "detail": "装扮图片"},
        "frames": {"count": 40, "detail": "相框：20 横 + 20 竖"},
        "welcome": {"count": 1, "detail": "欢迎视频"},
    },
    "source_note": "素材源目录 ~/workspace/banpai/assets/（本仓库不复制大文件）",
}


@router.get("/manifest")
def manifest(user: User = Depends(get_current_user)):
    return MANIFEST


@router.get("/usage")
def usage(user: User = Depends(get_current_user)):
    return {"assets_dir_configured": bool(ASSETS_DIR),
            "assets_dir": ASSETS_DIR or "未配置（ASSETS_DIR）",
            "detail": "占位：素材不由本服务直接托管，需配置对象存储/CDN"}


@router.get("/{kind}/{name}")
def asset(kind: str, name: str, user: User = Depends(get_current_user)):
    """按种类+文件名取素材。未配置 ASSETS_DIR 时返回占位说明，不 404 误导。"""
    if kind not in MANIFEST["kinds"]:
        return JSONResponse(status_code=404, content={"detail": f"未知素材种类：{kind}"})
    if not ASSETS_DIR:
        return {"kind": kind, "name": name, "status": "placeholder",
                "detail": "素材未配置：请设置 ASSETS_DIR 或接入对象存储/CDN 后再分发"}
    path = os.path.join(ASSETS_DIR, kind, name)
    if not os.path.exists(path):
        return JSONResponse(status_code=404, content={"detail": "素材不存在"})
    return {"kind": kind, "name": name, "status": "ready", "path": path}
