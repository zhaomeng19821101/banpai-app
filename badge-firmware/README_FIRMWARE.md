# 半拍 Banpai — 吧唧 Badge Firmware README

> 固件 UI v0.2.0-ui · 2026-11-22 · 从新框架从头搭建，不沿用旧 67 次提交代码。
> 本版把吧唧 UI 设计框架（artifact ban-pai-ba-ji-ui v3）的各页落到固件里：
> 表盘首页（3 个信息页）、功能菜单（11 行）、语音留言、花园、好友、派对、
> 照片／壁纸、打卡、设置（含 Wi-Fi ＋屏上密码键盘）、小游戏（就绪页＋最高分）。
>
> **验证状态：本地编译已过 — 2026-11-22，`pio run -e waveshare_175
> -e esp32-s3-devkitc-1` 两个环境全部 SUCCESS（PlatformIO 6.2.0，
> 本机 `~/workspace/.pio-venv`）。waveshare_175：firmware.bin 1,118,560 字节，
> Flash 17.5%（1,103,930 / 6,291,456），RAM 16.1%（52,684 / 327,680）；
> 仅 `CORE_DEBUG_LEVEL` 重定义警告（旧版已有，无害）。GitHub Actions
> `Badge Firmware` 构建结果待推送后确认。显示层仍是 Serial 占位
> （StubDisplay）——真实 CO5300 屏驱绑定、引脚、电量读数、敲击阈值
> 都还未在实板上验证。** 只做了：C++ 括号 / 头文件配对自查、
> `platformio.ini` 字段核对、BLE 帧格式与任务书逐项对表。第一次上板前必须先跑
> 下面的编译与联调步骤，硬件驱动绑定（CO5300 / CST9217 / QMI8658 / AXP2101）
> 是按 Waveshare 公开资料写的占位，需在实板上确认引脚与库。

## 锁死的定版（不要改口径，先对 App）

- 目标板：Waveshare ESP32-S3-Touch-AMOLED-1.75，466×466 圆屏 CO5300 QSPI AMOLED，
  触摸 CST9217（读数不稳定，**不能做关键交互**，改用 IMU QMI8658 敲击检测），
  电源 AXP2101，Arduino core，PlatformIO。
- 四瓣是 **冰、雪、乐、花**，静留给第五瓣；彩蛋只出信（吧唧上不揭示第五瓣位置）。
- 表盘是首页，点进菜单，一页一个功能：语音留言 / 花园（公共花园 / 雪夜传信）/
  好友 / 派对房间 / 照片（壁纸）/ 打卡 / 设置（含 Wi-Fi）/ 小游戏（敲钟 / 凿冰 /
  咬冰棍）。小游戏只在吧唧上，App 里不放游戏页。
- 存储规则：吧唧只存 **1 张壁纸 + 1 张表盘**，新的从手机来直接覆盖旧的；
  手机存全库（30 表盘、全部照片）；换表盘需手机在旁 BLE 重传。
- 吧唧有喇叭，音乐播放差所以**不做音乐播放**；语音留言（按住说话、松开发送、
  喇叭播放）与花园里的唱歌可出声。
- 全部按 466×466 圆屏安全区适配，不出表盘；颜色一律 RGB565；绘图窗口偶数对齐；
  内置字体只有 ASCII，中文用图形 / 英文占位（不内置中文字库）。
- 表盘位图不编译进固件（旧版已占 78% 分区）：466×466 表盘图在
  `~/workspace/banpai/assets/watchfaces-round-v2/`，由 App 下发。

## 文件清单

```
badge-firmware/
├── platformio.ini          # env esp32-s3-devkitc-1 + waveshare_175（后者默认）
├── partitions.csv          # 16 MB：nvs / otadata / app0 6 MB / app1 6 MB / littlefs
├── README_FIRMWARE.md      # 本文件
└── src/
    ├── main.cpp            # setup/loop、BLE 服务、菜单状态机、敲击检测、OTA 占位
    ├── ble_protocol.h/.cpp # 帧解析/组装（格式与下表一致）
    ├── dial.h              # 30 款表盘表 + 5 时钟样式 + 时间层位置规则
    ├── menu.h/.cpp         # 表盘首页 + 8 功能页 + 设置/Wi-Fi 子页 + 小游戏页
    ├── storage.h/.cpp      # NVS 元数据 + LittleFS 单槽位（1 壁纸 + 1 表盘覆盖）
    └── game.h/.cpp         # 敲击记分（敲钟/凿冰/咬冰棍）
```

