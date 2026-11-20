package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.WELCOME_VIDEO
import com.banpai.app.data.introCardAsset
import com.banpai.app.ui.media.AssetImage
import com.banpai.app.ui.media.AssetVideo

/**
 * 欢迎页：欢迎视频（内置，循环静音播）→ 4 图轮播。
 * 视频播完点按钮进认识半拍；轮播图用认识半拍的 4 张卡图。
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("你好呀，我是半拍。")
        Spacer(modifier = Modifier.height(16.dp))
        // 欢迎视频：雪夜哈尔滨背景，内置离线播。
        AssetVideo(
            assetPath = WELCOME_VIDEO,
            modifier = Modifier.fillMaxWidth().height(300.dp),
            loop = true,
            muted = true,
            showControls = false
        )
        Spacer(modifier = Modifier.height(16.dp))
        // 4 图轮播。
        Row(modifier = Modifier.fillMaxWidth()) {
            for (i in 1..4) {
                AssetImage(
                    assetPath = introCardAsset(i),
                    contentDescription = "轮播图 $i",
                    modifier = Modifier.weight(1f).height(96.dp),
                    contentScale = ContentScale.Crop
                )
                if (i < 4) Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        GoldButton("进入半拍的世界", onClick = onDone)
    }
}
