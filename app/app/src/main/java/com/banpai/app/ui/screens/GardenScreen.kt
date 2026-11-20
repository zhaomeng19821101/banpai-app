package com.banpai.app.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.banpai.app.data.SnowLetterRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 公共花园：发帖 + 雪夜传信。
 * 发帖：本机选照片（真能选，系统相册）/ 直接拍照（CameraX 下一步），
 * 拍完保存 / 发花园 / 重拍；发完的照片出现在花园顶部，标“刚刚”。
 * 雪夜传信：放进飘雪、100m 内捡、24h 化掉（写一封会出现在花园顶部）。
 * 现在发帖只存本地（下一步接后端接口）；直接拍照还是占位。
 */
@Composable
fun GardenScreen() {
    val context = LocalContext.current
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var feed by remember { mutableStateOf<List<String>>(emptyList()) }
    var hint by remember { mutableStateOf("还没有帖子") }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedUri = uri
            hint = "已选一张本机照片，可以保存或发到花园"
        }
    }

    // 本机选的照片解码显示（IO 线程解码）。
    val pickedBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, pickedUri) {
        value = pickedUri?.let { uri ->
            withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("公共花园", "发帖 · 雪夜传信")
        Text(hint)
        Spacer(modifier = Modifier.height(8.dp))
        // 选中的照片预览。
        pickedBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "选中的照片",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        GoldButton("本机选照片", onClick = {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        })
        GoldButton("直接拍照（CameraX 下一步）", onClick = { hint = "直接拍照还没接 CameraX，先用本机选照片" })
        GoldButton("保存", onClick = {
            if (pickedUri != null) hint = "已保存到本机（本地假数据）" else hint = "先选一张照片"
        })
        GoldButton("发到花园", onClick = {
            if (pickedUri != null) {
                feed = listOf("刚刚 · 你发了一张照片到花园") + feed
                pickedUri = null
                hint = "已发到花园，出现在最上面"
            } else {
                hint = "先选一张照片"
            }
        })
        GoldButton("重拍", onClick = {
            pickedUri = null
            hint = "还没有帖子"
        })
        Spacer(modifier = Modifier.height(16.dp))
        InfoCard("雪夜传信：把信放进飘雪，${SnowLetterRules.PICKUP_RADIUS_M}m 内可以捡，" +
            "${SnowLetterRules.EXPIRE_HOURS}h 后化掉。")
        GoldButton("写一封雪夜传信", onClick = {
            feed = listOf("刚刚 · 你把一封雪夜传信放进了飘雪（100m 内可捡，24h 后化掉）") + feed
            hint = "信已放进飘雪，等路过的人捡到它"
        })
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(feed) { post ->
                Text(post, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}
