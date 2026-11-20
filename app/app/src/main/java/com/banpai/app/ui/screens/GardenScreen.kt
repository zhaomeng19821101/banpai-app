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
import com.banpai.app.data.SnowLetterRules

/**
 * 公共花园：发帖 + 雪夜传信。
 * 发帖：本机选照片 / 直接拍照二选一，拍完保存 / 发花园 / 重拍。
 * 雪夜传信：放进飘雪、100m 内捡、24h 化掉。
 */
@Composable
fun GardenScreen() {
    var draft by remember { mutableStateOf("还没有帖子") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("公共花园", "发帖 · 雪夜传信")
        Text("帖子草稿：$draft")
        GoldButton("本机选照片（占位）", onClick = { draft = "已选一张本机照片（占位）" })
        GoldButton("直接拍照（占位）", onClick = { draft = "刚拍了一张照片（占位）" })
        GoldButton("保存", onClick = { draft = "已保存到本机（占位）" })
        GoldButton("发到花园", onClick = { draft = "已发到花园（占位）" })
        GoldButton("重拍", onClick = { draft = "还没有帖子" })
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard("雪夜传信：把信放进飘雪，${SnowLetterRules.PICKUP_RADIUS_M}m 内可以捡，" +
            "${SnowLetterRules.EXPIRE_HOURS}h 后化掉。")
        GoldButton("写一封雪夜传信（占位）", onClick = { })
    }
}
