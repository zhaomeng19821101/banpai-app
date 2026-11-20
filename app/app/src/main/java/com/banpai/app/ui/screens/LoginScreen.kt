package com.banpai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 登录页：手机号 + 短信码 / 微信关联 / 先逛逛。
 * 演示版不真发短信、不真调微信；登录后进入首页。
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onBrowseFirst: () -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("登录", "手机号 + 短信码，或微信关联，也可以先逛逛")
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("手机号") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("短信验证码") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { codeSent = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (codeSent) "已发送（演示：60s 后可重发）" else "获取验证码")
        }
        Spacer(modifier = Modifier.height(16.dp))
        GoldButton("登录 / 注册", onClick = onLoggedIn)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onLoggedIn, modifier = Modifier.fillMaxWidth()) {
            Text("微信关联登录（占位）")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onBrowseFirst, modifier = Modifier.fillMaxWidth()) {
            Text("暂不登录，先逛逛")
        }
    }
}
