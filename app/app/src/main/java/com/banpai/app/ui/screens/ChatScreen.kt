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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.banpai.app.data.BANPAI_EXPRESSIONS
import com.banpai.app.data.BanpaiGuardrails
import com.banpai.app.data.BanpaiLlama
import com.banpai.app.data.HuayuRules
import com.banpai.app.data.LOCKED_LINES
import com.banpai.app.ui.media.AssetImage
import kotlinx.coroutines.launch

/**
 * 花语（聊天）页：端侧 1.7B 小模型 + 联网打听，不接大模型兜底。
 * 范围：吃 / 住 / 行 / 心情 / 感受 + 哈尔滨建筑历史。
 *
 * 回答顺序（BanpaiGuardrails）：第五瓣拦截 → 唱歌拒绝 → 15 句锁定台词
 * lookup → 马迭尔/红肠事实 lookup → 端侧模型生成（llama.cpp，Qwen3 ChatML）。
 * 模型没就绪时不假装回答：发送按钮等模型就绪才放行模型问题，
 * 兜底命中（锁定台词等）不需要模型，随时可答。
 * 生成时显示“半拍在想…”，不做流式，整段返回。
 */
@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modelState by BanpaiLlama.state.collectAsState()
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var messages by remember {
        mutableStateOf(listOf("半拍" to LOCKED_LINES[0].text))
    }

    // 进页即开始准备模型：assets → filesDir 复制（带进度）→ llama 加载。
    LaunchedEffect(Unit) { BanpaiLlama.ensureReady(context) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("花语", "小模型 + 联网打听 · 只用 15 句锁定台词回答")
        // 模型状态行：复制中 / 加载中 / 就绪 / 失败（失败给重试）。
        when (val s = modelState) {
            is BanpaiLlama.ModelState.Copying ->
                Text("模型复制中 ${(s.progress * 100).toInt()}%（首次启动，1.1 GB）")
            is BanpaiLlama.ModelState.Loading -> Text("模型加载中…")
            is BanpaiLlama.ModelState.Ready -> Text("花语模型已就绪（端侧 1.7B）")
            is BanpaiLlama.ModelState.Failed -> {
                Text("模型加载失败：${s.reason}")
                OutlinedButton(onClick = { scope.launch { BanpaiLlama.ensureReady(context) } }) {
                    Text("重试加载")
                }
            }
            is BanpaiLlama.ModelState.Idle -> Text("模型准备中…")
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { (who, text) ->
                Text("$who：$text")
            }
            if (thinking) {
                item { Text("半拍：半拍在想…") }
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
                    if (input.isBlank() || thinking) return@OutlinedButton
                    val question = input
                    messages = messages + ("你" to question)
                    input = ""
                    // 先过端侧兜底（第五瓣 → 唱歌 → 锁定台词 → 食物事实）。
                    val guard = BanpaiGuardrails.guardrailReply(question)
                    if (guard != null) {
                        messages = messages + ("半拍" to guard)
                    } else if (modelState !is BanpaiLlama.ModelState.Ready) {
                        // 模型没就绪：不假装回答，只报状态。
                        messages = messages + ("系统" to "模型还在准备中，等它就绪再问我哦。")
                    } else {
                        thinking = true
                        scope.launch {
                            val raw = BanpaiLlama.generate(question)
                            val reply = raw?.let { BanpaiGuardrails.stripThink(it) }
                                ?: LOCKED_LINES[0].text
                            messages = messages + ("半拍" to reply)
                            thinking = false
                        }
                    }
                },
                enabled = !thinking,
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
