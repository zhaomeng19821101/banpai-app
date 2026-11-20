// menu.h — 半拍 Banpai badge menu state machine.
// Structure (LOCKED): the DIAL is the home page. Entering the menu
// shows one function per page — no combined mega-pages.
//   HOME      dial home (time layer per dial anchor + 5 clock styles)
//   VOICE     语音留言： press-and-hold to talk, release to send,
//             speaker playback. (Badge HAS a speaker; music playback
//             is poor, so NO music playback on the badge.)
//   GARDEN    花园： public garden / 雪夜传信 (letter pickup + send)
//   FRIENDS   好友
//   PARTY     派对房间 (join / voice room; audio via phone + badge mic/speaker)
//   PHOTOS    照片： the ONE stored wallpaper; pick a new one = phone
//             re-sends over BLE (single-slot rule, see storage.h)
//   CHECKIN   打卡 (today's station, distance, confirm)
//   SETTINGS  设置 (brightness, clock style, tap mode, about)
//   WIFI      设置的子页： scan nearby networks, enter password on the
//             badge, connect. ESP32-S3 has built-in 2.4 GHz Wi-Fi.
//   GAMES     小游戏： 敲钟 / 凿冰 / 咬冰棍 (badge ONLY, no App page)
//             + one sub-page per game while playing.
//
// Navigation (touch CST9217 reads unstably — it must NEVER be the only
// path for a critical action):
//   - single knock / tap ......... next item / advance
//   - double knock ............... confirm / enter (in camera mode this
//                                  is the shutter instead — see main.cpp)
//   - touch tap .................. best-effort shortcut for the same
//                                  actions above; never required.
//   - long-press / edge swipe .... back to HOME (touch best-effort;
//                                  knock-hold fallback in code).
// All labels on the badge are ASCII (built-in font is ASCII-only);
// Chinese names live in the App and in comments here. Where the design
// calls for Chinese, the badge draws a graphic glyph placeholder.
#pragma once

#include <Arduino.h>
#include "dial.h"
#include "game.h"

namespace menu {

// Minimal display interface. main.cpp binds this to the real CO5300
// QSPI driver; until then a Serial stub is used (see main.cpp).
// All coordinates are pixels on the 466x466 round screen; every rect
// passed here must already be even-aligned (see draw helpers in menu.cpp).
struct IDisplay {
  virtual ~IDisplay() {}
  virtual void clear(uint16_t color) = 0;
  virtual void text(int x, int y, const String& s, uint16_t color, int size) = 0;
  virtual void fillCircle(int cx, int cy, int r, uint16_t color) = 0;
  virtual void circle(int cx, int cy, int r, uint16_t color) = 0;
  virtual void fillRect(int x, int y, int w, int h, uint16_t color) = 0;
  virtual void drawPetalMark(int cx, int cy, int r) = 0;  // 4-petal mark
};

enum class Page : uint8_t {
  HOME = 0,
  VOICE = 1,    // 语音留言
  GARDEN = 2,   // 花园 / 雪夜传信
  FRIENDS = 3,  // 好友
  PARTY = 4,    // 派对房间
  PHOTOS = 5,   // 照片（壁纸）
  CHECKIN = 6,  // 打卡
  SETTINGS = 7, // 设置
  WIFI = 8,     // 设置 > Wi-Fi
  GAMES = 9,    // 小游戏列表
  GAME_PLAY = 10,  // 小游戏进行中（敲钟/凿冰/咬冰棍）
};

// Menu order (what the user scrolls through from HOME).
inline const Page kMenuOrder[8] = {
    Page::VOICE, Page::GARDEN, Page::FRIENDS, Page::PARTY,
    Page::PHOTOS, Page::CHECKIN, Page::SETTINGS, Page::GAMES,
};

struct UiState {
  int energy = 0;       // from STATE frame
  int lit = 0;          // 已点亮 0..24
  int bloom = 0;        // 开花 0/1
  String pointsCsv;
  int battPercent = 100;
  bool charging = false;
  uint32_t unixSec = 0;  // from TIME frame; 0 = not synced yet
  int tzMinutes = 480;   // from TIME frame; default Asia/Shanghai +08:00
  bool timeSynced = false;
  bool bleConnected = false;
  bool wifiConnected = false;
  String wifiSsid;
  bool goldUnlocked = false;  // easter egg (dial 24)
  // 语音留言
  bool voiceRecording = false;
  bool voiceHasPlayback = false;
  // 花园 / 雪夜传信
  bool hasLetter = false;  // a letter waits (easter egg: only the letter)
  // 打卡
  String checkinStation = "NEXT STATION";
  int checkinMeters = -1;  // -1 = unknown
};

class Menu {
 public:
  void begin();
  Page page() const { return page_; }
  void goHome();
  void enterMenu();            // HOME -> first menu page
  void nextItem();             // single knock / tap: advance
  void confirm();              // double knock: enter / select
  void back();                 // back one level (never past HOME)
  void tick(uint32_t nowMs);   // per-page timers (game timeout etc.)
  void render(IDisplay& d);

  // Page-specific actions (called by confirm() or by main.cpp events).
  void selectGame(game::GameId id);
  void startGame(int difficulty);
  void endGameAndReport();     // ends session; main.cpp sends SCORE
  game::GameSession& session() { return session_; }
  UiState& ui() { return ui_; }
  const UiState& ui() const { return ui_; }
  void setWifiNetworks(const String* ssids, int count);
  int wifiCount() const { return wifiCount_; }

 private:
  Page page_ = Page::HOME;
  int menuIndex_ = 0;          // index into kMenuOrder
  int settingIndex_ = 0;       // 0 brightness, 1 clock style, 2 tap mode
  int wifiIndex_ = 0;
  int gameIndex_ = 0;
  game::GameSession session_;
  UiState ui_;
  String wifiSsids_[16];
  int wifiCount_ = 0;

  void renderHome(IDisplay& d);
  void renderVoice(IDisplay& d);
  void renderGarden(IDisplay& d);
  void renderFriends(IDisplay& d);
  void renderParty(IDisplay& d);
  void renderPhotos(IDisplay& d);
  void renderCheckin(IDisplay& d);
  void renderSettings(IDisplay& d);
  void renderWifi(IDisplay& d);
  void renderGames(IDisplay& d);
  void renderGamePlay(IDisplay& d);
  void renderHeader(IDisplay& d, const char* titleEn);
  void renderFooterHint(IDisplay& d, const char* hintEn);
};

} // namespace menu
