package com.banpai.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.CheckInRules
import com.banpai.app.data.guardianCardAsset
import com.banpai.app.ui.media.AssetImage

/**
 * 打卡页：距离环 3 状态（未到 / 可打卡 / 已打卡）+ AR 问答 + 成功故事流（AR 导览已删）。
 * 规则：首次 +80、再访 +20、每点每天 1 次、拍照 +10、乱入 +5/天。
 * 任务线按目的地动态生成（跟着半拍走 X）：目的地定任务名、AMap 步行路线、
 * 沿途站点、AR 问答、到达解锁的守护宠；不在真机上时距离为占位。
 * 敲吧唧两下拍照：BLE TAP 通知 T:shutter 触发快门（见 BleManager / CameraScreen）。
 */
@Composable
fun CheckInScreen(onOpenCamera: () -> Unit) {
    var state by remember { mutableStateOf("可打卡") }
    var energy by remember { mutableStateOf(1280) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("打卡", "索菲亚·钟 · 距离环：$state")
        // 本站守护宠卡片（索菲亚·钟）。
        AssetImage(
            assetPath = guardianCardAsset(1),
            contentDescription = "索菲亚·钟",
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 距离环（三态）：未到 / 可打卡 / 已打卡。距离是占位（真机 GPS + AMap 步行距离）。
        val ringProgress = when (state) {
            "已打卡" -> 1f
            "可打卡" -> 0.85f
            else -> 0.25f
        }
        val ringDistance = when (state) {
            "已打卡" -> "已打卡 · 0 米"
            "可打卡" -> "可打卡 · 约 86 米"
            else -> "还没到 · 约 320 米"
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawArc(
                    color = Color(0xFFE8E0F0),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color(0xFFE9A83C),
                    startAngle = -90f,
                    sweepAngle = 360f * ringProgress,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state)
                Text(ringDistance)
                Text("围栏 80–150m 内自动打卡")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("当前能量：$energy")
        Text("下一站 · 索菲亚·钟 · 80–150m 围栏内自动打卡（AMap 步行距离，真机 GPS）")
        Spacer(modifier = Modifier.height(8.dp))
        InfoCard("首次 +${CheckInRules.FIRST_VISIT} · 再访 +${CheckInRules.REVISIT} · " +
            "每点每天 ${CheckInRules.PER_POINT_PER_DAY} 次 · " +
            "拍照 +${CheckInRules.PHOTO} · 乱入 +${CheckInRules.CAMEO_PER_DAY}/天")
        InfoCard("AR 问答：索菲亚教堂是哪一年建成的？答对加能量（AR 问答与定位 + 方向同机制）")
        InfoCard("成功故事流：打卡成功 → 守护宠到手 → +${CheckInRules.FIRST_VISIT} 能量（重访 +${CheckInRules.REVISIT}）→ 日记自动记一笔")
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
