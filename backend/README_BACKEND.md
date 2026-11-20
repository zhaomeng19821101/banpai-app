# 半拍 Banpai 后端 README

新工程后端，从头重建（2026-11-20 定版），不沿用旧 67 次提交代码。
FastAPI + SQLAlchemy + SQLite（开发）/可切 PostgreSQL，端口 8000。

## 快速启动

```bash
cp .env.example .env        # 按需填真实 key；不填也能跑（占位模式）
bash run.sh --seed          # 首次：建 venv、装依赖、跑种子数据、启动
# 或手动：
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/python -m app.seed
.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

健康检查：`curl http://127.0.0.1:8000/api/v1/health` → `{"ok":true,"status":"ok",...}`

交互文档：http://127.0.0.1:8000/docs

演示账号（种子数据）：手机号 `13800000000` / 密码 `demo1234`，昵称 Larry，Lv.3，能量 1280。

## 目录结构

```
backend/
  requirements.txt  fastapi / uvicorn / sqlalchemy / pydantic / python-jose / passlib / httpx / pytest
  .env.example      只放占位名，不填真值
  run.sh            一键启动脚本
  README_BACKEND.md 本文件
  API.md            统一 API 契约文档（每个路由的方法/路径/字段/错误码/鉴权）
  app/
    main.py         FastAPI 入口，挂载全部路由
    db.py           engine / Session / get_db / init_db
    models.py       全部数据模型
    schemas.py      Pydantic 请求/响应模型
    security.py     JWT HS256 + 密码哈希（secret 走环境变量）
    energy.py       apply_energy() —— 所有能量变动的唯一入口
    constants.py    锁定常量：15 句台词 / 四瓣 / 24 守护宠 / 30 表盘 / 16 装扮 / 好吃团
    seed.py         种子数据（24 守护宠、24 打卡点、30 表盘、16 装扮、Larry、日记 19 篇）
    routers/        auth / users / checkin / ble / outfits / chat / diary /
                    party / friends / garden / exchange / assets
  tests/            pytest，14 个用例（见下）
```

## 锁死定版（不要改口径，要改找 Jackson）

- 四瓣：冰、雪、乐、花；静留给第五瓣。彩蛋只出信，不播视频。
- 15 句锁定台词见 `app/constants.py` 的 `LOCKED_LINES`（与 App 侧同款）。
- 能量规则：打卡首次 +80、再访 +20、每点每天 1 次、拍照 +10、乱入 +5/天、寄明信片 +5；所有变动走 `energy.apply_energy()` 写 `energy_ledger`。
- 装扮只有 3 槽：服装/套装、背景、特效；配饰已去掉。
- 日记纯文字，不读出声；种子 2026-11-01 起每天一篇到 11-19 共 19 篇，每篇结尾锁定台词 #14「晚安，明天见。」。
- 花语只接小模型 + 联网打听，不接大模型兜底；范围吃/住/行/心情/感受 + 哈尔滨建筑历史；范围外或不知道一律回「我还小，帮你去打听」。
- 守护宠 24 只（seed 按 name 去重，不断扩充不写死总数）；守护宠不说中文台词，只用动物发音/可爱叫声。
- 吧唧小游戏只在吧唧上（敲钟/凿冰/咬冰棍），敲吧唧记分。
- 雪夜传信：放进飘雪，100m 内可捡，24h 化掉，一封只被一人捡。
- 好友语音留言是异步语音消息（文件存储 + HTTP），不用 Agora 实时；Agora 只用于派对房间语音，证书只留服务端。
- 素材不复制大文件：`/api/v1/assets/*` 只做清单与占位分发，源目录见 `ASSETS_DIR`。

## 配置（全走环境变量，见 .env.example）

| 变量 | 说明 |
| --- | --- |
| `DATABASE_URL` | 默认 `sqlite:///./banpai.db`；生产可切 `postgresql+psycopg2://...` |
| `JWT_SECRET` | JWT HS256 签名密钥，部署前必须换随机长串 |
| `AMAP_KEY` | 高德 Key；未配置时路线接口返回直线距离占位 |
| `AGORA_APP_ID` / `AGORA_APP_CERTIFICATE` | 派对语音；未配置时 token 接口返回占位串 |
| `SMALL_MODEL_URL` / `SMALL_MODEL_API_KEY` | 花语小模型服务地址；未配置时用本地回答/打听口径 |
| `ASSETS_DIR` | 素材源目录；未配置时素材接口返回占位说明 |

仓库里没有任何真实密钥：`.env.example` 只有占位名；JWT secret 默认是开发占位串。

## 验证结果（2026-11-20 本机实测，Python 3.12.3）

- `python -m py_compile` 全部通过（app + routers + tests，24 个文件）。
- `pip install -r requirements.txt` 通过（venv 内安装成功）。
- `pytest tests/ -q`：**19 passed**（健康、注册/登录、鉴权 401、打卡首次 +80、打卡同天幂等、能量账本、日记 19 篇种子及结尾台词、花语范围外打听口径、花语范围内、表盘 30 款、装扮 16 套仅 3 槽 + 1 种芽限定不售卖、守护宠 24 只、乱入每日 1 次、雪夜传信一封一人捡、种芽 5 项：初始状态/每天浇水 1 次/未开花不能领/浇满 7 次开花领限定装扮/限定装扮不售卖）。
- `uvicorn` 启动成功，`curl /api/v1/health` 返回 `ok`；实测登录（Larry）、打卡点、守护宠 24、日记 19、表盘 30、花语范围外回打听口径均正常。
- 路由总数：80 条业务路由（另有 FastAPI 自带的 /docs、/openapi.json 等 4 条）。

## 未验证清单（不要假装通过）

- 未做真实短信发送与微信授权联调：`/sms/send` 固定 demo 码 `123456`，`/sms/wechat` 为模拟授权。
- 未接真实高德接口：`/checkin/routes` 无 `AMAP_KEY` 时返回直线距离占位；有 Key 也尚未对接步行规划/地理围栏/POI。
- 未接真实 Agora：`/party/rooms/{id}/agora-token` 无证书时返回占位 token；有证书时仍需接入 Agora TokenBuilder 生成真实 token（代码里已留口子，未引入签名实现）。
- 未接真实小模型服务：无 `SMALL_MODEL_URL` 时花语用本地锁定回答/打听口径；有地址时才调 `/chat` 端点，未做真实联调。
- 未做真实 OTA 固件包分发：`/ble/ota/latest` 版本号与 URL 为占位。
- 未做月卡真实支付：`/exchange/month-card` 只发卡记录，不扣费。
- 未做生产部署验证：只在 SQLite 开发库上验证；PostgreSQL 切换、Alembic 迁移、HTTPS、反向代理均未验证。
- 全库时间统一用 naive UTC（SQLite 往返会丢 tzinfo，aware/naive 混比会报错）；切 PostgreSQL 时建议复核时区处理。
- `app/main.py` 仍用已废弃的 `@app.on_event("startup")`（能用，有 DeprecationWarning；后续可改为 lifespan）。
