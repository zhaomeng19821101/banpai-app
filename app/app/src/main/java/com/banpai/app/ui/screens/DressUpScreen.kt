package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.banpai.app.data.DRESSUP_SLOTS
import com.banpai.app.data.OUTFITS

/**
 * 装扮页：3 槽 — 服装/套装、背景、特效。配饰已去掉；服装 + 套装合并为一槽。
 * 16 套装扮假数据；成套换装，不做单件配饰。
 */
@Composable
fun DressUpScreen() {
    var slot by remember { mutableStateOf(DRESSUP_SLOTS[0]) }
    var picked by remember { mutableStateOf(OUTFITS[0].name) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("装扮", "3 槽：服装/套装 · 背景 · 特效（配饰已去掉）")
        Text("当前槽：$slot　已选：$picked")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(DRESSUP_SLOTS) { s ->
                InfoCard("槽位：$s${if (s == slot) "（当前）" else ""}")
            }
            items(OUTFITS) { outfit ->
                InfoCard("套装：${outfit.name}${if (outfit.name == picked) "（穿着中）" else ""}")
            }
        }
        GoldButton("穿上这套", onClick = { picked = OUTFITS[1].name })
    }
}
