// game.h — 半拍 Banpai badge mini-games (badge ONLY).
// The three games live ONLY on the badge. The App has NO game page.
// Scores are reported to the phone over BLE SCORE (b103):
//   "G:<game>|<difficulty>|<score>|<timestamp>"
// Knock input comes from the IMU (QMI8658) — the CST9217 touch panel
// reads unstably and must NOT be used for scoring-critical input.
// A game session: start -> knocks add points -> end -> SCORE notify.
// Difficulty 1..3 scales the points per knock and the time limit.
#pragma once

#include <Arduino.h>

namespace game {

enum class GameId : uint8_t {
  NONE = 0,
  BELL = 1,   // 敲钟
  ICE = 2,    // 凿冰
  STICK = 3,  // 咬冰棍
};

struct GameDef {
  GameId id;
  const char* code;    // SCORE frame game field: "bell" | "ice" | "stick"
  const char* nameEn;  // ASCII label (badge font is ASCII-only)
  const char* nameZh;  // Chinese name (App side / docs)
  uint16_t timeLimitSec;  // per difficulty below
};

inline const GameDef kGames[3] = {
    {GameId::BELL, "bell", "BELL", "敲钟", 30},
    {GameId::ICE, "ice", "ICE", "凿冰", 30},
    {GameId::STICK, "stick", "STICK", "咬冰棍", 20},
};

inline const GameDef* gameDef(GameId id) {
  for (auto& g : kGames) {
    if (g.id == id) return &g;
  }
  return nullptr;
}

// Points per valid knock at each difficulty (index 0 unused).
inline int pointsPerKnock(int difficulty) {
  static const int kPts[4] = {0, 10, 20, 30};
  if (difficulty < 1) difficulty = 1;
  if (difficulty > 3) difficulty = 3;
  return kPts[difficulty];
}

class GameSession {
 public:
  void start(GameId id, int difficulty);
  void end();                       // stops timer; score is kept for send
  // Badge unix time (from TIME frame) at the moment the game started.
  // MUST be set right after start() — otherwise timeUp() has no base.
  void setStartedAt(uint32_t unixSec) { startedAt_ = unixSec; }
  bool active() const { return active_; }
  GameId id() const { return id_; }
  int difficulty() const { return difficulty_; }
  int score() const { return score_; }
  uint32_t startedAt() const { return startedAt_; }
  uint32_t elapsedSec(uint32_t nowSec) const;
  bool timeUp(uint32_t nowSec) const;
  // Call on each validated IMU knock while active. Returns points added
  // (0 if the knock arrived inside the debounce window and was ignored).
  int onKnock(uint32_t nowMs);
  // SCORE frame payload for this session ("G:..." — see ble_protocol.h).
  String scoreFrame(uint32_t timestamp) const;

 private:
  GameId id_ = GameId::NONE;
  int difficulty_ = 1;
  int score_ = 0;
  uint32_t startedAt_ = 0;   // seconds (badge time base)
  uint32_t startedMs_ = 0;
  uint32_t lastKnockMs_ = 0;
  bool active_ = false;
  static const uint32_t kKnockDebounceMs = 120;  // ignore double-counts
};

} // namespace game
