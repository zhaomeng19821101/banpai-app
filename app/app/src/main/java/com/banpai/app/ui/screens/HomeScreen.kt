package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.BANPAI_STANDARD
import com.banpai.app.data.FAKE_USER
import com.banpai.app.data.MINI_GAME_NOTE
import com.banpai.app.data.SPROUT_REWARD_OUTFIT
import com.banpai.app.data.SPROUT_WATERS_TO_BLOOM
import com.banpai.app.data.SproutState
import com.banpai.app.ui.media.AssetImage

/**
 * 首页：定版一屏不下拉；旅行连胜已删。
 * 英雄卡（半拍标准图）+ 半拍问候 + 快捷入口；小游戏只在吧唧上，首页只留一句话说明。
 * 一起种芽在英雄卡下：每天浇水 1 次，累计 7 次开花，领限定装扮「芽开·丁香」（奖励型，2026-11-20 定）。
 */
@Composable
fun HomeScreen(
    onOpenExplore: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onOpenAtlas: () -> Unit,
    onOpenDressUp: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenWatchface: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenParty: () -> Unit,
    onOpenGarden: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenPostcard: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("你好呀，今天也要慢慢走哦。")
        Spacer(modifier = Modifier.height(8.dp))
        // 英雄卡：半拍标准图（正好 4 瓣）。
        AssetImage(
            assetPath = BANPAI_STANDARD,
            contentDescription = "半拍",
            modifier = Modifier.fillMaxWidth().height(190.dp),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("${FAKE_USER.name} · Lv.${FAKE_USER.level} ${FAKE_USER.levelName} · 能量 ${FAKE_USER.energy}")
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenExplore, modifier = Modifier.weight(1f)) { Text("去探索") }
            OutlinedButton(onClick = onOpenCheckIn, modifier = Modifier.weight(1f)) { Text("去打卡") }
            OutlinedButton(onClick = onOpenAtlas, modifier = Modifier.weight(1f)) { Text("图鉴") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenDressUp, modifier = Modifier.weight(1f)) { Text("装扮") }
            OutlinedButton(onClick = onOpenCamera, modifier = Modifier.weight(1f)) { Text("拍照") }
            OutlinedButton(onClick = onOpenWatchface, modifier = Modifier.weight(1f)) { Text("表盘") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenChat, modifier = Modifier.weight(1f)) { Text("花语") }
            OutlinedButton(onClick = onOpenParty, modifier = Modifier.weight(1f)) { Text("派对") }
            OutlinedButton(onClick = onOpenGarden, modifier = Modifier.weight(1f)) { Text("花园") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onOpenDiary, modifier = Modifier.weight(1f)) { Text("日记") }
            OutlinedButton(onClick = onOpenPostcard, modifier = Modifier.weight(1f)) { Text("明信片") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(MINI_GAME_NOTE)
        Spacer(modifier = Modifier.height(12.dp))
        // 一起种芽：浇水 → 开花 → 领限定装扮（奖励型，2026-11-20 定）。
        // 本地假数据状态；联调后改为 ApiClient.sprout() / water() / claim()。
        var sprout by remember { mutableStateOf(SproutState()) }
        Text("一起种芽")
        Text("已浇水 ${sprout.waters}/$SPROUT_WATERS_TO_BLOOM · 每天浇 1 次，浇满 7 次开花")
        if (!sprout.bloomed) {
            OutlinedButton(
                onClick = {
                    val waters = sprout.waters + 1
                    sprout = sprout.copy(
                        waters = waters,
                        wateredToday = true,
                        bloomed = waters >= SPROUT_WATERS_TO_BLOOM
                    )
                },
                enabled = !sprout.wateredToday,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (sprout.wateredToday) "今天已经浇过水啦" else "浇水") }
        } else if (!sprout.claimed) {
            Text("开花了！丁香开了，快领你的限定装扮。")
            GoldButton("领限定装扮「$SPROUT_REWARD_OUTFIT」") {
                sprout = sprout.copy(claimed = true)
            }
        } else {
            Text("已领取「$SPROUT_REWARD_OUTFIT」，去装扮看看吧。")
            OutlinedButton(onClick = onOpenDressUp, modifier = Modifier.fillMaxWidth()) { Text("去装扮") }
        }
    }
}
