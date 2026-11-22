// ble_protocol.h — 半拍 Banpai BLE protocol (badge side)
// Frame formats are LOCKED. Do not change without the App side.
//   Service UUID : 0000b001-0000-1000-8000-00805f9b34fb
//   DIAL  b101  App -> badge  write  "<dialCode>"
//   STATE b102  App -> badge  write  "S:<energy>|<lit>|<bloom>|<pointsCsv>"
//   SCORE b103  badge -> App  notify "G:<game>|<difficulty>|<score>|<timestamp>"
//   OTA   b104  bidirectional (control / status, see ble_protocol.cpp)
//   OTA_DATA b105 bidirectional (binary chunks)
//   TAP   b106  bidirectional
//           App -> badge  write  "M:camera" | "M:normal"
//           badge -> App  notify "T:<kind>|<timestamp>|<seq>"
//           kind: tap | knock  (shutter is DEPRECATED, see below)
//   CAMERA b109 bidirectional (added 2026-11-23, Jackson: photo key
//           replaces double-knock shutter — see TAP note)
//           badge -> App  notify "S:press|<timestamp>|<seq>"
//           App -> badge  write  "S:done" | "S:done|<timestamp>"
//   INFO  b107  badge -> App  notify JSON
//           {"name","fw","batt","chg","model","serial"}
//   TIME  b108  App -> badge  write  "T:<unixSec>|<tzMinutes>|<style0-4>"
//
// Photo shutter (2026-11-23, Jackson): the shutter moved OFF the knock
// gesture and onto an on-screen photo button. When the phone opens its
// camera it writes "M:camera" (b106, reused); the badge auto-opens its
// photo-button page; tapping the button notifies "S:press" on CAMERA
// (b109); the App takes the photo and writes back "S:done" on b109,
// and the badge shows "PHOTO TAKEN". Knocks NEVER take a photo — in
// photo mode a knock does nothing at all (anti-misfire). The old
// "T:shutter" TAP notify and TapKind::SHUTTER are DEPRECATED: kept on
// the wire only so old App builds still parse them, never emitted.
//
// Petal canon (locked): the four petals are 冰 / 雪 / 乐 / 花.
// 静 belongs to the fifth petal only. The easter egg only ever
// produces the letter — never a fifth-petal reveal on the badge UI.
#pragma once

#include <Arduino.h>

// ---------------------------------------------------------------- UUIDs
#define BANPAI_SVC_UUID   "0000b001-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_DIAL   "0000b101-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_STATE  "0000b102-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_SCORE  "0000b103-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_OTA    "0000b104-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_OTADATA "0000b105-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_TAP    "0000b106-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_INFO   "0000b107-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_TIME   "0000b108-0000-1000-8000-00805f9b34fb"
#define BANPAI_CHR_CAMERA "0000b109-0000-1000-8000-00805f9b34fb"

#define BANPAI_DEVICE_NAME "Banpai-Badge"
#define BANPAI_MODEL       "waveshare-1.75"
#define BANPAI_FW_VERSION_STR "0.2.0-ui"

namespace bleproto {

// ---------------------------------------------------------------- Types
// TapKind::SHUTTER is DEPRECATED (2026-11-23, Jackson: double-knock
// shutter cancelled, photo moved to the on-screen photo button, see
// CAMERA b109 above). The value is kept so the wire format stays
// compatible; classifyKnock() never returns it and main.cpp never
// emits a T:shutter notify.
enum class TapKind : uint8_t { TAP = 0, KNOCK = 1, SHUTTER = 2 /* DEPRECATED, never emitted */ };
enum class TapMode : uint8_t { NORMAL = 0, CAMERA = 1 };

struct StateFrame {
  int energy = 0;        // 冰晶 energy
  int lit = 0;           // 已点亮点位数 (0..24)
  int bloom = 0;         // 开花 0/1
  String pointsCsv;      // 已点亮点位 csv, e.g. "1,3,7"
  bool valid = false;
};

struct ScoreFrame {
  String game;           // bell | ice | stick  (敲钟/凿冰/咬冰棍)
  int difficulty = 1;    // 1..3
  int score = 0;
  uint32_t timestamp = 0;
};

struct TimeFrame {
  uint32_t unixSec = 0;
  int tzMinutes = 480;   // default Asia/Shanghai +08:00
  uint8_t clockStyle = 0; // 0..4, see dial.h
  bool valid = false;
};

struct OtaControl {
  // OTA b104 control strings (ASCII, App <-> badge):
  //   "OTA:START:<totalBytes>:<crc32hex>"  App -> badge
  //   "OTA:ABORT"                          App -> badge
  //   "OTA:READY" | "OTA:OK" | "OTA:ERR:<code>"  badge -> App
  // OTA_DATA b105 carries raw binary chunks; last chunk is followed by
  // "OTA:DONE" on b104, then the badge verifies CRC and reboots into
  // the other OTA partition (auto-rollback if the new image fails).
  enum class Cmd : uint8_t { NONE, START, ABORT, DONE, READY, OK, ERR };
  Cmd cmd = Cmd::NONE;
  uint32_t totalBytes = 0;
  uint32_t crc32 = 0;
  String errCode;
};

// --- parsing (App -> badge) -------------------------------------------
// Each returns false on malformed input; never throws.
bool parseState(const String& payload, StateFrame& out);
bool parseTime(const String& payload, TimeFrame& out);
bool parseTapMode(const String& payload, TapMode& out);
bool parseOtaControl(const String& payload, OtaControl& out);
// DIAL is a bare code string, e.g. "03" / "dial-03" / "24".
// Returns the dial number 1..30, or 0 if invalid / locked.
uint8_t parseDialCode(const String& payload);

// --- assembling (badge -> App) ----------------------------------------
String buildScore(const ScoreFrame& f);
String buildTapNotify(TapKind kind, uint32_t timestamp, uint32_t seq);
// CAMERA b109: badge -> App shutter press, "S:press|<ts>|<seq>".
String buildShutterPress(uint32_t timestamp, uint32_t seq);
// CAMERA b109: App -> badge photo-taken ack, "S:done" or
// "S:done|<timestamp>". Returns false on malformed input.
bool parseShutterDone(const String& payload, uint32_t* timestampOut = nullptr);
String buildInfoJson(int battPercent, bool charging, const String& serial);
String buildOtaStatus(const char* status, const char* detail = nullptr);

// --- helpers ------------------------------------------------------------
// Double-knock is NEVER a shutter any more (2026-11-23): it is just a
// knock, in every mode. The mode argument is kept for call-site
// compatibility and ignored. Photo mode knocks are dropped by the
// caller (main.cpp) before this is even reached.
TapKind classifyKnock(bool doubleKnock, TapMode mode);
bool isValidDial(uint8_t dial, bool goldUnlocked);

} // namespace bleproto
