// menu.h — 半拍 Banpai badge menu state machine (UI v0.2.0-ui).
// Structure (LOCKED): the DIAL is the home page. Tapping the dial (or
// double-knock) opens the function MENU — one row per function, one
// function per page. No combined mega-pages.
//   HOME      dial home (time layer per dial anchor + 5 clock styles)
//             Single knock cycles 3 info pages: time -> steps/energy/
//             lit -> next station. Double-knock / tap opens the menu.
//   CAMERA_SHUTTER  拍照键页 (2026-11-23, Jackson: replaces the old
//             double-knock shutter): auto-opens when the App writes
//             "M:camera" (b106). Big round photo button in the centre
//             of the 466 round safe area; tapping the button sends
//             "S:press" on CAMERA (b109); the App answers "S:done"
//             and the page shows "PHOTO TAKEN". Knocks do NOTHING on
//             this page (anti-misfire: only the button takes a photo).
//             "M:normal" (or back) returns to the page that was open.
//   MENU      function list (11 rows): voice, garden, friends, party,
//             games, photos, check-in, settings, notifications,
//             find phone, steps/energy. Single knock = next row,
//             double knock = enter. Back (long-press) = HOME.
//   VOICE     语音留言： hold to talk, release to send, speaker
//             playback. (Badge HAS a speaker; music playback is poor,
//             so NO music playback on the badge.) Double-knock toggles
//             record start/stop (knock fallback for failed touch).
//   GARDEN    花园： public garden / 雪夜传信 (letter pickup + send
//             a flower). Badge only receives / reads / returns one
//             flower; writing a new letter stays on the phone.
//   FRIENDS   好友： list with online dots; double-knock jumps to the
//             voice page to send a 1:1 async voice message.
//   PARTY     派对房间 (join / voice room; audio via phone + badge
//             mic/speaker). Invite card: single knock = accept,
//             double knock = decline (App interprets the TAP/KNOCK
//             notify in context — no new BLE frame).
//   PHOTOS    照片： the ONE stored wallpaper; pick a new one = phone
//             re-sends over BLE (single-slot rule, see storage.h).
//   CHECKIN   打卡 (today's station, distance, confirm). Display only:
//             the check-in decision is the phone's GPS. Arrived state
//             is shown when distance == 0.
//   SETTINGS  设置 (brightness, volume, clock style, tap mode, Wi-Fi,
//             time/date, device info, factory reset note).
//   WIFI      设置的子页： scan nearby networks, enter password on the
//             badge, connect. ESP32-S3 has built-in 2.4 GHz Wi-Fi.
//   WIFI_PASS 密码输入： knock-navigable ASCII keyboard (single knock
//             = next key, double knock = type key). Touch keyboard is
//             the design's primary path but needs CST9217 stability
//             verification; knock navigation is the fallback.
//   GAMES     小游戏： 敲钟 / 凿冰 / 咬冰棍 (badge ONLY, no App page)
//             + one sub-page per game while playing.
//   GAME_PLAY 小游戏进行中： ready screen ("TAP TO START") -> knocks
//             score points -> back ends the session and reports SCORE.
//
// Navigation (touch CST9217 reads unstably — it must NEVER be the only
// path for a critical action):
//   - single knock ........... next item / advance (HOME: next info page)
//   - double knock ........... confirm / enter (NEVER a shutter any
//                              more — 2026-11-23: photo moved to the
//                              on-screen photo button, see
//                              Page::CAMERA_SHUTTER)
//   - touch tap .............. best-effort shortcut: HOME -> menu,
//                              otherwise same as single knock; on the
//                              photo-button page a tap presses the
//                              button (the photo exception, 2026-11-23:
//                              the button fills the page centre, and
//                              touchAt(x, y) hit-tests it precisely
//                              once the CST9217 driver is bound)
//   - long-press (touch) ..... back one level (best-effort; knock has
//                              no hold, so no knock fallback exists)
// All labels on the badge are ASCII (built-in font is ASCII-only);
// Chinese names live in the App and in comments here. Where the design
// calls for Chinese, the badge draws a graphic glyph placeholder.
// Petal canon: four petals = 冰 / 雪 / 乐 / 花. 静 belongs to the
// fifth petal only — no page draws a fifth petal except the gold
// dial (dial 24, locked until the easter-egg unlock).
#pragma once

#include <Arduino.h>
#include "dial.h"
#include "game.h"

