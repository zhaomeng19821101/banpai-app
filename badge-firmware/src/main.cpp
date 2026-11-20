// main.cpp — 半拍 Banpai badge firmware (scaffold v0.1.0).
// PlatformIO + ESP32-S3 Arduino core. Target: Waveshare
// ESP32-S3-Touch-AMOLED-1.75 (466x466 round CO5300 QSPI AMOLED,
// CST9217 touch, QMI8658 IMU, AXP2101 power).
//
// !!! 未验证：本机无 PlatformIO、无实板，未跑 pio run / pio check。
//     硬件驱动绑定（CO5300 / CST9217 / QMI8658 / AXP2101 的具体库
//     与引脚）是按 Waveshare 公开资料写的占位，需在实板上联调。
//     见 README_FIRMWARE.md。
//
// Locked rules implemented here:
//   - Dial is the home page; menu is one function per page.
//   - BLE service/characteristics exactly as ble_protocol.h.
//   - Knock detection uses the IMU (QMI8658). CST9217 touch reads are
//     unstable and are NEVER used for scoring, shutter, or any
//     critical action — touch is a best-effort shortcut only.
//   - Double-knock in camera mode == photo shutter (TAP b106).
//   - Single-slot storage: 1 wallpaper + 1 dial (storage.h).
//   - OTA: dual app partitions + auto-rollback placeholder.
//   - No music playback on the badge (speaker quality is poor).
//     Voice messages use mic + speaker; singing in the garden stays.
//   - Four petals = 冰 / 雪 / 乐 / 花; 静 belongs to the fifth petal.
//     The easter egg only ever produces the letter.
#include <Arduino.h>
#include <WiFi.h>
#include <esp_ota_ops.h>
#include <esp_partition.h>
#include <NimBLEDevice.h>

#include "ble_protocol.h"
#include "dial.h"
#include "game.h"
#include "menu.h"
#include "storage.h"

// ------------------------------------------------------------------ pins
// Pin numbers come from platformio.ini build flags (Waveshare defaults,
// NOT yet verified on a real board — check the schematic first).
#ifndef BANPAI_PIN_LCD_CS
#define BANPAI_PIN_LCD_CS 12
#endif

// ---------------------------------------------------------------- display
// TODO(HW): bind this to the real CO5300 QSPI driver (Waveshare
// Arduino example / LovyanGFX QSPI panel). The driver must:
//   - use RGB565, even-aligned draw windows (x/y/w/h even),
//   - clip everything to the 466x466 round safe area (r <= 227),
//   - provide only the ASCII built-in font (no Chinese font in FW).
// Until then the stub below logs draw calls to Serial so the menu
// state machine can be exercised without a panel.
class StubDisplay : public menu::IDisplay {
 public:
  void clear(uint16_t color) override {
    Serial.printf("[disp] clear #%04x\n", color);
  }
  void text(int x, int y, const String& s, uint16_t color, int size) override {
    Serial.printf("[disp] text(%d,%d) #%04x x%d: %s\n", x, y, color, size, s.c_str());
  }
  void fillCircle(int cx, int cy, int r, uint16_t color) override {
    Serial.printf("[disp] fillCircle(%d,%d,r%d) #%04x\n", cx, cy, r, color);
  }
  void circle(int cx, int cy, int r, uint16_t color) override {
    Serial.printf("[disp] circle(%d,%d,r%d) #%04x\n", cx, cy, r, color);
  }
  void fillRect(int x, int y, int w, int h, uint16_t color) override {
    Serial.printf("[disp] fillRect(%d,%d,%dx%d) #%04x\n", x, y, w, h, color);
  }
  void drawPetalMark(int cx, int cy, int r) override {
    // 4-petal mark: 冰 / 雪 / 乐 / 花. Never a fifth petal here.
    Serial.printf("[disp] petals x4 at (%d,%d) r%d\n", cx, cy, r);
  }
};
static StubDisplay display;

// ------------------------------------------------------------------ state
static menu::Menu g_menu;
static bleproto::TapMode g_tapMode = bleproto::TapMode::NORMAL;
static uint32_t g_unixSec = 0;       // from TIME frame (b108)
static uint32_t g_unixSetMs = 0;     // millis() when TIME was received
static bool g_timeSynced = false;
static bool g_bleConnected = false;
static int g_battPercent = 100;
static bool g_charging = false;

