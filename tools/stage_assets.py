#!/usr/bin/env python3
"""把 ~/workspace/banpai/assets/ 里的定版素材，按干净短名拷进 App 的 assets/。

规则：
- 只拷定版文件：pack-* 清单、clean/v3/v2 最高版本；json/.bak/.blend/.glb/旧版目录一律不拷。
- 目标名全部 ASCII 短名，Kotlin 里用 Assets.kt 的函数按编号取。
- 四瓣红线：半拍主图已目检（标准图正好 4 瓣、金瓣隐藏 v3 正好 5 瓣）；
  其余成套素材（表盘/服装/守护宠卡/食物/明信片/相框）按生产记录已验过，
  本脚本只做"文件存在 + 尺寸合理"的校验，不逐张重验。
- 食物 11 种在素材库里没有对应图（见 FOOD_MAP 缺口），不硬塞错图，留空。
输出：app/app/src/main/assets/ + 本目录 assets-manifest.json（映射与跳过清单）。
"""
import json
import os
import re
import shutil

SRC = "/home/hatch/workspace/banpai/assets"
PACK = "/home/hatch/workspace/banpai"
DST = "/home/hatch/workspace/banpai-code/app/app/src/main/assets"

copied = []   # (dst_rel, src, bytes)
skipped = []  # (reason, detail)


def cp(src, dst_rel):
    assert os.path.exists(src), f"missing source: {src}"
    dst = os.path.join(DST, dst_rel)
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    copied.append((dst_rel, src, os.path.getsize(dst)))


def one_file(d, pattern):
    """目录 d 里匹配 pattern 的唯一文件（用于长 UUID 名）"""
    hits = [f for f in os.listdir(d) if re.search(pattern, f)]
    assert len(hits) == 1, f"{d} pattern {pattern}: {hits}"
    return os.path.join(d, hits[0])


def ver_pref(files, keywords):
    """多版本文件里挑最高版本：v4>v3>v2>无版本；同版本取最新 mtime。"""
    cands = [f for f in files if all(k in f for k in keywords)]
    assert cands, f"no file for {keywords}"
    def rank(f):
        m = re.search(r"-v(\d+)", f)
        return (int(m.group(1)) if m else 0,
                os.path.getmtime(os.path.join(SRC_DIR, f)))
    return max(cands, key=rank)


# ---------------- 1. 半拍主图 ----------------
B = f"{SRC}/banpai"
cp(one_file(B, r"standard-front-0-.*\.webp$"), "banpai/standard-front.webp")
cp(one_file(B, r"expressions-0-.*\.webp$"), "banpai/expressions.webp")
cp(one_file(B, r"actions-0-.*\.webp$"), "banpai/actions.webp")
cp(one_file(B, r"gold-hidden-v3-0-.*\.webp$"), "banpai/gold-hidden.webp")
cp(one_file(B, r"mini-q-v2-0-.*\.webp$"), "banpai/mini-q.webp")
cp(one_file(B, r"back-side-0-.*\.webp$"), "banpai/back-side.webp")

# ---------------- 2. 认识半拍 4 卡 / 欢迎视频 / 彩蛋 ----------------
I = f"{SRC}/intro-cards"
for n, slug in enumerate(["1-hello", "2-petals", "3-searching", "4-invite"], 1):
    cp(one_file(I, rf"intro-{slug}-0-.*\.webp$"), f"intro/{n:02d}.webp")
W = f"{SRC}/welcome"
cp(one_file(W, r"welcome-snowy-harbin-0-.*\.mp4$"), "welcome/welcome.mp4")
E = f"{SRC}/easter-egg"
cp(os.path.join(E, "半拍写给你的信.png"), "easter-egg/letter.png")
cp(one_file(E, r"fifth-petal-gold-0-.*\.webp$"), "easter-egg/emblem-gold.webp")
# 彩蛋视频 v2：按生产记录，uuid 07656a45 的是 v2（10s，768x1152）
cp(os.path.join(E, "media-generation-banpai-easter-egg-fifth-petal-0-07656a45-08d1-4749-9713-b91dce5ebf4a.mp4"),
   "easter-egg/fifth-petal-video.mp4")

# ---------------- 3. 表盘 30（pack-watchfaces，干净中文名，按编号） ----------------
PW = f"{PACK}/pack-watchfaces"
wfs = sorted(f for f in os.listdir(PW) if f.endswith(".webp"))
assert len(wfs) == 30, f"pack-watchfaces: {len(wfs)}"
for f in wfs:
    n = int(f.split("-")[0])
    cp(os.path.join(PW, f), f"watchfaces/{n:02d}.webp")

