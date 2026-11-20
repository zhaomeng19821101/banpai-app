"""半拍 Banpai 后端 — 数据模型（SQLAlchemy）。"""
from datetime import datetime, timezone

from sqlalchemy import (
    Boolean,
    Column,
    Date,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import relationship

from app.db import Base


def _now():
    # naive UTC：SQLite 往返会丢 tzinfo，全库统一用 naive UTC，避免 aware/naive 混比报错
    return datetime.now(timezone.utc).replace(tzinfo=None)


class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True)
    phone = Column(String(20), unique=True, nullable=False)
    nickname = Column(String(50), default="半拍的朋友")
    password_hash = Column(String(200), nullable=False)
    avatar_url = Column(String(500), default="")
    level = Column(Integer, default=1)
    energy = Column(Integer, default=0)  # 缓存总能量；以 energy_ledger 为准
    created_at = Column(DateTime, default=_now)


class Checkin(Base):
    __tablename__ = "checkins"
    __table_args__ = (UniqueConstraint("user_id", "point_id", "day", name="uq_checkin_user_point_day"),)
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    point_id = Column(Integer, ForeignKey("checkpoints.id"), nullable=False)
    day = Column(Date, nullable=False)  # 打卡日期（每点每天 1 次）
    lat = Column(Float)
    lng = Column(Float)
    photo_url = Column(String(500), default="")
    created_at = Column(DateTime, default=_now)


class EnergyLedger(Base):
    __tablename__ = "energy_ledger"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    amount = Column(Integer, nullable=False)  # 正为获得，负为消耗
    kind = Column(String(50), nullable=False)  # checkin_first / checkin_revisit / photo / cameo / postcard / redeem / unlock
    ref = Column(String(200), default="")
    balance_after = Column(Integer, nullable=False)
    created_at = Column(DateTime, default=_now)


class Guardian(Base):
    __tablename__ = "guardians"
    id = Column(Integer, primary_key=True)
    name = Column(String(100), unique=True, nullable=False)  # 如 中央大街·手风琴
    location = Column(String(100), nullable=False)
    pet = Column(String(50), nullable=False)
    sound = Column(String(100), default="")  # 动物发音/可爱叫声，不说中文台词
    line = Column(String(200), default="")  # 卡片文字
    chapter = Column(Integer, default=1)
    created_at = Column(DateTime, default=_now)


class Checkpoint(Base):
    __tablename__ = "checkpoints"
    id = Column(Integer, primary_key=True)
    name = Column(String(100), unique=True, nullable=False)
    lat = Column(Float, nullable=False)
    lng = Column(Float, nullable=False)
    guardian_id = Column(Integer, ForeignKey("guardians.id"))
    description = Column(String(300), default="")


class Dial(Base):
    __tablename__ = "dials"
    id = Column(Integer, primary_key=True)
    name = Column(String(100), unique=True, nullable=False)
    style = Column(String(50), default="")
    price = Column(Integer, default=0)  # 能量价格，0 为免费
    is_hidden = Column(Boolean, default=False)


class UserDial(Base):
    __tablename__ = "user_dials"
    __table_args__ = (UniqueConstraint("user_id", "dial_id", name="uq_user_dial"),)
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    dial_id = Column(Integer, ForeignKey("dials.id"), nullable=False)
    equipped = Column(Boolean, default=False)
    created_at = Column(DateTime, default=_now)


class Outfit(Base):
    __tablename__ = "outfits"
    id = Column(Integer, primary_key=True)
    name = Column(String(100), unique=True, nullable=False)
    slot = Column(String(20), nullable=False)  # 只 3 槽：服装/套装、背景、特效
    price = Column(Integer, default=0)


class UserOutfit(Base):
    __tablename__ = "user_outfits"
    __table_args__ = (UniqueConstraint("user_id", "outfit_id", name="uq_user_outfit"),)
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    outfit_id = Column(Integer, ForeignKey("outfits.id"), nullable=False)
    equipped = Column(Boolean, default=False)
    created_at = Column(DateTime, default=_now)


class Sprout(Base):
    """一起种芽：一人一颗芽；每天浇水 1 次，累计 7 次开花；
    开花后领限定装扮「芽开·丁香」（奖励型，2026-11-20 Jackson 定）。
    奖励不是守护宠（守护宠只靠打卡解锁），也不是表盘（金瓣 24 号只认第五瓣信件）。"""
    __tablename__ = "sprouts"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, unique=True)
    waters = Column(Integer, default=0)  # 累计浇水次数（不要求连续）
    last_watered = Column(Date)  # 最近一次浇水日期；每天只能浇 1 次
    bloomed = Column(Boolean, default=False)
    claimed = Column(Boolean, default=False)  # 限定装扮是否已领取
    created_at = Column(DateTime, default=_now)