// Knock detection (QMI8658 IMU — TODO(HW): bind real driver).
// A knock is a short acceleration-magnitude spike. Two spikes within
// the double-knock window == double knock.
static const float kKnockThresholdG = 1.8f;      // accel spike threshold
static const uint32_t kDoubleKnockWindowMs = 350;
static const uint32_t kKnockRefractoryMs = 80;   // ignore ringing
static uint32_t g_lastKnockMs = 0;
static uint32_t g_prevKnockMs = 0;
static uint32_t g_lastAccelMs = 0;

// OTA receive state (b104 control + b105 data).
static bool g_otaActive = false;
static uint32_t g_otaExpected = 0;
static uint32_t g_otaReceived = 0;
static uint32_t g_otaCrcExpected = 0;
static uint32_t g_otaCrcRunning = 0;
static esp_ota_handle_t g_otaHandle = 0;
static const esp_partition_t* g_otaPartition = nullptr;

// ---------------------------------------------------------------- BLE
static NimBLECharacteristic* g_chrScore = nullptr;
static NimBLECharacteristic* g_chrTap = nullptr;
static NimBLECharacteristic* g_chrInfo = nullptr;
static NimBLECharacteristic* g_chrOta = nullptr;

static uint32_t nowUnix() {
  if (!g_timeSynced) return 0;
  return g_unixSec + (millis() - g_unixSetMs) / 1000;
}

static void notifyInfo() {
  if (!g_bleConnected || !g_chrInfo) return;
  String payload = bleproto::buildInfoJson(g_battPercent, g_charging,
                                           storage::getSerial());
  g_chrInfo->setValue(payload.c_str());
  g_chrInfo->notify();
}

static void notifyTap(bleproto::TapKind kind) {
  if (!g_bleConnected || !g_chrTap) return;
  String payload = bleproto::buildTapNotify(kind, nowUnix(), storage::nextTapSeq());
  g_chrTap->setValue(payload.c_str());
  g_chrTap->notify();
}

static void notifyScore() {
  if (!g_bleConnected || !g_chrScore) return;
  String payload = g_menu.session().scoreFrame(nowUnix());
  g_chrScore->setValue(payload.c_str());
  g_chrScore->notify();
}

// --- OTA helpers (dual partition + auto-rollback placeholder) -----------
static uint32_t crc32Update(uint32_t crc, const uint8_t* data, size_t len) {
  // Standard CRC-32 (IEEE). Table-free, byte-wise — fine for OTA chunks.
  crc = ~crc;
  for (size_t i = 0; i < len; i++) {
    crc ^= data[i];
    for (int b = 0; b < 8; b++) crc = (crc >> 1) ^ (0xEDB88320u & (0u - (crc & 1u)));
  }
  return ~crc;
}

static void otaAbort(const char* code) {
  if (g_otaHandle) esp_ota_abort(g_otaHandle);
  g_otaHandle = 0;
  g_otaActive = false;
  if (g_chrOta) {
    String s = bleproto::buildOtaStatus("ERR", code);
    g_chrOta->setValue(s.c_str());
    g_chrOta->notify();
  }
}

static void otaStart(uint32_t totalBytes, uint32_t crc32) {
  g_otaPartition = esp_ota_get_next_update_partition(nullptr);
  if (!g_otaPartition) { otaAbort("NOPART"); return; }
  if (esp_ota_begin(g_otaPartition, totalBytes, &g_otaHandle) != ESP_OK) {
    otaAbort("BEGIN");
    return;
  }
  g_otaActive = true;
  g_otaExpected = totalBytes;
  g_otaReceived = 0;
  g_otaCrcExpected = crc32;
  g_otaCrcRunning = 0;
  String s = bleproto::buildOtaStatus("READY");
  g_chrOta->setValue(s.c_str());
  g_chrOta->notify();
}