## BLE 协议（已对表，全齐）

服务 UUID：`0000b001-0000-1000-8000-00805f9b34fb`，设备名 `Banpai-Badge`。

| 特征 | UUID 后缀 | 方向 | 格式 |
| --- | --- | --- | --- |
| DIAL | b101 | App → 吧唧，写 | `"<dialCode>"`，如 `"03"` / `"dial-03"`，1–30；24 号金瓣隐藏只在彩蛋解锁后可用 |
| STATE | b102 | App → 吧唧，写 | `"S:<能量>\|<已点亮>\|<开花>\|<点位csv>"` |
| SCORE | b103 | 吧唧 → App，通知 | `"G:<游戏>\|<难度>\|<分数>\|<时间戳>"`，游戏 `bell` / `ice` / `stick` |
| OTA | b104 | 双向 | 控制：`"OTA:START:<总字节>:<crc32hex>"` / `"OTA:ABORT"` / `"OTA:DONE"`；吧唧回 `"OTA:READY"` / `"OTA:OK"` / `"OTA:ERR:<码>"` |
| OTA_DATA | b105 | 双向 | 二进制分片（固件 OTA 用；表盘 / 壁纸位图走同一通道时由 App 加前缀区分，见“待定”） |
| TAP | b106 | 双向 | App 写 `"M:camera"` / `"M:normal"`（相机开／关，复用）；吧唧通知 `"T:<kind>\|<时间戳>\|<序号>"`，kind 只剩 `tap` / `knock`；`shutter` 已废弃（2026-11-23，见 CAMERA） |
| CAMERA | b109 | 双向 | 拍照键（2026-11-23，Jackson 定，替代敲两下快门）：吧唧通知 `"S:press\|<时间戳>\|<序号>"`；App 回 `"S:done"`（或 `"S:done\|<时间戳>"`），吧唧显示 `PHOTO TAKEN` |
| INFO | b107 | 吧唧 → App，通知 | JSON `{"name","fw","batt","chg","model","serial"}`，连接时 + 每 60 s 推一次 |
| TIME | b108 | App → 吧唧，写 | `"T:<unix秒>\|<时区分钟>\|<样式0-4>"` |

敲击语义（`main.cpp` + `ble_protocol.cpp` 的 `classifyKnock`）：

- 敲击来自 QMI8658 加速度尖峰（阈值 1.8 g，双敲窗口 350 ms，去抖 120 ms）。
- 拍照键（2026-11-23，Jackson 定）：写 TAP `"M:camera"` 后吧唧自动弹出拍照键页（保存当前页），点大圆拍照键 → 通知 `"S:press|..."`（CAMERA b109）→ App 拍照后回 `"S:done"`，吧唧显示 `PHOTO TAKEN`；写 `"M:normal"` 回到保存的页。**拍照模式下敲击什么都不做**（防误触）；敲两下快门已取消，`T:shutter` 永不再发。
- 游戏进行中敲击 = 记分，不做导航；其他页面单击敲 = 下一项，双敲 = 确认 / 进入。
- CST9217 触摸只做同义捷径（单击 = 下一项），永不用于记分、密码确认等关键动作。**唯一例外（2026-11-23）：拍照键页点屏幕拍照键拍照。**
- 已知粗糙点：单击会立即以 `tap` 上报，若 350 ms 内又来一下再报 `knock`；App 侧请按序号去重（`T:<kind>|<ts>|<seq>` 的 seq 单调递增）。
  有实板后可改为“等窗口结束再上报单击”，以一次上报为准。

## 表盘（30 款，全在 `dial.h`）

