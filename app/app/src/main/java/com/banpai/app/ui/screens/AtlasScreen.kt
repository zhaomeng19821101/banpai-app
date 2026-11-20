package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.banpai.app.data.FOODS
import com.banpai.app.data.GUARDIAN_PETS

/**
 * 图鉴页：半拍档案（第一页）+ 守护宠 24 只 + 好吃团 30 种 + 隐藏雪墩。
 * 彩蛋只出信不播视频：图鉴中央金色第五瓣常驻闪烁；
 * 市外点开全屏出信原图占位（不播视频、不剧透第五瓣位置）。
 * 四瓣：冰、雪、乐、花；静留给第五瓣。
 */
@Composable
fun AtlasScreen() {
    var showLetter by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("图鉴", "守护宠 · 好吃团 · 半拍档案")
        InfoCard("半拍档案：哈尔滨一年四季孕育出来的精灵。\n" +
            "四瓣：冰、雪、乐、花。静，留给第五瓣。\n" +
            "正在找第五瓣。徽章、能量规则见档案页。")
        InfoCard("✦ 金色第五瓣（常驻闪烁）— 点开出信，不播视频")
        GoldButton("点开第五瓣的信", onClick = { showLetter = true })
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(GUARDIAN_PETS) { pet ->
                InfoCard("${pet.title}\n${pet.line}\n声音：${pet.sound}")
            }
            items(FOODS) { food ->
                InfoCard("好吃团：${food.name}${if (food.hidden) "（隐藏）" else ""}")
            }
        }
    }

    if (showLetter) {
        AlertDialog(
            onDismissRequest = { showLetter = false },
            title = { Text("第五瓣的信") },
            text = { Text("信原图占位：全屏显示信的原图（图片资源接入后替换）。\n彩蛋只出信，不播视频。") },
            confirmButton = {
                TextButton(onClick = { showLetter = false }) { Text("收起来") }
            }
        )
    }
}