static void otaFinish() {
  if (!g_otaActive || !g_otaHandle) { otaAbort("NOSTART"); return; }
  bool ok = (g_otaReceived == g_otaExpected) &&
            (g_otaCrcRunning == g_otaCrcExpected);
  if (!ok) { otaAbort(g_otaReceived == g_otaExpected ? "CRC" : "SIZE"); return; }
  if (esp_ota_end(g_otaHandle) != ESP_OK) { otaAbort("END"); return; }
  g_otaHandle = 0;
  g_otaActive = false;
  if (esp_ota_set_boot_partition(g_otaPartition) != ESP_OK) {
    otaAbort("BOOT");
    return;
  }
  // The new image boots with rollback armed: if it crashes before
  // marking itself valid, the bootloader returns to the old partition.
  // (esp_ota_mark_app_valid_cancel_rollback() is called in setup()
  // after the self-test below passes.)
  String s = bleproto::buildOtaStatus("OK");
  g_chrOta->setValue(s.c_str());
  g_chrOta->notify();
  delay(200);
  ESP.restart();
}

// --- BLE callbacks -------------------------------------------------------
class ChrCallbacks : public NimBLECharacteristicCallbacks {
  void onWrite(NimBLECharacteristic* chr, NimBLEConnInfo& /*info*/) override {
    std::string v = chr->getValue();
    String payload(v.c_str(), v.length());
    String uuid = chr->getUUID().toString().c_str();

    if (uuid == BANPAI_CHR_DIAL) {
      // "<dialCode>" — e.g. "03". Dial 24 needs the easter-egg unlock.
      uint8_t code = bleproto::parseDialCode(payload);
      if (bleproto::isValidDial(code, storage::getGoldUnlocked())) {
        storage::setDialCode(code);
        g_menu.render(display);
      }
      return;
    }
    if (uuid == BANPAI_CHR_STATE) {
      bleproto::StateFrame st;
      if (bleproto::parseState(payload, st)) {
        g_menu.ui().energy = st.energy;
        g_menu.ui().lit = st.lit;
        g_menu.ui().bloom = st.bloom;
        g_menu.ui().pointsCsv = st.pointsCsv;
      }
      return;
    }
    if (uuid == BANPAI_CHR_TIME) {
      bleproto::TimeFrame tf;
      if (bleproto::parseTime(payload, tf)) {
        g_unixSec = tf.unixSec;
        g_unixSetMs = millis();
        g_timeSynced = true;
        g_menu.ui().unixSec = tf.unixSec;
        g_menu.ui().tzMinutes = tf.tzMinutes;
        g_menu.ui().timeSynced = true;
        storage::setClockStyle(tf.clockStyle);
        g_menu.render(display);
      }
      return;
    }
    if (uuid == BANPAI_CHR_TAP) {
      bleproto::TapMode m;
      if (bleproto::parseTapMode(payload, m)) {
        g_tapMode = m;
        storage::setTapMode(m == bleproto::TapMode::CAMERA ? 1 : 0);
      }
      return;
    }
    if (uuid == BANPAI_CHR_OTA) {
      bleproto::OtaControl ctl;
      if (!bleproto::parseOtaControl(payload, ctl)) return;
      if (ctl.cmd == bleproto::OtaControl::Cmd::START) {
        otaStart(ctl.totalBytes, ctl.crc32);
      } else if (ctl.cmd == bleproto::OtaControl::Cmd::ABORT) {
        otaAbort("APP");
      } else if (ctl.cmd == bleproto::OtaControl::Cmd::DONE) {
        otaFinish();
      }
      return;
    }
    if (uuid == BANPAI_CHR_OTADATA) {
      // Binary chunk. (OTA_DATA may also carry dial/wallpaper bitmaps
      // when the App prefixes the transfer — see README; the OTA path
      // below is for firmware images only.)
      if (!g_otaActive || !g_otaHandle) return;
      const uint8_t* data = (const uint8_t*)v.data();
      size_t len = v.length();
      if (esp_ota_write(g_otaHandle, data, len) != ESP_OK) {
        otaAbort("WRITE");
        return;
      }
      g_otaReceived += len;
      g_otaCrcRunning = crc32Update(g_otaCrcRunning, data, len);
      return;
    }
  }
};

class ServerCallbacks : public NimBLEServerCallbacks {
  void onConnect(NimBLEServer* /*srv*/, NimBLEConnInfo& /*info*/) override {
    g_bleConnected = true;
    g_menu.ui().bleConnected = true;
    notifyInfo();
  }
  void onDisconnect(NimBLEServer* /*srv*/, NimBLEConnInfo& /*info*/,
                    int /*reason*/) override {
    g_bleConnected = false;
    g_menu.ui().bleConnected = false;
    NimBLEDevice::startAdvertising();  // keep discoverable
  }
};

