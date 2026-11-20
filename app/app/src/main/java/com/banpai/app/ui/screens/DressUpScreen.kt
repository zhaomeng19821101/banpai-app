package com.banpai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.banpai.app.data.DRESSUP_SLOTS
import com.banpai.app.data.OUTFITS
import com.banpai.app.data.outfitAsset
import com.banpai.app.ui.media.AssetImage

/**
 * 装扮页：3 槽 — 服装/套装、背景、特效。配饰已去掉；服装 + 套装合并为一槽。
 * 16 套装扮 + 1 种芽限定「芽开·丁香」（17 号，种芽开花后领取，不售卖，锁定态）。
 * 成套换装，不做单件配饰。点一张图选上它，再点「穿上这套」换装。
 */
@Composable
fun DressUpScreen() {
    var slot by remember { mutableStateOf(DRESSUP_SLOTS[0]) }
    var picked by remember { mutableStateOf(OUTFITS[0]) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("装扮", "3 槽：服装/套装 · 背景 · 特效（配饰已去掉）")
        Text("当前槽：$slot　已选：${picked.name}")
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(DRESSUP_SLOTS) { s ->
                InfoCard("槽位：$s${if (s == slot) "（当前）" else ""}")
            }
            items(OUTFITS) { outfit ->
                Column(
                    modifier = Modifier.fillMaxWidth().clickable { picked = outfit }
                ) {
                    AssetImage(
                        assetPath = outfitAsset(outfit.id),
                        contentDescription = outfit.name,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "套装：${outfit.name}" +
                            if (outfit.locked) " 🔒 种芽开花后领取（不售卖）"
                            else if (outfit == picked) "（已选）" else ""
                    )
                }
            }
        }
        if (picked.locked) {
            Text("这套还没开花，领不到哦——先去首页种芽，浇满 7 次就开花啦。")
        } else {
            GoldButton("穿上「${picked.name}」", onClick = { /* 已穿上：本地假数据，联调后写 ApiClient */ })
        }
    }
}
