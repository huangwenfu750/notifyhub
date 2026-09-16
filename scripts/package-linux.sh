#!/usr/bin/env bash
# 构建并打包 NotifyHub Linux 发行包：
#   gradle :server:installDist  →  python scripts/package-linux.py
# 产物: build/linux/notifyhub-<version>-linux-x86_64.tar.gz
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> gradle :server:installDist"
gradle :server:installDist --no-daemon -q

PY=python3
command -v "$PY" >/dev/null 2>&1 || PY=python
echo "==> 打包 ($PY scripts/package-linux.py $*)"
"$PY" scripts/package-linux.py "$@"