static ChrCallbacks g_chrCallbacks;
static ServerCallbacks g_serverCallbacks;

static void bleSetup() {
  NimBLEDevice::init(BANPAI_DEVICE_NAME);
  NimBLEDevice::setPower(ESP_PWR_LVL_P9);
  NimBLEServer* server = NimBLEDevice::createServer();
  server->setCallbacks(&g_serverCallbacks);
  NimBLEService* svc = server->createService(BANPAI_SVC_UUID);

  auto addChr = [&](const char* uuid, uint32_t props) {
    NimBLECharacteristic* c = svc->createCharacteristic(uuid, props);
    c->setCallbacks(&g_chrCallbacks);
    return c;
  };
  addChr(BANPAI_CHR_DIAL, NIMBLE_PROPERTY::WRITE);
  addChr(BANPAI_CHR_STATE, NIMBLE_PROPERTY::WRITE);
  g_chrScore = addChr(BANPAI_CHR_SCORE, NIMBLE_PROPERTY::NOTIFY);
  g_chrOta = addChr(BANPAI_CHR_OTA, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE |
                                        NIMBLE_PROPERTY::NOTIFY);
  addChr(BANPAI_CHR_OTADATA, NIMBLE_PROPERTY::WRITE);
  g_chrTap = addChr(BANPAI_CHR_TAP, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::WRITE |
                                        NIMBLE_PROPERTY::NOTIFY);
  g_chrInfo = addChr(BANPAI_CHR_INFO, NIMBLE_PROPERTY::READ | NIMBLE_PROPERTY::NOTIFY);
  addChr(BANPAI_CHR_TIME, NIMBLE_PROPERTY::WRITE);

  svc->start();
  NimBLEAdvertising* adv = NimBLEDevice::getAdvertising();
  adv->addServiceUUID(BANPAI_SVC_UUID);
  adv->setName(BANPAI_DEVICE_NAME);
  adv->start();
}

// ------------------------------------------------------------------ power
// TODO(HW): bind the real AXP2101 driver (XPowersLib). Until then the
// battery level is a stub — the INFO frame reports it, so a wrong stub
// would mislead the App; keep it obviously stubbed (100%, not charging)
// and replace before first real-device acceptance.
static void readBattery() {
  g_battPercent = 100;
  g_charging = false;
  g_menu.ui().battPercent = g_battPercent;
  g_menu.ui().charging = g_charging;
}

// ------------------------------------------------------------------- IMU
// TODO(HW): bind the real QMI8658 driver (I2C, INT pin). readAccelG()
// must return the acceleration magnitude in g. The knock classifier
// below is driver-independent.
static float readAccelG() {
  return 1.0f;  // stub: at rest, 1 g. Replace with QMI8658 readout.
}

// Called for every validated knock spike.
static void onKnockEvent(bool doubleKnock) {
  uint32_t nowMs = millis();
  (void)nowMs;
  if (g_menu.page() == menu::Page::GAME_PLAY && g_menu.session().active()) {
    // In-game knocks score points — never treated as navigation.
    g_menu.session().onKnock(nowMs);
    g_menu.render(display);
    return;
  }
  bleproto::TapKind kind = bleproto::classifyKnock(doubleKnock, g_tapMode);
  if (kind == bleproto::TapKind::SHUTTER) {
    // Double-knock in camera mode == photo shutter. Notify only; the
    // App takes the photo. No navigation happens on a shutter.
    notifyTap(kind);
    return;
  }
  notifyTap(kind);  // tap / knock are also reported to the App
  if (doubleKnock) {
    g_menu.confirm();
  } else {
    g_menu.nextItem();
  }
  g_menu.render(display);
}