class Device(Base):
    __tablename__ = "devices"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    serial = Column(String(100), unique=True, nullable=False)
    name = Column(String(100), default="吧唧")
    firmware_version = Column(String(50), default="")
    battery = Column(Integer, default=100)
    last_seen = Column(DateTime)
    created_at = Column(DateTime, default=_now)


class DeviceEvent(Base):
    __tablename__ = "device_events"
    id = Column(Integer, primary_key=True)
    device_id = Column(Integer, ForeignKey("devices.id"), nullable=False)
    kind = Column(String(50), nullable=False)  # TAP / STATE / OTA / SYNC ...
    payload = Column(Text, default="")
    created_at = Column(DateTime, default=_now)


class GameScore(Base):
    __tablename__ = "game_scores"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    device_id = Column(Integer, ForeignKey("devices.id"))
    game = Column(String(20), nullable=False)  # 敲钟 / 凿冰 / 咬冰棍（只在吧唧上）
    score = Column(Integer, nullable=False)
    created_at = Column(DateTime, default=_now)


class Diary(Base):
    __tablename__ = "diaries"
    __table_args__ = (UniqueConstraint("user_id", "day", name="uq_diary_user_day"),)
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    day = Column(Date, nullable=False)
    text = Column(Text, nullable=False)  # 纯文字，不读出声
    created_at = Column(DateTime, default=_now)


class Room(Base):
    __tablename__ = "rooms"
    id = Column(Integer, primary_key=True)
    owner_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    title = Column(String(100), nullable=False)
    topic = Column(String(200), default="")
    lat = Column(Float)
    lng = Column(Float)
    max_members = Column(Integer, default=20)
    is_live = Column(Boolean, default=True)
    created_at = Column(DateTime, default=_now)


class RoomMember(Base):
    __tablename__ = "room_members"
    __table_args__ = (UniqueConstraint("room_id", "user_id", name="uq_room_member"),)
    id = Column(Integer, primary_key=True)
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    role = Column(String(20), default="listener")  # owner / speaker / listener
    muted = Column(Boolean, default=False)
    hand_raised = Column(Boolean, default=False)
    joined_at = Column(DateTime, default=_now)


class RoomMessage(Base):
    __tablename__ = "room_messages"
    id = Column(Integer, primary_key=True)
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    text = Column(Text, nullable=False)
    created_at = Column(DateTime, default=_now)


class Friend(Base):
    __tablename__ = "friends"
    __table_args__ = (UniqueConstraint("user_id", "friend_id", name="uq_friend"),)
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    friend_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    status = Column(String(20), default="accepted")
    created_at = Column(DateTime, default=_now)


class FriendMessage(Base):
    __tablename__ = "friend_messages"
    id = Column(Integer, primary_key=True)
    sender_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    receiver_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    text = Column(Text, default="")
    voice_url = Column(String(500), default="")  # 语音留言文件地址（异步，不走 Agora 实时）
    duration_sec = Column(Integer, default=0)
    is_read = Column(Boolean, default=False)
    created_at = Column(DateTime, default=_now)


class GardenPost(Base):
    __tablename__ = "garden_posts"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    text = Column(Text, nullable=False)
    image_url = Column(String(500), default="")
    created_at = Column(DateTime, default=_now)


class GardenComment(Base):
    __tablename__ = "garden_comments"
    id = Column(Integer, primary_key=True)
    post_id = Column(Integer, ForeignKey("garden_posts.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    text = Column(Text, nullable=False)
    created_at = Column(DateTime, default=_now)


class GardenLike(Base):
    __tablename__ = "garden_likes"
    __table_args__ = (UniqueConstraint("post_id", "user_id", name="uq_garden_like"),)
    id = Column(Integer, primary_key=True)
    post_id = Column(Integer, ForeignKey("garden_posts.id"), nullable=False)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    created_at = Column(DateTime, default=_now)


class Letter(Base):
    __tablename__ = "letters"
    id = Column(Integer, primary_key=True)
    sender_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    picker_id = Column(Integer, ForeignKey("users.id"))
    lat = Column(Float, nullable=False)
    lng = Column(Float, nullable=False)
    text = Column(Text, nullable=False)
    created_at = Column(DateTime, default=_now)
    expires_at = Column(DateTime, nullable=False)  # 24h 后化掉
    picked_at = Column(DateTime)


class ExchangeCode(Base):
    __tablename__ = "exchange_codes"
    id = Column(Integer, primary_key=True)
    code = Column(String(100), unique=True, nullable=False)
    reward_energy = Column(Integer, default=0)
    redeemed_by = Column(Integer, ForeignKey("users.id"))
    redeemed_at = Column(DateTime)


class MonthCard(Base):
    __tablename__ = "month_cards"
    id = Column(Integer, primary_key=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    started_at = Column(DateTime, default=_now)
    ends_at = Column(DateTime, nullable=False)
    active = Column(Boolean, default=True)
