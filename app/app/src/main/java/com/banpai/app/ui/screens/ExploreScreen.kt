package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.banpai.app.BuildConfig
import com.banpai.app.data.GUARDIAN_PETS

/**
 * 探索页：做真地图，留高德 SDK 接入点（com.amap.api.maps，占位）。
 * 无 key 时显示占位说明「请在 local.properties 配 AMAP_KEY」，不要画假地图。
 * 要点：松花江 / 中央大街 / 索菲亚等真实轮廓、下一站脉动、已亮金点 / 未亮灰点、
 * 真实步行路线 + 距离、80-150m 地理围栏自动打卡占位。
 */
@Composable
fun ExploreScreen(onOpenCheckIn: () -> Unit) {
    val hasKey = BuildConfig.AMAP_KEY.isNotEmpty()
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("探索", "真地图：下一站脉动，已亮金点，未亮灰点")
        if (!hasKey) {
            InfoCard("地图占位：请在 local.properties 配 AMAP_KEY\n" +
                "接入后显示高德真地图：松花江 / 中央大街 / 索菲亚真实轮廓、" +
                "真实步行路线 + 距离、80-150m 地理围栏自动打卡。")
        } else {
            InfoCard("高德地图已配置（占位容器）：MapView 接入点在此挂载。")
        }
        InfoCard("下一站：索菲亚·钟（脉动中）· 步行路线占位：真实步行距离由高德步行规划返回")
        InfoCard("地理围栏自动打卡占位：进入点位 80-150m 自动打卡，需定位权限 + 后台定位合规")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(GUARDIAN_PETS) { pet ->
                // 假数据：前 5 只已点亮（金点），其余未亮（灰点）
                val lit = GUARDIAN_PETS.indexOf(pet) < 5
                InfoCard("${if (lit) "● 已亮" else "○ 未亮"} ${pet.title} — ${pet.line}")
            }
        }
        GoldButton("去打卡", onClick = onOpenCheckIn)
        Text("", modifier = Modifier.padding(8.dp))
    }
}
