// menu.cpp — 半拍 Banpai badge menu pages (see menu.h).
// UI v0.2.0-ui: pages follow the ban-pai-ba-ji-ui design frame.
// Drawing rules enforced here:
//   - 466x466 round safe area (r <= 227 usable); every label is placed
//     through centerText/leftText, which clamp into the safe chord at
//     that y (the old header/footer at x=20 were OUTSIDE the circle
//     and are fixed here). Nothing outside.
//   - RGB565 only; every rect even-aligned (x, y, w, h even).
//   - ASCII text only (built-in font); Chinese appears as English
//     placeholder labels + the 4-petal graphic mark.
//   - Four petals = 冰 / 雪 / 乐 / 花. 静 belongs to the fifth petal.
//     No page draws a fifth petal except the gold dial (dial 24).
//   - The easter egg only ever shows the letter (hasLetter) — never a
//     fifth-petal reveal, never a location hint.
//   - Guardian-pet roster is open-ended: lit counts are shown WITHOUT
//     a fixed total (no "/ 24" anywhere on the badge).
//   - No demo constants are invented here: steps / friends / party /
//     notice fields show "--" / "SYNC FROM PHONE" until the App feeds
//     them. Battery is still the 100% stub (AXP2101 not bound yet).
#include "menu.h"
#include "ble_protocol.h"
#include "storage.h"
#include <math.h>

