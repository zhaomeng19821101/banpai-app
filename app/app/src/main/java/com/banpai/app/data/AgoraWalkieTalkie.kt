package com.banpai.app.data

import android.content.Context
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

/**
 * 好友对讲控件的声网封装：只给好友 1:1 实时对讲用。
 * 公共区域（派对房间 / 公共花园）不用声网实时语音（2026-11-20 用户锁定：合规风险）。
 * 频道名与 token 都由后端定（频道 banpai-friend-<小 id>-<大 id>），证书不下发客户端。
 * 对讲机模式：加入频道后默认静音，按住说话时才开麦，松开静音；离开即退出频道并销毁引擎。
 */
class AgoraWalkieTalkie(
    context: Context,
    private val appId: String,
    private val onEvent: (String) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private var engine: RtcEngine? = null

    /** 加入频道。返回 joinChannel 是否成功发出（进频道成功靠 onJoinChannelSuccess 回调）。 */
    fun start(token: String, channel: String, uid: Int): Boolean {
        if (appId.isEmpty()) {
            onEvent("未配置 Agora App ID，对讲不可用")
            return false
        }
        return try {
            val config = RtcEngineConfig().apply {
                mContext = appContext
                mAppId = appId
                mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                        onEvent("对讲中：按住说话，松开静音")
                    }

                    override fun onUserJoined(uid: Int, elapsed: Int) {
                        onEvent("对方加入了对讲")
                    }

                    override fun onUserOffline(uid: Int, reason: Int) {
                        onEvent("对方离开了对讲")
                    }

                    override fun onError(err: Int) {
                        onEvent("对讲出错：$err")
                    }
                }
            }
            engine = RtcEngine.create(config)
            engine?.setEnableSpeakerphone(true) // 对讲机：对方声音走扬声器
            engine?.muteLocalAudioStream(true) // 默认静音；按住说话时才开麦
            val options = ChannelMediaOptions().apply {
                autoSubscribeAudio = true
                publishMicrophoneTrack = true
            }
            (engine?.joinChannel(token, channel, uid, options) ?: -1) == 0
        } catch (e: Exception) {
            onEvent("对讲启动失败：${e.message}")
            false
        }
    }

    /** 按住说话：开麦；松开：静音。 */
    fun setTalking(talking: Boolean) {
        try {
            engine?.muteLocalAudioStream(!talking)
        } catch (_: Exception) {
        }
    }

    /** 离开即退出频道并销毁引擎。 */
    fun stop() {
        try {
            engine?.leaveChannel()
        } catch (_: Exception) {
        }
        try {
            RtcEngine.destroy()
        } catch (_: Exception) {
        }
        engine = null
    }
}
