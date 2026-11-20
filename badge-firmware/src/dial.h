// dial.h — 半拍 Banpai 30 watch dials + 5 clock styles (badge side).
// LOCKED list. Dial 24 (gold, hidden) is only usable after the
// easter-egg unlock; the badge never unlocks it on its own.
//
// Bitmaps are NOT compiled into firmware (old build already used ~78%
// of its app partition; OTA needs two app partitions). The 466x466
// dial artwork lives at ~/workspace/banpai/assets/watchfaces-round-v2/
// and is delivered by the phone App over BLE (DIAL + OTA_DATA), then
// stored in the single dial slot (see storage.h). This file only holds
// the table + the time-layer layout rules.
//
// Display rules (all dials, 466x466 round screen):
//   - Round safe area: centre (233,233), radius 233. Nothing is drawn
//     outside the circle. Keep a 6 px inner margin: usable r <= 227.
//   - All colours are RGB565. All draw windows are even-aligned
//     (x, y, w, h all even) — the CO5300 QSPI window requires it.
//   - Built-in font is ASCII only. Chinese on the badge is shown as
//     graphic glyphs / English placeholders (see menu.cpp); do not
//     embed a Chinese font in firmware.
//   - Time position is chosen automatically per dial style (see
//     ClockAnchor below), never hard-coded per dial in App code.
//   - Petal canon: four petals = 冰 / 雪 / 乐 / 花. 静 belongs to the
//     fifth petal only — no dial draws a fifth petal except dial 24.
#pragma once

#include <Arduino.h>

// RGB565 helpers (5-6-5). All badge colours go through these.
#define RGB565(r, g, b) \
  (uint16_t)(((uint16_t)(r) >> 3 << 11) | ((uint16_t)(g) >> 2 << 5) | ((uint16_t)(b) >> 3))
#define BANPAI_ICE_BLUE   RGB565(150, 200, 255)  // 冰蓝
#define BANPAI_SNOW_WHITE RGB565(240, 244, 255)  // 雪白
#define BANPAI_LILAC      RGB565(196, 160, 255)  // 丁香紫
#define BANPAI_GOLD       RGB565(232, 190, 90)   // 暖金
#define BANPAI_NIGHT      RGB565(10, 14, 34)     // 雪夜底
#define BANPAI_PLUM_RED   RGB565(200, 60, 70)    // 红梅
#define BANPAI_NEON_CYAN  RGB565(80, 240, 220)   // 霓虹青

// 466x466 round screen geometry (locked).
#define BANPAI_SCREEN_W 466
#define BANPAI_SCREEN_H 466
#define BANPAI_SCREEN_CX 233
#define BANPAI_SCREEN_CY 233
#define BANPAI_SCREEN_R 233
#define BANPAI_SAFE_R 227  // 6 px inner margin

// ---------------------------------------------------------------- Clock
// 5 clock styles, selected by TIME frame style 0..4 (App writes it;
// badge also remembers the last one in NVS).
enum class ClockStyle : uint8_t {
  CLASSIC_GOLD = 0,  // 经典金
  MINIMAL_WHITE = 1, // 极简白
  NEON_CYAN = 2,     // 霓虹青
  RED_PLUM = 3,      // 红梅
  FLIP = 4,          // 翻页
};

struct ClockStyleInfo {
  ClockStyle style;
  const char* id;        // ASCII id (badge font is ASCII-only)
  const char* nameEn;    // English placeholder label on badge
  uint16_t fg;           // RGB565 foreground
  uint16_t bg;           // RGB565 background (time layer only)
  bool showSeconds;
  bool showDate;
};

inline const ClockStyleInfo& clockStyleInfo(ClockStyle s) {
  static const ClockStyleInfo kStyles[5] = {
      {ClockStyle::CLASSIC_GOLD, "gold",   "CLASSIC GOLD", BANPAI_GOLD,       BANPAI_NIGHT,      false, true},
      {ClockStyle::MINIMAL_WHITE, "white", "MINIMAL WHITE", BANPAI_SNOW_WHITE, BANPAI_NIGHT,      false, false},
      {ClockStyle::NEON_CYAN,    "neon",   "NEON CYAN",    BANPAI_NEON_CYAN,  BANPAI_NIGHT,      true,  false},
      {ClockStyle::RED_PLUM,     "plum",   "RED PLUM",     BANPAI_PLUM_RED,   BANPAI_NIGHT,      false, true},
      {ClockStyle::FLIP,         "flip",   "FLIP",         BANPAI_SNOW_WHITE, RGB565(24, 28, 52), true,  true},
  };
  return kStyles[(uint8_t)s <= 4 ? (uint8_t)s : 0];
}

// Where the time layer sits on a dial. The dial artwork leaves one of
// these zones clear; the table below says which zone each dial uses.
// The time layer is drawn by firmware (text/segments), never baked
// into the delivered bitmap — so one bitmap works with all 5 styles.
enum class ClockAnchor : uint8_t {
  TOP = 0,     // time at top, Banpai figure lower-centre
  BOTTOM = 1,  // time at bottom, figure upper-centre
  CENTER = 2,  // time centred over a quiet background zone
};

// ---------------------------------------------------------------- Dials
struct DialInfo {
  uint8_t code;          // 1..30
  const char* id;        // ASCII id, e.g. "dial-01"
  const char* nameEn;    // English placeholder label (badge is ASCII-only)
  const char* nameZh;    // Chinese name (documentation / App side;
                         // badge shows nameEn or a graphic glyph)
  ClockAnchor anchor;    // where the time layer goes
  uint16_t accent;       // RGB565 accent colour for this dial
  bool hidden;           // true only for dial 24 (gold, easter egg)
};