namespace menu {

namespace {
// Even-align a rect: shrink to even x/y/w/h (CO5300 window rule).
void evenRect(int& x, int& y, int& w, int& h) {
  if (x & 1) { x += 1; w -= 1; }
  if (y & 1) { y += 1; h -= 1; }
  if (w & 1) w -= 1;
  if (h & 1) h -= 1;
}

// Half-width of the safe chord (r=227) at screen height y.
int safeHalfChord(int y) {
  int dy = y - BANPAI_SCREEN_CY;
  int dy2 = dy * dy;
  int r2 = BANPAI_SAFE_R * BANPAI_SAFE_R;
  if (dy2 >= r2) return 0;
  return (int)sqrtf((float)(r2 - dy2));
}

// Adafruit built-in font: 6 px wide (incl. spacing) per char at size 1.
int textWidthPx(const String& s, int size) {
  return (int)s.length() * 6 * size;
}

// Wi-Fi password keyboard: 46 chars + DEL / OK / CANCEL = 49 keys,
// laid out 7x7. Knock navigates, double-knock types (touch keyboard
// is the design's primary path, pending CST9217 stability checks).
const char kWifiKeys[] = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%&*-_.";
const int kWifiKeyCount = 49;  // 46 chars + "<-" + "OK" + "X"
String wifiKeyLabel(int idx) {
  if (idx < 46) return String(kWifiKeys[idx]);
  if (idx == 46) return "<-";
  if (idx == 47) return "OK";
  return "X";
}
} // namespace

void Menu::begin() {
  page_ = Page::HOME;
  infoPage_ = 0;
  menuIndex_ = 0;
  ui_.goldUnlocked = storage::getGoldUnlocked();
  ui_.brightness = storage::getBrightness();
  ui_.volume = storage::getVolume();
}

void Menu::enterCameraPage() {
  // Auto-popup when the App opens its camera ("M:camera", b106).
  // Already on the photo page: keep the saved return page.
  if (page_ == Page::CAMERA_SHUTTER) return;
  returnPage_ = page_;
  photoTaken_ = false;
  shutterPressed_ = false;
  page_ = Page::CAMERA_SHUTTER;
}

void Menu::exitCameraPage() {
  // Camera closed ("M:normal", b106) or back: return to the page
  // that was open before the camera popped up.
  if (page_ != Page::CAMERA_SHUTTER) return;
  photoTaken_ = false;
  shutterPressed_ = false;
  page_ = returnPage_;
}

bool Menu::pressShutterButton() {
  // The ONLY way to take a photo from the badge (2026-11-23).
  // Knocks never reach here.
  if (page_ != Page::CAMERA_SHUTTER) return false;
  shutterPressed_ = true;
  photoTaken_ = false;  // clear the old confirmation until "S:done"
  return true;
}

bool Menu::takeShutterPress() {
  if (!shutterPressed_) return false;
  shutterPressed_ = false;
  return true;
}

void Menu::setPhotoTaken(bool taken) {
  photoTaken_ = taken;
  photoTakenMs_ = millis();
}

void Menu::goHome() {
  if (session_.active()) session_.end();
  page_ = Page::HOME;
}

void Menu::enterMenu() {
  page_ = Page::MENU;
}

void Menu::nextItem() {
  switch (page_) {
    case Page::HOME:
      // Single knock on the dial cycles the 3 info pages (design).
      infoPage_ = (infoPage_ + 1) % 3;
      break;
    case Page::MENU:
      menuIndex_ = (menuIndex_ + 1) % 11;
      break;
    case Page::VOICE: case Page::GARDEN: case Page::FRIENDS:
    case Page::PARTY: case Page::PHOTOS: case Page::CHECKIN:
    case Page::SETTINGS: case Page::GAMES: {
      // Single knock cycles through the function pages (keeps the
      // badge usable without touch); MENU is reached via back().
      int idx = -1;
      for (int i = 0; i < 8; i++) {
        if (kMenuOrder[i] == page_) { idx = i; break; }
      }
      if (idx >= 0) page_ = kMenuOrder[(idx + 1) % 8];
      break;
    }
    case Page::WIFI:
      if (wifiCount_ > 0) wifiIndex_ = (wifiIndex_ + 1) % wifiCount_;
      break;
    case Page::WIFI_PASS:
      wifiKeyIndex_ = (wifiKeyIndex_ + 1) % kWifiKeyCount;
      break;
    case Page::GAME_PLAY:
      if (!session_.active()) startGame(1);  // ready screen: tap to start
      break;
    case Page::CAMERA_SHUTTER:
      // Photo mode: knocks do NOTHING (anti-misfire — only the
      // on-screen photo button takes a photo, 2026-11-23).
      break;
  }
}

void Menu::tap() {
  // Touch tap: on the dial it opens the menu (design: "点表盘开菜单");
  // on the photo-button page it presses the big button (the button
  // fills the page centre — the photo exception, 2026-11-23);
  // everywhere else it is the same best-effort shortcut as a knock.
  if (page_ == Page::HOME) {
    enterMenu();
    return;
  }
  if (page_ == Page::CAMERA_SHUTTER) {
    pressShutterButton();
    return;
  }
  nextItem();
}

void Menu::touchAt(int x, int y) {
  // Touch tap with coordinates (real CST9217 driver path). On the
  // photo-button page only a tap inside the big button takes a photo;
  // a tap elsewhere on the page does nothing. Everywhere else this
  // is the same best-effort shortcut as tap().
  if (page_ == Page::CAMERA_SHUTTER) {
    int dx = x - BANPAI_SCREEN_CX;
    int dy = y - BANPAI_SCREEN_CY;
    if (dx * dx + dy * dy <= 110 * 110) pressShutterButton();
    return;
  }
  tap();
}

void Menu::confirm() {
  switch (page_) {
    case Page::HOME:
      enterMenu();
      break;
    case Page::MENU: {
      // Rows 0..7 are the 8 function pages; 8 = notices, 9 = find
      // phone, 10 = steps/energy shortcut.
      if (menuIndex_ < 8) {
        page_ = kMenuOrder[menuIndex_];
      } else if (menuIndex_ == 8) {
        noticeShown_ = !noticeShown_;
      } else if (menuIndex_ == 9) {
        findPhoneNote_ = true;  // needs an App link (see renderMenu)
      } else {
        infoPage_ = 1;
        goHome();
      }
      break;
    }
    case Page::VOICE: {
      // Double-knock: stop recording if recording; else play the
      // newest message if one waits; else start recording.
      if (ui_.voiceRecording) {
        ui_.voiceRecording = false;
        ui_.voiceSentFlash = true;
        voiceSentMs_ = millis();
        ui_.voiceMsgCount += 1;
      } else if (ui_.voiceHasPlayback) {
        ui_.voicePlaying = true;
        voicePlayMs_ = millis();
      } else {
        ui_.voiceRecording = true;
        recordStartMs_ = millis();
        ui_.voiceSentFlash = false;
      }
      break;
    }
    case Page::GARDEN:
      if (ui_.hasLetter && !ui_.letterRead) {
        ui_.letterRead = true;  // read the letter
      } else if (ui_.hasLetter && ui_.letterRead && !ui_.flowerSent) {
        ui_.flowerSent = true;  // return one flower
      }
      break;
    case Page::FRIENDS:
      // Tap a friend -> voice page for a 1:1 async voice message.
      page_ = Page::VOICE;
      break;
    case Page::PARTY:
      if (ui_.partyInvite) {
        // Double-knock = decline the invite (single knock = accept,
        // handled in nextItem; the App interprets the TAP/KNOCK
        // notify in context — no new BLE frame).
        ui_.partyInvite = false;
      }
      break;
    case Page::SETTINGS: {
      // Row actions: 0 brightness, 1 volume, 2 clock style, 3 tap
      // mode, 4 Wi-Fi page, 5/6 info only, 7 factory reset (App only).
      switch (settingIndex_) {
        case 0:
          ui_.brightness = ui_.brightness >= 100 ? 40 : ui_.brightness + 20;
          storage::setBrightness((uint8_t)ui_.brightness);
          break;
        case 1:
          ui_.volume = ui_.volume >= 100 ? 0 : ui_.volume + 30;
          if (ui_.volume > 100) ui_.volume = 100;
          storage::setVolume((uint8_t)ui_.volume);
          break;
        case 2: {
          uint8_t s = (uint8_t)((storage::getClockStyle() + 1) % 5);
          storage::setClockStyle(s);
          break;
        }
        case 3: {
          uint8_t m = storage::getTapMode() == 1 ? 0 : 1;
          storage::setTapMode(m);
          break;
        }
        case 4:
          page_ = Page::WIFI;
          break;
        default:
          break;
      }
      break;
    }
    case Page::WIFI:
      // Confirm a network -> password entry on the badge.
      if (wifiCount_ > 0) {
        wifiPass_ = "";
        wifiKeyIndex_ = 0;
        page_ = Page::WIFI_PASS;
      }
      break;
    case Page::WIFI_PASS: {
      if (wifiKeyIndex_ < 46) {
        if (wifiPass_.length() < 32) wifiPass_ += kWifiKeys[wifiKeyIndex_];
      } else if (wifiKeyIndex_ == 46) {
        if (wifiPass_.length() > 0)
          wifiPass_.remove(wifiPass_.length() - 1);
      } else if (wifiKeyIndex_ == 47) {
        // OK: ask main.cpp to run WiFi.begin(); the result comes back
        // via setWifiConnectResult().
        wifiConnectSsid_ = wifiSsids_[wifiIndex_];
        wifiConnectPassword_ = wifiPass_;
        wifiPass_ = "";
        ui_.wifiState = WifiState::CONNECTING;
        wifiConnectRequested_ = true;
        page_ = Page::WIFI;
      } else {
        wifiPass_ = "";
        page_ = Page::WIFI;
      }
      break;
    }
    case Page::GAMES:
      selectGame((game::GameId)(gameIndex_ + 1));
      break;
    case Page::GAME_PLAY:
      if (!session_.active()) startGame(1);
      break;
    case Page::CAMERA_SHUTTER:
      // Double-knock on the photo page does NOTHING: it is not a
      // shutter any more (2026-11-23) and must not navigate away
      // while the phone camera is open.
      break;
    default:
      break;
  }
}

void Menu::back() {
  if (page_ == Page::CAMERA_SHUTTER) { exitCameraPage(); return; }
  if (page_ == Page::WIFI_PASS) { page_ = Page::WIFI; return; }
  if (page_ == Page::WIFI) { page_ = Page::SETTINGS; return; }
  if (page_ == Page::GAME_PLAY) {
    endGameAndReport();
    page_ = Page::GAMES;
    return;
  }
  if (page_ == Page::MENU) { goHome(); return; }
  page_ = Page::MENU;  // function pages go back to the menu list
}

void Menu::tick(uint32_t nowMs) {
  if (ui_.voiceSentFlash && nowMs - voiceSentMs_ > 3000) {
    ui_.voiceSentFlash = false;
  }
  // "PHOTO TAKEN" confirmation clears after 5 s (button stays live).
  if (photoTaken_ && nowMs - photoTakenMs_ > 5000) {
    photoTaken_ = false;
  }
  if (ui_.voicePlaying && nowMs - voicePlayMs_ > 8000) {
    ui_.voicePlaying = false;
  }
  if (page_ == Page::GAME_PLAY && session_.active() &&
      ui_.timeSynced && session_.timeUp(ui_.unixSec)) {
    endGameAndReport();
    page_ = Page::GAMES;
  }
}

void Menu::selectGame(game::GameId id) {
  const game::GameDef* def = game::gameDef(id);
  if (!def) return;
  pendingGame_ = id;
  page_ = Page::GAME_PLAY;  // ready screen; tap/knock to start
}

void Menu::startGame(int difficulty) {
  if (page_ != Page::GAME_PLAY) return;
  if (session_.active()) return;
  if (pendingGame_ == game::GameId::NONE) return;
  session_.start(pendingGame_, difficulty);
  session_.setStartedAt(ui_.unixSec);  // time base for the timeout
}

void Menu::endGameAndReport() {
  if (session_.active()) {
    // Keep the per-game best score (NVS, badge-local).
    int best = storage::getBestScore((int)session_.id());
    if (session_.score() > best)
      storage::setBestScore((int)session_.id(), session_.score());
    session_.end();
  }
  // main.cpp reads session().scoreFrame(unixSec) and notifies SCORE b103.
}

void Menu::setWifiNetworks(const String* ssids, int count) {
  wifiCount_ = count < 0 ? 0 : (count > 16 ? 16 : count);
  for (int i = 0; i < wifiCount_; i++) wifiSsids_[i] = ssids[i];
  if (wifiIndex_ >= wifiCount_) wifiIndex_ = 0;
  ui_.wifiState = WifiState::SCANNING;
}

void Menu::setWifiConnectResult(bool ok, const String& ssid) {
  ui_.wifiConnected = ok;
  ui_.wifiState = ok ? WifiState::CONNECTED : WifiState::FAILED;
  if (ok) ui_.wifiSsid = ssid;
  page_ = Page::WIFI;  // show the result on the Wi-Fi page
}

// ---------------------------------------------------------------- render
void Menu::render(IDisplay& d) {
  switch (page_) {
    case Page::HOME: renderHome(d); break;
    case Page::MENU: renderMenu(d); break;
    case Page::VOICE: renderVoice(d); break;
    case Page::GARDEN: renderGarden(d); break;
    case Page::FRIENDS: renderFriends(d); break;
    case Page::PARTY: renderParty(d); break;
    case Page::PHOTOS: renderPhotos(d); break;
    case Page::CHECKIN: renderCheckin(d); break;
    case Page::SETTINGS: renderSettings(d); break;
    case Page::WIFI: renderWifi(d); break;
    case Page::WIFI_PASS: renderWifiPass(d); break;
    case Page::GAMES: renderGames(d); break;
    case Page::GAME_PLAY: renderGamePlay(d); break;
    case Page::CAMERA_SHUTTER: renderCameraShutter(d); break;
  }
}

void Menu::renderHeader(IDisplay& d, const char* titleEn) {
  // Title centred near the top of the safe circle + a short gold rule.
  centerText(d, 56, titleEn, BANPAI_SNOW_WHITE, 2);
  int rx = BANPAI_SCREEN_CX - 40, ry = 82, rw = 80, rh = 2;
  evenRect(rx, ry, rw, rh);
  d.fillRect(rx, ry, rw, rh, BANPAI_GOLD);
}

void Menu::renderFooterHint(IDisplay& d, const char* hintEn) {
  centerText(d, 404, hintEn, BANPAI_LILAC, 1);
}

void Menu::centerText(IDisplay& d, int y, const String& s, uint16_t color, int size) {
  int w = textWidthPx(s, size);
  int half = safeHalfChord(y + 4 * size);
  int maxW = half * 2 - 16;
  if (w > maxW) size = 1, w = textWidthPx(s, size);  // shrink if too wide
  int x = BANPAI_SCREEN_CX - w / 2;
  int minX = BANPAI_SCREEN_CX - half + 8;
  if (x < minX) x = minX;
  d.text(x, y, s, color, size);
}

void Menu::leftText(IDisplay& d, int y, const String& s, uint16_t color, int size) {
  int half = safeHalfChord(y + 4 * size);
  int x = BANPAI_SCREEN_CX - half + 12;
  if (x < 12) x = 12;
  d.text(x, y, s, color, size);
}

void Menu::renderHome(IDisplay& d) {
  // Dial home: the stored dial bitmap (if present) + the time layer.
  // No bitmap on the badge yet -> placeholder dial: night-snow field,
  // 4-petal mark, and the time in the dial's anchor zone.
  const DialInfo* dial = dialInfo(storage::getDialCode());
  ClockStyle style = (ClockStyle)storage::getClockStyle();
  const ClockStyleInfo& cs = clockStyleInfo(style);
  d.clear(BANPAI_NIGHT);
  // Top status: BLE + battery (battery is the 100% stub until AXP2101).
  String status = ui_.bleConnected ? "BLE OK  " : "BLE OFF  ";
  status += "BATT " + String(ui_.battPercent) + "%";
  centerText(d, 36, status, BANPAI_LILAC, 1);

  if (infoPage_ == 0) {
    // Info page 0: time (+ date line + dial name). The 4-petal mark is
    // drawn only where it cannot collide with the time window.
    int x, y, w, h;
    ClockAnchor anchor = dial ? dial->anchor : ClockAnchor::BOTTOM;
    clockWindow(anchor, x, y, w, h);
    evenRect(x, y, w, h);
    String hhmm = "--:--";
    if (ui_.timeSynced) {
      uint32_t local = ui_.unixSec + (uint32_t)(ui_.tzMinutes * 60);
      unsigned hh = (local / 3600) % 24;
      unsigned mm = (local / 60) % 60;
      char buf[8];
      snprintf(buf, sizeof(buf), "%02u:%02u", hh, mm);
      hhmm = buf;
    }
    centerText(d, y + 14, hhmm, cs.fg, 4);
    if (anchor != ClockAnchor::BOTTOM && dial)
      centerText(d, y + 52, dial->nameEn, BANPAI_LILAC, 1);
    if (anchor != ClockAnchor::CENTER)
      d.drawPetalMark(BANPAI_SCREEN_CX, 300, 56);  // 4 petals: 冰/雪/乐/花
    if (ui_.hasLetter) centerText(d, 392, "LETTER WAITING", BANPAI_GOLD, 1);
  } else if (infoPage_ == 1) {
    // Info page 1: steps / energy / lit (no fixed total — open roster).
    centerText(d, 120, "STEPS", BANPAI_LILAC, 1);
    centerText(d, 140, ui_.steps >= 0 ? String(ui_.steps) : "--",
               BANPAI_SNOW_WHITE, 3);
    centerText(d, 210, "ENERGY " + String(ui_.energy), BANPAI_GOLD, 2);
    centerText(d, 260, "LIT " + String(ui_.lit), BANPAI_SNOW_WHITE, 2);
    if (ui_.bloom) centerText(d, 300, "BLOOM!", BANPAI_GOLD, 2);
    d.drawPetalMark(BANPAI_SCREEN_CX, 360, 34);
  } else {
    // Info page 2: next station + distance + auto check-in note.
    centerText(d, 120, "NEXT STATION", BANPAI_LILAC, 1);
    centerText(d, 150, ui_.checkinStation, BANPAI_SNOW_WHITE, 2);
    if (ui_.checkinMeters >= 0) {
      centerText(d, 200, String(ui_.checkinMeters) + " m", BANPAI_GOLD, 3);
      int mins = (ui_.checkinMeters + 79) / 80;  // ~80 m/min walking
      centerText(d, 250, "WALK ~" + String(mins) + " MIN", BANPAI_LILAC, 1);
    } else {
      centerText(d, 200, "DISTANCE --", BANPAI_LILAC, 2);
    }
    centerText(d, 290, "AUTO CHECK-IN 80-150 m", BANPAI_LILAC, 1);
    if (!ui_.bleConnected)
      centerText(d, 320, "OFFLINE: NEAR PHONE", BANPAI_PLUM_RED, 1);
    d.drawPetalMark(BANPAI_SCREEN_CX, 368, 30);
  }
  centerText(d, 404, "KNOCK: INFO  DBL: MENU", BANPAI_LILAC, 1);
}

void Menu::renderMenu(IDisplay& d) {
  // 11-row function list, 6 visible rows, scrolls with the selection.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "MENU");
  const char* rows[11] = {
      "VOICE MSG", "GARDEN", "FRIENDS", "PARTY ROOM", "MINI GAMES",
      "PHOTOS", "CHECK-IN", "SETTINGS", "NOTICES", "FIND PHONE",
      "STEPS / ENERGY"};
  int top = menuIndex_ < 3 ? 0 : (menuIndex_ > 7 ? 5 : menuIndex_ - 2);
  for (int r = 0; r < 6; r++) {
    int i = top + r;
    if (i > 10) break;
    int y = 108 + r * 40;
    String line = (i == menuIndex_ ? "> " : "  ") + String(rows[i]);
    if (i == 0 && ui_.voiceUnread > 0) line += " (" + String(ui_.voiceUnread) + ")";
    if (i == 1 && ui_.hasLetter) line += " (1)";
    if (i == 2 && ui_.friendsTotal > 0)
      line += " " + String(ui_.friendsOnline) + " ON";
    if (i == 3 && ui_.partyRoom.length() > 0) line += " " + ui_.partyRoom;
    if (i == 8 && ui_.hasNotice) line += " NEW";
    leftText(d, y, line, i == menuIndex_ ? BANPAI_GOLD : BANPAI_SNOW_WHITE, 2);
  }
  if (noticeShown_ && ui_.hasNotice) {
    centerText(d, 356, ui_.noticeText, BANPAI_GOLD, 1);
  } else if (findPhoneNote_) {
    centerText(d, 356, "PHONE RING NEEDS APP LINK", BANPAI_LILAC, 1);
  } else {
    centerText(d, 356, "DIAL: CHANGE ON PHONE", BANPAI_LILAC, 1);
  }
  renderFooterHint(d, "KNOCK: NEXT  DBL: ENTER");
}

void Menu::renderVoice(IDisplay& d) {
  // 语音留言： hold to talk, release to send, speaker playback.
  // Badge mic + speaker. No music playback (speaker quality is poor).
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "VOICE MSG");
  d.drawPetalMark(BANPAI_SCREEN_CX, 170, 44);
  if (ui_.voiceRecording) {
    int secs = (int)((millis() - recordStartMs_) / 1000);
    centerText(d, 240, "RECORDING " + String(secs) + "s",
               BANPAI_PLUM_RED, 2);
    centerText(d, 280, "DBL-KNOCK: SEND", BANPAI_LILAC, 1);
  } else if (ui_.voiceSentFlash) {
    centerText(d, 240, "SENT OK", BANPAI_GOLD, 2);
    centerText(d, 280, "MAX 60s PER MSG", BANPAI_LILAC, 1);
  } else if (ui_.voicePlaying) {
    centerText(d, 240, "PLAYING...", BANPAI_GOLD, 2);
    centerText(d, 280, "SPEAKER", BANPAI_LILAC, 1);
  } else {
    if (ui_.voiceMsgCount > 0) {
      centerText(d, 230, String(ui_.voiceMsgCount) + " MSG" +
                              (ui_.voiceMsgCount > 1 ? "S" : "") +
                              (ui_.voiceUnread > 0
                                   ? "  " + String(ui_.voiceUnread) + " NEW"
                                   : ""),
                 BANPAI_SNOW_WHITE, 2);
      if (ui_.voiceHasPlayback)
        centerText(d, 270, "DBL-KNOCK: PLAY NEWEST", BANPAI_GOLD, 1);
    } else {
      centerText(d, 230, "NO VOICE YET", BANPAI_SNOW_WHITE, 2);
    }
    centerText(d, 310, "HOLD TO TALK / DBL: REC", BANPAI_LILAC, 1);
    centerText(d, 336, "MAX 60s  KNOCK: START/STOP", BANPAI_LILAC, 1);
  }
  renderFooterHint(d, "KNOCK: NEXT  DBL: REC/PLAY");
}

