package com.banpai.app.data

/**
 * 半拍 Banpai — 假数据层（FakeRepository）。
 * 后端未联调前的本地假数据；联调后逐步替换为 ApiClient。
 *
 * 锁死口径：
 * - 守护宠 24 只（不断扩充，不写死总数；此处先列 24 只）
 * - 表盘工坊 30 款；时钟样式 5 种，时间按款式自动选位置
 * - 装扮 3 槽：服装/套装、背景、特效（配饰已去掉）；16 套装扮 + 1 种芽限定「芽开·丁香」
 *   （奖励型，2026-11-20 Jackson 定：种芽开花后送限定装扮；不售卖）
 * - 好吃团 30 种 + 隐藏雪墩
 * - 四瓣：冰、雪、乐、花；静留给第五瓣
 * - 日记纯文字、不读出声；2026-11-01 起每天一篇补到 11-19，共 19 篇；
 *   每篇结尾锁定台词 #14「晚安，明天见。」；11-01 是半拍生日，也是用户生日
 * - 花语只用 15 句锁定台词做 lookup；不会就说「我还小，帮你去打听」
 * - 打卡：首次 +80、再访 +20、每点每天 1 次、拍照 +10、乱入 +5/天
 */

// ---------- 锁定台词（15 句，只做 lookup，不改写） ----------
data class LockedLine(val id: Int, val text: String)

val LOCKED_LINES: List<LockedLine> = listOf(
    LockedLine(1, "嗯，我在听呀，慢慢说。"),
    LockedLine(2, "等一下，我在听呀。"),
    LockedLine(3, "听到了，是风的声音。"),
    LockedLine(4, "我就在这儿，慢慢找第五瓣。"),
    LockedLine(5, "你好呀，我是半拍。"),
    LockedLine(6, "你好呀，今天也要慢慢走哦。"),
    LockedLine(7, "打卡成功啦，这里真好听。"),
    LockedLine(8, "哇，是音符，里面会是什么呢？"),
    LockedLine(9, "这件衣服好漂亮，我喜欢。"),
    LockedLine(10, "好冷呀，不过我喜欢雪。"),
    LockedLine(11, "春天来啦，丁香都开啦。"),
    LockedLine(12, "夏天到啦，江边风好凉快。"),
    LockedLine(13, "秋天来啦，叶子都黄啦。"),
    LockedLine(14, "晚安，明天见。"),
    LockedLine(15, "谢谢你，陪我一起听。")
)

// ---------- 守护宠 24 只（不断扩充，不写死总数） ----------
data class GuardianPet(
    val place: String,
    val name: String,
    val sound: String, // 动物叫声 / 可爱拟声，不说中文台词
    val line: String   // 卡片文字行（只显示，不朗读）
) {
    val title: String get() = "$place·$name" // 卡片标题只放地点+名字，干净
}

val GUARDIAN_PETS: List<GuardianPet> = listOf(
    GuardianPet("中央大街", "手风琴", "呜~", "手风琴一拉，整条街都慢下来。"),
    GuardianPet("索菲亚", "钟", "当~", "我慢，但我准。"),
    GuardianPet("松花江", "冰裂", "咔嚓~", "江面裂开一点点，春天就漏进来了。"),
    GuardianPet("老会堂", "弦乐", "啦~", "琴弦一响，老会堂的灯都亮了。"),
    GuardianPet("大剧院", "合唱", "啊~", "大家一起唱，声音能飘到江对岸。"),
    GuardianPet("老道外", "叫卖", "吆~", "老街的吆喝声，是哈尔滨的闹钟。"),
    GuardianPet("极地公园", "企鹅", "嘎~", "企鹅走路摇摇晃晃，但从不摔跤。"),
    GuardianPet("虎林园", "呼噜", "呼噜~", "老虎打呼噜，整个园子都震三震。"),
    GuardianPet("太阳岛", "口哨", "嘘~", "口哨一吹，太阳岛的风就跟上了。"),
    GuardianPet("防洪纪念塔", "水声", "哗~", "江水拍着塔基，数着哈尔滨的年岁。"),
    GuardianPet("马迭尔", "咬冰棍", "咔滋~", "冰棍要咬着吃，甜味才跑得快。"),
    GuardianPet("冰雪大世界", "凿冰", "叮~", "一凿子下去，冰就亮了。"),
    GuardianPet("伏尔加庄园", "巴拉莱卡琴", "噔~", "毛茸茸的小家伙抱着琴，眼睛笑成弯弯。"),
    GuardianPet("果戈里大街", "书页", "沙沙~", "书页翻动的声音，是果戈里大街的晚安曲。"),
    GuardianPet("斯大林公园", "江风", "呼~", "江风从斯大林公园吹来，带着雪的味道。"),
    GuardianPet("中东铁路桥", "火车汽笛", "呜呜~", "火车一过桥，汽笛能传好远好远。"),
    GuardianPet("兆麟公园", "冰灯", "叮咚~", "冰灯一亮，兆麟公园就成了星星的家。"),
    GuardianPet("哈尔滨音乐厅", "管风琴", "呜~", "管风琴一响，整个音乐厅都在震动。"),
    GuardianPet("龙塔", "瞭望", "哇~", "站得高，看得远，哈尔滨全在脚下。"),
    GuardianPet("俄罗斯风情小镇", "套娃", "呀~", "套娃一层一层，藏着好多惊喜。"),
    GuardianPet("亚布力滑雪场", "滑雪板", "嗖~", "滑雪板一滑，雪就飞起来啦。"),
    GuardianPet("雪乡", "雪蘑菇", "噗~", "雪蘑菇胖乎乎的，软得像棉花糖。"),
    GuardianPet("音乐长廊", "小提琴", "吱~", "小提琴一拉，长廊里的灯都跟着晃。"),
    GuardianPet("哈尔滨啤酒博物馆", "气泡", "啵~", "气泡一个一个往上冒，像在开派对。")
)

