"""半拍 Banpai 后端 — Pydantic 请求/响应模型。"""
from datetime import date, datetime
from typing import Optional

from pydantic import BaseModel, Field


# ---- 通用 ----
class Ok(BaseModel):
    ok: bool = True
    detail: str = ""


class TokenOut(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user_id: int
    nickname: str


# ---- 认证 ----
class RegisterIn(BaseModel):
    phone: str = Field(min_length=6, max_length=20)
    password: str = Field(min_length=6, max_length=64)
    nickname: str = "半拍的朋友"


class LoginIn(BaseModel):
    phone: str
    password: str


class SmsSendIn(BaseModel):
    phone: str


class SmsLoginIn(BaseModel):
    phone: str
    code: str


class WechatLoginIn(BaseModel):
    code: str  # 微信授权 code（demo 模拟授权）


# ---- 用户 ----
class UserOut(BaseModel):
    id: int
    phone: str
    nickname: str
    avatar_url: str = ""
    level: int
    energy: int

    model_config = {"from_attributes": True}


class UserPatchIn(BaseModel):
    nickname: Optional[str] = None
    avatar_url: Optional[str] = None


class EnergyOut(BaseModel):
    energy: int
    level: int
    ledger: list = []


# ---- 打卡 ----
class CheckinIn(BaseModel):
    point_id: int
    lat: Optional[float] = None
    lng: Optional[float] = None
    photo_url: str = ""


class CheckinOut(BaseModel):
    id: int
    point_id: int
    day: date
    energy_gained: int
    first_time: bool


class CameoIn(BaseModel):
    photo_url: str = ""


class PointOut(BaseModel):
    id: int
    name: str
    lat: float
    lng: float
    guardian_id: Optional[int] = None
    visited: bool = False


class GuardianOut(BaseModel):
    id: int
    name: str
    location: str
    pet: str
    sound: str
    line: str
    chapter: int
    unlocked: bool = False

    model_config = {"from_attributes": True}


class RouteOut(BaseModel):
    from_point_id: int
    to_point_id: int
    distance_m: int
    walk_minutes: int
    polyline: list = []
    source: str = "straight-line"  # 有 AMAP_KEY 时为 amap


# ---- 表盘 / 装扮 ----
class DialOut(BaseModel):
    id: int
    name: str
    style: str
    price: int
    owned: bool = False
    equipped: bool = False

    model_config = {"from_attributes": True}


class OutfitOut(BaseModel):
    id: int
    name: str
    slot: str
    price: int
    owned: bool = False
    equipped: bool = False

    model_config = {"from_attributes": True}


# ---- 设备 ----
class DeviceIn(BaseModel):
    serial: str
    name: str = "吧唧"


class DeviceOut(BaseModel):
    id: int
    serial: str
    name: str
    firmware_version: str = ""
    battery: int
    user_id: Optional[int] = None

    model_config = {"from_attributes": True}


class DeviceEventIn(BaseModel):
    kind: str
    payload: str = ""


class GameScoreIn(BaseModel):
    game: str  # 敲钟 / 凿冰 / 咬冰棍
    score: int
    device_id: Optional[int] = None


# ---- 花语 ----
class ChatIn(BaseModel):
    text: str
    lat: Optional[float] = None
    lng: Optional[float] = None


class ChatOut(BaseModel):
    reply: str
    source: str  # small_model / local / out_of_scope
    line_no: Optional[int] = None


class FoodOut(BaseModel):
    name: str
    place: str
    is_hidden: bool


# ---- 日记 ----
class DiaryOut(BaseModel):
    id: int
    day: date
    text: str

    model_config = {"from_attributes": True}


class DiaryGenerateIn(BaseModel):
    day: Optional[date] = None
    mood: str = ""


# ---- 派对 ----
class RoomIn(BaseModel):
    title: str
    topic: str = ""
    lat: Optional[float] = None
    lng: Optional[float] = None
    max_members: int = 20


class RoomOut(BaseModel):
    id: int
    owner_id: int
    title: str
    topic: str = ""
    lat: Optional[float] = None
    lng: Optional[float] = None
    members: int = 0
    is_live: bool = True

    model_config = {"from_attributes": True}


class RoomMessageIn(BaseModel):
    text: str


class RoomMessageOut(BaseModel):
    id: int
    room_id: int
    user_id: int
    nickname: str = ""
    text: str
    created_at: datetime

    model_config = {"from_attributes": True}


# ---- 好友 ----
class FriendMessageIn(BaseModel):
    text: str = ""
    voice_url: str = ""
    duration_sec: int = 0


class FriendMessageOut(BaseModel):
    id: int
    sender_id: int
    receiver_id: int
    text: str
    voice_url: str = ""
    duration_sec: int = 0
    is_read: bool
    created_at: datetime

    model_config = {"from_attributes": True}


# ---- 花园 / 雪夜传信 ----
class PostIn(BaseModel):
    text: str
    image_url: str = ""


class PostOut(BaseModel):
    id: int
    user_id: int
    nickname: str = ""
    text: str
    image_url: str = ""
    likes: int = 0
    comments: int = 0
    created_at: datetime

    model_config = {"from_attributes": True}


class CommentIn(BaseModel):
    text: str


class LetterIn(BaseModel):
    text: str
    lat: float
    lng: float


class LetterOut(BaseModel):
    id: int
    sender_id: int
    lat: float
    lng: float
    text: str
    expires_at: datetime
    picked: bool = False

    model_config = {"from_attributes": True}


# ---- 兑换 ----
class RedeemCodeIn(BaseModel):
    code: str


class MonthCardOut(BaseModel):
    active: bool
    ends_at: Optional[datetime] = None