namespace menu {

// Minimal display interface. main.cpp binds this to the real CO5300
// QSPI driver; until then a Serial stub is used (see main.cpp).
// All coordinates are pixels on the 466x466 round screen; every rect
// passed here must already be even-aligned (see draw helpers in menu.cpp).
// All text drawn through Menu is inside the round safe area (r <= 227):
// see safeHalfChord() in menu.cpp — x ranges are computed per y.
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
  MENU = 1,    // 功能菜单（11 行）
  VOICE = 2,   // 语音留言
  GARDEN = 3,  // 花园 / 雪夜传信
  FRIENDS = 4, // 好友
  PARTY = 5,   // 派对房间
  PHOTOS = 6,  // 照片（壁纸）
  CHECKIN = 7, // 打卡
  SETTINGS = 8, // 设置
  WIFI = 9,    // 设置 > Wi-Fi
  WIFI_PASS = 10, // Wi-Fi 密码输入（屏上键盘，敲击导航备用）
  GAMES = 11,  // 小游戏列表
  GAME_PLAY = 12,  // 小游戏进行中（敲钟/凿冰/咬冰棍）
  CAMERA_SHUTTER = 13,  // 拍照键页（2026-11-23：点屏幕拍照键拍照）
};

// Menu order (rows on the MENU list page, in design order).
inline const Page kMenuOrder[8] = {
    Page::VOICE, Page::GARDEN, Page::FRIENDS, Page::PARTY,
    Page::PHOTOS, Page::CHECKIN, Page::SETTINGS, Page::GAMES,
};

// Wi-Fi connection state (badge side).
enum class WifiState : uint8_t {
  IDLE = 0,       // not connected, not scanning
  SCANNING = 1,   // scan in progress / list shown
  CONNECTING = 2, // password entered, WiFi.begin() running
  CONNECTED = 3,  // connected to an AP
  FAILED = 4,     // connect failed (wrong password / timeout)
};

struct UiState {
  int energy = 0;       // from STATE frame
  int lit = 0;          // 已点亮 (open-ended roster: NO fixed total shown)
  int bloom = 0;        // 开花 0/1
  String pointsCsv;
  int battPercent = 100;  // STUB until AXP2101 is bound — do not trust
  bool charging = false;
  uint32_t unixSec = 0;  // from TIME frame; 0 = not synced yet
  int tzMinutes = 480;   // from TIME frame; default Asia/Shanghai +08:00
  bool timeSynced = false;
  bool bleConnected = false;
  int steps = -1;        // -1 = no data yet (pedometer/App feed pending)
  bool wifiConnected = false;
  String wifiSsid;
  WifiState wifiState = WifiState::IDLE;
  bool goldUnlocked = false;  // easter egg (dial 24)
  // 语音留言
  bool voiceRecording = false;
  bool voiceHasPlayback = false;
  bool voicePlaying = false;
  int voiceUnread = 0;   // unread voice messages (App feed pending)
  int voiceMsgCount = 0; // total voice messages known
  bool voiceSentFlash = false;  // transient "SENT" after a send
  // 花园 / 雪夜传信
  bool hasLetter = false;  // a letter waits (easter egg: only the letter)
  bool letterRead = false; // current letter opened
  bool flowerSent = false; // a flower was returned for this letter
  // 好友
  int friendsTotal = 0;  // App feed pending
  int friendsOnline = 0; // App feed pending
  // 派对房间
  String partyRoom;      // "" = not in a room (App feed pending)
  int partyOnline = 0;   // App feed pending
  bool partyInvite = false;  // an invite card is showing
  String partyInviteRoom;
  String partyInviteFrom;
  // 照片 / 壁纸
  int wallpaperSyncPct = -1;  // -1 = idle; 0..100 while a sync runs
  // 打卡
  String checkinStation = "NEXT STATION";
  int checkinMeters = -1;  // -1 = unknown; 0 = arrived (success state)
  // 通知
  bool hasNotice = false;  // App feed pending
  String noticeText;
  // 设置（本机可调，NVS 持久化）
  int brightness = 80;   // 0..100 (40/60/80/100 steps)
  int volume = 70;       // 0..100 (0/30/70/100 steps)
};

class Menu {
 public:
  void begin();
  Page page() const { return page_; }
  void goHome();
  void enterMenu();            // HOME -> MENU list page
  void nextItem();             // single knock: advance (HOME: info page)
  void tap();                  // touch tap: HOME -> menu, else nextItem
  void touchAt(int x, int y);  // touch tap with coordinates: on the
                               // photo-button page hit-tests the button,
                               // everywhere else same as tap()
  void confirm();              // double knock: enter / select / action
  void back();                 // back one level (never past HOME)
  void tick(uint32_t nowMs);   // per-page timers (game timeout etc.)
  void render(IDisplay& d);