void Menu::renderGarden(IDisplay& d) {
  // 花园： public garden + 雪夜传信 (letter pickup / send a flower).
  // Badge only receives / reads / returns one flower; writing a new
  // letter stays on the phone.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "GARDEN");
  d.drawPetalMark(BANPAI_SCREEN_CX, 170, 44);
  if (!ui_.hasLetter) {
    centerText(d, 250, "NO LETTER TONIGHT", BANPAI_SNOW_WHITE, 2);
    centerText(d, 290, "SNOW NIGHT LETTERS", BANPAI_LILAC, 1);
    centerText(d, 320, "WRITE NEW LETTER ON PHONE", BANPAI_LILAC, 1);
  } else if (!ui_.letterRead) {
    centerText(d, 250, "A LETTER WAITS FOR YOU", BANPAI_GOLD, 2);
    centerText(d, 290, "SNOW NIGHT LETTER", BANPAI_LILAC, 1);
    centerText(d, 320, "DBL-KNOCK: READ IT", BANPAI_LILAC, 1);
  } else if (!ui_.flowerSent) {
    centerText(d, 250, "LETTER READ", BANPAI_SNOW_WHITE, 2);
    centerText(d, 290, "SEND BACK A FLOWER?", BANPAI_GOLD, 1);
    centerText(d, 320, "DBL-KNOCK: SEND FLOWER", BANPAI_LILAC, 1);
  } else {
    centerText(d, 250, "FLOWER SENT", BANPAI_GOLD, 2);
    centerText(d, 290, "LETTER READ", BANPAI_SNOW_WHITE, 1);
  }
  renderFooterHint(d, "KNOCK: NEXT  DBL: READ/FLOWER");
}