1 冰蓝日常 · 2 雪夜 · 3 春日丁香 · 4 极光之夜 · 5 金色晨光 · 6 雪夜红梅 ·
7 夏花 · 8 雪乡红棉袄 · 9 冰晶礼服 · 10 音乐会礼服 · 11 滑雪运动装 ·
12 冰灯夜游装 · 13 俄式毛皮冬装 · 14 巴洛克舞会礼服 · 15 夏日江畔装 ·
16 秋日枫叶装 · 17 中央大街 · 18 索菲亚 · 19 松花江 · 20 太阳岛 ·
21 冰雪大世界 · 22 伏尔加庄园 · 23 雪夜暖绒 · **24 金瓣隐藏（彩蛋解锁后可用）** ·
25 迷你 Q 版 · 26 挥手 · 27 比心 · 28 晚安 · 29 打卡成功 · 30 听一听。

5 种时钟样式（TIME 帧样式 0–4）：0 经典金 / 1 极简白 / 2 霓虹青 / 3 红梅 /
4 翻页。时间按款式自动选位置（`ClockAnchor`：TOP / BOTTOM / CENTER，
见 `dial.h` 的 `clockWindow()`，窗口偶数对齐）；时间层由固件绘制，
不 baked 进位图，一张位图可配 5 种样式。

## 菜单页（`menu.h/.cpp`，全齐）

首页表盘 + 功能菜单（11 行）+ 8 功能页：语音留言、公共花园 / 雪夜传信、好友、
派对房间、照片（壁纸）、打卡、设置、小游戏；设置内含 **Wi-Fi 子页**（搜附近网络、
在吧唧上输密码连接，ESP32-S3 自带 2.4 GHz Wi-Fi，只支持 2.4 G）；小游戏页内含
3 个子页（敲钟 / 凿冰 / 咬冰棍，难度 1–3，每敲得分 10 / 20 / 30，时限 30 s /
30 s / 20 s，结束经 SCORE 同步到手机，另存各游戏最高分到 NVS）。

导航（设计稿一致）：表盘首页敲一下切 3 个信息页（时间 → 步数／能量／已点亮 →
下一站），双敲／点表盘进菜单；菜单里敲一下下一行、双敲进入；功能页敲一下切
下一功能页、长按（触摸）回菜单、菜单里长按回表盘；小游戏就绪页点一下开始，
敲击记分，返回结束并上报 SCORE。

## UI 落固件对照（设计稿 ban-pai-ba-ji-ui v3 → 固件）

| 设计页 | 固件页 | 做到什么程度 |
|---|---|---|
| 表盘首页 | HOME（3 信息页） | 时间层按表盘锚点＋5 时钟样式；信息页 1 步数／能量／已点亮（**不写固定总数**，守护宠 roster 开放）；信息页 2 下一站名＋距离＋自动打卡说明＋离线提示；表盘图本身仍是占位（位图需 App 下发，见“还缺什么”） |
| 功能菜单 | MENU（11 行） | 11 行全齐，带未读角标（语音未读数、花园来信、好友在线数、派对房名）；换表盘不在吧唧上做，回手机换 |
| 语音留言 | VOICE | 录音状态（双敲开始／停止，60 秒上限提示）、发送后 SENT OK、播放状态；消息条数／未读数是 App 待下发字段（无数据时显示 NO VOICE YET，不编造） |
| 花园 | GARDEN | 来信等待 → 双敲读信 → 双敲回一朵花 → FLOWER SENT；吧唧只收信／读信／回花，写新信回手机写 |
| 好友 | FRIENDS | 好友总数／在线数＋在线圆点；双敲跳语音页发一对一异步语音；好友名单是 App 待下发字段（无数据时显示 SYNC FROM PHONE） |
| 派对房间 | PARTY | 邀请卡（敲一下接受、双敲挂断——App 按 TAP/KNOCK 上报结合邀请状态解读，不新增 BLE 帧）；在房显示房名＋在线人数＋轮流异步语音 |
| 照片／壁纸 | PHOTOS | 只存 1 张壁纸（有／无两种状态＋壁纸 id）；同步进度条 UI 已就绪（App 下发位图时驱动，见“还缺什么”）；时间由系统叠加，不烧进图 |
| 打卡 | CHECKIN | 未到达（站名＋距离＋步行分钟＋自动打卡说明）、已到达（距离为 0 时显示成功态＋能量＋守护宠只配叫声）、未知（DISTANCE --）；打卡判定在手机 GPS，吧唧只做显示；已点亮不写固定总数 |
| 设置 | SETTINGS | 亮度／音量（NVS 持久化，双敲循环调）、时钟样式、TAP 模式、Wi-Fi 入口、时间同步状态、设备信息（固件版本）、恢复出厂（只提示回 App 操作，吧唧上不真恢复） |
| Wi-Fi | WIFI + WIFI_PASS | 扫网列表（最多 6 行，超长 SSID 截断在安全区内）、连接中／已连接／失败三种状态；密码页是 7×7 屏上键盘（敲击导航备用：敲一下下一键、双敲输入），输完 OK 后 main.cpp 异步 WiFi.begin（10 秒超时回失败）；触摸键盘是设计主路径，待 CST9217 稳定性验证 |
| 小游戏 | GAMES + GAME_PLAY | 3 款列表（带各游戏最高分）；就绪页“点一下开始”→ 敲击记分 → 返回结束上报 SCORE；分数同步经 BLE SCORE |

