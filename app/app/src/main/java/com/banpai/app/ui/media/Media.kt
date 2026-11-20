package com.banpai.app.ui.media

import android.graphics.BitmapFactory
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 内置素材的两个显示组件：图片（assets 解码）与视频（Media3 ExoPlayer）。
 * 素材全部在 app/src/main/assets/（见 data/Assets.kt），离线可看，不走网络。
 * 找不到文件时显示灰底 + 文件名，绝不白屏。
 */

/** 从 assets 解码一张图（IO 线程解码，失败返回 null）。 */
@Composable
fun rememberAssetBitmap(assetPath: String): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, assetPath) {
        value = withContext(Dispatchers.IO) {
            try {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    return bitmap
}

/** 显示 assets 里的一张图；解不出来就显示灰底占位 + 文件名。 */
@Composable
fun AssetImage(
    assetPath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap = rememberAssetBitmap(assetPath)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier
                .background(Color(0xFF2A2F45)),
            contentAlignment = Alignment.Center
        ) {
            Text("图片缺失：$assetPath", color = Color(0xFFB9C0D8))
        }
    }
}

/**
 * 播 assets 里的一个视频（asset:/// 路径）。
 * @param loop 循环播（乱入 / 欢迎视频用）；播完停就 loop=false。
 * @param muted 默认静音（乱入循环播不打扰）；守护宠视频页可传 muted=false。
 * @param showControls 显示播放控制条（守护宠视频用）；乱入循环播不用。
 * 播不出来（缺文件/解码失败）时显示图片封面兜底，再不行显示文字，不白屏。
 */
@Composable
fun AssetVideo(
    assetPath: String,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    muted: Boolean = true,
    showControls: Boolean = false,
    posterAssetPath: String? = null
) {
    val context = LocalContext.current
    val player = remember(assetPath) {
        ExoPlayer.Builder(context).build().apply {
            volume = if (muted) 0f else 1f
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            setMediaItem(MediaItem.fromUri("asset:///$assetPath"))
            prepare()
            playWhenReady = true
        }
    }
    // 封面只在起播前盖着：首帧就绪后撤掉，避免一直盖住画面。
    var started by remember { mutableStateOf(false) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) started = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = showControls
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // 封面兜底：视频未起播前先盖一张图（有 poster 才盖，避免闪白）。
        if (posterAssetPath != null && !started) {
            AssetImage(
                assetPath = posterAssetPath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/** 一行视频占位条（列表里用的小尺寸视频位）——与 AssetVideo 同逻辑的轻量封装。 */
@Composable
fun AssetVideoBox(assetPath: String, posterAssetPath: String?, heightDp: Int = 200) {
    AssetVideo(
        assetPath = assetPath,
        modifier = Modifier.fillMaxWidth().height(heightDp.dp),
        loop = false,
        muted = false,
        showControls = true,
        posterAssetPath = posterAssetPath
    )
}