# ---------------- 4. 服装 16（outfits/，按 slug 映射到 FakeRepository 顺序） ----------------
OUTFIT_SLUG = {1: "ice-blue-daily", 2: "ice-crystal-gown", 3: "summer-flower-dr",
               4: "snow-night-warm", 5: "spring-lilac", 6: "aurora-night",
               7: "golden-morning", 8: "snow-red-plum", 9: "russian-fur",
               10: "baroque-ball", 11: "ice-lantern-nigh", 12: "ski-sport",
               13: "snow-town-red", 14: "concert-gown", 15: "summer-river",
               16: "autumn-maple"}
O = f"{SRC}/outfits"
for n, slug in OUTFIT_SLUG.items():
    cp(one_file(O, rf"outfit-{slug}.*\.webp$"), f"outfits/{n:02d}.webp")
# 17 芽开·丁香：沿用春日丁香图（v3 做法：复用丁香装扮图，不新造）
shutil.copyfile(os.path.join(DST, "outfits/05.webp"), os.path.join(DST, "outfits/17.webp"))
copied.append(("outfits/17.webp", "reuse of outfits/05.webp", os.path.getsize(os.path.join(DST, "outfits/17.webp"))))

# ---------------- 5. 守护宠卡片 24（最高版本优先） ----------------
PET_SLUGS = ["central-street", "sophia", "songhua", "old-hall", "grand-theatre",
             "laodaowai", "polar", "tiger", "sun-island", "flood-memorial",
             "modern-icepop", "ice-snow-world", "volga", "gogol", "stalin",
             "railway-bridge", "zhaolin", "music-hall", "dragon-tower",
             "russian-town", "yabuli", "snow-town", "music-corridor", "beer-museum"]
PET_KW = {"laodaowai": ["laodao"], "modern-icepop": ["modern-ice"],
          "polar": ["polar"], "tiger": ["tiger"]}
GC = f"{SRC}/guardian-cards"
SRC_DIR = GC
gcfiles = [f for f in os.listdir(GC) if f.endswith(".webp")]
for i, slug in enumerate(PET_SLUGS, 1):
    kw = PET_KW.get(slug, [slug])
    f = ver_pref(gcfiles, kw)
    cp(os.path.join(GC, f), f"guardian-cards/{i:02d}.webp")

# ---------------- 6. 守护宠视频 24（v2 优先，否则最新 mtime） ----------------
GV = f"{SRC}/guardian-videos"
SRC_DIR = GV
gvfiles = [f for f in os.listdir(GV) if f.endswith(".mp4")]
for i, slug in enumerate(PET_SLUGS, 1):
    kw = PET_KW.get(slug, [slug])
    if slug == "modern-icepop":
        kw = ["modern-ice"]
    f = ver_pref(gvfiles, kw)
    cp(os.path.join(GV, f), f"guardian-videos/{i:02d}.mp4")

# ---------------- 7. 食物（文件编号 → FakeRepository 食物 id；11 种无图，留空） ----------------
FOOD_MAP = {1: "04", 2: "03", 3: "05", 4: "01", 5: "09", 7: "26", 8: "27",
            9: "14", 10: "06", 11: "07", 13: "31", 15: "18", 17: "30", 18: "12", 19: "12",
            21: "11", 22: "10", 28: "29", 29: "20", 31: "15"}
# id 13：FakeRepository 原名“列巴”（与 3 号大列巴重复），已改名为“华梅面包”，
# 对应素材库 31-华梅面包.webp（v3 定版已解锁 3 种之一）。
F = f"{SRC}/food"
for fid, fnum in FOOD_MAP.items():
    srcf = [x for x in os.listdir(F) if x.startswith(f"{fnum}-") and x.endswith(".webp")]
    assert len(srcf) == 1, f"food {fnum}: {srcf}"
    cp(os.path.join(F, srcf[0]), f"food/{fid:02d}.webp")
for fid in [6, 12, 14, 16, 20, 23, 24, 25, 26, 27, 30]:
    skipped.append(("food-no-image", f"food id {fid} 素材库无对应图，卡片保持纯文字"))

# ---------------- 8. 明信片 24 + 海报 4（pack-postcards） ----------------
PP = f"{PACK}/pack-postcards"
for f in sorted(os.listdir(PP)):
    if re.match(r"\d\d-.*\.webp$", f):
        n = int(f.split("-")[0])
        cp(os.path.join(PP, f), f"postcards/{n:02d}.webp")
    elif f.startswith("海报"):
        n = int(f[2:4])
        cp(os.path.join(PP, f), f"posters/{n:02d}.webp")

# ---------------- 9. 相框：竖 20（pack-frames-9x16）+ 横 20（frames-horizontal 干净名） ----------------
PF = f"{PACK}/pack-frames-9x16"
vfs = sorted(f for f in os.listdir(PF) if f.endswith(".png"))
assert len(vfs) == 20, f"pack-frames-9x16: {len(vfs)}"
for f in vfs:
    n = int(f.split("-")[0])
    cp(os.path.join(PF, f), f"frames/vertical/{n:02d}.png")
