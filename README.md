# 半拍 Banpai · 新工程骨架（从 UI 框架 v3 从头建）

> 决定：2026-11-20 04:05，用户决定从新框架从头建一个，**不沿用**电脑上那份 67 次提交的旧代码。
> 当前状态（2026-11-20 晚）：**管线验证版 + v1 模型**——App v0.3 已在 GitHub Actions
> 多次编绿并发布；端侧 1.7B（Q4_K_M，v1）已内置，花语走 llama.cpp 端侧推理。
> 固件因本机缺少工具链/实板，尚未真编译，见下文“未验证”清单。
> 全部密钥一律只用占位符，本仓库里没有任何真实 key、证书、keystore。

## 目录

```
banpai-code/
├── README.md                 本文件
├── .gitignore
├── app/                      Android App：Kotlin + Jetpack Compose，com.banpai.app
│   ├── README_APP.md         App 编译说明
│   ├── settings.gradle.kts / build.gradle.kts / gradle.properties
│   ├── local.properties.example   （只有空的 AMAP_KEY=、AGORA_APP_ID=）
│   ├── gradlew               占位脚本（非真正 Wrapper，见 README_APP.md）
│   └── app/src/main/java/com/banpai/app/
│       ├── MainActivity.kt / ui/theme/ / ui/navigation/
│       ├── data/  ApiClient.kt / BleManager.kt / FakeRepository.kt
│       └── ui/screens/  16 个屏
├── backend/                  FastAPI 后端，端口 8000
│   ├── README_BACKEND.md / API.md（统一 API 契约，80 条路由）
│   ├── requirements.txt / .env.example / run.sh
│   ├── app/  main.py models.py schemas.py db.py seed.py energy.py security.py constants.py
│   ├── app/routers/  12 个模块（auth/users/checkin/ble/outfits/chat/diary/party/friends/garden/exchange/assets）
│   └── tests/  14 个 pytest 用例
└── badge-firmware/           ESP32-S3 固件，PlatformIO
    ├── README_FIRMWARE.md / platformio.ini / partitions.csv
    └── src/  main.cpp ble_protocol.* dial.h menu.* storage.* game.*
```

## 锁死的定版（2026-11-20，不再问）

- 装扮 3 槽：服装/套装、背景、特效；配饰去掉。16 套装扮 + 1 种芽限定「芽开·丁香」（奖励型，2026-11-20 Jackson 定：种芽每天浇水 1 次，累计 7 次开花后领取，不售卖）。
- 日记纯文字、不读出声、加小表情装饰；2026-11-01（半拍生日，也是用户生日）起每天一篇补到 11-19，共 19 篇，每篇结尾锁定台词 #14「晚安，明天见。」
- 四瓣是冰、雪、乐、花；静留给第五瓣；彩蛋只出信，不播视频。
- 探索页留高德、做真地图；白天/夜晚/跟随系统三档；首页一屏不下拉；旅行连胜已删。
- 小游戏只在吧唧上，敲吧唧记分，App 里不放游戏页；拍照乱入用真绿幕视频；花语用小模型 + 联网打听，不接大模型兜底。
- 守护宠现有 24 只、不断扩充；好吃团 30 种 + 隐藏雪墩；表盘 30 款。
- 能量：打卡首次 +80、再访 +20、每点每天 1 次、拍照 +10、乱入 +5/天、寄明信片 +5，全部走 `apply_energy()` 写 `energy_ledger`。
- BLE 服务 `0000b001-0000-1000-8000-00805f9b34fb`；DIAL b101 / STATE b102 / SCORE b103 / OTA b104 / OTA_DATA b105 / TAP b106 / INFO b107 / TIME b108；敲吧唧两下 = 拍照快门。
- 吧唧只存 1 张壁纸 + 1 张表盘，新的直接覆盖旧的；手机存全库。

## 各端如何编译运行

### 后端（已在本机验证通过）

