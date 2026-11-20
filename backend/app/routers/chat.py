"""花语：小模型 + 联网打听，不接大模型兜底。

范围：吃 / 住 / 行 / 心情 / 感受 + 哈尔滨建筑历史。
范围外或不知道：一律回「我还小，帮你去打听」，不编造。
"""
import os
import re

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.constants import CHAT_DAILY_LIMIT, FOODS, LOCKED_LINES, OUT_OF_SCOPE_REPLY
from app.db import get_db
from app.models import User
from app.schemas import ChatIn, ChatOut, FoodOut
from app.security import get_current_user

router = APIRouter(prefix="/api/v1/chat", tags=["chat"])

# 范围关键词（吃/住/行/心情/感受/哈尔滨建筑历史）
IN_SCOPE_PATTERNS = [
    r"吃|喝|美食|餐厅|小吃|锅包肉|红肠|冰棍|好吃",
    r"住|酒店|民宿|宾馆|睡觉|住宿",
    r"去|怎么走|路线|地铁|公交|打车|交通|在哪|位置|怎么去|导航",
    r"心情|开心|难过|累|想哭|孤单|害怕|紧张|感受|感觉|委屈|想家",
    r"索菲亚|中央大街|教堂|建筑|历史|老房子|巴洛克|拜占庭|大剧院|音乐厅|老会堂|铁路桥",
    r"你好|在吗|半拍|你是谁|你叫什么",
    r"天气|冷|雪|下雪|温度",
    r"玩|好玩|景点|打卡|去哪|推荐",
]

LOCAL_ANSWERS = [
    (r"你好|在吗|半拍|你是谁", LOCKED_LINES[5], 5),
    (r"吃|美食|好吃|锅包肉|红肠", "中央大街的马迭尔冰棍和老道外的锅包肉，都值得慢慢吃。想看完整的好吃团，问我“好吃团”就好呀。", None),
    (r"索菲亚|教堂|建筑|历史", "索菲亚教堂是哈尔滨的地标，红砖绿顶，拜占庭风格，1907 年建的木教堂后来改成了砖石的。站在教堂前面听钟声，时间会慢下来。", None),
    (r"冷|雪|天气", LOCKED_LINES[10], 10),
    (r"心情|难过|累|孤单", "嗯，我在听呀，慢慢说。难过的时候，就来江边吹吹风，我陪你。", 1),
    (r"怎么走|路线|在哪|怎么去", "告诉我你要去哪，我帮你打听路线。中央大街一带步行最好，慢慢走，风景都在路上。", None),
]


def _in_scope(text: str) -> bool:
    return any(re.search(p, text) for p in IN_SCOPE_PATTERNS)


def _local_answer(text: str):
    for pattern, reply, line_no in LOCAL_ANSWERS:
        if re.search(pattern, text):
            return reply, line_no
    return None, None


@router.get("/messages")
def list_messages(user: User = Depends(get_current_user)):
    # 演示：聊天记录暂不落库，返回空列表（后续接消息表）
    return {"messages": [], "detail": "聊天记录暂未持久化"}


@router.post("/messages", response_model=ChatOut)
def chat(body: ChatIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    text = body.text.strip()
    if not _in_scope(text):
        return ChatOut(reply=OUT_OF_SCOPE_REPLY, source="out_of_scope")
    # 1) 本地锁定台词 / 常识回答
    reply, line_no = _local_answer(text)
    if reply:
        return ChatOut(reply=reply, source="local", line_no=line_no)
    # 2) 小模型服务（只接小模型；未配置或调用失败 → 回打听口径，不接大模型兜底）
    url = os.environ.get("SMALL_MODEL_URL", "")
    if url:
        try:
            import httpx

            resp = httpx.post(
                url.rstrip("/") + "/chat",
                json={"text": text, "user_id": user.id},
                headers={"Authorization": f"Bearer {os.environ.get('SMALL_MODEL_API_KEY', '')}"},
                timeout=10.0,
            )
            if resp.status_code == 200:
                data = resp.json()
                return ChatOut(reply=data.get("reply", OUT_OF_SCOPE_REPLY), source="small_model")
        except Exception:
            pass
    return ChatOut(reply=OUT_OF_SCOPE_REPLY, source="out_of_scope")


@router.get("/foods", response_model=list[FoodOut])
def foods(user: User = Depends(get_current_user)):
    return [FoodOut(**f) for f in FOODS]


@router.get("/limit")
def limit(user: User = Depends(get_current_user)):
    return {"daily_limit": CHAT_DAILY_LIMIT, "used_today": 0,
            "small_model_configured": bool(os.environ.get("SMALL_MODEL_URL")),
            "detail": "花语只接小模型 + 联网打听，不接大模型兜底"}
