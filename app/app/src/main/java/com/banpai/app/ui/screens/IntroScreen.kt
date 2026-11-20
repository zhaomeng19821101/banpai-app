package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.introCardAsset
import com.banpai.app.ui.media.AssetImage

/**
 * 认识半拍：4 卡 swipeable intro（可跳过，首次之后不再出现，我的·关于半拍可重看）。
 * 四瓣定版：冰、雪、乐、花；静留给第五瓣（它未来会静静地看那封信）。
 * 半拍是哈尔滨一年四季孕育出来的精灵（不只是雪夜）。
 */
data class IntroCard(val title: String, val body: String, val line: String)

private val INTRO_CARDS = listOf(
    IntroCard("你好呀，我是半拍", "哈尔滨一年四季孕育出来的小精灵。", "你好呀，我是半拍。"),
    IntroCard("四片花瓣", "冰是眼睛看到的，雪是身体感受到的，乐是哈尔滨的音乐，花是开满夏天的丁香。", "四片花瓣是冰、雪、乐、花。"),
    IntroCard("第五瓣丢了", "我一直在找它。静，留给第五瓣。", "第五瓣丢了，我一直在找。"),
    IntroCard("跟我一起走吧", "慢慢走，慢慢听，哈尔滨会把歌唱给你。", "跟我一起走吧。")
)

@Composable
fun IntroScreen(onDone: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("跳过") }
        }
        ScreenHeader("认识半拍", "4 张卡片，滑一滑认识我（可跳过）")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(INTRO_CARDS.withIndex().toList()) { (index, card) ->
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    AssetImage(
                        assetPath = introCardAsset(index + 1),
                        contentDescription = card.title,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoCard("${card.title}\n${card.body}\n「${card.line}」")
                }
            }
        }
        Text("第 1 张起：半拍档案在图鉴第一页", modifier = Modifier.padding(16.dp))
        GoldButton("开始", onClick = onDone)
        Spacer(modifier = Modifier.height(8.dp))
    }
}