void Menu::renderFriends(IDisplay& d) {
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "FRIENDS");
  if (ui_.friendsTotal <= 0) {
    centerText(d, 200, "FRIENDS SYNC FROM PHONE", BANPAI_SNOW_WHITE, 2);
    centerText(d, 240, "NO DATA YET", BANPAI_LILAC, 1);
  } else {
    centerText(d, 170, String(ui_.friendsTotal) + " FRIENDS",
               BANPAI_SNOW_WHITE, 2);
    // Online dots: one filled dot per online friend (count only —
    // per-friend identity is App data, not invented here).
    for (int i = 0; i < ui_.friendsOnline && i < 8; i++) {
      d.fillCircle(BANPAI_SCREEN_CX - (ui_.friendsOnline - 1) * 16 + i * 32,
                   220, 8, BANPAI_NEON_CYAN);
    }
    centerText(d, 260, String(ui_.friendsOnline) + " ONLINE", BANPAI_NEON_CYAN, 2);
    centerText(d, 300, "DBL-KNOCK: VOICE MSG", BANPAI_GOLD, 1);
  }
  centerText(d, 340, "TAP A FRIEND: 1:1 VOICE", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL: VOICE");
}

void Menu::renderParty(IDisplay& d) {
  // 派对房间： join a room / accept invite on the badge; voice via
  // badge mic + speaker, room signalling via phone BLE.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "PARTY ROOM");
  if (ui_.partyInvite) {
    // Invite card: single knock accepts, double-knock declines
    // (the App interprets the TAP/KNOCK notify in context).
    d.drawPetalMark(BANPAI_SCREEN_CX, 150, 40);
    centerText(d, 210, "INVITE", BANPAI_GOLD, 2);
    centerText(d, 250, ui_.partyInviteRoom, BANPAI_SNOW_WHITE, 2);
    if (ui_.partyInviteFrom.length() > 0)
      centerText(d, 285, "FROM " + ui_.partyInviteFrom, BANPAI_LILAC, 1);
    centerText(d, 330, "KNOCK: ACCEPT", BANPAI_NEON_CYAN, 1);
    centerText(d, 356, "DBL-KNOCK: DECLINE", BANPAI_LILAC, 1);
  } else if (ui_.partyRoom.length() > 0) {
    centerText(d, 170, ui_.partyRoom, BANPAI_SNOW_WHITE, 2);
    centerText(d, 210, String(ui_.partyOnline) + " ONLINE", BANPAI_NEON_CYAN, 2);
    centerText(d, 260, "TAKE TURNS, ASYNC VOICE", BANPAI_LILAC, 1);
    centerText(d, 290, "HOLD TO TALK", BANPAI_GOLD, 1);
    centerText(d, 320, "MIC + SPEAKER", BANPAI_LILAC, 1);
  } else {
    centerText(d, 200, "NO ROOM", BANPAI_SNOW_WHITE, 2);
    centerText(d, 240, "JOIN FROM PHONE", BANPAI_LILAC, 1);
    centerText(d, 270, "INVITES POP UP HERE", BANPAI_LILAC, 1);
  }
  renderFooterHint(d, "KNOCK: NEXT  DBL: BACK");
}

