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
        // 高德地图 SDK：2026-11-20 接入，3D 地图 10.0.600（与旧 67 提交版同款）。
        maven { url = uri("https://maven.amap.com/repository") }
    }
}
rootProject.name = "banpai"
include(":app")
