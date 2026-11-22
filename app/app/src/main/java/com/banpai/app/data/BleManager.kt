package com.banpai.app.data

import java.util.UUID

/**
 * 半拍 Banpai — BLE 协议占位（吧唧 ESP32-S3 徽章）。
 * 全部为常量 + 帧格式说明 + 空实现骨架；真机联调时再接 android.bluetooth。
 *
 * Service UUID：0000b001-0000-1000-8000-00805f9b34fb
 * 特征码：DIAL b101、STATE b102、SCORE b103、OTA b104、OTA_DATA b105、
 *         TAP b106、INFO b107、TIME b108、CAMERA b109
 * 帧格式：
 *   DIAL  "<dialCode>"                        例："24"（金瓣隐藏表盘）
 *   STATE "S:<能量>|<已点亮>|<开花>|<点位csv>" 例："S:1280|5|0|1,3,7"
 *   SCORE "G:<游戏>|<难度>|<分数>|<时间戳>"   例："G:knock|1|120|1730000000"
 *   TAP   写 "M:camera" / "M:normal"（切换吧唧模式；M:camera 还会让吧唧
 *         自动弹出拍照键页，M:normal 让它回到之前的页）
 *   通知  "T:<kind>|<时间戳>|<序号>"          例："T:knock|1730000000|1"
 *         kind 只剩 tap / knock。T:shutter 已废弃（2026-11-23，Jackson：
 *         敲两下拍照取消，改为屏幕拍照键；固件永不再发 T:shutter）。
 *   CAMERA b109（2026-11-23 新增，与固件同一套定义）：
 *         吧唧→App 通知 "S:press|<时间戳>|<序号>"（点屏幕拍照键）
 *         App→吧唧 写 "S:done|<时间戳>"（拍照完成回执，吧唧显示“已拍照”）
 *   TIME  "T:<unix秒>|<时区分钟>|<样式0-4>"    例："T:1730000000|480|2"
 * 拍照键拍照：手机打开相机 → 写 M:camera → 吧唧自动弹出拍照键 → 点键发
 * S:press → App 执行 CameraX 快门 → 写 S:done → 吧唧显示“已拍照”；
 * 相机关闭写 M:normal。拍照模式下敲击不拍。
 */
object BleProtocol {
    val SERVICE_UUID: UUID = UUID.fromString("0000b001-0000-1000-8000-00805f9b34fb")

    // 特征 UUID：b101..b108（短码拼进 128-bit 蓝牙基 UUID）
    private fun charUuid(short: String): UUID =
        UUID.fromString("0000$short-0000-1000-8000-00805f9b34fb")

    val DIAL_UUID: UUID = charUuid("b101")
    val STATE_UUID: UUID = charUuid("b102")
    val SCORE_UUID: UUID = charUuid("b103")
    val OTA_UUID: UUID = charUuid("b104")
    val OTA_DATA_UUID: UUID = charUuid("b105")
    val TAP_UUID: UUID = charUuid("b106")
    val INFO_UUID: UUID = charUuid("b107")
    val TIME_UUID: UUID = charUuid("b108")
    val CAMERA_UUID: UUID = charUuid("b109")

    fun dialFrame(dialCode: String): String = "\"$dialCode\""

    fun stateFrame(energy: Int, litCount: Int, bloomed: Int, litCsv: String): String =
        "S:$energy|$litCount|$bloomed|$litCsv"

    fun scoreFrame(game: String, difficulty: Int, score: Int, timestamp: Long): String =
        "G:$game|$difficulty|$score|$timestamp"

    fun timeFrame(unixSeconds: Long, tzMinutes: Int, style0to4: Int): String =
        "T:$unixSeconds|$tzMinutes|$style0to4"

    /** 敲击通知解析：返回 kind（tap / knock）与序号，格式不对返回 null。
     *  T:shutter 已废弃（2026-11-23）：固件永不再发，收到也只当作未知 kind
     *  丢弃——调用方不要再按 shutter 触发快门。 */
    fun parseTapNotify(payload: String): Pair<String, Int>? {
        val parts = payload.split("|")
        if (parts.size != 3 || !parts[0].startsWith("T:")) return null
        val kind = parts[0].removePrefix("T:")
        if (kind == "shutter") return null // 已废弃：敲两下拍照取消
        val seq = parts[2].toIntOrNull() ?: return null
        return kind to seq
    }

    /** 吧唧拍照键通知 "S:press|<时间戳>|<序号>"：解析成功返回（时间戳，序号）。 */
    fun parseShutterPress(payload: String): Pair<Long, Int>? {
        val parts = payload.split("|")
        if (parts.size != 3 || parts[0] != "S:press") return null
        val timestamp = parts[1].toLongOrNull() ?: return null
        val seq = parts[2].toIntOrNull() ?: return null
        return timestamp to seq
    }

