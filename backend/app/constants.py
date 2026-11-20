"""半拍 Banpai 后端 — 锁定常量：台词、四瓣、守护宠、能量规则、食物团。"""

# 15 句锁定台词（人声始祖，2026-11-18 定版；与 App 侧同款）
LOCKED_LINES = {
    1: "嗯，我在听呀，慢慢说。",
    2: "等一下，我在听呀。",
    3: "听到了，是风的声音。",
    4: "我就在这儿，慢慢找第五瓣。",
    5: "你好呀，我是半拍。",
    6: "你好呀，今天也要慢慢走哦。",
    7: "打卡成功啦，这里真好听。",
    8: "哇，是音符，里面会是什么呢？",
    9: "这件衣服好漂亮，我喜欢。",
    10: "好冷呀，不过我喜欢雪。",
    11: "春天来啦，丁香都开啦。",
    12: "夏天到啦，江边风好凉快。",
    13: "秋天来啦，叶子都黄啦。",
    14: "晚安，明天见。",
    15: "谢谢你，陪我一起听。",
}

# 四瓣：冰、雪、乐、花；静留给第五瓣
PETALS = ["冰", "雪", "乐", "花"]
FIFTH_PETAL = "静"

# 花语范围外统一口径（不会就去打听，不编、不接大模型兜底）
OUT_OF_SCOPE_REPLY = "我还小，帮你去打听"

# 能量规则（所有变动走 energy.apply_energy() 写 energy_ledger）
ENERGY_FIRST_CHECKIN = 80   # 打卡：首次
ENERGY_REVISIT = 20         # 打卡：再访
ENERGY_PHOTO = 10           # 拍照
ENERGY_CAMEO_PER_DAY = 5    # 乱入 +5/天
ENERGY_POSTCARD = 5         # 寄明信片
ENERGY_CHECKIN_PER_POINT_PER_DAY = 1  # 每点每天 1 次

# 守护宠 24 只：不断扩充，不写死总数（seed 时以 name 为键去重）
# sound 为动物发音/可爱叫声（守护宠不说中文台词）；line 为卡片文字
GUARDIANS = [
    {"name": "中央大街·手风琴", "location": "中央大街", "pet": "手风琴", "sound": "手风琴声", "line": "我慢，但我准。", "chapter": 1},
    {"name": "索菲亚·钟", "location": "索菲亚教堂", "pet": "钟", "sound": "钟声", "line": "我慢，但我准。", "chapter": 1},
    {"name": "松花江·冰裂", "location": "松花江", "pet": "冰裂", "sound": "冰裂声", "line": "江面裂开的时候，春天就来了。", "chapter": 1},
    {"name": "老会堂·弦乐", "location": "老会堂音乐厅", "pet": "弦乐", "sound": "弦乐声", "line": "老墙会记得每一个音符。", "chapter": 2},
    {"name": "大剧院·合唱", "location": "哈尔滨大剧院", "pet": "合唱", "sound": "合唱声", "line": "一起唱，声音才够亮。", "chapter": 2},
    {"name": "老道外·叫卖", "location": "老道外", "pet": "叫卖", "sound": "吆喝声", "line": "热气腾腾的，才叫老道外。", "chapter": 2},
    {"name": "极地公园·企鹅", "location": "极地公园", "pet": "企鹅", "sound": "企鹅叫", "line": "摇摇晃晃，也能走很远。", "chapter": 3},
    {"name": "虎林园·呼噜", "location": "东北虎林园", "pet": "呼噜", "sound": "呼噜声", "line": "打个盹，梦里也在巡山。", "chapter": 3},
    {"name": "太阳岛·口哨", "location": "太阳岛", "pet": "口哨", "sound": "口哨声", "line": "风一吹，我就想吹口哨。", "chapter": 3},
    {"name": "防洪纪念塔·水声", "location": "防洪纪念塔", "pet": "水声", "sound": "水声", "line": "江水退了，塔还在。", "chapter": 4},
    {"name": "马迭尔·咬冰棍", "location": "马迭尔", "pet": "咬冰棍", "sound": "咔嚓咬冰棍", "line": "冬天吃冰棍，才够哈尔滨。", "chapter": 4},
    {"name": "冰雪大世界·凿冰", "location": "冰雪大世界", "pet": "凿冰", "sound": "凿冰声", "line": "一凿一凿，凿出一个冬天。", "chapter": 4},
    {"name": "伏尔加庄园·巴拉莱卡琴", "location": "伏尔加庄园", "pet": "巴拉莱卡琴", "sound": "巴拉莱卡琴声", "line": "毛茸茸的手，也能弹出好曲子。", "chapter": 5},
    {"name": "果戈里大街·书页", "location": "果戈里大街", "pet": "书页", "sound": "翻书声", "line": "每一页，都是一个冬天。", "chapter": 5},
    {"name": "斯大林公园·江风", "location": "斯大林公园", "pet": "江风", "sound": "风声", "line": "江风一吹，烦恼就散了。", "chapter": 5},
    {"name": "中东铁路桥·火车汽笛", "location": "中东铁路桥", "pet": "火车汽笛", "sound": "火车汽笛", "line": "呜——百年前的火车开过来了。", "chapter": 6},
    {"name": "兆麟公园·冰灯", "location": "兆麟公园", "pet": "冰灯", "sound": "冰灯轻响", "line": "冰做的灯，也能暖一整晚。", "chapter": 6},
    {"name": "哈尔滨音乐厅·管风琴", "location": "哈尔滨音乐厅", "pet": "管风琴", "sound": "管风琴声", "line": "低音一响，整座城都安静了。", "chapter": 6},
    {"name": "龙塔·瞭望", "location": "龙塔", "pet": "瞭望", "sound": "风声", "line": "站得高，看得远。", "chapter": 7},
    {"name": "俄罗斯风情小镇·套娃", "location": "俄罗斯风情小镇", "pet": "套娃", "sound": "木头轻响", "line": "打开我，里面还有一个我。", "chapter": 7},
    {"name": "亚布力滑雪场·滑雪板", "location": "亚布力滑雪场", "pet": "滑雪板", "sound": "滑雪声", "line": "摔倒了，爬起来继续滑。", "chapter": 7},
    {"name": "雪乡·雪蘑菇", "location": "雪乡", "pet": "雪蘑菇", "sound": "踩雪声", "line": "雪落下来，像蘑菇一样。", "chapter": 8},
    {"name": "音乐长廊·小提琴", "location": "音乐长廊", "pet": "小提琴", "sound": "小提琴声", "line": "拉一首，给路过的人听。", "chapter": 8},
    {"name": "哈尔滨啤酒博物馆·气泡", "location": "哈尔滨啤酒博物馆", "pet": "气泡", "sound": "气泡声", "line": "咕嘟咕嘟，快乐冒泡。", "chapter": 8},
]

