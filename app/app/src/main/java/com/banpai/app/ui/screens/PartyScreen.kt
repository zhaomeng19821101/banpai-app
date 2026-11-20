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
import androidx.compose.ui.unit.dp
import com.banpai.app.data.FRIENDS
import com.banpai.app.data.PARTY_ROOMS

/**
 * 派对 / 好友页：房间列表 + 好友一对一。
 * 语音：Agora 接入点占位（AGORA_APP_ID 在 local.properties）；
 * 按住说话 + 打字键盘切换 + 表情包占位。
 * 吧唧上可直接进房 / 接受好友邀请（BLE 占位，见 BleManager）。
 */
@Composable
fun PartyScreen() {
    var holdToTalk by remember { mutableStateOf(false) }
    var useKeyboard by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("派对", "房间列表 · 好友一对一 · Agora 语音占位")
        Text("房间：")
        PARTY_ROOMS.forEach { room ->
            InfoCard("${room.name} · ${room.members} 人在线")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("好友：")
        FRIENDS.forEach { friend ->
            InfoCard(friend.name)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { holdToTalk = !holdToTalk },
                modifier = Modifier.weight(1f)
            ) { Text(if (holdToTalk) "松开结束说话" else "按住说话（占位）") }
            OutlinedButton(
                onClick = { useKeyboard = !useKeyboard },
                modifier = Modifier.weight(1f)
            ) { Text(if (useKeyboard) "切换到语音" else "切换到打字键盘") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("表情包占位。吧唧上可直接进房 / 接受邀请。")
    }
}
