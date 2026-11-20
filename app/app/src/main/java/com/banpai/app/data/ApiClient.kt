package com.banpai.app.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 半拍 Banpai — 后端 API 占位（Retrofit）。
 * 默认指向 Android 模拟器访问本机后端的地址 http://10.0.2.2:8000/api/v1。
 * 真机联调时改成局域网 / 部署地址。接口按后端 FastAPI 的 80 路由逐步补齐，
 * 此处只留健康检查 + 种芽 3 条，业务接口后续按需加。
 */
object ApiClient {
    const val BASE_URL = "http://10.0.2.2:8000/api/v1"

    /** 一起种芽状态：浇水次数 / 需浇 7 次 / 今天是否已浇 / 是否开花 / 是否已领奖。 */
    data class SproutStatus(
        val waters: Int,
        val waters_needed: Int,
        val watered_today: Boolean,
        val bloomed: Boolean,
        val claimed: Boolean,
        val reward_outfit: String
    )

    /** 好友对讲控件取 token 的响应（后端 /friends/{id}/space-token）。 */
    data class SpaceToken(
        val app_id: String,
        val token: String,
        val channel: String,
        val uid: Int,
        val expires_in: Int,
        val placeholder: Boolean
    )

    interface BanpaiApi {
        @GET("health")
        suspend fun health(): Map<String, String>

        // 一起种芽（奖励型，2026-11-20 定）：浇水 → 开花 → 领限定装扮「芽开·丁香」。
        // water() 的响应比 SproutStatus 多一个 detail 字段，Gson 会忽略多余字段。
        @GET("sprout")
        suspend fun sprout(): SproutStatus

        @POST("sprout/water")
        suspend fun waterSprout(): SproutStatus

        @POST("sprout/claim")
        suspend fun claimSprout(): Map<String, Any>

        // 好友对讲控件：好友 1:1 实时对讲取 token（声网）。auth 为登录 Bearer token；
        // placeholder=true 时不要真连，界面应提示原因（后端未配置证书或未部署）。
        @GET("friends/{friendId}/space-token")
        suspend fun friendSpaceToken(
            @Path("friendId") friendId: Int,
            @Header("Authorization") auth: String
        ): SpaceToken
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder().build()

    val api: BanpaiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BanpaiApi::class.java)
    }
}
