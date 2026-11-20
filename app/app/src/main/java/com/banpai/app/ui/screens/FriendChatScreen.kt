package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.BANPAI_EXPRESSIONS
import com.banpai.app.ui.media.AssetImage

/**
 * 好友 1:1 聊天：异步语音消息（按住说话占位）+ 打字键盘切换 + 表情包（半拍 9 格真图）。
 * 语音现在是占位：Agora 未接；异步语音消息（录一段、发过去、对方方便时听）
 * 走后端接口是下一步。好友来源：附近打卡、派对房间、扫码添加。
 */
@Composable
fun FriendChatScreen(friendName: String, onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var useKeyboard by remember { mutableStateOf(true) }
    var holdToTalk by remember { mutableStateOf(false) }
    var messages by remember {
        mutableStateOf(listOf("系统" to "和 $friendName 的聊天开始啦，慢慢说。"))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader(friendName, "1:1 好友聊天 · 异步语音 + 文字")
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
                OutlinedButton(
                    onClick = { holdToTalk = !holdToTalk },
                    modifier = Modifier.weight(1f)
                ) { Text(if (holdToTalk) "松开结束说话" else "按住说话（占位）") }
            }
            OutlinedButton(
                onClick = { useKeyboard = !useKeyboard },
                modifier = Modifier.weight(1f)
            ) { Text(if (useKeyboard) "切换到语音" else "切换到打字键盘") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回派对") }
    }
}
