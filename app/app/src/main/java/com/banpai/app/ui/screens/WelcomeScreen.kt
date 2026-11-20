package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 欢迎页：视频 → 4 图轮播（占位）。
 * 欢迎视频与 4 张轮播图为 assets 占位，真机接入时用 ExoPlayer / Coil 加载，
 * 此处不复制大文件、不画假视频。
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        Text("你好呀，我是半拍。")
        Spacer(modifier = Modifier.height(16.dp))
        Text("欢迎视频占位（播放完成后进入 4 图轮播）")
        Text("轮播图 1 / 2 / 3 / 4 占位")
        Spacer(modifier = Modifier.height(32.dp))
        GoldButton("进入半拍的世界", onClick = onDone)
    }
}