// Locked 30-dial table. Order and names are fixed; do not renumber.
inline const DialInfo kDials[30] = {
    {1,  "dial-01", "ICE BLUE DAILY",   "冰蓝日常",       ClockAnchor::BOTTOM, BANPAI_ICE_BLUE,   false},
    {2,  "dial-02", "SNOW NIGHT",       "雪夜",           ClockAnchor::TOP,    BANPAI_NIGHT,      false},
    {3,  "dial-03", "SPRING LILAC",     "春日丁香",       ClockAnchor::BOTTOM, BANPAI_LILAC,      false},
    {4,  "dial-04", "AURORA NIGHT",     "极光之夜",       ClockAnchor::CENTER, BANPAI_NEON_CYAN,  false},
    {5,  "dial-05", "GOLDEN MORNING",   "金色晨光",       ClockAnchor::TOP,    BANPAI_GOLD,       false},
    {6,  "dial-06", "SNOW RED PLUM",    "雪夜红梅",       ClockAnchor::BOTTOM, BANPAI_PLUM_RED,   false},
    {7,  "dial-07", "SUMMER BLOOM",     "夏花",           ClockAnchor::TOP,    BANPAI_LILAC,      false},
    {8,  "dial-08", "SNOW RED COAT",    "雪乡红棉袄",     ClockAnchor::BOTTOM, BANPAI_PLUM_RED,   false},
    {9,  "dial-09", "CRYSTAL GOWN",     "冰晶礼服",       ClockAnchor::CENTER, BANPAI_ICE_BLUE,   false},
    {10, "dial-10", "CONCERT GOWN",     "音乐会礼服",     ClockAnchor::CENTER, BANPAI_GOLD,       false},
    {11, "dial-11", "SKI SPORT",        "滑雪运动装",     ClockAnchor::TOP,    BANPAI_ICE_BLUE,   false},
    {12, "dial-12", "ICE LANTERN NIGHT","冰灯夜游装",     ClockAnchor::BOTTOM, BANPAI_GOLD,       false},
    {13, "dial-13", "RUSSIAN FUR",      "俄式毛皮冬装",   ClockAnchor::BOTTOM, BANPAI_SNOW_WHITE, false},
    {14, "dial-14", "BAROQUE BALL",     "巴洛克舞会礼服", ClockAnchor::CENTER, BANPAI_GOLD,       false},
    {15, "dial-15", "SUMMER RIVERSIDE", "夏日江畔装",     ClockAnchor::TOP,    BANPAI_ICE_BLUE,   false},
    {16, "dial-16", "AUTUMN MAPLE",     "秋日枫叶装",     ClockAnchor::BOTTOM, BANPAI_PLUM_RED,   false},
    {17, "dial-17", "CENTRAL STREET",   "中央大街",       ClockAnchor::TOP,    BANPAI_GOLD,       false},
    {18, "dial-18", "ST SOPHIA",        "索菲亚",         ClockAnchor::CENTER, BANPAI_GOLD,       false},
    {19, "dial-19", "SONGHUA RIVER",    "松花江",         ClockAnchor::TOP,    BANPAI_ICE_BLUE,   false},
    {20, "dial-20", "SUN ISLAND",       "太阳岛",         ClockAnchor::BOTTOM, BANPAI_SNOW_WHITE, false},
    {21, "dial-21", "ICE SNOW WORLD",   "冰雪大世界",     ClockAnchor::CENTER, BANPAI_ICE_BLUE,   false},
    {22, "dial-22", "VOLGA MANOR",      "伏尔加庄园",     ClockAnchor::BOTTOM, BANPAI_GOLD,       false},
    {23, "dial-23", "SNOW WARM FLEECE", "雪夜暖绒",       ClockAnchor::BOTTOM, BANPAI_SNOW_WHITE, false},
    {24, "dial-24", "GOLD PETAL HIDDEN","金瓣隐藏",       ClockAnchor::CENTER, BANPAI_GOLD,       true},  // 彩蛋解锁后可用
    {25, "dial-25", "MINI Q",           "迷你Q版",        ClockAnchor::BOTTOM, BANPAI_LILAC,      false},
    {26, "dial-26", "WAVING",           "挥手",           ClockAnchor::TOP,    BANPAI_ICE_BLUE,   false},
    {27, "dial-27", "HEART",            "比心",           ClockAnchor::CENTER, BANPAI_PLUM_RED,   false},
    {28, "dial-28", "GOOD NIGHT",       "晚安",           ClockAnchor::BOTTOM, BANPAI_NIGHT,      false},
    {29, "dial-29", "CHECKIN OK",       "打卡成功",       ClockAnchor::CENTER, BANPAI_GOLD,       false},
    {30, "dial-30", "LISTEN",           "听一听",         ClockAnchor::TOP,    BANPAI_NEON_CYAN,  false},
};

inline const DialInfo* dialInfo(uint8_t code) {
  if (code < 1 || code > 30) return nullptr;
  return &kDials[code - 1];
}

// Time-layer geometry for an anchor (even-aligned window, inside the
// safe circle). Returns x, y, w, h via out params; all values even.
inline void clockWindow(ClockAnchor a, int& x, int& y, int& w, int& h) {
  switch (a) {
    case ClockAnchor::TOP:    x = 118; y = 44;  w = 230; h = 64; break;
    case ClockAnchor::BOTTOM: x = 118; y = 358; w = 230; h = 64; break;
    case ClockAnchor::CENTER:
    default:                  x = 118; y = 201; w = 230; h = 64; break;
  }
}
