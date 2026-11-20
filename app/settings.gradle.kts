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
        // 高德 SDK 接入点（占位）：接入 AMap 时在此追加高德 maven 仓库，
        // 并在 local.properties 配 AMAP_KEY。无 key 时 Explore 页只显示占位说明，不画假地图。
        // maven { url = uri("https://maven.amap.com/repository") }
    }
}
rootProject.name = "banpai"
include(":app")
