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
import com.banpai.app.data.HuayuRules
import com.banpai.app.data.LOCKED_LINES
import com.banpai.app.ui.media.AssetImage

/**
 * 花语（聊天）页：小模型 + 联网打听，不接大模型兜底。
 * 范围：吃 / 住 / 行 / 心情 / 感受 + 哈尔滨建筑历史。
 * 回答只用 15 句锁定台词做 lookup；不会就说「我还小，帮你去打听」，
 * 搜不到就老实承认。输入支持打字键盘；语音输入占位。
 * 表情包：半拍 9 格表情真图（点一下发一个表情）。
 * 小模型还没接上：现在问什么都先回「我还小，帮你去打听」，等 Colab 训完接 API。
 */
@Composable
fun ChatScreen() {
    var input by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(listOf("半拍" to LOCKED_LINES[0].text))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("花语", "小模型 + 联网打听 · 只用 15 句锁定台词回答")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { (who, text) ->
                Text("$who：$text")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(HuayuRules.MODEL)
        Text("范围：${HuayuRules.SCOPE}")
        Spacer(modifier = Modifier.height(8.dp))
        // 表情包：半拍 9 格表情真图。
        AssetImage(
            assetPath = BANPAI_EXPRESSIONS,
            contentDescription = "半拍表情（9 格）",
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentScale = ContentScale.Crop
        )
        OutlinedButton(
            onClick = {
                messages = messages + ("你" to "（发了一个半拍表情）") +
                    ("半拍" to LOCKED_LINES[8].text)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("发表情") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("慢慢说，我在听呀") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    if (input.isNotBlank()) {
                        messages = messages + ("你" to input) + ("半拍" to HuayuRules.DONT_KNOW)
                        input = ""
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("问半拍")
            }
            OutlinedButton(
                onClick = { messages = messages + ("半拍" to LOCKED_LINES[3].text) },
                modifier = Modifier.weight(1f)
            ) {
                Text("语音输入（占位）")
            }
        }
    }
}
