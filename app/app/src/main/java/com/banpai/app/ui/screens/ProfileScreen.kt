package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.banpai.app.data.BANPAI_STANDARD
import com.banpai.app.data.FAKE_USER
import com.banpai.app.ui.media.AssetImage
import com.banpai.app.ui.theme.ThemeMode

/**
 * 我的页：用户信息、徽章连接状态占位、昼夜三档切换、入口（装扮 / 表盘 / 日记 / 明信片 / 关于半拍）。
 * 昼夜三档：白天 / 夜晚 / 跟随系统。
 */
@Composable
fun ProfileScreen(
    onOpenDressUp: () -> Unit,
    onOpenWatchface: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenPostcard: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("我的", "${FAKE_USER.name} · Lv.${FAKE_USER.level} ${FAKE_USER.levelName}")
        // 头像：半拍标准图（正好 4 瓣），圆形裁切。
        AssetImage(
            assetPath = BANPAI_STANDARD,
            contentDescription = "半拍头像",
            modifier = Modifier.size(72.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("能量 ${FAKE_USER.energy} · 冰晶 36（假数据）")
        Text("吧唧徽章：已连接（占位，BLE 未真连）")
        Spacer(modifier = Modifier.height(16.dp))
        Text("昼夜三档：")
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onThemeModeChange(ThemeMode.Day) }, modifier = Modifier.weight(1f)) {
                Text("白天")
            }
            OutlinedButton(onClick = { onThemeModeChange(ThemeMode.Night) }, modifier = Modifier.weight(1f)) {
                Text("夜晚")
            }
            OutlinedButton(onClick = { onThemeModeChange(ThemeMode.System) }, modifier = Modifier.weight(1f)) {
                Text("跟随系统")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onOpenDressUp, modifier = Modifier.fillMaxWidth()) { Text("装扮") }
        OutlinedButton(onClick = onOpenWatchface, modifier = Modifier.fillMaxWidth()) { Text("表盘工坊") }
        OutlinedButton(onClick = onOpenDiary, modifier = Modifier.fillMaxWidth()) { Text("日记") }
        OutlinedButton(onClick = onOpenPostcard, modifier = Modifier.fillMaxWidth()) { Text("明信片") }
        Spacer(modifier = Modifier.height(8.dp))
        Text("关于半拍：认识半拍 4 卡可重看（占位）。")
    }
}
