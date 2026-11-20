# 半拍 Banpai API 契约文档（v3 新工程）

基础地址：`http://<host>:8000`，全部业务路由前缀 `/api/v1`。
鉴权：除健康检查与认证接口外，全部需要 `Authorization: Bearer <JWT>`。
JWT 为 HS256，登录/注册返回 `access_token`；过期/无效返回 401。
通用错误码：400 参数/业务错误（如能量不足、重复操作）、401 未登录或 token 无效、
403 无权限（如隐藏表盘、非房主操作）、404 资源不存在、409 冲突（如信已被捡、兑换码已用）、410 信已化掉。
交互文档：`/docs`（OpenAPI）。

## 健康

| 方法 | 路径 | 鉴权 | 请求 | 响应 | 说明 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/health` | 否 | — | `{ok, status:"ok", service, version}` | 健康检查 |

## 认证

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | 否 | `phone, password, nickname?` | `{access_token, token_type, user_id, nickname}` | 400 手机号已注册 |
| POST | `/api/v1/auth/login` | 否 | `phone, password` | 同上 | 401 手机号或密码错误 |
| POST | `/api/v1/auth/sms/send` | 否 | `phone` | `{ok, detail, demo_code}`（demo 码 123456，不发真实短信） | — |
| POST | `/api/v1/auth/sms/login` | 否 | `phone, code` | 同注册（未注册号码自动注册） | 401 验证码错误 |
| POST | `/api/v1/auth/sms/wechat` | 否 | `code`（微信授权 code，demo 模拟） | 同注册 | — |

## 用户

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/users/me` | 是 | — | `{id, phone, nickname, avatar_url, level, energy}` | 401 |
| PATCH | `/api/v1/users/me` | 是 | `nickname?, avatar_url?` | 同上 | 401 |
| GET | `/api/v1/users/me/energy` | 是 | — | `{energy, level, ledger:[{amount, kind, ref, balance_after, created_at}]}`（近 50 条） | 401 |

## 打卡

能量：首次 +80、再访 +20、拍照 +10（带 `photo_url` 时叠加）；每点每天 1 次，重复打卡幂等返回 `energy_gained:0`。

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/v1/checkin` | 是 | `point_id, lat?, lng?, photo_url?` | `{id, point_id, day, energy_gained, first_time}` | 404 打卡点不存在 |
| POST | `/api/v1/checkin/cameo-reward` | 是 | `photo_url?` | `{ok, energy_gained, line, detail}`（乱入 +5/天，重复领返回 0） | 401 |
| GET | `/api/v1/checkin/points` | 是 | — | `[{id, name, lat, lng, guardian_id, visited}]`（24 点） | 401 |
| GET | `/api/v1/checkin/progress` | 是 | — | `{total, visited, energy}` | 401 |
| GET | `/api/v1/checkin/guardians` | 是 | — | `[{id, name, location, pet, sound, line, chapter, unlocked}]`（24 只） | 401 |
| GET | `/api/v1/checkin/guardians/{id}` | 是 | — | 同上单个 | 404 守护宠不存在 |
| GET | `/api/v1/checkin/routes?from_point_id&to_point_id` | 是 | query 两点 id | `{from_point_id, to_point_id, distance_m, walk_minutes, polyline, source}`（无 AMAP_KEY 时 source=straight-line） | 404 打卡点不存在 |

## 吧唧 BLE（表盘 / 设备 / OTA / 小游戏）

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/ble/dials` | 是 | — | `[{id, name, style, price, owned, equipped}]`（30 款） | 401 |
| POST | `/api/v1/ble/dials/{id}/redeem` | 是 | — | `{ok, detail}`（扣能量解锁） | 404/403 隐藏表盘不可直接兑换/400 能量不足 |
| POST | `/api/v1/ble/dials/{id}/equip` | 是 | — | `{ok, detail, dial_id}` | 400 未拥有 |
| GET | `/api/v1/ble/ota/latest` | 是 | — | `{version, url, detail}`（占位） | 401 |
| GET | `/api/v1/ble/devices` | 是 | — | `[{id, serial, name, firmware_version, battery, user_id}]` | 401 |
| POST | `/api/v1/ble/devices` | 是 | `serial, name?` | 同上单个 | 400 设备已登记 |
| POST | `/api/v1/ble/devices/bind` | 是 | `serial, name?` | 同上单个（不存在则新建并绑定） | 401 |
| GET | `/api/v1/ble/devices/{id}` | 是 | — | 同上单个 | 404 |
| GET | `/api/v1/ble/devices/{id}/state` | 是 | — | `{device_id, serial, battery, firmware_version, equipped_dial_id, last_seen}` | 404 |
| POST | `/api/v1/ble/devices/{id}/events` | 是 | `kind, payload?` | `{ok, detail}` | 404 |
| GET | `/api/v1/ble/devices/{id}/ota-report` | 是 | — | `{device_id, firmware_version, latest, up_to_date}` | 404 |
| GET | `/api/v1/ble/devices/{id}/ota-status` | 是 | — | `{device_id, status, progress}` | 404 |
| POST | `/api/v1/ble/devices/{id}/sync` | 是 | — | `{ok, energy, equipped_dial_id, time}` | 404 |
| POST | `/api/v1/ble/game-scores` | 是 | `game`（敲钟/凿冰/咬冰棍）, `score`, `device_id?` | `{ok, id, game, score}` | 400 未知小游戏 |
| GET | `/api/v1/ble/game-scores?game?` | 是 | query 可选 game | `[{game, score, created_at}]`（前 20，分数倒序） | 401 |

