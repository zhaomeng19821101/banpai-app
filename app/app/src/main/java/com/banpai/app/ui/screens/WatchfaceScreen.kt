package com.banpai.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
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
 * 表盘工坊：30 款圆形表盘真图；时间模板 8 种可点选（见 TIME_TEMPLATES，
 * 与时间模板页的 8 种一致：大字上下排、横排大字、细体大数字、时间加星期日期、
 * 极简小字、底部时间、带秒数、胶囊底衬）。
 * 圆形表盘构图，无数字无文字（时间由系统按模板自动排布）；
 * 每款姿势 / 表情不同；24 号金瓣隐藏款锁定（第五瓣信件解锁前不能选）。
 * 大预览里的时间是真走字（每秒刷新）。表盘自带的 5 种时钟样式仍保留，
 * 选表盘时模板默认跟它的时钟样式走，之后可自由切换 8 种模板。
 */
private val TIME_TEMPLATES = listOf(
    "大字上下排", "横排大字", "细体大数字", "时间加星期日期",
    "极简小字", "底部时间", "带秒数", "胶囊底衬"
)

@Composable
fun WatchfaceScreen() {
    var picked by remember { mutableStateOf(WATCHFACES[0]) }
    var template by remember { mutableStateOf(WATCHFACES[0].clockStyle) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = LocalDateTime.now()
        }
    }
    // 模板决定时间的排布：位置、字号、是否带秒、是否胶囊底衬。
    val showSeconds = template == 6
    val timeText = if (showSeconds) {
        now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    } else {
        now.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    val dateText = now.format(DateTimeFormatter.ofPattern("M月d日 EEEE"))
    val showDate = template == 0 || template == 3
    val timeAlign = when (template) {
        0, 3, 4 -> Alignment.TopCenter
        5, 7 -> Alignment.BottomCenter
        else -> Alignment.Center
    }
    val timeSize = when (template) {
        4 -> 24.sp
        6 -> 36.sp
        else -> 40.sp
    }
    val timeWeight = if (template == 2) FontWeight.Light else FontWeight.Normal
    val capsule = template == 7

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("表盘工坊", "30 款 · 时间模板 8 种（点下面切换）")
        Text("当前表盘：${picked.id} 号 ${picked.name} · 模板：${TIME_TEMPLATES[template]}")
        // 时间模板选择器（8 种）。
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            items(TIME_TEMPLATES.withIndex().toList()) { (index, name) ->
                OutlinedButton(
                    onClick = { template = index },
                    modifier = Modifier.padding(end = 8.dp)
                ) { Text(name) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 大预览：真表盘图 + 按模板排布的走字时间。
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                if (capsule) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(timeText, color = Color.White, fontSize = timeSize, fontWeight = timeWeight)
                    }
                } else {
                    Text(timeText, color = Color.White, fontSize = timeSize, fontWeight = timeWeight)
                }
                if (showDate) Text(dateText, color = Color.White, fontSize = 14.sp)
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
                                template = face.clockStyle
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
