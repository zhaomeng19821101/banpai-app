// 半拍 Banpai — app 模块 build.gradle.kts
// 包名 com.banpai.app；Kotlin + Jetpack Compose；minSdk 26 / targetSdk 34。
// 目标机型：一加 Ace 2（1240×2772，20:9，约 412×924 dp）。
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.banpai.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.banpai.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-fresh" // 全新工程 v3 第一版
        vectorDrawables {
            useSupportLibrary = true
        }
        // 高德 Key 占位：local.properties 配 AMAP_KEY 后注入 BuildConfig；
        // 无 key 时 Explore 页只显示占位说明。不要把真 key 写进代码。
        // 注意：Gradle Kotlin DSL 脚本里不能写 java.util.Properties()，
        // 因为 java 这个名字被 Gradle 的 Java 插件扩展占了，会报 Unresolved reference: util；
        // 必须用文件顶部的 import java.util.Properties 再直接写 Properties()。
        // （2026-11-20 CI 第四跑的教训。）
        val localProps = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) localFile.inputStream().use { localProps.load(it) }
        buildConfigField("String", "AMAP_KEY", "\"${localProps.getProperty("AMAP_KEY", "")}\"")
        buildConfigField("String", "AGORA_APP_ID", "\"${localProps.getProperty("AGORA_APP_ID", "")}\"")
        // 高德 Key 注入 AndroidManifest 的 com.amap.api.v2.apikey；
        // CI 上无 local.properties 时为空串，探索页只显示占位说明，不创建 MapView。
        manifestPlaceholders["AMAP_KEY"] = localProps.getProperty("AMAP_KEY", "")

        // 端侧推理：llama.cpp（JNI，见 src/main/cpp/）。NDK 由 CI 装
        // ndk;27.2.12479018；瘦身（2026-11-20）：管线验证版只留 arm64-v8a，
        // 去掉 armeabi-v7a（jniLibs 里的 v7a .so 会被自动忽略，不用删文件），
        // 约省 30–40 MB；目标机 OnePlus Ace 2 是 arm64。正式版如需覆盖
        // 老机型，把 abiFilters 恢复成 arm64-v8a + armeabi-v7a 即可。
        ndkVersion = "27.2.12479018"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // llama.cpp 桥接：src/main/cpp/CMakeLists.txt（llama.cpp 源码由 CI 按 pin 的
    // commit 现 clone 到 src/main/cpp/llama.cpp，不进 git）。
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        // 固定 debug keystore（2026-11-20）：CI 每次构建用临时生成的 keystore，
        // 签名 SHA1 每次都变，高德控制台登记的 debug SHA1 只对某一个包有效。
        // 改用仓库里这份固定 debug.keystore（标准 android/android 口令，
        // 不是机密，可以进仓库），以后每次构建 SHA1 都一样。
        // SHA1：92:94:48:3F:DA:14:78:B6:A3:6B:C5:A3:03:01:C4:05:87:B4:C7:68
        // 高德控制台登记这一个 debug SHA1 即可。
        getByName("debug") {
            storeFile = file("../debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 素材全部内置：视频不压缩进包（ExoPlayer 可直接读 assets，加载快）；
    // 图片 webp/png 本身已压缩，noCompress 只影响存储方式，不影响体积。
    // gguf 模型也不压缩：1.1 GB 模型压缩后几乎不省体积，还会拖慢打包与解压，
    // 且 AssetManager 读未压缩 asset 才能直接拷到 filesDir 给 llama 用。
    aaptOptions {
        noCompress("mp4", "gguf")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // 后端 API：Retrofit 占位，指向 http://10.0.2.2:8000/api/v1（见 data/ApiClient.kt）
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // 视频播放：Media3 ExoPlayer（守护宠视频 / 绿幕乱入 / 欢迎视频）。
    // 素材在 app/src/main/assets/（见 tools/stage_assets.py 与 assets-manifest.json），
    // 播 asset:/// 路径，不走网络。
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // ---- 接入点占位（未启用，按需取消注释）----
    // CameraX 接入点：相机页（ui/screens/CameraScreen.kt）预留，拍照乱入用真绿幕视频。
    // implementation("androidx.camera:camera-core:1.3.4")
    // implementation("androidx.camera:camera-camera2:1.3.4")
    // implementation("androidx.camera:camera-lifecycle:1.3.4")
    // implementation("androidx.camera:camera-view:1.3.4")
    // 高德地图 SDK：本地内置（官网一键下载包里的合包 JAR，3D 地图 11.3.100 +
    // 搜索 9.8.1 + 定位 11.3.101；.so 在 src/main/jniLibs）。
    // 不用远程依赖：高德 maven 仓库（maven.amap.com）在 CI 上连不上
    // （2026-11-20 run 44695000131：Connect timed out），所以 SDK 直接放仓库里。
    implementation(files("libs/AMap3DMap_11.3.100_AMapSearch_9.8.1_AMapLocation_11.3.101_20260924.jar"))
    // 声网 RTC：只给好友 1:1 实时对讲（好友对讲控件）用；公共区域不用声网实时语音。
    implementation("io.agora.rtc:full-sdk:4.6.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
