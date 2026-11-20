package com.banpai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.banpai.app.data.EASTER_LETTER
import com.banpai.app.data.FOODS
import com.banpai.app.data.GUARDIAN_PETS
import com.banpai.app.data.foodAsset
import com.banpai.app.data.guardianCardAsset
import com.banpai.app.data.guardianVideoAsset
import com.banpai.app.ui.media.AssetImage
import com.banpai.app.ui.media.AssetVideo

/**
 * 图鉴页：半拍档案（第一页）+ 守护宠 24 只 + 好吃团 30 种 + 隐藏雪墩。
 * 彩蛋只出信不播视频：图鉴中央金色第五瓣常驻闪烁；
 * 点开全屏显示信的原图（可滚动，右上角小 × 与底部「收起这封信」不遮信）。
 * 四瓣：冰、雪、乐、花；静留给第五瓣。
 * 守护宠卡片是真图，点开播真视频（守护宠 10 秒视频，内置离线播）；
 * 好吃团 30 种里 12 种素材库无图（卡片保持纯文字，不硬塞错图），
 * 已解锁 3 种：马迭尔冰棍、秋林红肠、华梅面包（华梅面包有专属新图）。
 */
@Composable
fun AtlasScreen() {
    var showLetter by remember { mutableStateOf(false) }
    var playingPet by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("图鉴", "守护宠 · 好吃团 · 半拍档案")
        InfoCard("半拍档案：哈尔滨一年四季孕育出来的精灵。\n" +
            "四瓣：冰、雪、乐、花。静，留给第五瓣。\n" +
            "正在找第五瓣。徽章、能量规则见档案页。")
        InfoCard("✦ 金色第五瓣（常驻闪烁）— 点开出信，不播视频")
        GoldButton("点开第五瓣的信", onClick = { showLetter = true })
        LazyColumn(modifier = Modifier.weight(1f)) {
            itemsIndexed(GUARDIAN_PETS) { index, pet ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { playingPet = index }
                ) {
                    AssetImage(
                        assetPath = guardianCardAsset(index),
                        contentDescription = pet.title,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${pet.title}\n${pet.line}\n声音：${pet.sound}\n点开看 10 秒视频 ▶")
                }
            }
            item {
                Text(
                    "好吃团 · 30 种 · 已解锁 3/30（马迭尔冰棍、秋林红肠、华梅面包）",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            itemsIndexed(FOODS) { _, food ->
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    val asset = foodAsset(food.id)
                    if (asset != null) {
                        AssetImage(
                            assetPath = asset,
                            contentDescription = food.name,
                            modifier = Modifier.size(64.dp),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Column {
                        Text("好吃团：${food.name}${if (food.hidden) "（隐藏彩蛋，不是吃的）" else ""}")
                        if (asset == null) Text("（图还没画好，先留文字）")
                    }
                }
            }
        }
    }

    // 第五瓣的信：全屏原图。
    if (showLetter) {
        Dialog(onDismissRequest = { showLetter = false }) {
            Box(modifier = Modifier.fillMaxSize()) {
                AssetImage(
                    assetPath = EASTER_LETTER,
                    contentDescription = "半拍写给你的信",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                TextButton(
                    onClick = { showLetter = false },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) { Text("×") }
                TextButton(
                    onClick = { showLetter = false },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) { Text("收起这封信") }
            }
        }
    }

    // 守护宠视频：点开卡片播 10 秒视频（内置离线，带控制条，封面是卡片图）。
    playingPet?.let { index ->
        val pet = GUARDIAN_PETS[index]
        Dialog(onDismissRequest = { playingPet = null }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(pet.title, modifier = Modifier.padding(8.dp))
                AssetVideo(
                    assetPath = guardianVideoAsset(index),
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    loop = false,
                    muted = false,
                    showControls = true,
                    posterAssetPath = guardianCardAsset(index)
                )
                TextButton(onClick = { playingPet = null }) { Text("关掉") }
            }
        }
    }
}
