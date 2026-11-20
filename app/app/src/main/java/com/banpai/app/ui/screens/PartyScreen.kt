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
import com.banpai.app.data.BANPAI_EXPRESSIONS
import com.banpai.app.data.FRIENDS
import com.banpai.app.data.Friend
import com.banpai.app.data.PARTY_ROOMS
import com.banpai.app.ui.media.AssetImage

/**
 * 派对 / 好友页：房间列表 + 好友一对一（房间 / 好友切换）。
 * 语音：派对房间只用异步语音消息（录一段、发过去、对方方便时听），
 * 不用声网实时语音——公共区域陌生人实时语音有合规风险（2026-11-20 用户锁定）。
 * 好友 1:1 实时对讲在 FriendChatScreen 的好友对讲控件里（打字 / 语音消息 / 实时对讲）。
 * 吧唧上可直接进房 / 接受好友邀请（BLE 占位，见 BleManager）。
 * 点好友名进 1:1 聊天页。
 */
@Composable
fun PartyScreen(onOpenFriendChat: (Friend) -> Unit = {}) {
    var holdToTalk by remember { mutableStateOf(false) }
    var useKeyboard by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf("房间") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("派对", "房间列表 · 好友一对一 · 异步语音消息")
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { tab = "房间" }, modifier = Modifier.weight(1f)) { Text("房间") }
            OutlinedButton(onClick = { tab = "好友" }, modifier = Modifier.weight(1f)) { Text("好友") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (tab == "房间") {
            PARTY_ROOMS.forEach { room ->
                InfoCard("${room.name} · ${room.members} 人在线")
            }
        } else {
            FRIENDS.forEach { friend ->
                OutlinedButton(
                    onClick = { onOpenFriendChat(friend) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("和 ${friend.name} 聊天") }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { holdToTalk = !holdToTalk },
                modifier = Modifier.weight(1f)
            ) { Text(if (holdToTalk) "松开发送语音消息" else "按住录语音消息（占位）") }
            OutlinedButton(
                onClick = { useKeyboard = !useKeyboard },
                modifier = Modifier.weight(1f)
            ) { Text(if (useKeyboard) "切换到语音" else "切换到打字键盘") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // 表情包：半拍 9 格表情真图。
        AssetImage(
            assetPath = BANPAI_EXPRESSIONS,
            contentDescription = "半拍表情（9 格）",
            modifier = Modifier.fillMaxWidth().height(120.dp),
            contentScale = ContentScale.Crop
        )
        Text("表情包：半拍表情（9 格）+ 守护宠。吧唧上可直接进房 / 接受邀请。")
    }
}