FH = f"{SRC}/frames-horizontal"
hfs = sorted(f for f in os.listdir(FH)
             if re.match(r"\d\d-.*-horizontal\.png$", f))
assert len(hfs) == 20, f"frames-horizontal: {len(hfs)}"
for f in hfs:
    n = int(f.split("-")[0])
    cp(os.path.join(FH, f), f"frames/horizontal/{n:02d}.png")
skipped.append(("frame-known-flaw", "横 09 夏日江畔：柳枝框饰顶到边角，小角不透明（框饰本身，非夜空底）；横 03 小半拍花瓣浮在头顶周围（4 瓣数对）。生产记录已知，照常内置"))

# ---------------- 10. 相机风格 8 + 表情库 + 半身像框 5 ----------------
CS = f"{SRC}/camera-styles"
cp(os.path.join(CS, "00-base-central-street-dusk.jpg"), "camera-styles/base.jpg")
for n, slug in enumerate(["bingcheng-jishi", "xueye-dianying", "nuanyang-renxiang",
                          "dingxiang-chunri", "laojie-heibai", "huangjin-shike",
                          "binglan-jingmi", "1998-pailide"], 1):
    srcf = [x for x in os.listdir(CS) if x.startswith(f"{n:02d}-{slug}") and x.endswith(".jpg")]
    assert len(srcf) == 1, f"camera style {n}: {srcf}"
    cp(os.path.join(CS, srcf[0]), f"camera-styles/style-{n:02d}.jpg")
cp(os.path.join(CS, "09-expression-library-6-v3.webp"), "camera-styles/expressions.webp")
cp(os.path.join(CS, "10-half-body-frame-sample-v3.webp"), "camera-styles/halfbody-01.webp")
for n, slug in enumerate(["spring-lilac", "snow-night-warm", "ice-lantern", "summer-river"], 1):
    srcf = [x for x in os.listdir(CS)
            if re.match(rf"{10+n}-half-body-frame-sample-{slug}-v1\.webp$", x)]
    assert len(srcf) == 1, f"halfbody {n}: {srcf}"
    cp(os.path.join(CS, srcf[0]), f"camera-styles/halfbody-{n + 1:02d}.webp")

# ---------------- 11. 绿幕视频：根目录 43 个动作 + 灵动岛 6 个定版 ----------------
GS = f"{SRC}/greenscreen"
for f in sorted(os.listdir(GS)):
    if not f.endswith(".mp4"):
        continue
    if f.startswith("backup-"):
        slug = f[:-4]  # backup-01-...
    else:
        m = re.match(r"media-generation-banpai-greenscreen-(.+?)-0-.*\.mp4$", f)
        assert m, f"greenscreen name parse: {f}"
        slug = m.group(1)
    cp(os.path.join(GS, f), f"greenscreen/{slug}.mp4")
DI = f"{GS}/dynamic-island-v2"
for src_name, dst_name in [("banpai-di-peek-v7.mp4", "di-peek-v7.mp4"),
                           ("banpai-di-roll-v3.mp4", "di-roll-v3.mp4"),
                           ("banpai-di-hug-v2.mp4", "di-hug-v2.mp4"),
                           ("banpai-di-kiss-v2.mp4", "di-kiss-v2.mp4"),
                           ("banpai-di-stretch-v2.mp4", "di-stretch-v2.mp4"),
                           ("banpai-di-snowball-v2.mp4", "di-snowball-v2.mp4")]:
    cp(os.path.join(DI, src_name), f"greenscreen/{dst_name}")
skipped.append(("greenscreen-note", "灵动岛 peek/roll 旧版 v2/v3/v4/v5/v6 及 redo 版未内置，只留定版 v7/v3；其余 43 个绿幕动作全量内置"))

# ---------------- 清单 ----------------
total = sum(b for _, _, b in copied)
manifest = {
    "copied": [{"dst": d, "src": s, "bytes": b} for d, s, b in copied],
    "skipped": [{"reason": r, "detail": x} for r, x in skipped],
    "total_files": len(copied),
    "total_bytes": total,
    "total_mb": round(total / 1024 / 1024, 1),
    "max_single_file_mb": round(max(b for _, _, b in copied) / 1024 / 1024, 1),
}
with open("/home/hatch/workspace/banpai-code/assets-manifest.json", "w", encoding="utf-8") as f:
    json.dump(manifest, f, ensure_ascii=False, indent=1)
print(f"copied {len(copied)} files, {manifest['total_mb']} MB total, "
      f"max single {manifest['max_single_file_mb']} MB")
print(f"skipped notes: {len(skipped)}")
for d, s, b in copied:
    if b > 20 * 1024 * 1024:
        print(f"  big: {d} {round(b/1024/1024,1)} MB")