```bash
cd backend
bash run.sh --seed        # 建 venv、装依赖、启动 uvicorn（:8000）
# 或手动：
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env      # 填占位值（见下表）
uvicorn app.main:app --reload --port 8000
curl http://127.0.0.1:8000/api/v1/health   # -> {"status":"ok"}
pytest tests/ -q          # 19 passed（本机实测，含种芽 5 项）
```

### App（推荐走 GitHub Actions；本机未验证）

**首选：GitHub Actions 自动编 APK（2026-11-20 Jackson 定）**——本机内存紧、Gradle daemon 起不来，
所以编译改在 GitHub 的免费机器上跑（公开仓库免费无限，4 vCPU / 16 GB；私有仓库每月 2000 分钟免费）。

```bash
# 1. 在 GitHub 上新建一个仓库（公开或私有都行），比如 banpai-app
# 2. 把整个 banpai-code 推上去（.gitignore 已排掉 local.properties / keystore / .venv / build 产物，
#    推之前我已 grep 核对过：源码里没有任何真实密钥值，可以放心公开）
cd ~/workspace/banpai-code
git init
git add -A
git commit -m "banpai fresh build v0.1"
git branch -M main
git remote add origin https://github.com/<你的用户名>/banpai-app.git
git push -u origin main
# 3. 打开仓库的 Actions 页，等 "Android Debug APK" 跑完，
#    在 Artifacts 里下载 banpai-app-debug（app-debug.apk，保留 7 天）
# 4. 以后每次 push 到 main，都会自动再编一版
```

工作流在 `.github/workflows/android-debug.yml`：JDK 17 + Android SDK 34 + Gradle 8.7，
直接编 `./gradlew` 的替代命令（工程里的 `gradlew` 目前还是占位脚本，CI 不用它）。
不需要任何密钥就能编出 debug 包（无 key 时地图页是占位）。

**备选：本地 Android Studio 编译**

