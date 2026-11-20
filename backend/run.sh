#!/usr/bin/env bash
# 半拍 Banpai 后端启动脚本
# 用法：bash run.sh        （首次自动建 venv 并安装依赖）
#      bash run.sh --seed （启动前跑一次种子数据）
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -d .venv ]; then
  python3 -m venv .venv
fi
.venv/bin/pip install -q -r requirements.txt

if [ "${1:-}" = "--seed" ]; then
  .venv/bin/python -m app.seed
fi

exec .venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