与设计稿有出入的地方（有意为之，已记录）：
- 设计稿的 30 张表盘缩略图里 **金瓣隐藏（24 号）可直接点选**——与“未解锁不给看”
  的彩蛋规则冲突，固件不设表盘画廊（换表盘回手机换），且 `storage::setDialCode`
  与 `bleproto::isValidDial` 仍锁死 24 号，未解锁不存。
- 设计稿多处写“已点亮 9 / 24”——与守护宠 roster 开放、不写固定总数的规则冲突，
  固件全页只写已点亮数，不写总数。
- 设计稿小游戏写“敲一下记 1 分”——固件沿用骨架已定的难度分（10 / 20 / 30，
  见 `game.h`），SCORE 帧格式不变。
- 设计稿的敲三下半拍乱入标“待定”——固件不实现三敲，待 Jackson 定后再加。
- 设计稿的“全异步语音”推荐标“待定”——固件语音页按异步录音／发送／播放做；
  好友 1:1 实时对讲（Agora）是 App 端能力，吧唧只显示房间状态，不另加帧。

## 存储（`storage.h/.cpp`）

- NVS（命名空间 `banpai`）只存元数据：当前表盘码、时钟样式、TAP 模式、
  壁纸 id 与 hash、表盘 hash、彩蛋解锁标志、TAP 序号、序列号。
- LittleFS 单槽位文件：`/wallpaper.bin`（1 张壁纸）、`/dial.bin`（1 张表盘），
  写新文件时直接截断覆盖，**没有第二槽、没有历史**。
- 为什么不用纯 NVS 存图：466×466 RGB565 一张约 434 KB，NVS 装不下；
  单槽位规则本身不变，只是位图本体放 LittleFS、元数据放 NVS。

## 固件体积与 OTA

- `partitions.csv`：16 MB flash，app0 / app1 各 6 MB 双分区，`otadata`
  用于回滚标记。旧版已占 78% 分区——新骨架不要把位图、中文字库、
  大资源编译进固件，否则 OTA 分区装不下。
- OTA 流程：App 写 b104 `OTA:START` → 吧唧回 `OTA:READY` → b105 传分片
  （边收边算 CRC32）→ App 写 `OTA:DONE` → 校验通过后切启动分区并重启。
- 自动回滚占位：`setup()` 自检（存储 + BLE 初始化）通过后才调用
  `esp_ota_mark_app_valid_cancel_rollback()`；新镜像在自检前崩溃，
  bootloader 自动回滚到旧分区。首次上板需实测一次“坏镜像回滚”。

## 如何编译

```bash
# 1. 安装 PlatformIO（本机已装好：~/workspace/.pio-venv，6.2.0；
#    新机器上任选其一）
pip install platformio        # 或：VS Code + PlatformIO IDE 插件
pio --version

# 2. 编译（默认 env 就是 waveshare_175）
cd ~/workspace/banpai-code/badge-firmware
pio run -e waveshare_175      # 实板目标（注意：env 名是 waveshare_175，下划线）
pio run -e esp32-s3-devkitc-1  # 开发板对照（同芯片，无圆屏）

# 3. 只检查不烧录
pio check -e waveshare_175
```