# 打卡点 24 个（与守护宠一一对应；经纬度为哈尔滨真实地标附近）
CHECKPOINTS = [
    {"name": "中央大街", "lat": 45.7712, "lng": 126.6175, "guardian": "中央大街·手风琴"},
    {"name": "索菲亚教堂", "lat": 45.7685, "lng": 126.6248, "guardian": "索菲亚·钟"},
    {"name": "松花江", "lat": 45.7790, "lng": 126.6140, "guardian": "松花江·冰裂"},
    {"name": "老会堂音乐厅", "lat": 45.7590, "lng": 126.6320, "guardian": "老会堂·弦乐"},
    {"name": "哈尔滨大剧院", "lat": 45.8040, "lng": 126.5640, "guardian": "大剧院·合唱"},
    {"name": "老道外", "lat": 45.7790, "lng": 126.6470, "guardian": "老道外·叫卖"},
    {"name": "极地公园", "lat": 45.7800, "lng": 126.5900, "guardian": "极地公园·企鹅"},
    {"name": "东北虎林园", "lat": 45.7950, "lng": 126.6000, "guardian": "虎林园·呼噜"},
    {"name": "太阳岛", "lat": 45.7820, "lng": 126.6100, "guardian": "太阳岛·口哨"},
    {"name": "防洪纪念塔", "lat": 45.7710, "lng": 126.6140, "guardian": "防洪纪念塔·水声"},
    {"name": "马迭尔", "lat": 45.7710, "lng": 126.6170, "guardian": "马迭尔·咬冰棍"},
    {"name": "冰雪大世界", "lat": 45.7960, "lng": 126.5850, "guardian": "冰雪大世界·凿冰"},
    {"name": "伏尔加庄园", "lat": 45.6980, "lng": 126.6200, "guardian": "伏尔加庄园·巴拉莱卡琴"},
    {"name": "果戈里大街", "lat": 45.7600, "lng": 126.6400, "guardian": "果戈里大街·书页"},
    {"name": "斯大林公园", "lat": 45.7750, "lng": 126.6150, "guardian": "斯大林公园·江风"},
    {"name": "中东铁路桥", "lat": 45.7760, "lng": 126.6100, "guardian": "中东铁路桥·火车汽笛"},
    {"name": "兆麟公园", "lat": 45.7620, "lng": 126.6220, "guardian": "兆麟公园·冰灯"},
    {"name": "哈尔滨音乐厅", "lat": 45.7570, "lng": 126.6450, "guardian": "哈尔滨音乐厅·管风琴"},
    {"name": "龙塔", "lat": 45.7590, "lng": 126.6700, "guardian": "龙塔·瞭望"},
    {"name": "俄罗斯风情小镇", "lat": 45.7820, "lng": 126.6000, "guardian": "俄罗斯风情小镇·套娃"},
    {"name": "亚布力滑雪场", "lat": 44.8000, "lng": 128.4500, "guardian": "亚布力滑雪场·滑雪板"},
    {"name": "雪乡", "lat": 44.6000, "lng": 128.9000, "guardian": "雪乡·雪蘑菇"},
    {"name": "音乐长廊", "lat": 45.7720, "lng": 126.6160, "guardian": "音乐长廊·小提琴"},
    {"name": "哈尔滨啤酒博物馆", "lat": 45.7800, "lng": 126.6300, "guardian": "哈尔滨啤酒博物馆·气泡"},
]

