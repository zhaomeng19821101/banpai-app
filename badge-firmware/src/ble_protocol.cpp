// ble_protocol.cpp — 半拍 Banpai BLE frame parse / assemble.
// Formats are LOCKED (see ble_protocol.h). Keep parsers strict: a
// malformed frame is dropped, never partially applied.
#include "ble_protocol.h"
#include "dial.h"

namespace bleproto {

static bool splitN(const String& s, char sep, String* out, int maxN) {
  int start = 0;
  int n = 0;
  for (int i = 0; i <= (int)s.length() && n < maxN; i++) {
    if (i == (int)s.length() || s[i] == sep) {
      out[n++] = s.substring(start, i);
      start = i + 1;
    }
  }
  return n == maxN;
}

static bool isDigits(const String& s) {
  if (s.length() == 0) return false;
  for (size_t i = 0; i < s.length(); i++) {
    if (!isDigit(s[i])) return false;
  }
  return true;
}

// "S:<energy>|<lit>|<bloom>|<pointsCsv>"
bool parseState(const String& payload, StateFrame& out) {
  out = StateFrame();
  if (!payload.startsWith("S:")) return false;
  String body = payload.substring(2);
  String parts[4];
  if (!splitN(body, '|', parts, 4)) return false;
  if (!isDigits(parts[0]) || !isDigits(parts[1]) || !isDigits(parts[2])) {
    return false;
  }
  out.energy = parts[0].toInt();
  out.lit = parts[1].toInt();
  out.bloom = parts[2].toInt();
  out.pointsCsv = parts[3];  // may be empty when lit == 0
  if (out.lit < 0 || out.lit > 24) return false;
  if (out.bloom != 0 && out.bloom != 1) return false;
  out.valid = true;
  return true;
}

// "T:<unixSec>|<tzMinutes>|<style0-4>"  (tzMinutes may be negative)
bool parseTime(const String& payload, TimeFrame& out) {
  out = TimeFrame();
  if (!payload.startsWith("T:")) return false;
  String body = payload.substring(2);
  String parts[3];
  if (!splitN(body, '|', parts, 3)) return false;
  if (!isDigits(parts[0])) return false;
  String tz = parts[1];  // tzMinutes, may be negative
  bool tzNeg = tz.startsWith("-");
  String tzDigits = tzNeg ? tz.substring(1) : tz;
  if (!isDigits(tzDigits) || !isDigits(parts[2])) return false;
  out.unixSec = (uint32_t)strtoul(parts[0].c_str(), nullptr, 10);
  out.tzMinutes = tz.toInt();
  out.clockStyle = (uint8_t)parts[2].toInt();
  if (out.clockStyle > 4) return false;
  out.valid = true;
  return true;
}

// "M:camera" | "M:normal"
bool parseTapMode(const String& payload, TapMode& out) {
  if (payload == "M:camera") { out = TapMode::CAMERA; return true; }
  if (payload == "M:normal") { out = TapMode::NORMAL; return true; }
  return false;
}

bool parseOtaControl(const String& payload, OtaControl& out) {
  out = OtaControl();
  if (payload == "OTA:ABORT") { out.cmd = OtaControl::Cmd::ABORT; return true; }
  if (payload == "OTA:DONE") { out.cmd = OtaControl::Cmd::DONE; return true; }
  if (payload.startsWith("OTA:START:")) {
    // "OTA:START:<totalBytes>:<crc32hex>"
    String body = payload.substring(10);
    int colon = body.indexOf(':');
    if (colon < 0) return false;
    String total = body.substring(0, colon);
    String crc = body.substring(colon + 1);
    if (!isDigits(total) || crc.length() == 0) return false;
    out.cmd = OtaControl::Cmd::START;
    out.totalBytes = (uint32_t)strtoul(total.c_str(), nullptr, 10);
    out.crc32 = (uint32_t)strtoul(crc.c_str(), nullptr, 16);
    return true;
  }
  return false;
}

// Accepts "03", "3", "dial-03", "dial-3". Returns 1..30, else 0.
// Dial 24 (gold, hidden) is rejected here unless goldUnlocked — the
// caller passes the easter-egg state from storage; the badge never
// unlocks it on its own.
uint8_t parseDialCode(const String& payload) {
  String s = payload;
  s.trim();
  if (s.startsWith("dial-")) s = s.substring(5);
  if (!isDigits(s)) return 0;
  int n = s.toInt();
  if (n < 1 || n > 30) return 0;
  return (uint8_t)n;
}

bool isValidDial(uint8_t dial, bool goldUnlocked) {
  if (dial < 1 || dial > 30) return false;
  if (dial == 24 && !goldUnlocked) return false;  // 金瓣隐藏：彩蛋解锁后可用
  return true;
}

// "G:<game>|<difficulty>|<score>|<timestamp>"
String buildScore(const ScoreFrame& f) {
  String s = "G:";
  s += f.game;
  s += "|";
  s += String(f.difficulty);
  s += "|";
  s += String(f.score);
  s += "|";
  s += String(f.timestamp);
  return s;
}

// "T:<kind>|<timestamp>|<seq>"  kind: tap | knock | shutter
String buildTapNotify(TapKind kind, uint32_t timestamp, uint32_t seq) {
  const char* k = "tap";
  if (kind == TapKind::KNOCK) k = "knock";
  if (kind == TapKind::SHUTTER) k = "shutter";
  String s = "T:";
  s += k;
  s += "|";
  s += String(timestamp);
  s += "|";
  s += String(seq);
  return s;
}

// {"name","fw","batt","chg","model","serial"} — exact key order.
String buildInfoJson(int battPercent, bool charging, const String& serial) {
  String s = "{\"name\":\"";
  s += BANPAI_DEVICE_NAME;
  s += "\",\"fw\":\"";
  s += BANPAI_FW_VERSION_STR;
  s += "\",\"batt\":";
  s += String(constrain(battPercent, 0, 100));
  s += ",\"chg\":";
  s += charging ? "true" : "false";
  s += ",\"model\":\"";
  s += BANPAI_MODEL;
  s += "\",\"serial\":\"";
  s += serial;
  s += "\"}";
  return s;
}

String buildOtaStatus(const char* status, const char* detail) {
  String s = "OTA:";
  s += status;
  if (detail && detail[0]) {
    s += ":";
    s += detail;
  }
  return s;
}

TapKind classifyKnock(bool doubleKnock, TapMode mode) {
  if (doubleKnock && mode == TapMode::CAMERA) return TapKind::SHUTTER;
  if (doubleKnock) return TapKind::KNOCK;
  return TapKind::TAP;
}

} // namespace bleproto