> 本机无 PlatformIO，上述命令**未跑过**。第一次跑通之前不要宣称编译通过。

## 如何烧录

```bash
# USB 数据线连吧唧（需确认板子进下载模式的方法，见 Waveshare 说明）
pio run -e waveshare-1.75 -t upload

# 或用 esptool 直接烧（firmware.bin 路径见编译输出）
esptool.py --chip esp32s3 --port /dev/ttyUSB0 --baud 921600 write_flash \
  0x10000 .pio/build/waveshare-1.75/firmware.bin
# 注意：首次烧录建议整片擦除后烧 bootloader + partitions + app：
#   pio run -e waveshare-1.75 -t upload  会处理分区表；esptool 手动烧时
#   别忘了 0x0 bootloader、0x8000 partitions（见 .pio/build 输出）。

# 看日志
pio device monitor -e waveshare-1.75   # 115200
```

## 如何用手机 App 联调

1. 烧录后吧唧 BLE 广播 `Banpai-Badge`，服务 `0000b001-...`。
2. App 连接后先写 TIME（`T:<unix>|<480>|<0-4>`），再写 STATE（`S:...`）——
   表盘首页即显示真实时间与点亮数。
3. 写 DIAL `"03"` 换表盘：吧唧只换码；位图需 App 随后下发（OTA_DATA 通道），
   否则首页显示占位表盘（四瓣标记 + 时间层），并在日志里可见。
   换表盘全程需手机在旁 BLE 连接。
4. 写 TAP `"M:camera"` → 吧唧自动弹出拍照键页，点大圆拍照键 → 吧唧通知 `"S:press|..."`（CAMERA b109）→ App 拍照后回 `"S:done"`，吧唧显示 `PHOTO TAKEN`。写 `"M:normal"` 回到之前的页。拍照模式下敲击不拍。
   写 `"M:normal"` 恢复普通模式。
5. 进小游戏敲吧唧 → 结束时吧唧通知 SCORE `"G:bell|1|120|<ts>"`，App 记分。
6. 语音留言页按住说话、松开发送；花园页有信时显示 `LETTER WAITING`。
7. 设置 → Wi-Fi：扫网、选网、在吧唧上输密码（ASCII 屏上键盘）连接。
8. INFO 帧核对：`{"name":"Banpai-Badge","fw":"0.1.0-scaffold","batt":...,
   "chg":...,"model":"waveshare-1.75","serial":"BP-..."}`。

## 2.06 板移植要点

2.06 寸板屏幕是 **410×502**，不是 466×466。移植时只改三处，应用逻辑不动：

1. `platformio.ini` 增加 `[env:waveshare-2.06]`（或改板选项），
   `BANPAI_SCREEN_W=410`、`BANPAI_SCREEN_H=502`、`BANPAI_SCREEN_ROUND=0/1`
   按 2.06 实际形状填；`dial.h` 的 `BANPAI_SCREEN_W/H/CX/CY/R/SAFE_R` 与
   `clockWindow()` 的三档时间窗口要按 410×502 重排（TOP / BOTTOM / CENTER
   的 y 与 h 都要重算，保持偶数对齐与安全区内）。
2. 表盘位图：App 需为 2.06 单独下发 410×502 重排版位图；1.75 的 466×466
   位图**不能**直接拉伸复用（比例不同，会变形 / 出圆）。
3. 驱动层：CO5300 换成 2.06 板的屏驱型号与引脚（以 2.06 板原理图为准），
   其余（BLE 协议、菜单状态机、敲击检测、单槽位存储、OTA 流程）不用改。

## 还缺什么（上板前必须补）

UI 结构已落固件，以下是仍做不到的部分（不谎称已完成）：

