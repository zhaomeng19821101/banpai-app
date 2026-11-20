"""认证：注册 / 登录 / 短信（demo）/ 微信（demo 模拟授权）。"""
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db import get_db
from app.models import User
from app.schemas import LoginIn, RegisterIn, SmsLoginIn, SmsSendIn, TokenOut, WechatLoginIn
from app.security import create_access_token, hash_password, verify_password

router = APIRouter(prefix="/api/v1/auth", tags=["auth"])

# demo 短信码：演示环境固定 123456，不发真实短信
DEMO_SMS_CODE = "123456"


def _token(user: User) -> TokenOut:
    return TokenOut(access_token=create_access_token(user.id), user_id=user.id, nickname=user.nickname)


@router.post("/register", response_model=TokenOut)
def register(body: RegisterIn, db: Session = Depends(get_db)):
    if db.query(User).filter_by(phone=body.phone).first():
        raise HTTPException(status_code=400, detail="手机号已注册")
    user = User(phone=body.phone, nickname=body.nickname, password_hash=hash_password(body.password))
    db.add(user)
    db.commit()
    db.refresh(user)
    return _token(user)


@router.post("/login", response_model=TokenOut)
def login(body: LoginIn, db: Session = Depends(get_db)):
    user = db.query(User).filter_by(phone=body.phone).first()
    if user is None or not verify_password(body.password, user.password_hash):
        raise HTTPException(status_code=401, detail="手机号或密码错误")
    return _token(user)


@router.post("/sms/send")
def sms_send(body: SmsSendIn):
    # demo：不发真实短信，固定验证码仅用于演示联调
    return {"ok": True, "detail": "验证码已发送（demo）", "demo_code": DEMO_SMS_CODE}


@router.post("/sms/login", response_model=TokenOut)
def sms_login(body: SmsLoginIn, db: Session = Depends(get_db)):
    if body.code != DEMO_SMS_CODE:
        raise HTTPException(status_code=401, detail="验证码错误")
    user = db.query(User).filter_by(phone=body.phone).first()
    if user is None:  # 未注册号码自动注册
        user = User(phone=body.phone, nickname="半拍的朋友", password_hash=hash_password(DEMO_SMS_CODE))
        db.add(user)
        db.commit()
        db.refresh(user)
    return _token(user)


@router.post("/sms/wechat", response_model=TokenOut)
def wechat_login(body: WechatLoginIn, db: Session = Depends(get_db)):
    # demo 模拟授权：用微信 code 派生一个演示账号，不调真实微信接口
    phone = f"wx-{body.code[:12]}"
    user = db.query(User).filter_by(phone=phone).first()
    if user is None:
        user = User(phone=phone, nickname="微信用户", password_hash=hash_password(body.code))
        db.add(user)
        db.commit()
        db.refresh(user)
    return _token(user)
