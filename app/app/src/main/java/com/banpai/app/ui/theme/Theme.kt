package com.banpai.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 半拍 Banpai — 昼夜三档主题。
 * 白天：明亮丁香光；夜晚：雪夜灯火，提亮不压抑（不是纯黑压抑夜）。
 * 暖金 #F5C97B 是唯一行动色；冰蓝 #7FC4E8；丁香 #C9BDF0。
 * 四瓣：冰、雪、乐、花；静留给第五瓣（金）。
 */
object BanpaiColors {
    val WarmGold = Color(0xFFF5C97B)   // 唯一行动色：主按钮 / 打卡凸起 / 已点亮
    val IceBlue = Color(0xFF7FC4E8)    // 冰蓝：冰瓣 / 水面 / 次要强调
    val Lilac = Color(0xFFC9BDF0)      // 丁香：丁香光 / 选中态
    val GoldPetal = Color(0xFFE8B64C)  // 第五瓣金（隐藏款）
    val SnowWhite = Color(0xFFF7F5FF)
    val InkNight = Color(0xFF1B2340)   // 夜晚底色：深蓝紫，非纯黑
}

enum class ThemeMode { Day, Night, System }

private val DayScheme: ColorScheme = lightColorScheme(
    primary = BanpaiColors.WarmGold,
    onPrimary = Color(0xFF3A2A12),
    secondary = BanpaiColors.IceBlue,
    tertiary = BanpaiColors.Lilac,
    background = Color(0xFFF6F1FF),      // 明亮丁香光
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF241E33),
    onSurface = Color(0xFF241E33)
)

private val NightScheme: ColorScheme = darkColorScheme(
    primary = BanpaiColors.WarmGold,
    onPrimary = Color(0xFF3A2A12),
    secondary = BanpaiColors.IceBlue,
    tertiary = BanpaiColors.Lilac,
    background = Color(0xFF232B4A),      // 雪夜灯火：提亮的深蓝紫，不压抑
    surface = Color(0xFF2E3858),
    onBackground = Color(0xFFF2ECFF),
    onSurface = Color(0xFFF2ECFF)
)

@Composable
fun BanpaiTheme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.Day -> false
        ThemeMode.Night -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) NightScheme else DayScheme,
        content = content
    )
}
