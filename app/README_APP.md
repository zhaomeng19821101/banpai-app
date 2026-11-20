# 半拍 Banpai — Android App 工程（全新工程 v3）

2026-11-20 04:05 定版：从新 UI 框架从头建，不沿用旧 67 次提交代码。
包名 `com.banpai.app`，Kotlin + Jetpack Compose，minSdk 26 / targetSdk 34。
目标机型：一加 Ace 2（1240×2772，20:9，约 412×924 dp）。

## 目录

```
app/                          Gradle 工程根目录（settings.gradle.kts 在此）
├── settings.gradle.kts
├── build.gradle.kts          根构建脚本
├── gradle.properties
├── local.properties.example  占位 key 名（复制为 local.properties 后填）
├── gradlew                   占位脚本（见下“编译验证状态”）
├── README_APP.md             本文件
└── app/                      应用模块
    ├── build.gradle.kts
    ├── proguard-rules.pro    占位
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/banpai/app/
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── ApiClient.kt       Retrofit 占位 → http://10.0.2.2:8000/api/v1
        │   │   ├── BleManager.kt      BLE 协议占位（见下）
        │   │   └── FakeRepository.kt  假数据：24 宠 / 30 表盘 / 16 装扮 /
        │   │                          30 美食+隐藏雪墩 / 19 篇日记 / 15 锁定台词
        │   ├── ui/theme/Theme.kt      昼夜三档 + BanpaiColors
        │   ├── ui/navigation/NavGraph.kt
        │   └── ui/screens/            16 屏（各一个 Composable，见“屏幕清单”）
        └── res/values/strings.xml、themes.xml、colors.xml
            res/drawable/ic_launcher_foreground.xml（启动图标前景占位）
            res/mipmap-anydpi-v26/ic_launcher.xml（自适应图标占位）
```

## 屏幕清单（16 屏，全部齐全）

| 屏 | 文件 | 状态 |
|---|---|---|
| 欢迎（视频→4图轮播） | WelcomeScreen.kt | v0.2：欢迎视频循环播 + 4 图轮播（真素材内置） |
| 认识半拍（4卡） | IntroScreen.kt | 完成：四瓣 冰/雪/乐/花，静留给第五瓣；v0.2：4 卡真图 + 跳过按钮 |
| 登录（手机号+短信码/微信/先逛逛） | LoginScreen.kt | 占位：不真发短信、不真调微信 |
| 首页（一屏不下拉，旅行连胜已删） | HomeScreen.kt | 完成；v0.2：英雄卡接半拍标准图 |
| 探索（真地图） | ExploreScreen.kt | 高德占位：无 key 显示「请在 local.properties 配 AMAP_KEY」，不画假地图；v0.2：打卡点列表接守护宠卡片真图 |
| 打卡 | CheckInScreen.kt | 完成：首次+80/再访+20/每点每天1次/拍照+10/乱入+5/天；v0.2：本站守护宠卡片真图 |
| 图鉴 | AtlasScreen.kt | 完成：24宠+30美食+隐藏雪墩；彩蛋只出信不播视频；v0.2：24 卡真图 + 点开播 10s 真视频 + 信全屏原图 + 好吃团 20 种有图 |
| 装扮（3槽） | DressUpScreen.kt | 完成：服装/套装、背景、特效；配饰去掉；17套（含芽开·丁香）；v0.2：17 套真图，修“穿上”bug，17 号锁定 |
| 相机 | CameraScreen.kt | CameraX 占位；v0.2：乱入真绿幕视频循环播（3 入场 + 开关）+ 8 大师风格真图 + 40 相框缩略图 + 表情库/半身像框真图 |
| 表盘工坊 | WatchfaceScreen.kt | 完成：30款；v0.2：30 款真图 + 大预览 + 走字时间；24 号金瓣锁定 |
| 花语（聊天） | ChatScreen.kt | 小模型+联网打听占位；只用15句锁定台词 lookup；v0.2：消息气泡列表 + 9 格表情真图 |
| 派对/好友 | PartyScreen.kt | Agora 占位；按住说话+打字键盘切换；v0.2：房间/好友切换 + 表情真图 |
| 公共花园 | GardenScreen.kt | 发帖二选一+保存/发花园/重拍；雪夜传信100m/24h；v0.2：本机选照片真能选 + 发完上墙标“刚刚” |
| 日记 | DiaryScreen.kt | 完成：纯文字不读出声；19篇（11-01～11-19），每篇结尾「晚安，明天见。」；v0.2：页顶迷你 Q 图 |
| 明信片 | PostcardScreen.kt | 完成：24张画廊+分享海报4张；v0.2：24+4 真图 + 点开放大 |
| 我的 | ProfileScreen.kt | 完成：昼夜三档切换入口；v0.2：头像换半拍标准图 |

底部 5 Tab：陪伴 / 探索 / 打卡 / 图鉴 / 我的，打卡为中间暖金凸起按钮。

## 锁死口径（已落进代码）

