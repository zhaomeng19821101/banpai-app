// game.cpp — badge mini-game scoring (knock-driven, see game.h).
#include "game.h"
#include "ble_protocol.h"

namespace game {

void GameSession::start(GameId id, int difficulty) {
  id_ = id;
  difficulty_ = difficulty < 1 ? 1 : (difficulty > 3 ? 3 : difficulty);
  score_ = 0;
  startedAt_ = 0;  // filled by caller with badge unix time when known
  startedMs_ = millis();
  lastKnockMs_ = 0;
  active_ = true;
}

void GameSession::end() { active_ = false; }

uint32_t GameSession::elapsedSec(uint32_t nowSec) const {
  if (!active_ && startedAt_ == 0) return 0;
  if (nowSec >= startedAt_) return nowSec - startedAt_;
  return 0;
}

bool GameSession::timeUp(uint32_t nowSec) const {
  const GameDef* def = gameDef(id_);
  if (!def) return true;
  return elapsedSec(nowSec) >= def->timeLimitSec;
}

int GameSession::onKnock(uint32_t nowMs) {
  if (!active_) return 0;
  if (nowMs - lastKnockMs_ < kKnockDebounceMs) return 0;  // debounce
  lastKnockMs_ = nowMs;
  int pts = pointsPerKnock(difficulty_);
  score_ += pts;
  return pts;
}

String GameSession::scoreFrame(uint32_t timestamp) const {
  const GameDef* def = gameDef(id_);
  bleproto::ScoreFrame f;
  f.game = def ? def->code : "none";
  f.difficulty = difficulty_;
  f.score = score_;
  f.timestamp = timestamp;
  return bleproto::buildScore(f);
}

} // namespace game
