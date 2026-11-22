// storage.cpp — single-slot NVS + LittleFS store (see storage.h).
// Rule enforced here: ONE wallpaper file, ONE dial file, overwrite in
// place. There is deliberately no list / no second slot / no delete
// of "the other one" — there is no other one.
#include "storage.h"

#include <LittleFS.h>
#include <Preferences.h>

namespace storage {

const char* kNvsNamespace = "banpai";
const char* kKeyDialCode = "dial";
const char* kKeyClockStyle = "clk";
const char* kKeyTapMode = "tapmode";
const char* kKeyWallpaperId = "wpid";
const char* kKeyWallpaperHash = "wphash";
const char* kKeyDialHash = "dialhash";
const char* kKeyGoldUnlocked = "gold";
const char* kKeyTapSeq = "tapseq";
const char* kKeySerial = "serial";
const char* kKeyBrightness = "brt";
const char* kKeyVolume = "vol";
const char* kKeyBestBell = "bbell";
const char* kKeyBestIce = "bice";
const char* kKeyBestStick = "bstick";

const char* kWallpaperPath = "/wallpaper.bin";
const char* kDialPath = "/dial.bin";
const uint32_t kMaxBitmapBytes = 466u * 466u * 2u;  // RGB565, 434,312 B

static Preferences prefs;
static bool begun = false;

void begin() {
  if (begun) return;
  begun = true;
  prefs.begin(kNvsNamespace, false);
  if (!LittleFS.begin(true)) {
    // LittleFS mount failed even after format attempt: bitmap slots
    // are unavailable; metadata still works. The UI shows the
    // placeholder dial until this is fixed (see README troubleshooting).
    Serial.println("[storage] LittleFS mount FAILED");
  }
  if (getSerial().length() == 0) {
    // Stable serial, generated once from the chip id. Never a secret.
    uint64_t mac = ESP.getEfuseMac();
    char buf[24];
    snprintf(buf, sizeof(buf), "BP-%06llX", (unsigned long long)(mac & 0xFFFFFF));
    prefs.putString(kKeySerial, buf);
  }
}

static uint8_t getU8(const char* key, uint8_t def) {
  return prefs.isKey(key) ? prefs.getUChar(key, def) : def;
}

uint8_t getDialCode() {
  uint8_t c = getU8(kKeyDialCode, 1);
  return (c >= 1 && c <= 30) ? c : 1;
}

void setDialCode(uint8_t code) {
  if (code < 1 || code > 30) return;
  if (code == 24 && !getGoldUnlocked()) return;  // 金瓣隐藏：未解锁不存
  prefs.putUChar(kKeyDialCode, code);
}

uint8_t getClockStyle() {
  uint8_t s = getU8(kKeyClockStyle, 0);
  return s <= 4 ? s : 0;
}

void setClockStyle(uint8_t style) {
  if (style > 4) return;
  prefs.putUChar(kKeyClockStyle, style);
}

uint8_t getTapMode() { return getU8(kKeyTapMode, 0) == 1 ? 1 : 0; }

void setTapMode(uint8_t mode) { prefs.putUChar(kKeyTapMode, mode == 1 ? 1 : 0); }

bool getGoldUnlocked() { return prefs.getBool(kKeyGoldUnlocked, false); }

void setGoldUnlocked(bool v) { prefs.putBool(kKeyGoldUnlocked, v); }

uint32_t nextTapSeq() {
  uint32_t s = prefs.getUInt(kKeyTapSeq, 0) + 1;
  prefs.putUInt(kKeyTapSeq, s);
  return s;
}

String getSerial() { return prefs.getString(kKeySerial, ""); }

uint8_t getBrightness() {
  uint8_t v = getU8(kKeyBrightness, 80);
  return v > 100 ? 80 : v;
}

void setBrightness(uint8_t v) {
  prefs.putUChar(kKeyBrightness, v > 100 ? 100 : v);
}

uint8_t getVolume() {
  uint8_t v = getU8(kKeyVolume, 70);
  return v > 100 ? 70 : v;
}

void setVolume(uint8_t v) {
  prefs.putUChar(kKeyVolume, v > 100 ? 100 : v);
}

static const char* bestKey(int gameId) {
  if (gameId == 2) return kKeyBestIce;
  if (gameId == 3) return kKeyBestStick;
  return kKeyBestBell;  // 1 = bell, default
}

int getBestScore(int gameId) {
  if (gameId < 1 || gameId > 3) return 0;
  return (int)prefs.getUInt(bestKey(gameId), 0);
}

void setBestScore(int gameId, int score) {
  if (gameId < 1 || gameId > 3 || score <= 0) return;
  const char* key = bestKey(gameId);
  uint32_t cur = prefs.getUInt(key, 0);
  if ((uint32_t)score > cur) prefs.putUInt(key, (uint32_t)score);
}

// --- single-slot bitmap writes: truncate the same file, then write. ----
static bool saveSlot(const char* path, const uint8_t* data, size_t len) {
  if (len == 0 || len > kMaxBitmapBytes || data == nullptr) return false;
  File f = LittleFS.open(path, "w");  // "w" truncates: overwrite in place
  if (!f) return false;
  size_t written = f.write(data, len);
  f.close();
  return written == len;
}

bool saveWallpaper(const uint8_t* data, size_t len, const String& id,
                   const String& hash) {
  if (!saveSlot(kWallpaperPath, data, len)) return false;
  prefs.putString(kKeyWallpaperId, id);
  prefs.putString(kKeyWallpaperHash, hash);
  return true;
}

bool saveDialBitmap(const uint8_t* data, size_t len, const String& hash) {
  if (!saveSlot(kDialPath, data, len)) return false;
  prefs.putString(kKeyDialHash, hash);
  return true;
}

static bool slotExists(const char* path, size_t& sizeOut) {
  File f = LittleFS.open(path, "r");
  if (!f) return false;
  sizeOut = f.size();
  f.close();
  return sizeOut > 0;
}

bool hasWallpaper() {
  size_t s = 0;
  return slotExists(kWallpaperPath, s);
}

bool hasDialBitmap() {
  size_t s = 0;
  return slotExists(kDialPath, s);
}

size_t wallpaperSize() {
  size_t s = 0;
  slotExists(kWallpaperPath, s);
  return s;
}

size_t dialBitmapSize() {
  size_t s = 0;
  slotExists(kDialPath, s);
  return s;
}

String getWallpaperId() { return prefs.getString(kKeyWallpaperId, ""); }
String getWallpaperHash() { return prefs.getString(kKeyWallpaperHash, ""); }
String getDialHash() { return prefs.getString(kKeyDialHash, ""); }

bool dialBitmapPresentFor(uint8_t code) {
  // The badge cannot verify the bitmap's dial code from the pixels
  // alone; the App sends the expected hash with the DIAL frame, and
  // the bitmap is accepted only with a matching hash (checked by the
  // OTA_DATA / DIAL receive path in main.cpp). Here: present + hashed.
  if (code < 1 || code > 30) return false;
  return hasDialBitmap() && getDialHash().length() > 0;
}

} // namespace storage
