package com.banpai.app.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.banpai.app.BuildConfig
import com.banpai.app.data.GUARDIAN_PETS
import com.banpai.app.data.guardianCardAsset
import com.banpai.app.ui.media.AssetImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 探索：地图是主角。2026-11-20 接入高德 3D 地图（官网合包 JAR 本地内置，
 * 3D 地图 11.3.100 + 搜索 9.8.1 + 定位 11.3.101）：
 * 真 MapView + 24 只守护宠落点（已点亮金点、未点亮灰点、下一站脉动）。
 * 中心哈尔滨中央大街；＋/－ 用高德原生缩放按钮，双指缩放是 AMap 默认手势。
 *
 * 注意：这个 Key 只登记了 release SHA1，CI 出的 debug 包地图仍会是黑的——
 * 这是预期内的；真机验地图要打 release 包，或在高德控制台补登记 debug SHA1。
 * 无 Key 时仍只显示占位说明，不创建 MapView，不装成真的。
 * 真实步行路线 / 距离 / 地理围栏自动打卡尚未接：路线规划与定位权限合规是下一步。
 */
@Composable
fun ExploreScreen(
    onOpenCheckIn: () -> Unit,
    onOpenAtlas: () -> Unit = {}
) {
    val hasKey = BuildConfig.AMAP_KEY.isNotEmpty()
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader("探索 · 地图是主角", "高德真地图 · 24 只守护宠落点")
        if (!hasKey) {
            InfoCard("地图占位：请在 local.properties 配 AMAP_KEY\n" +
                "接入后显示高德真地图：松花江 / 中央大街 / 索菲亚真实轮廓、" +
                "真实步行路线 + 距离、80-150m 地理围栏自动打卡。")
        } else {
            BanpaiMapView(
                modifier = Modifier.fillMaxWidth().height(380.dp).padding(horizontal = 16.dp)
            )
            InfoCard("下一站：索菲亚·钟（地图上脉动中）· 金点是已点亮，灰点是未点亮")
        }
        InfoCard("地理围栏自动打卡占位：进入点位 80-150m 自动打卡，需定位权限 + 后台定位合规")
        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            itemsIndexed(GUARDIAN_PETS) { index, pet ->
                val lit = index < 5 // 假数据：前 5 只已点亮（金点），其余未亮（灰点）
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    AssetImage(
                        assetPath = guardianCardAsset(index),
                        contentDescription = pet.title,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${if (lit) "● 已亮" else "○ 未亮"} ${pet.title}")
                        Text(pet.line)
                        Text("声音：${pet.sound}")
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("缩放：＋/－（AMap 原生缩放；真机上还可双指缩放）", modifier = Modifier.padding(horizontal = 16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedButton(onClick = onOpenCheckIn, modifier = Modifier.weight(1f)) { Text("去打卡") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onOpenAtlas, modifier = Modifier.weight(1f)) { Text("图鉴") }
        }
    }
}

// 下一站：前 5 只已点亮（与列表假数据一致），第 6 只索菲亚·钟是下一站。
private const val NEXT_STATION_INDEX = 5
private const val CENTER_LAT = 45.7750 // 中央大街
private const val CENTER_LNG = 126.6175

/** 高德真地图：MapView + 24 落点。只在有 Key 时创建（见 ExploreScreen）。 */
@Composable
private fun BanpaiMapView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var nextMarker by remember { mutableStateOf<Marker?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    // MapView 生命周期跟随当前 LifecycleOwner。
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView?.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
            mapView = null
        }
    }

    // 下一站点脉动：透明度 1.0 ↔ 0.35 循环。只动透明度，不动位置。
    LaunchedEffect(nextMarker) {
        val marker = nextMarker ?: return@LaunchedEffect
        while (isActive) {
            marker.alpha = 0.35f
            delay(650)
            marker.alpha = 1f
            delay(650)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                onCreate(null)
                mapView = this
                val aMap: AMap = map ?: return@apply
                aMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(CENTER_LAT, CENTER_LNG), 14.5f)
                )
                // ＋/－ 用高德原生缩放按钮；双指缩放是 AMap 默认手势，不用另写。
                aMap.uiSettings.isZoomControlsEnabled = true
                aMap.uiSettings.isZoomGesturesEnabled = true
                aMap.setOnMarkerClickListener { marker ->
                    marker.showInfoWindow()
                    true
                }
                val goldDot = dotBitmap(0xFFF5C97B.toInt(), 0xFFFFFFFF.toInt(), 28, 14)
                val greyDot = dotBitmap(0xFF9AA3B2.toInt(), 0xFFFFFFFF.toInt(), 24, 12)
                val nextDot = dotBitmap(0xFFE9A83C.toInt(), 0xFFFFFFFF.toInt(), 44, 22)
                GUARDIAN_PETS.forEachIndexed { index, pet ->
                    if (pet.lat == 0.0 && pet.lng == 0.0) return@forEachIndexed
                    val marker = aMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(pet.lat, pet.lng))
                            .title(pet.title)
                            .snippet(pet.line)
                            .icon(
                                BitmapDescriptorFactory.fromBitmap(
                                    when {
                                        index == NEXT_STATION_INDEX -> nextDot
                                        index < NEXT_STATION_INDEX -> goldDot
                                        else -> greyDot
                                    }
                                )
                            )
                    )
                    if (index == NEXT_STATION_INDEX) nextMarker = marker
                }
            }
        }
    )
}

/** 在 Bitmap 上画一个带白边的实心圆点，供 Marker 图标用。 */
private fun dotBitmap(fillColor: Int, strokeColor: Int, sizePx: Int, radiusPx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = strokeColor
    }
    val c = sizePx / 2f
    canvas.drawCircle(c, c, radiusPx.toFloat(), paint)
    canvas.drawCircle(c, c, radiusPx.toFloat(), stroke)
    return bitmap
}