## 装扮（只有 3 槽：服装/套装、背景、特效）

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/outfits` | 是 | — | `[{id, name, slot, price, owned, equipped}]`（16 套 + 1 种芽限定） | 401 |
| POST | `/api/v1/outfits/{id}/unlock` | 是 | — | `{ok, detail}`（扣能量解锁；种芽限定 price -1 拒绝：不售卖） | 404/400 能量不足 |
| POST | `/api/v1/outfits/{id}/equip` | 是 | — | `{ok, detail, slot, outfit_id}`（同槽只能装备一件） | 400 未拥有 |

## 一起种芽（奖励型：开花送限定装扮，2026-11-20 Jackson 定）

每天浇水 1 次，累计浇 7 次开花（不要求连续）；开花后领一次限定装扮「芽开·丁香」（种芽限定，不售卖）。奖励不是守护宠（守护宠只靠打卡解锁），也不是表盘（金瓣 24 号只认第五瓣信件）。

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/sprout` | 是 | — | `{waters, waters_needed:7, watered_today, bloomed, claimed, reward_outfit}` | 401 |
| POST | `/api/v1/sprout/water` | 是 | — | 同上 + `detail`（每天 1 次；第 7 次返回“开花了！”） | 400 今天已浇过 |
| POST | `/api/v1/sprout/claim` | 是 | — | `{ok, detail, outfit_id, outfit_name}`（已领过时幂等返回 ok） | 400 还没开花 |

## 花语

范围：吃/住/行/心情/感受 + 哈尔滨建筑历史。范围外或不知道一律回「我还小，帮你去打听」，不接大模型兜底。

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/chat/messages` | 是 | — | `{messages:[], detail}`（记录暂未持久化） | 401 |
| POST | `/api/v1/chat/messages` | 是 | `text, lat?, lng?` | `{reply, source, line_no?}`（source: local / small_model / out_of_scope） | 401 |
| GET | `/api/v1/chat/foods` | 是 | — | `[{name, place, is_hidden}]`（好吃团 30 种 + 隐藏雪墩） | 401 |
| GET | `/api/v1/chat/limit` | 是 | — | `{daily_limit:50, used_today, small_model_configured, detail}` | 401 |

## 日记（纯文字，不读出声）

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/diary` | 是 | — | `[{id, day, text}]`（倒序；种子 2026-11-01～11-19 共 19 篇，每篇结尾「晚安，明天见。」） | 401 |
| GET | `/api/v1/diary/today` | 是 | — | 同上单个 | 404 今天的日记还没有 |
| POST | `/api/v1/diary/generate` | 是 | `day?, mood?` | 同上单个（模板生成，结尾固定台词 #14） | 400 这天的日记已经有了 |

## 派对房间（Agora 语音 + 房间消息 IM）

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/v1/party/rooms` | 是 | `title, topic?, lat?, lng?, max_members?` | `{id, owner_id, title, topic, lat, lng, members, is_live}` | 401 |
| GET | `/api/v1/party/rooms` | 是 | — | 同上列表（进行中） | 401 |
| GET | `/api/v1/party/rooms/{id}` | 是 | — | 同上单个 | 404 |
| POST | `/api/v1/party/rooms/{id}/join` | 是 | — | `{ok, detail}` | 404/400 房间已满 |
| POST | `/api/v1/party/rooms/{id}/leave` | 是 | — | `{ok, detail}`（房主离开则房间结束） | 400 不在房间里 |
| GET | `/api/v1/party/rooms/{id}/agora-token` | 是 | — | `{app_id, token, channel, uid, expires_in, placeholder}`（未配证书为占位 token，不可真连；已配证书需接入 Agora TokenBuilder） | 404/403 请先加入房间 |
| POST | `/api/v1/party/rooms/{id}/raise-hand` | 是 | — | `{ok, hand_raised}`（举手开关切换） | 400 不在房间里 |
| GET | `/api/v1/party/rooms/{id}/speakers` | 是 | — | `[{user_id, role, muted}]` | 401 |
| POST | `/api/v1/party/rooms/{id}/speakers?user_id=` | 是 | query user_id | `{ok, detail}`（仅房主） | 403/404 |
| POST | `/api/v1/party/rooms/{id}/mute?user_id=&muted=` | 是 | query user_id, muted | `{ok, muted}`（仅房主） | 403/404 |
| POST | `/api/v1/party/rooms/{id}/kick?user_id=` | 是 | query user_id | `{ok, detail}`（仅房主） | 403/404 |
| GET | `/api/v1/party/rooms/{id}/location` | 是 | — | `{room_id, lat, lng}` | 404 |
| GET | `/api/v1/party/rooms/{id}/messages` | 是 | — | `[{id, room_id, user_id, nickname, text, created_at}]`（近 100 条） | 401 |
| POST | `/api/v1/party/rooms/{id}/messages` | 是 | `text` | 同上单个 | 403 请先加入房间 |
| GET | `/api/v1/party/rooms/{id}/online` | 是 | — | `{room_id, online, members:[{user_id, role}]}` | 401 |

## 好友（异步语音留言，不用 Agora 实时）

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/friends` | 是 | — | `[{id, phone, nickname, avatar_url, level, energy}]` | 401 |
| POST | `/api/v1/friends/{id}/messages` | 是 | `text?, voice_url?, duration_sec?`（文字或语音至少一种；语音先上传文件存储再传 URL） | `{id, sender_id, receiver_id, text, voice_url, duration_sec, is_read, created_at}` | 404 好友不存在/400 空消息 |
| GET | `/api/v1/friends/{id}/messages` | 是 | — | 同上列表（近 100 条，双向） | 401 |