// ---------- 表盘工坊 30 款；时钟样式 5 种，时间按款式自动选位置 ----------
data class Watchface(val id: Int, val name: String, val clockStyle: Int) // clockStyle 0..4

val WATCHFACES: List<Watchface> = listOf(
    Watchface(1, "冰蓝日常", 0),
    Watchface(2, "雪夜", 1),
    Watchface(3, "春日丁香", 2),
    Watchface(4, "极光之夜", 3),
    Watchface(5, "金色晨光", 4),
    Watchface(6, "雪夜红梅", 1),
    Watchface(7, "夏花", 2),
    Watchface(8, "雪乡红棉袄", 0),
    Watchface(9, "冰晶礼服", 3),
    Watchface(10, "音乐会礼服", 4),
    Watchface(11, "滑雪运动装", 0),
    Watchface(12, "冰灯夜游装", 1),
    Watchface(13, "俄式毛皮冬装", 2),
    Watchface(14, "巴洛克舞会礼服", 3),
    Watchface(15, "夏日江畔装", 4),
    Watchface(16, "秋日枫叶装", 0),
    Watchface(17, "中央大街", 1),
    Watchface(18, "索菲亚", 2),
    Watchface(19, "松花江", 3),
    Watchface(20, "太阳岛", 4),
    Watchface(21, "冰雪大世界", 0),
    Watchface(22, "伏尔加庄园", 1),
    Watchface(23, "雪夜暖绒", 2),
    Watchface(24, "金瓣隐藏", 3), // 第五瓣金瓣隐藏款
    Watchface(25, "迷你Q版", 4),
    Watchface(26, "挥手", 0),
    Watchface(27, "比心", 1),
    Watchface(28, "晚安", 2),
    Watchface(29, "打卡成功", 3),
    Watchface(30, "听一听", 4)
)

// 时钟样式 0..4：时间按款式自动选位置（占位描述，真机按款式布局，不写死坐标）。
val CLOCK_STYLE_NAMES: List<String> = listOf("顶部居中", "底部居中", "左上", "右下", "环绕表盘")

// ---------- 装扮 3 槽：服装/套装、背景、特效（配饰已去掉）；16 套 ----------
data class Outfit(val id: Int, val name: String, val slot: String = "服装/套装")

val OUTFITS: List<Outfit> = listOf(
    Outfit(1, "冰蓝日常"),
    Outfit(2, "冰晶礼服"),
    Outfit(3, "夏花裙"),
    Outfit(4, "雪夜暖绒"),
    Outfit(5, "春日丁香"),
    Outfit(6, "极光之夜"),
    Outfit(7, "金色晨光"),
    Outfit(8, "雪夜红梅"),
    Outfit(9, "俄式毛皮冬装"),
    Outfit(10, "巴洛克舞会礼服"),
    Outfit(11, "冰灯夜游装"),
    Outfit(12, "滑雪运动装"),
    Outfit(13, "雪乡红棉袄"),
    Outfit(14, "音乐会礼服"),
    Outfit(15, "夏日江畔装"),
    Outfit(16, "秋日枫叶装"),
    Outfit(17, "芽开·丁香") // 种芽限定：不售卖，只能开花后领取（奖励型，2026-11-20 定）
)

val DRESSUP_SLOTS: List<String> = listOf("服装/套装", "背景", "特效") // 3 槽，配饰去掉

