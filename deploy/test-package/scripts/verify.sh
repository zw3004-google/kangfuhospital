#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname "$0")/common.sh"

failed=0
check() {
  local label="$1"; shift
  if "$@" >/dev/null 2>&1; then echo "[通过] ${label}"; else echo "[失败] ${label}"; failed=1; fi
}

check "操作系统版本" grep -q '22.03' /etc/openEuler-release
check "CPU 架构为 x86_64" test "$(uname -m)" = "x86_64"
check "时区为 Asia/Shanghai" test "$(timedatectl show -p Timezone --value)" = "Asia/Shanghai"
check "PostgreSQL 可用" pg_isready -h 127.0.0.1 -p 5432
check "后端服务运行" systemctl is-active --quiet kangfu-server
check "Nginx 运行" systemctl is-active --quiet nginx
check "后端健康接口" curl -fsS http://127.0.0.1:8080/actuator/health
check "首页可访问" curl -fsS http://127.0.0.1/
check "8080 未对外监听" bash -c "! ss -lnt | grep -qE '0\.0\.0\.0:8080|\[::\]:8080'"

if [[ ${failed} -eq 0 ]]; then
  echo "全部检查通过。测试地址：http://172.16.196.111/"
else
  echo "存在失败项，请查看：journalctl -u kangfu-server -n 200 --no-pager" >&2
  exit 1
fi
