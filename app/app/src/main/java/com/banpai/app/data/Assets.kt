package com.banpai.app.data

/**
 * 素材映射：FakeRepository 的每条数据对应 app/src/main/assets/ 里的一个文件。
 * 文件由 tools/stage_assets.py 从 ~/workspace/banpai/assets/ 定版素材拷入并改名
 * （见根目录 assets-manifest.json）；编号与 FakeRepository 的 id 一一对应。
 *
 * 四瓣红线：半拍主图已目检——标准图正好 4 瓣，金瓣隐藏款正好 5 瓣（一金 + 四冰紫）。
 * 食物 31 种全部有图（2026-11-22 补齐：11 张缺图 + 冻柿子独立图 +
 * 俄式罐焖牛肉、松仁小肚、熏鸡重画），foodAsset() 对 1–31 号一律返回文件路径。
 */

// ---------- 半拍主图 / 欢迎 / 彩蛋 ----------
const val BANPAI_STANDARD = "banpai/standard-front.webp" // 正好 4 瓣
const val BANPAI_EXPRESSIONS = "banpai/expressions.webp" // 9 格表情
const val BANPAI_ACTIONS = "banpai/actions.webp"         // 6 格动作
const val BANPAI_GOLD_HIDDEN = "banpai/gold-hidden.webp" // 正好 5 瓣（一金 + 四冰紫）
const val BANPAI_MINI_Q = "banpai/mini-q.webp"
const val WELCOME_VIDEO = "welcome/welcome.mp4"
const val EASTER_LETTER = "easter-egg/letter.png" // 第五瓣的信原图
const val EASTER_EMBLEM_GOLD = "easter-egg/emblem-gold.webp"
const val EASTER_VIDEO = "easter-egg/fifth-petal-video.mp4" // 彩蛋视频 v2（流程只出信，视频备用）

fun introCardAsset(index1to4: Int): String = "intro/%02d.webp".format(index1to4)

// ---------- 表盘 30 ----------
fun watchfaceAsset(id: Int): String = "watchfaces/%02d.webp".format(id)

// ---------- 装扮 17（17 芽开·丁香复用 05 春日丁香图） ----------
fun outfitAsset(id: Int): String = "outfits/%02d.webp".format(id)

// ---------- 守护宠卡片 / 视频（顺序与 GUARDIAN_PETS 一致） ----------
fun guardianCardAsset(index0to23: Int): String = "guardian-cards/%02d.webp".format(index0to23 + 1)
fun guardianVideoAsset(index0to23: Int): String = "guardian-videos/%02d.mp4".format(index0to23 + 1)

// ---------- 食物（31 种全部有图） ----------
// staged 文件名就是食物 id：food/01.webp = 食物 1 号马迭尔冰棍，以此类推。
private val FOOD_WITH_IMAGE = setOf(
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
    21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31
)

fun foodAsset(foodId: Int): String? =
    if (foodId in FOOD_WITH_IMAGE) "food/%02d.webp".format(foodId) else null

// ---------- 明信片 24 + 海报 4 ----------
fun postcardAsset(id: Int): String = "postcards/%02d.webp".format(id)
fun posterAsset(index1to4: Int): String = "posters/%02d.webp".format(index1to4)

// ---------- 相框：竖 20 / 横 20 ----------
fun frameVerticalAsset(id: Int): String = "frames/vertical/%02d.png".format(id)
fun frameHorizontalAsset(id: Int): String = "frames/horizontal/%02d.png".format(id)

// ---------- 相机风格 8 + 表情库 + 半身像框 5 ----------
fun cameraStyleAsset(index1to8: Int): String = "camera-styles/style-%02d.jpg".format(index1to8)
const val CAMERA_STYLE_BASE = "camera-styles/base.jpg"
const val CAMERA_EXPRESSIONS = "camera-styles/expressions.webp" // 6 表情库 v3（正好 4 瓣）
fun halfBodyAsset(index1to5: Int): String = "camera-styles/halfbody-%02d.webp".format(index1to5)

// ---------- 绿幕乱入：3 种入场（探头 v7 色彩校正版 / 挥手 / 惊喜跳入） ----------
object Cameo {
    const val PEEK = "greenscreen/di-peek-v7.mp4" // 探头（色彩校正 v7）
    const val WAVE = "greenscreen/wave.mp4"       // 挥手
    const val JUMP = "greenscreen/jump.mp4"       // 惊喜跳入
}

fun greenscreenAsset(slug: String): String = "greenscreen/$slug.mp4"
