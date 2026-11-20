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
import com.banpai.app.data.CLOCK_STYLE_NAMES
import com.banpai.app.data.WATCHFACES

/**
 * 表盘工坊：30 款表盘；5 种时钟样式占位，时间按款式自动选位置。
 * 圆形表盘构图，无数字无文字（时间由系统按样式自动排布）；
 * 每款姿势 / 表情不同；24 号金瓣隐藏款对应 BLE DIAL "24"。
 */
@Composable
fun WatchfaceScreen() {
    var picked by remember { mutableStateOf(WATCHFACES[0]) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("表盘工坊", "30 款 · 时钟样式 ${CLOCK_STYLE_NAMES[picked.clockStyle]}（时间按款式自动选位置）")
        Text("当前表盘：${picked.id} 号 ${picked.name} · 时钟样式 ${picked.clockStyle}（${CLOCK_STYLE_NAMES[picked.clockStyle]}）")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(WATCHFACES) { face ->
                InfoCard("${face.id} 号 ${face.name} · 时钟样式 ${face.clockStyle}${if (face == picked) "（已选）" else ""}")
            }
        }
        GoldButton("同步到吧唧（BLE DIAL 占位）", onClick = { /* TODO BleManager.writeDial(picked.id.toString()) */ })
    }
}
