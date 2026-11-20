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
    }

    buildTypes {
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
    aaptOptions {
        noCompress("mp4")
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
    // 高德地图 SDK 接入点：探索页（ui/screens/ExploreScreen.kt）预留，包名 com.amap.api.maps。
    // 需在 settings.gradle.kts 追加高德 maven 仓库，并在 local.properties 配 AMAP_KEY。
    // implementation("com.amap.api:map3d:9.8.0")
    // implementation("com.amap.api:location:6.4.1")
    // Agora 语音接入点：派对/好友语音（ui/screens/PartyScreen.kt、ChatScreen.kt）预留。
    // implementation("io.agora.rtc:full-sdk:4.4.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
