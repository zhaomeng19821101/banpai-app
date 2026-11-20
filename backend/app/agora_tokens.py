"""声网 RTC token 签发（服务端专用，证书绝不下发客户端）。

只给好友 1:1 实时对讲（好友对讲控件）用；公共区域（派对房间 / 公共花园）
不用声网实时语音（2026-11-20 用户锁定：合规风险）。
未配置 AGORA_APP_ID / AGORA_APP_CERTIFICATE 时返回 None，调用方回占位并写明原因。
"""
import os
import time

from agora_token_builder import RtcTokenBuilder

# agora-token-builder 1.0.0 的 RtcTokenBuilder 不导出角色常量；
# 按其 docstring：Role_Publisher = 1（可发音频），Role_Subscriber = 2（只听）。
ROLE_PUBLISHER = 1
TOKEN_TTL_SECONDS = 3600


def build_rtc_token(channel: str, uid: int, expire_seconds: int = TOKEN_TTL_SECONDS):
    """用证书签发真实 RTC token；未配置时返回 None。"""
    app_id = os.environ.get("AGORA_APP_ID", "")
    cert = os.environ.get("AGORA_APP_CERTIFICATE", "")
    if not app_id or not cert:
        return None
    expire_ts = int(time.time()) + expire_seconds
    token = RtcTokenBuilder.buildTokenWithUid(
        app_id, cert, channel, uid, ROLE_PUBLISHER, expire_ts)
    return {"app_id": app_id, "token": token, "channel": channel,
            "uid": uid, "expires_in": expire_seconds, "placeholder": False}


def friend_channel(my_id: int, friend_id: int) -> str:
    """好友对讲频道：双方 id 排序拼出来，两边算出同一个频道名。"""
    a, b = sorted((my_id, friend_id))
    return f"banpai-friend-{a}-{b}"