void Menu::renderPhotos(IDisplay& d) {
  // 照片（壁纸）: shows the ONE stored wallpaper. A new wallpaper must
  // be re-sent from the phone (single-slot rule) — the badge cannot
  // browse the phone's full photo library on its own. Time is overlaid
  // by the system, never baked into the image.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "PHOTOS / WALLPAPER");
  if (ui_.wallpaperSyncPct >= 0) {
    centerText(d, 180, "SYNCING " + String(ui_.wallpaperSyncPct) + "%",
               BANPAI_GOLD, 2);
    int bx = BANPAI_SCREEN_CX - 100, by = 220, bw = 200, bh = 12;
    evenRect(bx, by, bw, bh);
    d.fillRect(bx, by, bw, bh, RGB565(40, 44, 80));
    int fw = bw * ui_.wallpaperSyncPct / 100;
    if (fw > 0) {
      int fx = bx, fy = by, fww = fw, fh = bh;
      evenRect(fx, fy, fww, fh);
      d.fillRect(fx, fy, fww, fh, BANPAI_GOLD);
    }
    centerText(d, 260, "BLE SYNC, RESUMES IF CUT", BANPAI_LILAC, 1);
  } else if (storage::hasWallpaper()) {
    centerText(d, 180, "WALLPAPER SET", BANPAI_SNOW_WHITE, 2);
    centerText(d, 220, storage::getWallpaperId(), BANPAI_LILAC, 1);
    centerText(d, 260, "ONLY 1 STORED", BANPAI_GOLD, 1);
  } else {
    centerText(d, 180, "NO WALLPAPER YET", BANPAI_SNOW_WHITE, 2);
    centerText(d, 220, "SEND ONE FROM PHONE", BANPAI_LILAC, 1);
  }
  centerText(d, 320, "TIME OVERLAY BY SYSTEM", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL: BACK");
}

void Menu::renderCheckin(IDisplay& d) {
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "CHECK-IN");
  if (ui_.checkinMeters < 0) {
    centerText(d, 170, ui_.checkinStation, BANPAI_SNOW_WHITE, 2);
    centerText(d, 220, "DISTANCE --", BANPAI_LILAC, 2);
    centerText(d, 260, "SYNC FROM PHONE", BANPAI_LILAC, 1);
  } else if (ui_.checkinMeters == 0) {
    // Arrived: success state. Guardian pets make sounds only — no
    // Chinese lines on the badge.
    d.drawPetalMark(BANPAI_SCREEN_CX, 160, 44);
    centerText(d, 230, "CHECKED IN!", BANPAI_GOLD, 2);
    centerText(d, 270, ui_.checkinStation, BANPAI_SNOW_WHITE, 1);
    centerText(d, 300, "ENERGY " + String(ui_.energy), BANPAI_GOLD, 1);
    centerText(d, 330, "GUARDIAN SOUND ONLY", BANPAI_LILAC, 1);
  } else {
    centerText(d, 170, ui_.checkinStation, BANPAI_SNOW_WHITE, 2);
    centerText(d, 220, String(ui_.checkinMeters) + " m", BANPAI_GOLD, 3);
    int mins = (ui_.checkinMeters + 79) / 80;
    centerText(d, 270, "WALK ~" + String(mins) + " MIN", BANPAI_LILAC, 1);
    centerText(d, 300, "AUTO CHECK-IN 80-150 m", BANPAI_LILAC, 1);
  }
  centerText(d, 350, "LIT " + String(ui_.lit), BANPAI_LILAC, 1);
  renderFooterHint(d, "CHECK-IN ON PHONE GPS");
}

