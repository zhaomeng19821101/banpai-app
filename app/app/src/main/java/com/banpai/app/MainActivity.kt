package com.banpai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.banpai.app.ui.navigation.BanpaiNavGraph
import com.banpai.app.ui.theme.BanpaiTheme
import com.banpai.app.ui.theme.ThemeMode

/**
 * 半拍 Banpai — 入口 Activity（全新工程，2026-11-20）。
 * 导航：欢迎 → 认识半拍 → 登录 → 首页（底部 5 Tab，打卡为中间暖金凸起按钮）。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // 昼夜三档：白天 / 夜晚 / 跟随系统（默认跟随系统；设置页可切换，见 ProfileScreen）。
            var themeMode by remember { mutableStateOf(ThemeMode.System) }
            BanpaiTheme(themeMode = themeMode) {
                BanpaiNavGraph(
                    onThemeModeChange = { themeMode = it }
                )
            }
        }
    }
}
