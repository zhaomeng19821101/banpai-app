package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import com.banpai.app.data.POSTCARDS
import com.banpai.app.data.POSTER_COUNT

/**
 * 明信片页：24 张画廊 + 分享海报 4 张。
 * 明信片由打卡点位生成；分享海报占位（图片资源接入后替换）。
 */
@Composable
fun PostcardScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("明信片", "24 张画廊 · 分享海报 $POSTER_COUNT 张")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(POSTCARDS) { card ->
                InfoCard("明信片 ${card.id} 号 · ${card.place}")
            }
            items((1..POSTER_COUNT).toList()) { i ->
                InfoCard("分享海报 $i 号（占位）")
            }
        }
        GoldButton("分享一张明信片（占位）", onClick = { })
    }
}