void Menu::renderSettings(IDisplay& d) {
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "SETTINGS");
  const char* rows[8] = {
      "BRIGHTNESS", "VOLUME", "CLOCK STYLE", "TAP MODE",
      "WI-FI >", "TIME / DATE", "DEVICE INFO", "FACTORY RESET"};
  for (int i = 0; i < 8; i++) {
    int y = 106 + i * 34;
    String line = (i == settingIndex_ ? "> " : "  ") + String(rows[i]);
    if (i == 0) line += " " + String(ui_.brightness) + "%";
    if (i == 1) line += " " + String(ui_.volume) + "%";
    if (i == 2) line += ": " + String(clockStyleInfo((ClockStyle)storage::getClockStyle()).nameEn);
    if (i == 3) line += ": " + String(storage::getTapMode() == 1 ? "CAMERA" : "NORMAL");
    if (i == 5) line += ui_.timeSynced ? ": SYNCED" : ": NOT SYNCED";
    if (i == 6) line += " " + String(BANPAI_FW_VERSION_STR);
    leftText(d, y, line, i == settingIndex_ ? BANPAI_GOLD : BANPAI_SNOW_WHITE, 2);
  }
  centerText(d, 384, "BATT " + String(ui_.battPercent) + "%  466x466",
             BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL: SELECT");
}

