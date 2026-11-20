// storage.h — 半拍 Banpai badge storage (single-slot rule, LOCKED).
//
// Rule: the badge stores exactly ONE wallpaper + ONE dial bitmap.
// A new one arriving from the phone overwrites the old one in place.
// The phone keeps the full library (30 dials, all photos); to change
// dial / wallpaper the phone must be nearby and re-send over BLE.
//
// Why NVS + LittleFS instead of NVS alone: a 466x466 RGB565 bitmap is
// ~434 KB, far beyond NVS. So:
//   NVS  (namespace "banpai") — small metadata only: current dial code,
//          wallpaper id + hash, clock style, tap mode, counters,
//          easter-egg unlock flag, serial number.
//   LittleFS single-slot files — the bitmap bodies:
//          /wallpaper.bin (ONE wallpaper), /dial.bin (ONE dial).
//   Writing a new bitmap truncates the same file — the old one is gone,
//   there is no second slot and no history. That IS the rule.
//
// 2.06 board note: the 2.06" panel is 410x502, not 466x466. Bitmaps
// delivered for 2.06 must be re-laid-out by the App (see README);
// the single-slot rule itself does not change.
#pragma once

#include <Arduino.h>

namespace storage {

// NVS keys (namespace "banpai"). Do not rename — the App never reads
// these directly, but OTA must preserve them across updates.
extern const char* kNvsNamespace;
extern const char* kKeyDialCode;      // uint8:  current dial 1..30
extern const char* kKeyClockStyle;    // uint8:  0..4
extern const char* kKeyTapMode;       // uint8:  0 normal / 1 camera
extern const char* kKeyWallpaperId;   // string: id of the stored wallpaper
extern const char* kKeyWallpaperHash; // string: sha/crc of stored wallpaper
extern const char* kKeyDialHash;      // string: sha/crc of stored dial bitmap
extern const char* kKeyGoldUnlocked;  // bool:   easter egg unlocked (dial 24)
extern const char* kKeyTapSeq;        // uint32: TAP notify sequence
extern const char* kKeySerial;        // string: device serial (set once)

extern const char* kWallpaperPath;    // "/wallpaper.bin" — the ONE slot
extern const char* kDialPath;         // "/dial.bin"      — the ONE slot
extern const uint32_t kMaxBitmapBytes;  // 466*466*2 = 434,312

void begin();  // init NVS + LittleFS. Call once in setup().

// --- metadata (NVS) -----------------------------------------------------
uint8_t getDialCode();              // default 1
void setDialCode(uint8_t code);     // validates 1..30; 24 needs goldUnlocked
uint8_t getClockStyle();            // default 0
void setClockStyle(uint8_t style);  // 0..4
uint8_t getTapMode();               // 0 normal / 1 camera
void setTapMode(uint8_t mode);
bool getGoldUnlocked();
void setGoldUnlocked(bool v);       // set ONLY via the easter-egg letter flow
uint32_t nextTapSeq();              // increments + persists the TAP sequence
String getSerial();                 // generated once, then stable

// --- single-slot bitmaps (LittleFS, overwrite-in-place) -----------------
// Each write truncates the slot file first: new overwrites old, no
// history is kept. Returns false if data is too large or write fails.
bool saveWallpaper(const uint8_t* data, size_t len, const String& id,
                   const String& hash);
bool saveDialBitmap(const uint8_t* data, size_t len, const String& hash);
bool hasWallpaper();
bool hasDialBitmap();
size_t wallpaperSize();
size_t dialBitmapSize();
String getWallpaperId();
String getWallpaperHash();
String getDialHash();

// True when the stored dial bitmap matches the current dial code's
// expected hash (i.e. the bitmap on the badge is the one the App sent
// for this dial). If false, the badge draws the placeholder dial and
// waits for the App to re-send — dial change needs the phone nearby.
bool dialBitmapPresentFor(uint8_t code);

} // namespace storage