## 花园 / 雪夜传信

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/garden/feed` | 是 | — | `[{id, user_id, nickname, text, image_url, likes, comments, created_at}]`（近 50） | 401 |
| GET | `/api/v1/garden/today` | 是 | — | 同上（今日） | 401 |
| POST | `/api/v1/garden/posts` | 是 | `text, image_url?` | 同上单个 | 401 |
| GET | `/api/v1/garden/posts/{id}` | 是 | — | 同上单个 | 404 |
| GET | `/api/v1/garden/posts/{id}/comments` | 是 | — | `[{id, user_id, nickname, text, created_at}]` | 401 |
| POST | `/api/v1/garden/posts/{id}/comments` | 是 | `text` | `{ok, id, text}` | 404 |
| POST | `/api/v1/garden/posts/{id}/like` | 是 | — | `{ok, detail}`（重复点赞幂等） | 404 |
| DELETE | `/api/v1/garden/posts/{id}/like` | 是 | — | `{ok, detail}` | 401 |
| POST | `/api/v1/garden/letters` | 是 | `text, lat, lng` | `{id, sender_id, lat, lng, text, expires_at, picked}`（24h 后化掉） | 401 |
| GET | `/api/v1/garden/letters/nearby?lat&lng` | 是 | query 位置 | 同上列表（100m 内未捡未过期；未捡到前不返回全文） | 401 |
| POST | `/api/v1/garden/letters/{id}/pick` | 是 | — | 同上单个（picked=true，含全文） | 404/409 已被别人捡走/410 已化掉 |

## 兑换

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/exchange/balance` | 是 | — | `{energy, level}` | 401 |
| POST | `/api/v1/exchange/redeem-code` | 是 | `code` | `{ok, energy_gained}` | 404 兑换码不存在/409 已被使用 |
| GET | `/api/v1/exchange/month-card` | 是 | — | `{active, ends_at?}` | 401 |
| POST | `/api/v1/exchange/month-card` | 是 | — | `{active, ends_at}`（30 天；占位：不扣费，只发卡记录） | 401 |

## 素材分发（占位，不复制大文件）

| 方法 | 路径 | 鉴权 | 请求字段 | 响应 | 错误 |
| --- | --- | --- | --- | --- | --- |
| GET | `/api/v1/assets/manifest` | 是 | — | `{version, kinds:{...}, source_note}` | 401 |
| GET | `/api/v1/assets/usage` | 是 | — | `{assets_dir_configured, assets_dir, detail}` | 401 |
| GET | `/api/v1/assets/{kind}/{name}` | 是 | — | `{kind, name, status, detail/path}`（未配 ASSETS_DIR 时 status=placeholder） | 404 未知种类/素材不存在 |

## 数据模型一览

users、checkins（user+point+day 唯一）、energy_ledger、guardians、checkpoints、
dials/user_dials、outfits/user_outfits、devices/device_events、game_scores、
diaries（user+day 唯一）、rooms/room_members/room_messages、friends/friend_messages、
garden_posts/garden_comments/garden_likes、letters、exchange_codes、month_cards、
sprouts（一人一颗芽：累计浇水/开花/领奖状态）。
详见 `app/models.py`。

## 锁定常量

- 15 句台词、四瓣（冰/雪/乐/花，静留给第五瓣）、24 守护宠、30 表盘、16 装扮 + 1 种芽限定、好吃团 30+1：见 `app/constants.py`。
- 一起种芽（2026-11-20 Jackson 定：奖励型）：每天浇水 1 次，累计 7 次开花，领限定装扮「芽开·丁香」。
- 能量规则：打卡首次 +80、再访 +20、拍照 +10、乱入 +5/天、寄明信片 +5；全部走 `energy.apply_energy()`。
