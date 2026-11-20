// 半拍 Banpai — 全新工程（2026-11-20 从新框架从头建，不沿用旧 67 次提交代码）
// 根 settings：只含 :app 一个模块。
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 高德 maven 仓库不用：maven.amap.com 在 CI 上连不上
        // （2026-11-20 run 44695000131：Connect timed out），
        // 高德 SDK 已本地内置（app/app/libs/*.jar + src/main/jniLibs）。
        // maven { url = uri("https://maven.amap.com/repository") }
    }
}
rootProject.name = "banpai"
include(":app")