# 表盘 30 款（名字 + 风格；价格能量，0 为免费/默认）
DIALS = [
    {"name": "冰蓝日常", "style": "日常", "price": 0},
    {"name": "雪夜丁香", "style": "夜雪", "price": 0},
    {"name": "江畔晨光", "style": "日常", "price": 20},
    {"name": "冰灯初上", "style": "夜雪", "price": 20},
    {"name": "丁香初绽", "style": "春日", "price": 20},
    {"name": "夏日江风", "style": "夏日", "price": 20},
    {"name": "秋叶金黄", "style": "秋日", "price": 20},
    {"name": "雪夜暖绒", "style": "夜雪", "price": 40},
    {"name": "冰雪大世界", "style": "夜雪", "price": 40},
    {"name": "中央大街", "style": "城市", "price": 40},
    {"name": "索菲亚之钟", "style": "城市", "price": 40},
    {"name": "松花江畔", "style": "日常", "price": 40},
    {"name": "太阳岛雪博", "style": "雪景", "price": 40},
    {"name": "伏尔加庄园", "style": "田园", "price": 60},
    {"name": "音乐厅之夜", "style": "音乐", "price": 60},
    {"name": "大剧院合唱", "style": "音乐", "price": 60},
    {"name": "老道外烟火", "style": "城市", "price": 60},
    {"name": "极地企鹅", "style": "动物", "price": 60},
    {"name": "虎林园呼噜", "style": "动物", "price": 60},
    {"name": "龙塔瞭望", "style": "城市", "price": 80},
    {"name": "雪乡蘑菇", "style": "雪景", "price": 80},
    {"name": "亚布力滑雪", "style": "运动", "price": 80},
    {"name": "斯大林江风", "style": "日常", "price": 80},
    {"name": "果戈里书页", "style": "文艺", "price": 80},
    {"name": "兆麟冰灯", "style": "夜雪", "price": 100},
    {"name": "马迭尔冰棍", "style": "日常", "price": 100},
    {"name": "中东铁路桥", "style": "城市", "price": 100},
    {"name": "音乐长廊", "style": "音乐", "price": 100},
    {"name": "啤酒气泡", "style": "日常", "price": 100},
    {"name": "第五瓣·金", "style": "隐藏", "price": 0},
]

