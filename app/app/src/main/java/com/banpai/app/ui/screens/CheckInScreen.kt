package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.banpai.app.data.CheckInRules

/**
 * 打卡页：距离环 3 状态（未到 / 可打卡 / 已打卡）+ AR 问答 / AR 导航轻量版占位。
 * 规则：首次 +80、再访 +20、每点每天 1 次、拍照 +10、乱入 +5/天。
 * 敲吧唧两下拍照：BLE TAP 通知 T:shutter 触发快门（见 BleManager / CameraScreen）。
 */
@Composable
fun CheckInScreen(onOpenCamera: () -> Unit) {
    var state by remember { mutableStateOf("可打卡") }
    var energy by remember { mutableStateOf(1280) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("打卡", "索菲亚·钟 · 距离环：${state}")
        Text("当前能量：$energy")
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard("首次 +${CheckInRules.FIRST_VISIT} · 再访 +${CheckInRules.REVISIT} · " +
            "每点每天 ${CheckInRules.PER_POINT_PER_DAY} 次 · " +
            "拍照 +${CheckInRules.PHOTO} · 乱入 +${CheckInRules.CAMEO_PER_DAY}/天")
        InfoCard("AR 问答占位：答对加能量；AR 导航轻量版占位：高德步行路线 + 箭头指引")
        Spacer(modifier = Modifier.height(16.dp))
        GoldButton("打卡", onClick = {
            if (state == "可打卡") {
                energy += CheckInRules.FIRST_VISIT
                state = "已打卡"
            }
        })
        Spacer(modifier = Modifier.height(8.dp))
        GoldButton("拍照打卡", onClick = onOpenCamera)
        Spacer(modifier = Modifier.height(8.dp))
        Text("也可以敲吧唧两下拍照（BLE TAP 占位）。")
    }
}