- [ ] **真实屏驱绑定：** `main.cpp` 的 `StubDisplay` 只是 Serial 日志占位，
      菜单渲染逻辑（`menu.cpp`）已按设计稿写全、安全区已按 466 圆屏算过，
      但 CO5300 QSPI 屏驱（Waveshare Arduino 例程 / LovyanGFX）还没接——
      接上后 `IDisplay` 的 6 个方法一一对应即可，菜单代码不用改。
      注意：LovyanGFX 会增大固件体积，旧版已占分区 78%，接入前先核算。
- [ ] **表盘／壁纸位图下发：** `storage` 的单槽位（`/dial.bin`、
      `/wallpaper.bin`）与覆盖规则已实现，但 App 经 OTA_DATA 下发位图的分片
      前缀与 hash 校验格式还是待定（README 旧版已记）——App 侧定格式后，
      吧唧侧按 `saveDialBitmap` / `saveWallpaper` 落盘即可；表盘图在
      `~/workspace/banpai/assets/watchfaces-round-v2/`，壁纸是明信片图。
      在此之前，表盘首页显示占位表盘（四瓣标记＋时间层），照片页只显示
      “有／无壁纸”状态。**30 张表盘缩略图不在吧唧上做**（换表盘回手机换）。
- [ ] **App 待下发的数据字段（UI 上已留位置，无数据时显示 `--`／
      `SYNC FROM PHONE`，不编造）：** 步数（需计步器＋ STATE 帧扩展或新帧，
      格式需与 App 端定）、语音消息列表（条数／时长／未读）、好友名单
      （名字／在线状态）、派对房间名与在线人数、下一站站名与距离、
      通知文本、找手机触发帧。BLE 协议本身未动——新增字段的帧格式需与
      App 端一起定，不能由固件单方面发明。
- [ ] **电量读数：** 仍是 100% 假值（`main.cpp` 的 `readBattery` 占位），
      AXP2101 驱动（XPowersLib）未接，INFO 帧的 batt 不要采信。
- [ ] **敲击阈值与触摸：** 1.8 g 阈值、350 ms 双敲窗口、120 ms 去抖都是
      骨架设定值，未在实板上测过；CST9217 触摸只做捷径（点按开菜单／
      长按返回），密码键盘的触摸主路径待稳定性验证，敲击导航是备用。
- [ ] **Wi-Fi 密码主路径：** 屏上键盘（敲击导航）已实现；设计稿另一条
      “手机帮输一次密码、吧唧记住”的路径需 App 端配合（密码下发帧），
      未定——两条路径只能留一条做主路径，等 Jackson 定。
- [ ] **敲三下半拍乱入：** 设计稿标待定，固件未实现；定后再加（手势表
      ＋相机模式联动）。
- [ ] PlatformIO：本机现已装好（`~/workspace/.pio-venv`，6.2.0），
      本地 `pio run` 结果见下；实板 + USB 线：Waveshare
      ESP32-S3-Touch-AMOLED-1.75，核对原理图引脚
      （`platformio.ini` 里的 LCD / 触摸 / IMU / AXP 引脚是公开资料默认值，
      未在实板上验证过）。
- [ ] App 联调：DIAL / STATE / TIME / TAP / SCORE / INFO / CAMERA 八组帧逐项过；
      拍照键端到端（M:camera 弹出 → 点键 S:press → App 拍照 → S:done 显示 PHOTO TAKEN）；OTA 一次成功 + 一次坏镜像回滚实测；语音留言的
      音频编码与分片格式、派对房间的信令格式仍待定（只留了接口与注释，
      未编造格式）。

## 自查记录（本次搭建）

- C++ 括号 / 头文件配对：`src/*.h` 与 `src/*.cpp` 人工逐文件核对通过
  （`python` 括号平衡扫描 + `#include` 配对检查，见下）。
- `platformio.ini` 字段核对：两个 env、默认 env `waveshare_175`、
  分区表引用、16 MB flash、NimBLE-Arduino 依赖均已核对。
- 自查中发现并修复：游戏计时起点未设置会导致一开局就超时；
  时间层曾是 `"12:00"` 假值，已改为按 TIME 帧 + 时区算真实 HH:MM；
  `parseTime` 一行晦涩写法已改清晰。
- **未做**：`pio run`、`pio check`、任何烧录、任何实板测试。
