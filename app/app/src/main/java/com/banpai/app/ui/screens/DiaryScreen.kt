package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.BANPAI_MINI_Q
import com.banpai.app.data.DIARY_ENTRIES
import com.banpai.app.ui.media.AssetImage

/**
 * 日记页：纯文字，不读出声；加小表情装饰，文字是主角。
 * 从 2026-11-01 起每天一篇补到 11-19，共 19 篇；
 * 每篇结尾锁定台词 #14「晚安，明天见。」；
 * 11-01 是半拍生日，也是用户生日。
 * 页顶是迷你 Q 版半拍小图（正好 4 瓣）。
 */
@Composable
fun DiaryScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("日记", "纯文字 · 每天一篇 · 从 2026-11-01 开始")
        AssetImage(
            assetPath = BANPAI_MINI_Q,
            contentDescription = "迷你半拍",
            modifier = Modifier.fillMaxWidth().height(140.dp).padding(horizontal = 16.dp),
            contentScale = ContentScale.Fit
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(DIARY_ENTRIES) { entry ->
                InfoCard("❄ ${entry.date} · ${entry.title}\n${entry.text}")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("共 ${DIARY_ENTRIES.size} 篇 · 每篇结尾都是「晚安，明天见。」")
    }
}