# 装扮 16 套 —— 只有 3 个槽：服装/套装、背景、特效（配饰已去掉）
# 另有 1 套种芽限定「芽开·丁香」：price -1 表示不售卖，只能种芽开花领取
# （2026-11-20 Jackson 定：种芽开花后的奖励是奖励型 —— 送限定装扮）
SPROUT_WATERS_TO_BLOOM = 7  # 一起种芽：每天浇水 1 次，累计浇 7 次开花
SPROUT_REWARD_OUTFIT = "芽开·丁香"
OUTFIT_SLOTS = ["服装/套装", "背景", "特效"]
OUTFITS = [
    {"name": "冰蓝日常装", "slot": "服装/套装", "price": 0},
    {"name": "春日丁香装", "slot": "服装/套装", "price": 40},
    {"name": "雪夜暖绒装", "slot": "服装/套装", "price": 60},
    {"name": "冰灯夜游装", "slot": "服装/套装", "price": 80},
    {"name": "夏日江畔装", "slot": "服装/套装", "price": 60},
    {"name": "秋日金叶装", "slot": "服装/套装", "price": 60},
    {"name": "音乐厅礼服", "slot": "服装/套装", "price": 100},
    {"name": "雪乡棉袄装", "slot": "服装/套装", "price": 80},
    {"name": "雪夜背景", "slot": "背景", "price": 0},
    {"name": "丁香花海背景", "slot": "背景", "price": 40},
    {"name": "江畔晨光背景", "slot": "背景", "price": 40},
    {"name": "冰雪大世界背景", "slot": "背景", "price": 60},
    {"name": "飘雪特效", "slot": "特效", "price": 20},
    {"name": "音符飘落特效", "slot": "特效", "price": 40},
    {"name": "丁香花瓣特效", "slot": "特效", "price": 40},
    {"name": "金色第五瓣特效", "slot": "特效", "price": 0},
    {"name": "芽开·丁香", "slot": "服装/套装", "price": -1},  # 种芽限定：不售卖，开花后领取
]

# 好吃团 30 种 + 隐藏雪墩（雪墩为隐藏款，is_hidden=true）
FOODS = [
    {"name": "马迭尔冰棍", "place": "中央大街", "is_hidden": False},
    {"name": "红肠", "place": "秋林", "is_hidden": False},
    {"name": "锅包肉", "place": "老道外", "is_hidden": False},
    {"name": "大列巴", "place": "秋林", "is_hidden": False},
    {"name": "杀猪菜", "place": "老道外", "is_hidden": False},
    {"name": "得莫利炖鱼", "place": "松花江畔", "is_hidden": False},
    {"name": "哈尔滨啤酒", "place": "啤酒博物馆", "is_hidden": False},
    {"name": "冰糖葫芦", "place": "中央大街", "is_hidden": False},
    {"name": "冻梨", "place": "道外", "is_hidden": False},
    {"name": "冻柿子", "place": "道外", "is_hidden": False},
    {"name": "粘豆包", "place": "老道外", "is_hidden": False},
    {"name": "铁锅炖", "place": "郊区", "is_hidden": False},
    {"name": "地三鲜", "place": "家常菜馆", "is_hidden": False},
    {"name": "小鸡炖蘑菇", "place": "家常菜馆", "is_hidden": False},
    {"name": "烤冷面", "place": "夜市", "is_hidden": False},
    {"name": "油炸冰棍", "place": "中央大街", "is_hidden": False},
    {"name": "格瓦斯", "place": "秋林", "is_hidden": False},
    {"name": "酸菜饺子", "place": "老道外", "is_hidden": False},
    {"name": "熏鸡", "place": "老道外", "is_hidden": False},
    {"name": "酱骨头", "place": "道里", "is_hidden": False},
    {"name": "烧烤", "place": "师大夜市", "is_hidden": False},
    {"name": "豆腐脑", "place": "早市", "is_hidden": False},
    {"name": "油条豆浆", "place": "早市", "is_hidden": False},
    {"name": "哈尔滨红肠面包", "place": "秋林", "is_hidden": False},
    {"name": "松仁小肚", "place": "秋林", "is_hidden": False},
    {"name": "风干肠", "place": "秋林", "is_hidden": False},
    {"name": "俄罗斯列巴配红菜汤", "place": "俄罗斯风情小镇", "is_hidden": False},
    {"name": "奶油蘑菇汤", "place": "伏尔加庄园", "is_hidden": False},
    {"name": "冻秋梨膏", "place": "中央大街", "is_hidden": False},
    {"name": "冰城雪糕", "place": "中央大街", "is_hidden": False},
    {"name": "雪墩", "place": "？？？", "is_hidden": True},
]

# 吧唧小游戏（只在吧唧上，敲吧唧记分）
BADGE_GAMES = ["敲钟", "凿冰", "咬冰棍"]

# 雪夜传信规则
LETTER_PICKUP_RADIUS_M = 100
LETTER_TTL_HOURS = 24

# 花语每日上限（占位口径）
CHAT_DAILY_LIMIT = 50
