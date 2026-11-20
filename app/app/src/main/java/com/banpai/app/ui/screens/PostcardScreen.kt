package com.banpai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.banpai.app.data.POSTCARDS
import com.banpai.app.data.POSTER_COUNT
import com.banpai.app.data.postcardAsset
import com.banpai.app.data.posterAsset
import com.banpai.app.ui.media.AssetImage

/**
 * 明信片页：24 张画廊 + 分享海报 4 张，都是真图；点一张放大看。
 * 明信片由打卡点位生成；寄给朋友走微信分享（微信好友 / 朋友圈 / 保存到相册，
 * 真机接微信 Open SDK；分享或保存成功算寄出，+5 能量，每天每张限 1 次）。
 */
@Composable
fun PostcardScreen() {
    var enlarged by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("明信片", "24 张画廊 · 分享海报 $POSTER_COUNT 张")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(POSTCARDS) { card ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { enlarged = postcardAsset(card.id) }
                ) {
                    AssetImage(
                        assetPath = postcardAsset(card.id),
                        contentDescription = "明信片 ${card.id} 号 · ${card.place}",
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("明信片 ${card.id} 号 · ${card.place}（点开放大）")
                }
            }
            items((1..POSTER_COUNT).toList()) { i ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { enlarged = posterAsset(i) }
                ) {
                    AssetImage(
                        assetPath = posterAsset(i),
                        contentDescription = "分享海报 $i 号",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("分享海报 $i 号（点开放大）")
                }
            }
        }
        if (hint.isNotEmpty()) Text(hint, modifier = Modifier.padding(horizontal = 16.dp))
        GoldButton("寄给朋友（微信分享，下一步接 Open SDK）", onClick = {
            hint = "微信分享还没接 Open SDK；接好后：微信好友 / 朋友圈 / 保存到相册，寄出 +5 能量"
        })
    }

    // 放大看：全屏图。
    enlarged?.let { asset ->
        Dialog(onDismissRequest = { enlarged = null }) {
            Box(modifier = Modifier.fillMaxSize()) {
                AssetImage(
                    assetPath = asset,
                    contentDescription = "放大",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                TextButton(
                    onClick = { enlarged = null },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) { Text("×") }
            }
        }
    }
}
