// menu.cpp — 半拍 Banpai badge menu pages (see menu.h).
// Drawing rules enforced here:
//   - 466x466 round safe area (r <= 227 usable); nothing outside.
//   - RGB565 only; every rect even-aligned (x, y, w, h even).
//   - ASCII text only (built-in font); Chinese appears as English
//     placeholder labels + the 4-petal graphic mark.
//   - Four petals = 冰 / 雪 / 乐 / 花. 静 belongs to the fifth petal.
//     No page draws a fifth petal except the gold dial (dial 24).
//   - The easter egg only ever shows the letter (hasLetter) — never a
//     fifth-petal reveal, never a location hint.
#include "menu.h"
#include "storage.h"

namespace menu {

namespace {
// Even-align a rect: shrink to even x/y/w/h (CO5300 window rule).
void evenRect(int& x, int& y, int& w, int& h) {
  if (x & 1) { x += 1; w -= 1; }
  if (y & 1) { y += 1; h -= 1; }
  if (w & 1) w -= 1;
  if (h & 1) h -= 1;
}
} // namespace

void Menu::begin() {
  page_ = Page::HOME;
  menuIndex_ = 0;
  ui_.goldUnlocked = storage::getGoldUnlocked();
}

void Menu::goHome() {
  if (session_.active()) session_.end();
  page_ = Page::HOME;
}

void Menu::enterMenu() {
  page_ = kMenuOrder[menuIndex_];
}

void Menu::nextItem() {
  switch (page_) {
    case Page::HOME:
      enterMenu();
      break;
    case Page::VOICE: case Page::GARDEN: case Page::FRIENDS:
    case Page::PARTY: case Page::PHOTOS: case Page::CHECKIN:
    case Page::SETTINGS: case Page::GAMES:
      menuIndex_ = (menuIndex_ + 1) % 8;
      page_ = kMenuOrder[menuIndex_];
      break;
    case Page::WIFI:
      if (wifiCount_ > 0) wifiIndex_ = (wifiIndex_ + 1) % wifiCount_;
      break;
    case Page::GAME_PLAY:
      break;  // knocks during play score points (handled in main.cpp)
  }
}

void Menu::confirm() {
  switch (page_) {
    case Page::HOME:
      enterMenu();
      break;
    case Page::SETTINGS:
      // Cycle: 0 brightness -> 1 clock style -> 2 tap mode -> Wi-Fi page.
      settingIndex_ = (settingIndex_ + 1) % 4;
      if (settingIndex_ == 3) page_ = Page::WIFI;
      break;
    case Page::WIFI:
      // Confirm a network -> password entry (on-badge ASCII keyboard,
      // see renderWifi). Connection itself runs in main.cpp / wifi task.
      break;
    case Page::GAMES:
      selectGame((game::GameId)(gameIndex_ + 1));
      break;
    case Page::GAME_PLAY:
      break;
    default:
      break;
  }
}

void Menu::back() {
  if (page_ == Page::WIFI) { page_ = Page::SETTINGS; return; }
  if (page_ == Page::GAME_PLAY) {
    endGameAndReport();
    page_ = Page::GAMES;
    return;
  }
  goHome();
}

void Menu::tick(uint32_t nowMs) {
  (void)nowMs;
  if (page_ == Page::GAME_PLAY && session_.active() &&
      ui_.timeSynced && session_.timeUp(ui_.unixSec)) {
    endGameAndReport();
    page_ = Page::GAMES;
  }
}

void Menu::selectGame(game::GameId id) {
  const game::GameDef* def = game::gameDef(id);
  if (!def) return;
  page_ = Page::GAME_PLAY;
  session_.start(id, 1);  // difficulty 1 default; App / settings can raise
  session_.setStartedAt(ui_.unixSec);  // time base for the timeout
}

void Menu::startGame(int difficulty) {
  if (page_ == Page::GAME_PLAY) {
    session_.start(session_.id(), difficulty);
    session_.setStartedAt(ui_.unixSec);
  }
}

void Menu::endGameAndReport() {
  if (session_.active()) session_.end();
  // main.cpp reads session().scoreFrame(unixSec) and notifies SCORE b103.
}

void Menu::setWifiNetworks(const String* ssids, int count) {
  wifiCount_ = count < 0 ? 0 : (count > 16 ? 16 : count);
  for (int i = 0; i < wifiCount_; i++) wifiSsids_[i] = ssids[i];
  if (wifiIndex_ >= wifiCount_) wifiIndex_ = 0;
}

// ---------------------------------------------------------------- render
void Menu::render(IDisplay& d) {
  switch (page_) {
    case Page::HOME: renderHome(d); break;
    case Page::VOICE: renderVoice(d); break;
    case Page::GARDEN: renderGarden(d); break;
    case Page::FRIENDS: renderFriends(d); break;
    case Page::PARTY: renderParty(d); break;
    case Page::PHOTOS: renderPhotos(d); break;
    case Page::CHECKIN: renderCheckin(d); break;
    case Page::SETTINGS: renderSettings(d); break;
    case Page::WIFI: renderWifi(d); break;
    case Page::GAMES: renderGames(d); break;
    case Page::GAME_PLAY: renderGamePlay(d); break;
  }
}

void Menu::renderHeader(IDisplay& d, const char* titleEn) {
  d.fillRect(0, 0, 466, 2, BANPAI_LILAC);
  d.text(20, 28, titleEn, BANPAI_SNOW_WHITE, 2);
}

void Menu::renderFooterHint(IDisplay& d, const char* hintEn) {
  d.text(20, 428, hintEn, BANPAI_LILAC, 1);
}

void Menu::renderHome(IDisplay& d) {
  // Dial home: the stored dial bitmap (if present) + the time layer.
  // No bitmap on the badge yet -> placeholder dial: night-snow field,
  // 4-petal mark, and the time in the dial's anchor zone.
  const DialInfo* dial = dialInfo(storage::getDialCode());
  ClockStyle style = (ClockStyle)storage::getClockStyle();
  const ClockStyleInfo& cs = clockStyleInfo(style);
  d.clear(BANPAI_NIGHT);
  int x, y, w, h;
  clockWindow(dial ? dial->anchor : ClockAnchor::BOTTOM, x, y, w, h);
  evenRect(x, y, w, h);
  // Placeholder time text (real time comes from TIME frame; the time
  // layer is drawn by firmware, never baked into the dial bitmap).
  String hhmm = "--:--";
  if (ui_.timeSynced) {
    uint32_t local = ui_.unixSec + (uint32_t)(ui_.tzMinutes * 60);
    unsigned h = (local / 3600) % 24;
    unsigned m = (local / 60) % 60;
    char buf[8];
    snprintf(buf, sizeof(buf), "%02u:%02u", h, m);
    hhmm = buf;
  }
  d.text(x + 40, y + 14, hhmm, cs.fg, 4);
  d.drawPetalMark(BANPAI_SCREEN_CX, 300, 60);  // 4 petals: 冰/雪/乐/花
  if (dial) d.text(20, 400, dial->nameEn, BANPAI_LILAC, 1);
  if (ui_.hasLetter) d.text(20, 420, "LETTER WAITING", BANPAI_GOLD, 1);
}

void Menu::renderVoice(IDisplay& d) {
  // 语音留言： hold to talk, release to send, speaker playback.
  // Badge mic + speaker. No music playback (speaker quality is poor).
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "VOICE MSG");
  d.drawPetalMark(BANPAI_SCREEN_CX, 180, 48);
  d.text(40, 260, ui_.voiceRecording ? "RECORDING..." : "HOLD TO TALK",
         ui_.voiceRecording ? BANPAI_PLUM_RED : BANPAI_SNOW_WHITE, 2);
  d.text(40, 300, "RELEASE TO SEND", BANPAI_LILAC, 1);
  if (ui_.voiceHasPlayback) d.text(40, 330, "TAP PLAY: NEW MSG", BANPAI_GOLD, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: BACK");
}

void Menu::renderGarden(IDisplay& d) {
  // 花园： public garden + 雪夜传信 (letter pickup / send).
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "GARDEN");
  d.drawPetalMark(BANPAI_SCREEN_CX, 180, 48);
  d.text(40, 260, ui_.hasLetter ? "A LETTER WAITS FOR YOU" : "NO LETTER TONIGHT",
         BANPAI_GOLD, 2);
  d.text(40, 300, "SNOW NIGHT LETTERS", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: BACK");
}

void Menu::renderFriends(IDisplay& d) {
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "FRIENDS");
  d.text(40, 120, "FRIEND LIST ON PHONE", BANPAI_SNOW_WHITE, 2);
  d.text(40, 160, "BADGE: ACCEPT / JOIN", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: BACK");
}

void Menu::renderParty(IDisplay& d) {
  // 派对房间： join a room / accept invite on the badge; voice via
  // badge mic + speaker, room signalling via phone BLE.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "PARTY ROOM");
  d.text(40, 120, "JOIN ROOM ON BADGE", BANPAI_SNOW_WHITE, 2);
  d.text(40, 160, "VOICE: MIC + SPEAKER", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: BACK");
}

void Menu::renderPhotos(IDisplay& d) {
  // 照片（壁纸）: shows the ONE stored wallpaper. A new wallpaper must
  // be re-sent from the phone (single-slot rule) — the badge cannot
  // browse the phone's full photo library on its own.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "PHOTOS / WALLPAPER");
  if (storage::hasWallpaper()) {
    d.text(40, 160, "WALLPAPER SET", BANPAI_SNOW_WHITE, 2);
    d.text(40, 200, storage::getWallpaperId(), BANPAI_LILAC, 1);
  } else {
    d.text(40, 160, "NO WALLPAPER YET", BANPAI_SNOW_WHITE, 2);
    d.text(40, 200, "SEND ONE FROM PHONE", BANPAI_LILAC, 1);
  }
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: BACK");
}

void Menu::renderCheckin(IDisplay& d) {
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "CHECK-IN");
  d.text(40, 120, ui_.checkinStation, BANPAI_SNOW_WHITE, 2);
  if (ui_.checkinMeters >= 0) {
    d.text(40, 160, String(ui_.checkinMeters) + " m", BANPAI_GOLD, 2);
  } else {
    d.text(40, 160, "DISTANCE --", BANPAI_LILAC, 2);
  }
  d.text(40, 200, String("LIT ") + String(ui_.lit) + " / 24", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: CHECK IN");
}

void Menu::renderSettings(IDisplay& d) {
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "SETTINGS");
  d.text(40, 120, "1 BRIGHTNESS", BANPAI_SNOW_WHITE, 2);
  d.text(40, 160, "2 CLOCK STYLE", BANPAI_SNOW_WHITE, 2);
  d.text(40, 200, "3 TAP MODE", BANPAI_SNOW_WHITE, 2);
  d.text(40, 240, "4 WI-FI >", BANPAI_GOLD, 2);
  d.text(40, 280, String("BATT ") + String(ui_.battPercent) + "%", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: SELECT");
}

void Menu::renderWifi(IDisplay& d) {
  // 设置 > Wi-Fi: scan nearby networks, pick one, type the password on
  // the badge (ASCII on-screen keyboard — the badge font is ASCII-only,
  // so password entry works with the built-in font). 2.4 GHz only.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "WI-FI 2.4G");
  if (ui_.wifiConnected) {
    d.text(40, 120, "CONNECTED: " + ui_.wifiSsid, BANPAI_GOLD, 2);
  } else if (wifiCount_ == 0) {
    d.text(40, 120, "SCANNING...", BANPAI_SNOW_WHITE, 2);
    d.text(40, 160, "2.4 GHZ ONLY", BANPAI_LILAC, 1);
  } else {
    for (int i = 0; i < wifiCount_ && i < 6; i++) {
      String line = (i == wifiIndex_ ? "> " : "  ") + wifiSsids_[i];
      d.text(40, 120 + i * 30, line, BANPAI_SNOW_WHITE, 2);
    }
    d.text(40, 320, "TYPE PASSWORD ON BADGE", BANPAI_LILAC, 1);
  }
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: SELECT");
}

void Menu::renderGames(IDisplay& d) {
  // 小游戏只在吧唧上，App 里不放游戏页。
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "MINI GAMES");
  for (int i = 0; i < 3; i++) {
    const game::GameDef* g = game::gameDef((game::GameId)(i + 1));
    String line = (i == gameIndex_ ? "> " : "  ") + String(g->nameEn);
    d.text(40, 120 + i * 40, line, BANPAI_SNOW_WHITE, 2);
  }
  d.text(40, 280, "KNOCK TO SCORE", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL-KNOCK: PLAY");
}

void Menu::renderGamePlay(IDisplay& d) {
  const game::GameDef* g = game::gameDef(session_.id());
  d.clear(BANPAI_NIGHT);
  renderHeader(d, g ? g->nameEn : "GAME");
  d.text(40, 140, String("SCORE ") + String(session_.score()), BANPAI_GOLD, 4);
  d.text(40, 220, "KNOCK THE BADGE!", BANPAI_SNOW_WHITE, 2);
  d.text(40, 260, String("LV ") + String(session_.difficulty()), BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: SCORE  BACK: END + SEND");
}

} // namespace menu
