"""半拍 Banpai 后端 — FastAPI 入口。"""
from fastapi import FastAPI

from app.db import init_db
from app.routers import assets, auth, ble, chat, checkin, diary, exchange, friends, garden, outfits, party, sprout, users

app = FastAPI(title="半拍 Banpai API", version="3.0.0-fresh-build",
              description="半拍新工程后端（从头重建，不沿用旧 67 次提交代码）")


@app.on_event("startup")
def startup():
    init_db()


@app.get("/api/v1/health")
def health():
    return {"ok": True, "status": "ok", "service": "banpai-backend", "version": "3.0.0-fresh-build"}


app.include_router(auth.router)
app.include_router(users.router)
app.include_router(checkin.router)
app.include_router(ble.router)
app.include_router(outfits.router)
app.include_router(chat.router)
app.include_router(diary.router)
app.include_router(party.router)
app.include_router(sprout.router)
app.include_router(friends.router)
app.include_router(garden.router)
app.include_router(exchange.router)
app.include_router(assets.router)