void Menu::renderWifi(IDisplay& d) {
  // 设置 > Wi-Fi: scan nearby networks, pick one, type the password on
  // the badge (ASCII on-screen keyboard — the badge font is ASCII-only,
  // so password entry works with the built-in font). 2.4 GHz only.
  // Wi-Fi is only on when needed (download / OTA / voice sync);
  // the phone link stays on BLE to save power.
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "WI-FI 2.4G");
  if (ui_.wifiState == WifiState::CONNECTED) {
    centerText(d, 170, "CONNECTED", BANPAI_GOLD, 2);
    centerText(d, 210, ui_.wifiSsid, BANPAI_SNOW_WHITE, 2);
    centerText(d, 250, "WI-FI ONLY WHEN NEEDED", BANPAI_LILAC, 1);
  } else if (ui_.wifiState == WifiState::CONNECTING) {
    centerText(d, 180, "CONNECTING...", BANPAI_GOLD, 2);
    centerText(d, 220, ui_.wifiSsid, BANPAI_SNOW_WHITE, 1);
  } else if (ui_.wifiState == WifiState::FAILED) {
    centerText(d, 180, "CONNECT FAILED", BANPAI_PLUM_RED, 2);
    centerText(d, 220, "CHECK PASSWORD", BANPAI_LILAC, 1);
    centerText(d, 250, "DBL-KNOCK: RETRY", BANPAI_GOLD, 1);
  } else if (wifiCount_ == 0) {
    centerText(d, 180, "SCANNING...", BANPAI_SNOW_WHITE, 2);
    centerText(d, 220, "2.4 GHZ ONLY", BANPAI_LILAC, 1);
  } else {
    for (int i = 0; i < wifiCount_ && i < 6; i++) {
      // Truncate long SSIDs so the row stays inside the safe circle.
      String line = (i == wifiIndex_ ? "> " : "  ") +
                    wifiSsids_[i].substring(0, 24);
      leftText(d, 120 + i * 34, line, BANPAI_SNOW_WHITE, 2);
    }
    centerText(d, 340, "TYPE PASSWORD ON BADGE", BANPAI_LILAC, 1);
  }
  renderFooterHint(d, "KNOCK: NEXT  DBL: SELECT");
}

