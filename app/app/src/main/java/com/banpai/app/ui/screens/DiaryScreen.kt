package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.banpai.app.data.DIARY_ENTRIES

/**
 * 日记页：纯文字，不读出声；加小表情装饰，文字是主角。
 * 从 2026-11-01 起每天一篇补到 11-19，共 19 篇；
 * 每篇结尾锁定台词 #14「晚安，明天见。」；
 * 11-01 是半拍生日，也是用户生日。
 */
@Composable
fun DiaryScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("日记", "纯文字 · 每天一篇 · 从 2026-11-01 开始")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(DIARY_ENTRIES) { entry ->
                InfoCard("❄ ${entry.date} · ${entry.title}\n${entry.text}")
            }
        }
        Text("共 ${DIARY_ENTRIES.size} 篇 · 每篇结尾都是「晚安，明天见。」")
    }
}