    /** 拍照完成回执帧 "S:done|<时间戳>"（App → 吧唧，写 CAMERA b109）。 */
    fun shutterDoneFrame(timestamp: Long): String = "S:done|$timestamp"
}

/**
 * 吧唧拍照键链路（2026-11-23，Jackson 定：拍照键替代敲两下快门）。
 * CameraScreen 在进入时调 onCameraOpened、离开时调 onCameraClosed；
 * 吧唧点拍照键后 onShutterPressed 被回调，App 执行拍照成功后再调
 * notifyPhotoTaken() 回执吧唧（吧唧显示“已拍照”）。
 * 真机联调前只有占位实现：StubBadgeCameraLink 只记调用，不真发 BLE。
 */
interface BadgeCameraLink {
    /** 相机页打开：通知吧唧弹出拍照键（M:camera）。 */
    fun onCameraOpened()

    /** 相机页关闭：通知吧唧回到之前的页（M:normal）。 */
    fun onCameraClosed()

    /** 吧唧拍照键按下时的回调；App 在此执行拍照。 */
    var onShutterPressed: (() -> Unit)?

    /** 拍照成功后回执吧唧（S:done），吧唧显示“已拍照”。拍照没成功不要调。 */
    fun notifyPhotoTaken()
}

/** BadgeCameraLink 占位实现：调 BleManager 的占位写方法，不真连 BLE。 */
class StubBadgeCameraLink(
    private val ble: BleManager = BleManager()
) : BadgeCameraLink {
    override var onShutterPressed: (() -> Unit)? = null
        set(value) {
            field = value
            ble.onShutterPress = value
        }

    override fun onCameraOpened() {
        ble.writeTapMode(camera = true) // 占位：真机写 TAP b106 "M:camera"
    }

    override fun onCameraClosed() {
        ble.writeTapMode(camera = false) // 占位：真机写 TAP b106 "M:normal"
    }

    override fun notifyPhotoTaken() {
        ble.writeShutterDone() // 占位：真机写 CAMERA b109 "S:done|<ts>"
    }
}

/**
 * BleManager 占位：扫描 / 连接 / 读写 / 通知订阅的空实现骨架。
 * 真机联调时用 BluetoothLeScanner + BluetoothGatt 实现，回调里按 BleProtocol 解析帧。
 */
class BleManager {
    var isConnected: Boolean = false
        private set

    /** 吧唧拍照键（CAMERA b109 "S:press"）到达时的回调；真机由 GATT 通知触发。 */
    var onShutterPress: (() -> Unit)? = null

    /** 测试钩子：模拟吧唧点拍照键（无真机时验证 CameraScreen 的快门接线用）。 */
    fun simulateBadgeShutterPress() {
        onShutterPress?.invoke()
    }

    /** 占位：开始扫描吧唧（Service UUID 过滤）。 */
    fun startScan() {
        // TODO 真机：BluetoothLeScanner.startScan(filter by SERVICE_UUID)
    }

    /** 占位：连接吧唧后订阅 TAP/INFO 通知。 */
    fun connect(deviceAddress: String) {
        // TODO 真机：device.connectGatt(...)，发现服务后订阅 TAP_UUID 通知
        isConnected = false // 占位：未真连，保持 false
    }

    /** 占位：下发表盘。 */
    fun writeDial(dialCode: String) {
        val frame = BleProtocol.dialFrame(dialCode)
        // TODO 真机：gatt 写 DIAL_UUID，frame.toByteArray()
    }

    /** 占位：切换吧唧模式 M:camera / M:normal。 */
    fun writeTapMode(camera: Boolean) {
        val frame = if (camera) "M:camera" else "M:normal"
        // TODO 真机：gatt 写 TAP_UUID，frame.toByteArray()
    }

    /** 占位：拍照完成回执 S:done（写 CAMERA b109，吧唧显示“已拍照”）。 */
    fun writeShutterDone() {
        val frame = BleProtocol.shutterDoneFrame(System.currentTimeMillis() / 1000)
        // TODO 真机：gatt 写 CAMERA_UUID，frame.toByteArray()
    }

    /** 占位：对时。 */
    fun writeTime(unixSeconds: Long, tzMinutes: Int, style0to4: Int) {
        val frame = BleProtocol.timeFrame(unixSeconds, tzMinutes, style0to4)
        // TODO 真机：gatt 写 TIME_UUID，frame.toByteArray()
    }

    fun disconnect() {
        isConnected = false
        // TODO 真机：gatt.disconnect() / close()
    }
}
