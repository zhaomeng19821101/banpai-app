package com.banpai.app.data

import java.util.UUID

/**
 * 半拍 Banpai — BLE 协议占位（吧唧 ESP32-S3 徽章）。
 * 全部为常量 + 帧格式说明 + 空实现骨架；真机联调时再接 android.bluetooth。
 *
 * Service UUID：0000b001-0000-1000-8000-00805f9b34fb
 * 特征码：DIAL b101、STATE b102、SCORE b103、OTA b104、OTA_DATA b105、
 *         TAP b106、INFO b107、TIME b108
 * 帧格式：
 *   DIAL  "<dialCode>"                        例："24"（金瓣隐藏表盘）
 *   STATE "S:<能量>|<已点亮>|<开花>|<点位csv>" 例："S:1280|5|0|1,3,7"
 *   SCORE "G:<游戏>|<难度>|<分数>|<时间戳>"   例："G:knock|1|120|1730000000"
 *   TAP   写 "M:camera" / "M:normal"（切换吧唧模式）
 *   通知  "T:<kind>|<时间戳>|<序号>"          例："T:shutter|1730000000|1"
 *   TIME  "T:<unix秒>|<时区分钟>|<样式0-4>"    例："T:1730000000|480|2"
 * 敲吧唧两下拍照：吧唧发 TAP 通知 T:shutter，App 收到后触发 CameraX 快门（见 CameraScreen）。
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

    fun dialFrame(dialCode: String): String = "\"$dialCode\""

    fun stateFrame(energy: Int, litCount: Int, bloomed: Int, litCsv: String): String =
        "S:$energy|$litCount|$bloomed|$litCsv"

    fun scoreFrame(game: String, difficulty: Int, score: Int, timestamp: Long): String =
        "G:$game|$difficulty|$score|$timestamp"

    fun timeFrame(unixSeconds: Long, tzMinutes: Int, style0to4: Int): String =
        "T:$unixSeconds|$tzMinutes|$style0to4"

    /** 敲两下拍照的通知解析：返回 kind（例 shutter）与序号，格式不对返回 null。 */
    fun parseTapNotify(payload: String): Pair<String, Int>? {
        val parts = payload.split("|")
        if (parts.size != 3 || !parts[0].startsWith("T:")) return null
        val seq = parts[2].toIntOrNull() ?: return null
        return parts[0].removePrefix("T:") to seq
    }
}

/**
 * BleManager 占位：扫描 / 连接 / 读写 / 通知订阅的空实现骨架。
 * 真机联调时用 BluetoothLeScanner + BluetoothGatt 实现，回调里按 BleProtocol 解析帧。
 */
class BleManager {
    var isConnected: Boolean = false
        private set

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
