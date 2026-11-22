package com.banpai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.BadgeCameraLink
import com.banpai.app.data.CAMERA_EXPRESSIONS
import com.banpai.app.data.CAMERA_STYLE_BASE
import com.banpai.app.data.Cameo
import com.banpai.app.data.StubBadgeCameraLink
import com.banpai.app.data.cameraStyleAsset
import com.banpai.app.data.frameHorizontalAsset
import com.banpai.app.data.frameVerticalAsset
import com.banpai.app.data.halfBodyAsset
import com.banpai.app.ui.media.AssetImage
import com.banpai.app.ui.media.AssetVideo

/**
 * 相机页：CameraX 接入点占位（androidx.camera，见 app/build.gradle.kts 注释依赖），
 * 但乱入是真绿幕视频（内置，循环播）：3 种入场——探头（v7 色彩校正版）/ 挥手 / 惊喜跳入，
 * 带乱入开关；关掉乱入显示风格底图。
 * 8 种大师级风格（精品策展，非冷暖滤镜）、40 相框（20 横 + 20 竖，边框分类里挑，
 * 不堆一堆让用户翻）、6 表情库 + 5 半身像框样张都是真图。
 * 录像也支持乱入。HDR / 美颜占位。
 * 吧唧拍照键（2026-11-23，Jackson 定，替代敲两下快门）：进相机页调
 * BadgeCameraLink.onCameraOpened() 写 M:camera，吧唧自动弹出拍照键；点键发
 * S:press（CAMERA b109）→ 本页 onShutterPressed 回调执行拍照（CameraX 接入后），
 * 拍照成功再调 notifyPhotoTaken() 写 S:done，吧唧显示“已拍照”；离页调
 * onCameraClosed() 写 M:normal。拍照模式下敲击不拍。T:shutter 已废弃。
 * 注意：绿幕抠像（chroma-key）叠加到取景画面是下一步；现在乱入视频在取景框里循环预览。
 */
private val CAMERA_STYLES = listOf("冰城纪实", "雪夜电影", "暖阳人像", "丁香春日", "老街黑白", "黄金时刻", "冰灯夜游", "1998 拍立得")
private val CAMERA_SIZES = listOf("1:1", "4:3", "16:9", "3:4", "9:16")

@Composable
fun CameraScreen(
    badgeCameraLink: BadgeCameraLink? = null
) {
    // 未传链路时用占位实现（只记调用，不真连 BLE；真机联调时从上层注入真实现）。
    val link = badgeCameraLink ?: remember { StubBadgeCameraLink() }
    var style by remember { mutableStateOf(CAMERA_STYLES[0]) }
    var size by remember { mutableStateOf(CAMERA_SIZES[0]) }
    var cameoOn by remember { mutableStateOf(true) }
    var cameo by remember { mutableStateOf(Cameo.PEEK) }
    var frameV by remember { mutableStateOf(1) }
    var frameH by remember { mutableStateOf(1) }
    // 吧唧拍照键按下的次数（占位计数；CameraX 接入后这里改为真正执行拍照）。
    var badgeShutterCount by remember { mutableStateOf(0) }

    // 进相机页 → 通知吧唧弹出拍照键（M:camera）；离页 → M:normal。
    // 吧唧点拍照键（S:press）→ 执行拍照；拍照成功后必须调
    // link.notifyPhotoTaken() 写 S:done，吧唧才显示“已拍照”——现在 CameraX
    // 还没接进来，没有真正拍成照片，所以这里只计数、不回执（不骗吧唧）。
    DisposableEffect(link) {
        link.onShutterPressed = {
            badgeShutterCount++
            // TODO CameraX 接入后：在此调 takePicture()，成功回调里调
            // link.notifyPhotoTaken()；失败不调。
        }
        link.onCameraOpened()
        onDispose {
            link.onShutterPressed = null
            link.onCameraClosed()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader("拍照", "乱入是真绿幕视频 · 8 大师风格 · 40 相框")
        // 取景器：乱入视频循环播（开）或风格底图（关）。
        if (cameoOn) {
            AssetVideo(
                assetPath = cameo,
                modifier = Modifier.fillMaxWidth().height(320.dp),
                loop = true,
                muted = true,
                showControls = false
            )
            Text("乱入预览：绿幕视频循环播（绿幕抠像叠加到取景画面是下一步）")
        } else {
            AssetImage(
                assetPath = CAMERA_STYLE_BASE,
                contentDescription = "取景底图",
                modifier = Modifier.fillMaxWidth().height(320.dp),
                contentScale = ContentScale.Crop
            )
            Text("乱入已关：取景底图（中央大街黄昏）")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { cameoOn = !cameoOn }, modifier = Modifier.weight(1f)) {
                Text(if (cameoOn) "乱入：开" else "乱入：关")
            }
            OutlinedButton(onClick = { cameo = Cameo.PEEK }, modifier = Modifier.weight(1f)) { Text("探头") }
            OutlinedButton(onClick = { cameo = Cameo.WAVE }, modifier = Modifier.weight(1f)) { Text("挥手") }
            OutlinedButton(onClick = { cameo = Cameo.JUMP }, modifier = Modifier.weight(1f)) { Text("惊喜跳入") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("当前风格：$style（8 种大师级风格，精品策展，非冷暖滤镜）")
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(CAMERA_STYLES.withIndex().toList()) { (index, s) ->
                Column(modifier = Modifier.padding(end = 8.dp).clickable { style = s }) {
                    AssetImage(
                        assetPath = cameraStyleAsset(index + 1),
                        contentDescription = s,
                        modifier = Modifier.width(96.dp).height(72.dp),
                        contentScale = ContentScale.Crop
                    )
                    Text(s)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("当前尺寸：$size（共 5 种）")
        Text("相框 · 竖 20（已选 $frameV 号）")
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items((1..20).toList()) { n ->
                Column(modifier = Modifier.padding(end = 8.dp).clickable { frameV = n }) {
                    AssetImage(
                        assetPath = frameVerticalAsset(n),
                        contentDescription = "竖框 $n 号",
                        modifier = Modifier.width(54.dp).height(96.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text("$n 号")
                }
            }
        }
        Text("相框 · 横 20（已选 $frameH 号）")
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items((1..20).toList()) { n ->
                Column(modifier = Modifier.padding(end = 8.dp).clickable { frameH = n }) {
                    AssetImage(
                        assetPath = frameHorizontalAsset(n),
                        contentDescription = "横框 $n 号",
                        modifier = Modifier.width(96.dp).height(54.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text("$n 号")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("表情库（6 格）与半身像框（5 张样张）")
        AssetImage(
            assetPath = CAMERA_EXPRESSIONS,
            contentDescription = "6 表情库",
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentScale = ContentScale.Crop
        )
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items((1..5).toList()) { n ->
                AssetImage(
                    assetPath = halfBodyAsset(n),
                    contentDescription = "半身像框样张 $n",
                    modifier = Modifier.width(96.dp).height(128.dp).padding(end = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("录像也支持乱入。HDR / 美颜占位。")
        GoldButton("拍照", onClick = { /* TODO CameraX takePicture */ })
        Text("吧唧拍照键：手机打开相机后吧唧自动弹出拍照键，点键拍照（已收到 $badgeShutterCount 次；CameraX 接入后执行拍照，成功后回执吧唧显示“已拍照”）。敲击不拍。")
    }
}