1. 在有 JDK 17 + Android SDK 34 的机器上打开 `app/`，在 Android Studio 里生成标准 Gradle Wrapper（本机 `gradlew` 只是占位说明脚本，不能直接跑）。
2. `cp local.properties.example local.properties`，填 `AMAP_KEY`、`AGORA_APP_ID`（只填你自己的，别提交）。
3. `./gradlew assembleDebug`，产物在 `app/build/outputs/apk/debug/`；装到一加 Ace 2（1240×2772）上实测。
4. 详见 [app/README_APP.md](sandbox://workspace/banpai-code/app/README_APP.md)。

### 固件（本机未验证，需 PlatformIO + 实板）

```bash
cd badge-firmware
pio run -e waveshare-1.75        # 默认 env，目标 Waveshare 1.75 466×466 圆屏
pio run -e waveshare-1.75 -t upload --upload-port /dev/ttyUSB0
# 或 esptool：python -m esptool --chip esp32s3 --port <PORT> --baud 921600 \
#   --before default-reset --after hard-reset write-flash 0x10000 .pio/build/waveshare-1.75/firmware.bin
```

2.06 移植见 [badge-firmware/README_FIRMWARE.md](sandbox://workspace/banpai-code/badge-firmware/README_FIRMWARE.md)（board 选项 + 410×502 重排说明）。详见该文件。

## 需要哪些 key（只列名字，不填真值）

| Key / 配置 | 在哪配 | 没配时会怎样 |
|---|---|---|
| `AMAP_KEY` | `app/local.properties`、后端 `.env` | 无 Key 时探索页显示占位说明，不画假地图；后端路线接口返回直线距离占位。**已接通**：高德 SDK 本地内置（`app/app/libs/` 合包 JAR + `src/main/jniLibs/` .so，3D 地图 11.3.100，官网一键下载包 2026-09-24；因 `maven.amap.com` 在 CI 上连不上，见 run 44695000131）。**debug SHA1 已固定**（2026-11-20）：CI 不再用临时 keystore，改用仓库里的 `app/debug.keystore`（标准 android/android 口令，非机密），每次构建签名 SHA1 都是 `92:94:48:3F:DA:14:78:B6:A3:6B:C5:A3:03:01:C4:05:87:B4:C7:68`，高德控制台登记这一个 debug SHA1 即可；CI 每次构建后都会核对 APK 签名 SHA1，对不上就失败。Key 只登记了 release SHA1 时，debug 包地图仍会是黑的——预期内；真机验地图要打 release 包，或在高德控制台补登记上面这个 debug SHA1 |
| `AGORA_APP_ID` / `AGORA_APP_CERTIFICATE` | App `local.properties` / 后端 `.env`（证书只留服务端） | 未配置时 token 接口返回占位串，语音入会不可用。**范围（2026-11-20 用户锁定）**：公共区域（派对房间 / 公共花园）不用声网实时语音；只给好友 1:1 实时对讲（好友对讲控件）用。**TokenBuilder 已接通**：后端用 `agora-token-builder` 签发真实 006 格式 token（24 个后端测试含 5 个 token 用例，全过） |
| `SMALL_MODEL_URL` / `SMALL_MODEL_API_KEY` | 后端 `.env` | 花语用本地锁定回答 / “我还小，帮你去打听”口径 |
| `JWT_SECRET` | 后端 `.env` | 当前为开发占位串，部署前必须换随机长串 |
| `DATABASE_URL` | 后端 `.env` | 默认 SQLite 开发库 `banpai.db` |
| 签名 keystore | `app/` 外单独保管，绝不入库 | 无 keystore 只能出 debug 包 |

演示账号：后端种子里有演示用户 Larry，密码是占位值（见 `backend/app/seed.py`，部署前改掉）。

## 在一加和吧唧上如何实测

**一加 Ace 2（1240×2772）：**
1. 后端先跑起来（本机或局域网，App 里把 base URL 指过去，`ApiClient.kt` 默认 `http://10.0.2.2:8000/api/v1`，真机改局域网 IP）。
2. 装 debug APK，走一遍：欢迎→认识半拍→登录（短信/微信均为 demo 模拟）→首页→探索（配了 AMAP_KEY 才出真地图）→打卡→图鉴→装扮 3 槽→拍照（CameraX 占位，需接通后实拍）→表盘 30 款→花语→派对/好友→花园→日记 19 篇→明信片。
3. 对照 UI 框架 v3（artifact `ban-pai-app-ui-kuang-jia-v3-2`，live v13）逐屏核对。

**吧唧（Waveshare 1.75，466×466 圆屏）：**
1. 烧录固件后先看表盘首页是否完整显示、不出圆屏安全区。
2. App 蓝牙连上：下发表盘（DIAL）、同步状态（STATE）、对时（TIME）、看电量/版本（INFO）。
3. 敲两下：拍照模式下应发 `T:shutter`；小游戏页敲击记分，经 SCORE 同步到手机。
4. 设置里连 Wi-Fi（吧唧上输密码）；换壁纸/表盘：手机发新的，旧的直接被覆盖，只留 1 张。
5. OTA：先在测试板上做一次成功升级 + 一次坏镜像回滚实测，再动主力板。

## 哪些已接通、哪些是占位

| 项 | 状态 |
|---|---|
| 后端全部路由 + 能量账本 + 日记种子 + 花语打听口径 | 已接通（本机实测） |
| App 16 屏导航 + 昼夜三档 + 假数据层 | 已接通（假数据） |
| App 探索页高德真地图（SDK 本地内置 11.3.100 + MapView + 24 落点） | 已接通（代码层）；**真机地图待 release 包验证**（Key 只认 release SHA1，debug 包黑图是预期） |
| App 相机 CameraX、BLE 真机、登录短信/微信 | 占位（待 key + 真机） |
| 声网：好友 1:1 实时对讲（好友对讲控件）+ 后端真实 token 签发 | 已接通（代码层 + 24 测试）；**双人真机对讲待验证**（需登录接通 + 两台真机） |
| 声网：公共区域（派对房间 / 公共花园）实时语音 | **已取消**（2026-11-20 用户锁定：合规风险）；派对房间只用异步语音消息 |
| 花语端侧小模型 1.7B Q4_K_M（v1，管线验证版） | 已接通（代码层 + CI 打包）；**真机花语实测待验证**（见下节） |
| 固件 BLE 8 特征、菜单 8 功能页、敲击记分、OTA 双分区 | 骨架已接通（代码层），驱动层全部 `TODO(HW)` 占位 |
| 固件 CO5300 屏驱、QMI8658、 CST9217、AXP2101 电量 | 占位（Serial stub / 假值，实测前不要采信电量） |
| 素材 | **v0.3 已全量内置**：`app/app/src/main/assets/` 261 个文件、571.1 MB（图片 + 守护宠视频 + 绿幕视频，离线可看；清单 `assets-manifest.json`，打包脚本 `tools/stage_assets.py`）。APK 约 885 MB（不含模型），CI 改走 GitHub Release 附件（`v0.3-<短 commit>`），artifact 上传会超免费额度。带模型版约 2.0 GB，见下节 |

## 花语端侧模型（管线验证版，2026-11-20）

- **这次验证了什么**：llama.cpp（JNI，`app/app/src/main/cpp/`，pin 在提交
  `4c4a0f0a4246bc36a3724d12238d5624714dcb2f`，与评测同一份源码）能编进 APK；
  1.7B Q4_K_M（v1，1,107,408,832 字节，SHA256 `8d349e17…22df01`）由 CI 从
  GitHub Release（tag `model-17b-q4km-v1`，分 4 片下载、合并后 SHA256 校验）
  下载、打进 assets、随 APK 发布；CI 已绿并发布 v0.3-c75a120
  （APK 1,863,920,972 字节 ≈ 1.74 GiB，未超 2 GiB 上限）；本地已下载该
  Release APK 拆包核对：包内 `assets/banpai-17b-q4_k_m.gguf` SHA256 与
  `8d349e17…22df01` 一致、APK 签名 SHA1 与固定 debug 值
  `92:94:48:3F…C7:68` 一致；
  花语聊天屏（`ChatScreen.kt`）走“兜底 → 模型”顺序回答，模型没就绪不假装回答。
- **瘦身（2026-11-20）**：为压体积暂去 armeabi-v7a，只支持 arm64-v8a（目标机
  OnePlus Ace 2 是 arm64）；正式版如需覆盖老机型可恢复 `abiFilters`。
- **引擎选择**：llama.cpp，不选 MLC LLM——GGUF 原生加载，无需转格式；MLC 要额外
  转模型格式，这次只验证管线。API 以 clone 下来的 `include/llama.h` 为准
  （`llama_model_load_from_file` + `llama_init_from_model` + `llama_batch_ext` +
  `llama_process` + `llama_sampler_*`），见 `banpai_llama_jni.cpp` 注释。
- **App 侧五项兜底**（`data/BanpaiGuardrails.kt`，跑模型之前先过，按优先级）：
  (1) 第五瓣拦截——含“第五瓣/第五片/第5瓣/吧唧背面”一律挡回锁定台词 #4，
  绝不透露位置（最高红线）；(2) think-strip——去 `<think>…</think>` 与
  `[Start thinking]…` 及残留单标签，去完为空回锁定台词 #1；(3) 15 条锁定台词
  lookup（关键词命中直出原句）；(4) 唱歌请求固定拒绝（半拍只说话、不唱歌）；
  (5) 马迭尔/红肠事实 lookup（红肠是秋林里道斯/哈肉联的，马迭尔是冰棍+西餐厅）。
- **模型已知毛病**（v1，评测 20 题只过 9.5 题，见
  `~/workspace/banpai/small-model/eval-17b-q4km-2026-11-20.md`）：马迭尔/红肠
  归属错、第五瓣位置剧透、市花答成“雪”、会答应唱歌、不知道就现编。
  本次用户已知情并接受，只验证管线跑通；兜底先挡住最严重的几项，等 v3 模型替换。
- **未验证清单（花语）**：真机花语实测（首次复制 1.1 GB 耗时、llama 加载耗时、
  内存占用、回答速度与质量）、两台真机好友对讲仍待真机验；联网打听
  （`HuayuRules.DONT_KNOW` 那条路）仍是占位口径，未接搜索。
- **v3 模型替换时改哪几处**（只换模型，不动管线代码）：
  1. 把新 GGUF 发到 GitHub Release（新 tag，如 `model-17b-q4km-v3`，整文件
     传不上就分片上传，分片名保持 `banpai-17b-q4_k_m.gguf.part00–03` 格式）；
  2. `.github/workflows/android-debug.yml` 的下载 URL 与字节数、SHA256 核对值改成新的；
  3. `data/BanpaiLlama.kt` 里的 `MODEL_SIZE` / `MODEL_SHA256` 改成新的
     （文件名若变，`MODEL_ASSET` 一起改）；
  4. 按同一评测集重跑 20 题评测，通过门槛（马迭尔/红肠探针 + 4 道守栏题全过，
     20 题 ≥80%）过了才发版；兜底（`BanpaiGuardrails`）照常保留。

## 未验证清单（不要假装通过）

- **App：v0.3 已在 GitHub Actions 多次编绿并发布；带模型管线验证版已绿并发布（Release v0.3-c75a120，APK 1,863,920,972 字节 ≈ 1.74 GiB，含 1.7B Q4_K_M v1；CI run 44728173779）。** 本机无 JDK/Android SDK，未跑 `./gradlew assembleDebug`；`gradlew` 是占位脚本，需在 Android Studio 里生成标准 Wrapper。
- **固件：本机无 PlatformIO、无实板，未跑 `pio run` / `pio check`，未烧录**。`platformio.ini` 里的 LCD/触摸/IMU/AXP 引脚是公开资料默认值，未在实板验证；CO5300、QMI8658（敲击阈值 1.8g 需实测调）、CST9217、AXP2101 均未联调。
- **后端未验证**：短信/微信为 demo 模拟（固定码）；小模型未接（无服务地址）；月卡支付、PostgreSQL 切换、Alembic 迁移未验证；`app/main.py` 仍用已废弃的 `@app.on_event("startup")`（能用，有 DeprecationWarning）。Agora TokenBuilder 已接（`agora-token-builder==1.0.0`，24 测试全过），但真机入会仍待验证。
- **端到端未验证**：敲吧唧两下拍照、高德真地图（需 release 包，debug 包黑图是预期）、好友双人实时对讲（需登录接通 + 两台真机 + 录音权限）、房间 IM 真机、镜头变形/EXIF 方向实拍、OTA 回滚实测。
- 后端 `.venv/`（约 110MB）与 `banpai.db` 是本机验证产物，已列入 `.gitignore`，不要提交。

## 与旧版的口径差异（有意为之）

- 后端路由 80 条（旧版 63 条思路 + 本次补的同范围接口：好友消息列表、雪夜传信 nearby/pick、设备列表等，均在定版范围内，见 `backend/API.md`）。一起种芽 3 条（状态/浇水/领奖，2026-11-20 Jackson 定：奖励型，开花送限定装扮「芽开·丁香」）。
- 固件存储：位图本体放 LittleFS 单槽位文件、NVS 只存元数据（纯 NVS 装不下 466×466 RGB565 约 434KB 的位图），但“只存 1 壁纸 + 1 表盘、新的覆盖旧的”规则不变。
- 旧版踩过的坑（Compose 绘制中 Bitmap 不 recycle、TextureView 绑定时机、EXIF 方向、BLE notify 用原生 GATT、RGB565、偶数对齐等）已记在各端 README 的注意事项里，动手接真驱动/真相机前先读。