void Menu::renderWifiPass(IDisplay& d) {
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "WI-FI PASSWORD");
  centerText(d, 100, ui_.wifiSsid, BANPAI_SNOW_WHITE, 1);
  String masked = "PASS: ";
  for (size_t i = 0; i < wifiPass_.length(); i++) masked += "*";
  if (wifiPass_.length() == 0) masked += "(EMPTY)";
  centerText(d, 122, masked, BANPAI_GOLD, 1);
  // 7x7 keyboard grid. Selected key is gold-filled.
  for (int idx = 0; idx < kWifiKeyCount; idx++) {
    int col = idx % 7, row = idx / 7;
    int x = 80 + col * 44, y = 150 + row * 34;
    String label = wifiKeyLabel(idx);
    if (idx == wifiKeyIndex_) {
      int fx = x, fy = y, fw = 44, fh = 34;
      evenRect(fx, fy, fw, fh);
      d.fillRect(fx, fy, fw, fh, BANPAI_GOLD);
      d.text(x + (44 - (int)label.length() * 6) / 2, y + 13, label,
             BANPAI_NIGHT, 1);
    } else {
      d.text(x + (44 - (int)label.length() * 6) / 2, y + 13, label,
             BANPAI_LILAC, 1);
    }
  }
  renderFooterHint(d, "KNOCK: NEXT KEY  DBL: TYPE");
}

void Menu::renderGames(IDisplay& d) {
  // 小游戏只在吧唧上，App 里不放游戏页。
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "MINI GAMES");
  for (int i = 0; i < 3; i++) {
    const game::GameDef* g = game::gameDef((game::GameId)(i + 1));
    int best = storage::getBestScore(i + 1);
    String line = (i == gameIndex_ ? "> " : "  ") + String(g->nameEn) +
                  "  BEST " + String(best);
    leftText(d, 130 + i * 44, line, BANPAI_SNOW_WHITE, 2);
  }
  centerText(d, 300, "KNOCK TO SCORE", BANPAI_LILAC, 1);
  centerText(d, 330, "SCORE SYNC VIA BLE", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK: NEXT  DBL: PLAY");
}

void Menu::renderGamePlay(IDisplay& d) {
  const game::GameDef* g = game::gameDef(session_.id() != game::GameId::NONE
                                             ? session_.id()
                                             : pendingGame_);
  d.clear(BANPAI_NIGHT);
  renderHeader(d, g ? g->nameEn : "GAME");
  if (!session_.active()) {
    // Ready screen: tap / knock to start (design: "点屏开始").
    centerText(d, 170, "TAP TO START", BANPAI_GOLD, 2);
    centerText(d, 220, "KNOCK THE BADGE!", BANPAI_SNOW_WHITE, 2);
    if (g) {
      centerText(d, 260, "BEST " + String(storage::getBestScore((int)g->id)),
                 BANPAI_LILAC, 1);
      centerText(d, 290, "TIME LIMIT " + String(g->timeLimitSec) + "s",
                 BANPAI_LILAC, 1);
    }
  } else {
    centerText(d, 140, "SCORE " + String(session_.score()), BANPAI_GOLD, 4);
    centerText(d, 230, "KNOCK THE BADGE!", BANPAI_SNOW_WHITE, 2);
    centerText(d, 270, "LV " + String(session_.difficulty()), BANPAI_LILAC, 1);
    if (ui_.timeSynced && g) {
      int left = (int)g->timeLimitSec - (int)session_.elapsedSec(ui_.unixSec);
      if (left < 0) left = 0;
      centerText(d, 300, "LEFT " + String(left) + "s", BANPAI_LILAC, 1);
    }
    centerText(d, 330, "BEST " +
                            String(storage::getBestScore((int)session_.id())),
               BANPAI_LILAC, 1);
  }
  renderFooterHint(d, "KNOCK: SCORE  BACK: END + SEND");
}

void Menu::renderCameraShutter(IDisplay& d) {
  // 拍照键页 (2026-11-23, Jackson): big round photo button centred
  // in the 466 round safe area (centre 233,233, r=110 — well inside
  // the r=227 safe circle). Gold disc + dark inner disc + gold core,
  // like a camera shutter button. All labels ASCII (built-in font).
  d.clear(BANPAI_NIGHT);
  renderHeader(d, "CAMERA");
  d.fillCircle(BANPAI_SCREEN_CX, BANPAI_SCREEN_CY, 110, BANPAI_GOLD);
  d.fillCircle(BANPAI_SCREEN_CX, BANPAI_SCREEN_CY, 88, BANPAI_NIGHT);
  d.fillCircle(BANPAI_SCREEN_CX, BANPAI_SCREEN_CY, 30, BANPAI_GOLD);
  if (photoTaken_) {
    centerText(d, 362, "PHOTO TAKEN!", BANPAI_GOLD, 2);
  } else {
    centerText(d, 362, "TAP THE BUTTON", BANPAI_SNOW_WHITE, 1);
  }
  centerText(d, 386, "PHONE TAKES THE PHOTO", BANPAI_LILAC, 1);
  renderFooterHint(d, "KNOCK DOES NOT TAKE PHOTO");
}

} // namespace menu