// ---------- 好吃团 30 种 + 隐藏雪墩 ----------
data class Food(val id: Int, val name: String, val hidden: Boolean = false)

val FOODS: List<Food> = listOf(
    Food(1, "马迭尔冰棍"),
    Food(2, "秋林红肠"),
    Food(3, "秋林大列巴"),
    Food(4, "锅包肉"),
    Food(5, "杀猪菜"),
    Food(6, "地三鲜"),
    Food(7, "冰糖葫芦"),
    Food(8, "粘豆包"),
    Food(9, "哈尔滨啤酒"),
    Food(10, "格瓦斯"),
    Food(11, "俄式罐焖牛肉"),
    Food(12, "红菜汤"),
    Food(13, "列巴"),
    Food(14, "扒鸡"),
    Food(15, "酱骨头"),
    Food(16, "酸菜白肉血肠"),
    Food(17, "铁锅炖大鹅"),
    Food(18, "冻梨"),
    Food(19, "冻柿子"),
    Food(20, "烤冷面"),
    Food(21, "烤串"),
    Food(22, "饺子"),
    Food(23, "拉面"),
    Food(24, "朝鲜冷面"),
    Food(25, "粘玉米"),
    Food(26, "蓝莓"),
    Food(27, "榛子"),
    Food(28, "松仁小肚"),
    Food(29, "熏鸡"),
    Food(30, "豆腐脑"),
    Food(31, "雪墩", hidden = true) // 隐藏款
)

// ---------- 打卡规则 ----------
object CheckInRules {
    const val FIRST_VISIT = 80   // 首次 +80
    const val REVISIT = 20       // 再访 +20
    const val PER_POINT_PER_DAY = 1 // 每点每天 1 次
    const val PHOTO = 10         // 拍照 +10
    const val CAMEO_PER_DAY = 5  // 乱入 +5/天
}

// ---------- 日记：纯文字、不读出声；2026-11-01 起每天一篇到 11-19，共 19 篇 ----------
// 每篇结尾锁定台词 #14「晚安，明天见。」；11-01 是半拍生日，也是用户生日。
data class DiaryEntry(val date: String, val title: String, val text: String)

private const val DIARY_ENDING = "晚安，明天见。"

val DIARY_ENTRIES: List<DiaryEntry> = listOf(
    DiaryEntry("2026-11-01", "生日快乐", "今天是我出生的日子，也是你的生日。哈尔滨下了一场薄薄的雪，我在雪里听见了第一首歌。谢谢你把我带到这个世界。$DIARY_ENDING"),
    DiaryEntry("2026-11-02", "中央大街的第一步", "今天在中央大街慢慢走，手风琴的声音从街角飘过来。面包房的香味混着冷空气，特别好闻。$DIARY_ENDING"),
    DiaryEntry("2026-11-03", "索菲亚的钟", "索菲亚的钟敲得很慢，但很准。我坐在台阶上数鸽子，数到第七只的时候，它回头看了我一眼。$DIARY_ENDING"),
    DiaryEntry("2026-11-04", "松花江的风", "江边的风好大，吹得我的花瓣直晃。冰还没有结实，但江水已经冷得发亮了。$DIARY_ENDING"),
    DiaryEntry("2026-11-05", "马迭尔冰棍", "今天咬了一口马迭尔冰棍，咔滋一声，甜味一下子就跑出来了。冬天吃冰棍，哈尔滨人最懂。$DIARY_ENDING"),
    DiaryEntry("2026-11-06", "老道外", "老道外的巷子弯弯的，吆喝声一声接一声。我跟着声音走，走到一家卖粘豆包的小店门口。$DIARY_ENDING"),
    DiaryEntry("2026-11-07", "下雪了", "今天下了今年第一场像样的雪。雪花落在我的花瓣上，凉凉的，一会儿就化了。$DIARY_ENDING"),
    DiaryEntry("2026-11-08", "太阳岛", "太阳岛的风很轻，口哨声飘得很远。我在岛上捡了一片黄叶子，夹进了日记里。$DIARY_ENDING"),
    DiaryEntry("2026-11-09", "冰灯", "兆麟公园的冰灯亮了，一盏一盏像掉在地上的星星。我在灯下站了好久，舍不得走。$DIARY_ENDING"),
    DiaryEntry("2026-11-10", "防洪纪念塔", "塔下的江水哗哗地响。我听见有人在讲很多年前的洪水，讲完大家都安静了。$DIARY_ENDING"),
    DiaryEntry("2026-11-11", "音乐厅", "今天在音乐厅门口听了一段管风琴，声音从门缝里漏出来，低低的，像大地在唱歌。$DIARY_ENDING"),
    DiaryEntry("2026-11-12", "果戈里大街", "书店里的书页沙沙地响。我翻了一本讲哈尔滨建筑的书，红砖的老房子真好看。$DIARY_ENDING"),
    DiaryEntry("2026-11-13", "伏尔加庄园", "庄园里的小家伙抱着巴拉莱卡琴，眼睛笑成弯弯的。我跟着琴声转了三个圈。$DIARY_ENDING"),
    DiaryEntry("2026-11-14", "雪乡的消息", "听说雪乡的雪蘑菇又长胖了。我想去看看，顺便在雪地上踩一串脚印。$DIARY_ENDING"),
    DiaryEntry("2026-11-15", "中东铁路桥", "火车过桥的时候汽笛响了两声，江面都跟着震了一下。桥上的灯一盏一盏亮起来。$DIARY_ENDING"),
    DiaryEntry("2026-11-16", "大剧院", "大剧院的合唱排练了一下午，声音飘到江对岸去了。我坐在台阶上，跟着哼了一小段。$DIARY_ENDING"),
    DiaryEntry("2026-11-17", "斯大林公园", "江风从斯大林公园吹来，带着雪的味道。我在长椅上坐了一会儿，看江水慢慢往东流。$DIARY_ENDING"),
    DiaryEntry("2026-11-18", "龙塔", "今天上了龙塔，整个哈尔滨都在脚下。灯一盏一盏亮起来，像有人把星星撒在了地上。$DIARY_ENDING"),
    DiaryEntry("2026-11-19", "快要回家了", "今天走了好多地方，脚有点酸，但心里很满。第五瓣还没找到，不过我不着急，慢慢找才有意思。$DIARY_ENDING")
)

