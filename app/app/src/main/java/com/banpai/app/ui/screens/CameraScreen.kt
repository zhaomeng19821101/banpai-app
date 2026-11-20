package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 相机页：预留 CameraX 接入点（androidx.camera，占位，见 app/build.gradle.kts 注释依赖）。
 * 拍照乱入用真绿幕视频（assets 引用占位）：
 *   视频文件在 ~/workspace/banpai/assets/greenscreen/，不复制大文件进工程；
 *   接入时用 ExoPlayer + 绿幕抠像（chroma-key）叠加到取景画面，拍照与录像都支持乱入。
 * 6 风格、5 尺寸、40 相框（20 横 + 20 竖）；HDR / 美颜占位。
 * 敲吧唧两下拍照：BLE TAP 通知 T:shutter 到达后触发快门（见 BleManager）。
 */
private val CAMERA_STYLES = listOf("冰蓝", "雪夜", "丁香", "暖金", "复古", "胶片")
private val CAMERA_SIZES = listOf("1:1", "4:3", "16:9", "3:4", "9:16")

@Composable
fun CameraScreen() {
    var style by remember { mutableStateOf(CAMERA_STYLES[0]) }
    var size by remember { mutableStateOf(CAMERA_SIZES[0]) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("拍照", "CameraX 接入点占位 · 乱入用真绿幕视频")
        Text("取景器占位（CameraX PreviewView 接入点）")
        Text("当前风格：$style（共 6 种大师级风格，精品策展，非冷暖滤镜）")
        Text("当前尺寸：$size（共 5 种）")
        Text("相框：40 个（20 横 + 20 竖）占位")
        Text("绿幕乱入视频：~/workspace/banpai/assets/greenscreen/（引用占位，不复制大文件）")
        Text("录像也支持乱入。HDR / 美颜占位。")
        GoldButton("拍照", onClick = { /* TODO CameraX takePicture */ })
        GoldButton("敲吧唧两下也能拍（BLE TAP 占位）", onClick = { /* TODO BleManager 订阅 T:shutter */ })
    }
}