  // Photo-button page (2026-11-23, Jackson). enterCameraPage() saves
  // the current page and auto-opens the photo button; exitCameraPage()
  // returns to the saved page. Both are no-ops when not applicable.
  void enterCameraPage();
  void exitCameraPage();
  // Press the on-screen photo button. Only works on the photo-button
  // page; returns false anywhere else. Sets the pending shutter flag
  // for main.cpp to notify "S:press" on CAMERA (b109).
  bool pressShutterButton();
  // Consume the pending shutter flag (true once per button press).
  bool takeShutterPress();
  // App answered "S:done" on CAMERA (b109): show "PHOTO TAKEN".
  void setPhotoTaken(bool taken);
  bool photoTaken() const { return photoTaken_; }

  // Page-specific actions (called by confirm() or by main.cpp events).
  void selectGame(game::GameId id);
  void startGame(int difficulty);
  void endGameAndReport();     // ends session; main.cpp sends SCORE
  game::GameSession& session() { return session_; }
  UiState& ui() { return ui_; }
  const UiState& ui() const { return ui_; }
  void setWifiNetworks(const String* ssids, int count);
  int wifiCount() const { return wifiCount_; }
  // Wi-Fi password flow: main.cpp runs the actual connect when
  // wifiConnectRequested() is true, then clears it and reports the
  // result via setWifiConnectResult().
  bool wifiConnectRequested() const { return wifiConnectRequested_; }
  String wifiConnectSsid() const { return wifiConnectSsid_; }
  String wifiConnectPassword() const { return wifiConnectPassword_; }
  void clearWifiConnectRequest() { wifiConnectRequested_ = false; wifiConnectPassword_ = ""; }
  void setWifiConnectResult(bool ok, const String& ssid);

 private:
  Page page_ = Page::HOME;
  int infoPage_ = 0;         // HOME info pages: 0 time, 1 steps, 2 station
  int menuIndex_ = 0;        // index into the 11-row MENU list
  int settingIndex_ = 0;     // settings row cursor
  int wifiIndex_ = 0;
  int gameIndex_ = 0;
  game::GameId pendingGame_ = game::GameId::NONE;  // selected, not started
  bool noticeShown_ = false;   // notices row toggled open
  bool findPhoneNote_ = false; // find-phone row tapped (App link pending)
  uint32_t recordStartMs_ = 0; // voice record start (millis)
  uint32_t voiceSentMs_ = 0;   // "SENT" flash start (millis)
  uint32_t voicePlayMs_ = 0;   // playback start (millis)
  game::GameSession session_;
  UiState ui_;
  String wifiSsids_[16];
  int wifiCount_ = 0;
  // Photo-button page state (2026-11-23).
  Page returnPage_ = Page::HOME;  // page open before the camera popped up
  bool shutterPressed_ = false;   // pending "S:press" for main.cpp
  bool photoTaken_ = false;       // "S:done" received, show confirmation
  uint32_t photoTakenMs_ = 0;     // millis() when "S:done" arrived
  // Wi-Fi password keyboard state.
  bool wifiConnectRequested_ = false;
  String wifiConnectSsid_;
  String wifiConnectPassword_;
  String wifiPass_ = "";     // password being typed (cleared after use)
  int wifiKeyIndex_ = 0;     // cursor into the keyboard key list

  void renderHome(IDisplay& d);
  void renderMenu(IDisplay& d);
  void renderVoice(IDisplay& d);
  void renderGarden(IDisplay& d);
  void renderFriends(IDisplay& d);
  void renderParty(IDisplay& d);
  void renderPhotos(IDisplay& d);
  void renderCheckin(IDisplay& d);
  void renderSettings(IDisplay& d);
  void renderWifi(IDisplay& d);
  void renderWifiPass(IDisplay& d);
  void renderGames(IDisplay& d);
  void renderGamePlay(IDisplay& d);
  void renderCameraShutter(IDisplay& d);
  void renderHeader(IDisplay& d, const char* titleEn);
  void renderFooterHint(IDisplay& d, const char* hintEn);
  void centerText(IDisplay& d, int y, const String& s, uint16_t color, int size);
  void leftText(IDisplay& d, int y, const String& s, uint16_t color, int size);
};

} // namespace menu
