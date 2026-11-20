package com.banpai.app.ui.screens

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.BuildConfig
import com.banpai.app.data.GUARDIAN_PETS
import com.banpai.app.data.guardianCardAsset
import com.banpai.app.ui.media.AssetImage

/**
 * 探索：地图是主角。工程骨架里地图仍是占位文案（待接 AMap SDK + AMAP_KEY），
 * 但打卡点列表已用真实守护宠卡片图：前 5 只已点亮（金点），其余未亮（灰点）。
 * 列表的下一站、真实步行路线、距离按 AMap 实时算；打卡点跟随地图缩放。
 * 占位状态：明确说清“缺什么、缺哪一步”，不装成真的。
 */
@Composable
fun ExploreScreen(
    onOpenCheckIn: () -> Unit,
    onOpenAtlas: () -> Unit = {}
) {
    val hasKey = BuildConfig.AMAP_KEY.isNotEmpty()
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("探索 · 地图是主角", "地图占位；打卡点是真实守护宠卡片")
        if (!hasKey) {
            InfoCard("地图占位：请在 local.properties 配 AMAP_KEY\n" +
                "接入后显示高德真地图：松花江 / 中央大街 / 索菲亚真实轮廓、" +
                "真实步行路线 + 距离、80-150m 地理围栏自动打卡。")
        } else {
            InfoCard("高德地图已配置（占位容器）：MapView 接入点在此挂载。")
        }
        InfoCard("下一站：索菲亚·钟（脉动中）· 步行路线占位：真实步行距离由高德步行规划返回")
        InfoCard("地理围栏自动打卡占位：进入点位 80-150m 自动打卡，需定位权限 + 后台定位合规")
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            itemsIndexed(GUARDIAN_PETS) { index, pet ->
                val lit = index < 5 // 假数据：前 5 只已点亮（金点），其余未亮（灰点）
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    AssetImage(
                        assetPath = guardianCardAsset(index),
                        contentDescription = pet.title,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${if (lit) "● 已亮" else "○ 未亮"} ${pet.title}")
                        Text(pet.line)
                        Text("声音：${pet.sound}")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("缩放：＋/－（AMap 原生缩放；真机上还可双指缩放）", modifier = Modifier.padding(horizontal = 16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedButton(onClick = onOpenCheckIn, modifier = Modifier.weight(1f)) { Text("去打卡") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onOpenAtlas, modifier = Modifier.weight(1f)) { Text("图鉴") }
        }
    }
}