- 昼夜三档：白天 / 夜晚 / 跟随系统；白天明亮丁香光，夜晚雪夜灯火提亮不压抑；
  暖金 `#F5C97B` 是唯一行动色，冰蓝 `#7FC4E8`，丁香 `#C9BDF0`。
- 四瓣：冰、雪、乐、花；静留给第五瓣。半拍是哈尔滨一年四季孕育出来的精灵。
- 小游戏只在吧唧上，App 里不放游戏页（首页只留一句话说明）。
- 花语：小模型 + 联网打听，不接大模型兜底；不会就说「我还小，帮你去打听」，搜不到就老实承认。
- 日记纯文字、不读出声；11-01 是半拍生日，也是用户生日。

## BLE 协议占位（data/BleManager.kt）

Service `0000b001-0000-1000-8000-00805f9b34fb`；
DIAL b101、STATE b102、SCORE b103、OTA b104、OTA_DATA b105、TAP b106、INFO b107、TIME b108。
帧格式：DIAL `"<dialCode>"`、STATE `S:<能量>|<已点亮>|<开花>|<点位csv>`、
SCORE `G:<游戏>|<难度>|<分数>|<时间戳>`、TAP 写 `M:camera` / `M:normal`、
通知 `T:<kind>|<时间戳>|<序号>`、TIME `T:<unix秒>|<时区分钟>|<样式0-4>`。
敲吧唧两下拍照占位（T:shutter → CameraX 快门）。

## 密钥

**不要写任何真实密钥。** 高德 Key、Agora App ID/证书、LLM Key、keystore 全部用占位符：
`local.properties.example` 只有 `AMAP_KEY=`、`AGORA_APP_ID=` 两个空名。
复制为 `local.properties` 后填入真实值；`local.properties` 不应提交进仓库。

## 如何编译

需要：Android Studio（最新稳定版）+ JDK 17 + Android SDK 34（compileSdk/targetSdk 34）。
`local.properties` 需配 `sdk.dir`（Android Studio 首次打开会自动生成）；
无高德 Key 也能编译运行，只是探索页显示地图占位说明。

1. 用 Android Studio 打开 `~/workspace/banpai-code/app/`（Gradle 工程根目录）。
2. IDE 会提示生成标准 Gradle Wrapper（本机 `gradlew` 为占位脚本，见下）。
3. 同步 Gradle，连接真机或启动模拟器（建议 412×924 dp 左右，如 Pixel 7 Pro 近似）。
4. Run ▶。

**不用本地编译也能出包：GitHub Actions（2026-11-20 Jackson 定）**——把整个
`banpai-code` 推到 GitHub 仓库，工作流 `.github/workflows/android-debug.yml`
会自动编 debug APK（JDK 17 + SDK 34 + Gradle 8.7，CI 里不用占位 `gradlew`），
编完在 Actions → Artifacts 里下载。公开仓库免费无限（4 vCPU / 16 GB），
私有仓库每月 2000 分钟免费。详见根 README 的 App 一节。

## 编译验证状态（⚠️ 未验证：无 JDK/Android SDK，未跑 gradle build）

本机无 Java、无 Gradle，**没有跑过 `gradle build`**，不要假装它能跑。
已做的自查（2026-11-20）：

- Kotlin 语法自查：全部 `.kt` 文件括号/花括号配对检查通过，import 与使用项核对通过；
  16 个屏的 Composable 签名与 `NavGraph.kt` 的调用点一一核对通过。
- 文件清单核对：settings / 根+app build.gradle.kts / gradle.properties /
  local.properties.example / gradlew / AndroidManifest / MainActivity / Theme /
  NavGraph / ApiClient / BleManager / FakeRepository / 16 屏 / README 全部在位。
- 未验证项：Gradle 同步、Kotlin 编译、Compose 编译、APK 安装运行、真机联调
  （CameraX / 高德 / Agora / BLE 均为占位，需在有 SDK 的机器上逐项接通）。

## 还缺什么（下一步）

1. 在有 Android Studio + SDK 34 的机器上生成标准 Gradle Wrapper，跑通 `./gradlew assembleDebug`。
2. 接入真资源：欢迎视频/4 图轮播、绿幕乱入视频（`~/workspace/banpai/assets/greenscreen/`，
   引用即可，不复制大文件）、守护宠卡片/表盘/装扮美术资源、正式启动图标。
3. 接入高德 SDK（配 AMAP_KEY）：真地图、步行路线、80-150m 地理围栏自动打卡。
4. 接入 CameraX：取景、拍照/录像、乱入绿幕抠像、敲吧唧两下快门端到端。
5. 接入后端 FastAPI（`http://10.0.2.2:8000/api/v1`）：把 FakeRepository 逐步替换为真接口。
6. 接入 BLE 真机：扫描/连接/订阅 TAP 通知/DIAL 下发/OTA。
7. 接入 Agora（配 AGORA_APP_ID）：派对语音、好友一对一语音。
8. 花语小模型 + 联网打听的后端落点（不接大模型兜底）。