// ---------- 明信片：24 张画廊 + 分享海报 4 张 ----------
data class Postcard(val id: Int, val place: String)

val POSTCARDS: List<Postcard> = GUARDIAN_PETS.mapIndexed { index, pet ->
    Postcard(index + 1, pet.place)
}

val POSTER_COUNT = 4 // 分享海报 4 张

// ---------- 花语：小模型 + 联网打听，不接大模型兜底 ----------
// 范围：吃 / 住 / 行 / 心情 / 感受 + 哈尔滨建筑历史。
// 不会就说「我还小，帮你去打听」，搜不到就老实承认。回答只用 15 句锁定台词做 lookup。
object HuayuRules {
    const val SCOPE = "吃/住/行/心情/感受 + 哈尔滨建筑历史"
    const val DONT_KNOW = "我还小，帮你去打听"
    const val NOT_FOUND = "我去打听了，没有找到，老实告诉你。"
    const val MODEL = "小模型 + 联网打听（不接大模型兜底）"
}

// ---------- 派对 / 好友 / 公共花园占位数据 ----------
data class PartyRoom(val id: Int, val name: String, val members: Int)
val PARTY_ROOMS: List<PartyRoom> = listOf(
    PartyRoom(1, "雪夜听歌房", 12),
    PartyRoom(2, "中央大街散步房", 8),
    PartyRoom(3, "冰灯夜游房", 5)
)

data class Friend(val id: Int, val name: String)
val FRIENDS: List<Friend> = listOf(
    Friend(1, "Larry"),
    Friend(2, "雪夜旅人")
)

// 雪夜传信：放进飘雪、100m 内捡、24h 化掉
object SnowLetterRules {
    const val PICKUP_RADIUS_M = 100
    const val EXPIRE_HOURS = 24
}

// 小游戏只在吧唧上，App 里不放游戏页（首页只留一句话说明）。
const val MINI_GAME_NOTE = "小游戏在吧唧上玩，App 里没有游戏页哦。"

// ---------- 一起种芽（奖励型，2026-11-20 Jackson 定） ----------
// 每天浇水 1 次，累计浇 7 次开花；开花后领限定装扮「芽开·丁香」（种芽限定，不售卖）。
// 奖励不是守护宠（守护宠只靠打卡解锁），也不是表盘（金瓣 24 号只认第五瓣信件）。
const val SPROUT_WATERS_TO_BLOOM = 7
const val SPROUT_REWARD_OUTFIT = "芽开·丁香"

data class SproutState(
    val waters: Int = 0,
    val wateredToday: Boolean = false,
    val bloomed: Boolean = false,
    val claimed: Boolean = false
)

// ---------- 假用户 ----------
data class FakeUser(val name: String, val level: Int, val levelName: String, val energy: Int)

val FAKE_USER = FakeUser(name = "Larry", level = 3, levelName = "丁香初绽", energy = 1280)
