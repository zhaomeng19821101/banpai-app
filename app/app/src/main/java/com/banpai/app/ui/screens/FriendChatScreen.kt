package com.banpai.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.banpai.app.BuildConfig
import com.banpai.app.data.AgoraWalkieTalkie
import com.banpai.app.data.ApiClient
import com.banpai.app.data.BANPAI_EXPRESSIONS
import com.banpai.app.ui.media.AssetImage
import kotlinx.coroutines.launch

/**
 * 好友 1:1 聊天：好友对讲控件（2026-11-20 用户锁定，三个功能）：
 * （1）打字交流；（2）按住发送语音——异步语音消息（录一段、发过去、对方方便时听）；
 * （3）打开实时对讲——声网实时对讲：点「实时对讲」加入频道，按住说话、松开静音，
 * 点「结束对讲」即退出频道。实时对讲只在好友 1:1 里用；
 * 公共区域（派对房间 / 公共花园）不用声网实时语音（合规风险）。
 * 表情包（半拍 9 格真图）。好友来源：附近打卡、派对房间、扫码添加。
 */
@Composable
fun FriendChatScreen(
    friendId: Int,
    friendName: String,
    onBack: () -> Unit,
    authToken: String = "" // 登录 token；为空时实时对讲不可用（后端联调待做：登录尚未接通）
) {
    var input by remember { mutableStateOf("") }
    var useKeyboard by remember { mutableStateOf(true) }
    var messages by remember {
        mutableStateOf(listOf("系统" to "和 $friendName 的聊天开始啦，慢慢说。"))
    }

    // ---- 好友对讲控件：实时对讲状态 ----
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var walkieState by remember { mutableStateOf("未开始") }
    var inWalkie by remember { mutableStateOf(false) }
    var talking by remember { mutableStateOf(false) }
    var walkie by remember { mutableStateOf<AgoraWalkieTalkie?>(null) }
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    fun joinWalkie() {
        if (BuildConfig.AGORA_APP_ID.isEmpty()) {
            walkieState = "未配置 Agora App ID，对讲不可用"
            return
        }
        if (!hasMicPermission) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            walkieState = "需要录音权限：请允许后，再点一次「实时对讲」"
            return
        }
        if (authToken.isEmpty()) {
            walkieState = "登录后才能对讲（后端联调待做：登录 token 未接入）"
            return
        }
        walkieState = "正在加入对讲…"
        scope.launch {
            try {
                val tk = ApiClient.api.friendSpaceToken(friendId, "Bearer $authToken")
                if (tk.placeholder) {
                    walkieState = "后端未配置声网证书，对讲暂不可用"
                    return@launch
                }
                val engine = AgoraWalkieTalkie(context, BuildConfig.AGORA_APP_ID) { walkieState = it }
                if (engine.start(tk.token, tk.channel, tk.uid)) {
                    walkie = engine
                    inWalkie = true
                    walkieState = "对讲中：按住说话，松开静音"
                } else {
                    walkieState = "加入对讲失败"
                }
            } catch (_: Exception) {
                walkieState = "对讲失败：后端未部署或网络不通"
            }
        }
    }

    fun leaveWalkie() {
        walkie?.stop()
        walkie = null
        inWalkie = false
        talking = false
        walkieState = "已离开对讲"
    }

    DisposableEffect(Unit) { onDispose { walkie?.stop() } }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(friendName, "1:1 好友聊天 · 打字 / 语音消息 / 实时对讲")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { (who, text) ->
                Text("$who：$text")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 表情包：半拍 9 格表情真图。
        AssetImage(
            assetPath = BANPAI_EXPRESSIONS,
            contentDescription = "半拍表情（9 格）",
            modifier = Modifier.fillMaxWidth().height(96.dp),
            contentScale = ContentScale.Crop
        )
        OutlinedButton(
            onClick = {
                messages = messages + ("你" to "（发了一个半拍表情）")
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("发表情") }
        Spacer(modifier = Modifier.height(8.dp))
        // （1）打字交流
        if (useKeyboard) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("慢慢说，我在听呀") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            if (useKeyboard) {
                OutlinedButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            messages = messages + ("你" to input)
                            input = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("发送") }
            } else {
                // （2）按住发送语音：异步语音消息，按住录、松开发送。
                // 录音与上传待接后端，此处先以消息气泡占位，不装成已发出。
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    tryAwaitRelease()
                                    messages = messages +
                                        ("你" to "（语音消息 · 刚发送，占位：录音与上传待接后端）")
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) { Text("按住发语音") }
            }
            OutlinedButton(
                onClick = { useKeyboard = !useKeyboard },
                modifier = Modifier.weight(1f)
            ) { Text(if (useKeyboard) "切换到语音" else "切换到打字键盘") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // （3）打开实时对讲：声网实时对讲。加入后按住说话、松开静音，结束即退出频道。
        if (!inWalkie) {
            OutlinedButton(
                onClick = { joinWalkie() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("实时对讲") }
        } else {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    talking = true
                                    walkie?.setTalking(true)
                                    tryAwaitRelease()
                                    talking = false
                                    walkie?.setTalking(false)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) { Text(if (talking) "正在说话…" else "按住说话") }
                OutlinedButton(
                    onClick = { leaveWalkie() },
                    modifier = Modifier.weight(1f)
                ) { Text("结束对讲") }
            }
        }
        Text("对讲状态：$walkieState")
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回派对") }
    }
}
