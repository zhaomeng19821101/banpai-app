package com.banpai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banpai.app.data.CLOCK_STYLE_NAMES
import com.banpai.app.data.WATCHFACES
import com.banpai.app.data.watchfaceAsset
import com.banpai.app.ui.media.AssetImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/**
 * 表盘工坊：30 款圆形表盘真图；时间按款式自动选位置（5 种时钟样式占位描述，
 * 真机按款式布局，不写死坐标；v3 的 8 种时间模板下一步再对齐）。
 * 圆形表盘构图，无数字无文字（时间由系统按样式自动排布）；
 * 每款姿势 / 表情不同；24 号金瓣隐藏款锁定（第五瓣信件解锁前不能选）。
 * 大预览里的时间是真走字（每秒刷新）。
 */
@Composable
fun WatchfaceScreen() {
    var picked by remember { mutableStateOf(WATCHFACES[0]) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
        }
    }
    val timeText = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val dateText = now.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
    val timeAlign = when (picked.clockStyle) {
        0 -> Alignment.TopCenter
        1 -> Alignment.BottomCenter
        2 -> Alignment.TopStart
        3 -> Alignment.BottomEnd
        else -> Alignment.Center
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("表盘工坊", "30 款 · 时钟样式 ${CLOCK_STYLE_NAMES[picked.clockStyle]}（时间按款式自动选位置）")
        Text("当前表盘：${picked.id} 号 ${picked.name} · 时钟样式 ${picked.clockStyle}（${CLOCK_STYLE_NAMES[picked.clockStyle]}）")
        // 大预览：真表盘图 + 按款式位置走字的时间。
        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 16.dp),
            contentAlignment = timeAlign
        ) {
            AssetImage(
                assetPath = watchfaceAsset(picked.id),
                contentDescription = picked.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(timeText, color = Color.White, fontSize = 40.sp)
                Text(dateText, color = Color.White, fontSize = 14.sp)
            }
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(WATCHFACES) { face ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            if (face.locked) {
                                // 锁定态：点一下只提示，不切换。
                            } else {
                                picked = face
                            }
                        }
                ) {
                    AssetImage(
                        assetPath = watchfaceAsset(face.id),
                        contentDescription = face.name,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${face.id} 号 ${face.name} · 时钟样式 ${face.clockStyle}${if (face == picked) "（已选）" else ""}")
                        if (face.locked) Text("🔒 金瓣隐藏款：找到第五瓣的信才能解锁")
                    }
                }
            }
        }
        GoldButton("同步到吧唧（BLE DIAL 占位）", onClick = { /* TODO BleManager.writeDial(picked.id.toString()) */ })
    }
}
