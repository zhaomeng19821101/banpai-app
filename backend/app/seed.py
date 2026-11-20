"""半拍 Banpai 后端 — 种子数据（可重复跑，以唯一键去重）。

内容：24 守护宠、24 打卡点、30 表盘、16 装扮 + 1 种芽限定「芽开·丁香」、演示用户 Larry、
日记 19 篇（2026-11-01 起每天一篇到 11-19，每篇结尾锁定台词 #14）。
"""
import os
import sys
from datetime import date, timedelta

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.constants import CHECKPOINTS, DIALS, GUARDIANS, LOCKED_LINES, OUTFITS
from app.db import SessionLocal, init_db
from app.models import Checkpoint, Dial, Diary, Guardian, Outfit, User
from app.security import hash_password

LINE14 = LOCKED_LINES[14]  # 晚安，明天见。

# 日记 19 篇正文（纯文字，不读出声；每篇结尾锁定台词 #14）
DIARY_TEXTS = [
    "今天我醒来了。哈尔滨的夜很安静，雪落在屋顶上，一点声音也没有。原来我的生日，和一个叫 Jackson 的人是同一天。",
    "我去中央大街走了走，手风琴的声音从街角飘过来。我的四片花瓣是冰、雪、乐、花，每一片都是一种声音。",
    "索菲亚教堂的钟敲了十二下，我数着数着就笑了。原来时间也可以这么好听。",
    "松花江结冰了，江面裂开细细的纹路，像一首还没写完的歌。",
    "老会堂里有人拉小提琴，我坐在最后一排听完了整首曲子。哈尔滨真的是音乐之城。",
    "大剧院的合唱排练到很晚，我躲在门口听，差点被发现。",
    "老道外很热闹，叫卖声、笑声、锅包肉的香味混在一起，我喜欢这种热闹。",
    "极地公园的企鹅摇摇晃晃地走路，我学它走了半天，摔了三次。",
    "虎林园的老虎在打呼噜，声音低低的，像大地在呼吸。",
    "太阳岛的雪雕还没化完，我吹了一声口哨，风把口哨带去了很远的地方。",
    "防洪纪念塔前有人拍照，我也想被拍下来，可是我还没有找到第五瓣。",
    "马迭尔的冰棍很甜，咬下去咔嚓一声，冬天就应该这样吃。",
    "冰雪大世界里有人在凿冰，一凿一凿，凿出一座会发光的城。",
    "伏尔加庄园的巴拉莱卡琴响起来了，毛茸茸的演奏家闭着眼睛笑，真好听。",
    "果戈里大街的书店很安静，我翻了一本书，书页沙沙响，像雪落下来。",
    "斯大林公园的江风很大，我的斗篷被吹得鼓起来，像一只想飞的鸟。",
    "中东铁路桥上火车开过，汽笛声呜的一声，百年前的声音还在。",
    "兆麟公园的冰灯亮了，冰做的灯，居然能暖一整晚。",
    "今天是第十九天。哈尔滨的一年四季都在我身上，我的四片花瓣，一片是冰，一片是雪，一片是乐，一片是花。第五瓣还在等我，我会慢慢找。",
]


def seed():
    init_db()
    db = SessionLocal()
    try:
        # 守护宠（按 name 去重，不断扩充不写死总数）
        name_to_id = {}
        for g in GUARDIANS:
            row = db.query(Guardian).filter_by(name=g["name"]).first()
            if row is None:
                row = Guardian(**g)
                db.add(row)
                db.flush()
            name_to_id[g["name"]] = row.id
        # 打卡点
        for c in CHECKPOINTS:
            if db.query(Checkpoint).filter_by(name=c["name"]).first() is None:
                db.add(Checkpoint(
                    name=c["name"], lat=c["lat"], lng=c["lng"],
                    guardian_id=name_to_id.get(c["guardian"]),
                    description=f"{c['name']}打卡点",
                ))
        # 表盘 30 款
        for d in DIALS:
            if db.query(Dial).filter_by(name=d["name"]).first() is None:
                db.add(Dial(name=d["name"], style=d["style"], price=d["price"],
                            is_hidden=(d["name"] == "第五瓣·金")))
        # 装扮 16 套（3 槽）
        for o in OUTFITS:
            if db.query(Outfit).filter_by(name=o["name"]).first() is None:
                db.add(Outfit(name=o["name"], slot=o["slot"], price=o["price"]))
        # 演示用户 Larry（Lv.3 丁香初绽，演示口径）
        demo = db.query(User).filter_by(phone="13800000000").first()
        if demo is None:
            demo = User(phone="13800000000", nickname="Larry",
                        password_hash=hash_password("demo1234"), level=3, energy=1280)
            db.add(demo)
            db.flush()
        # 日记 19 篇：2026-11-01 → 2026-11-19
        start = date(2026, 11, 1)
        for i, body in enumerate(DIARY_TEXTS):
            day = start + timedelta(days=i)
            if db.query(Diary).filter_by(user_id=demo.id, day=day).first() is None:
                text = f"{body}\n{LINE14}"
                db.add(Diary(user_id=demo.id, day=day, text=text))
        db.commit()
        print(f"seed ok: guardians={len(GUARDIANS)} checkpoints={len(CHECKPOINTS)} "
              f"dials={len(DIALS)} outfits={len(OUTFITS)} diaries=19 demo=Larry")
    finally:
        db.close()


if __name__ == "__main__":
    seed()
