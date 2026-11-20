"""半拍 Banpai 后端 — 数据库连接与会话。"""
import os

from sqlalchemy import create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

DATABASE_URL = os.environ.get("DATABASE_URL", "sqlite:///./banpai.db")

connect_args = {}
if DATABASE_URL.startswith("sqlite"):
    # SQLite 需要 check_same_thread=False 才能配合 FastAPI 使用
    connect_args = {"check_same_thread": False}

engine = create_engine(DATABASE_URL, connect_args=connect_args)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def init_db():
    """建表（开发用；生产请用 Alembic 迁移）。"""
    from app import models  # noqa: F401  确保模型被注册到 Base

    Base.metadata.create_all(bind=engine)