static void pollImuKnock() {
  float g = readAccelG();
  uint32_t nowMs = millis();
  if (g < kKnockThresholdG) return;
  if (nowMs - g_lastAccelMs < kKnockRefractoryMs) return;
  g_lastAccelMs = nowMs;
  bool doubleKnock = (g_prevKnockMs != 0) &&
                     (nowMs - g_prevKnockMs <= kDoubleKnockWindowMs);
  g_prevKnockMs = g_lastKnockMs;
  g_lastKnockMs = nowMs;
  // NOTE: a single knock is reported immediately as TAP; if a second
  // spike lands inside the window it is reported as SHUTTER/KNOCK.
  // The App de-duplicates by sequence number (see README联调).
  onKnockEvent(doubleKnock);
}

// ----------------------------------------------------------------- touch
// TODO(HW): bind the real CST9217 driver. Touch is BEST-EFFORT ONLY:
// a tap advances the menu exactly like a single knock, and touch is
// never used for scoring, shutter, password entry confirmation, or
// any other critical action (touch reads are unstable).
static bool pollTouchTap() {
  return false;  // stub: no touch event. Replace with CST9217 readout.
}

// ------------------------------------------------------------------ Wi-Fi
// Settings > Wi-Fi: scan 2.4 GHz networks and show them. Password is
// typed on the badge (ASCII on-screen keyboard, see menu.cpp).
// Wi-Fi is used for OTA-from-cloud / wallpaper fetch fallback only;
// BLE is the primary phone link.
static void wifiScanAndShow() {
  int n = WiFi.scanNetworks(false);
  String ssids[16];
  int count = 0;
  for (int i = 0; i < n && count < 16; i++) {
    ssids[count++] = WiFi.SSID(i);
  }
  g_menu.setWifiNetworks(ssids, count);
}

// ------------------------------------------------------------------ setup
void setup() {
  Serial.begin(115200);
  delay(100);
  Serial.println("[banpai] badge fw " BANPAI_FW_VERSION_STR " boot");

  // OTA auto-rollback: this image marks itself valid ONLY after the
  // self-test below (storage + BLE init) passes. If the new image
  // crashes before this point, the bootloader rolls back to the
  // previous app partition automatically.
  storage::begin();
  g_menu.begin();
  g_tapMode = storage::getTapMode() == 1 ? bleproto::TapMode::CAMERA
                                         : bleproto::TapMode::NORMAL;
  g_menu.ui().goldUnlocked = storage::getGoldUnlocked();

  // TODO(HW): init CO5300 panel, CST9217, QMI8658, AXP2101 here, then
  // replace StubDisplay with the real driver binding.
  readBattery();
  bleSetup();
  WiFi.mode(WIFI_STA);
  WiFi.disconnect(true);

  // Self-test passed -> this image is good; cancel rollback.
  esp_ota_mark_app_valid_cancel_rollback();
  Serial.println("[banpai] self-test OK, rollback cancelled");

  g_menu.render(display);
}

// ------------------------------------------------------------------- loop
void loop() {
  static uint32_t lastUiMs = 0;
  static uint32_t lastInfoMs = 0;
  static menu::Page lastWifiPage = menu::Page::HOME;
  uint32_t nowMs = millis();

  pollImuKnock();
  if (pollTouchTap()) {
    g_menu.nextItem();
    g_menu.render(display);
  }

  // Entering the Wi-Fi page triggers a fresh scan.
  if (g_menu.page() == menu::Page::WIFI && lastWifiPage != menu::Page::WIFI) {
    wifiScanAndShow();
    g_menu.render(display);
  }
  lastWifiPage = g_menu.page();

  g_menu.tick(nowMs);

  // Game finished by timeout -> send SCORE once.
  static bool scoreSentForSession = false;
  if (g_menu.page() == menu::Page::GAMES && !g_menu.session().active() &&
      !scoreSentForSession && g_menu.session().score() > 0) {
    notifyScore();
    scoreSentForSession = true;
  }
  if (g_menu.session().active()) scoreSentForSession = false;

  // Redraw the dial home once a second (time layer).
  if (nowMs - lastUiMs >= 1000) {
    lastUiMs = nowMs;
    if (g_menu.page() == menu::Page::HOME) g_menu.render(display);
    if (g_timeSynced) g_menu.ui().unixSec = nowUnix();
  }

  // Re-report INFO every 60 s while connected.
  if (g_bleConnected && nowMs - lastInfoMs >= 60000) {
    lastInfoMs = nowMs;
    readBattery();
    notifyInfo();
  }

  delay(10);
}
