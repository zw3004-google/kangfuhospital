#!/usr/bin/env bash
set -Eeuo pipefail

PACKAGE_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ROOT=/opt/kangfu
CONFIG_ROOT=/etc/kangfu
LOG_ROOT=/var/log/kangfu
DATA_ROOT=/var/lib/kangfu

die() { echo "错误：$*" >&2; exit 1; }
info() { echo "[康复医院部署] $*"; }
require_root() { [[ ${EUID} -eq 0 ]] || die "请使用 root 用户执行此脚本"; }

detect_arch() {
  case "$(uname -m)" in
    x86_64|aarch64) uname -m ;;
    *) die "不支持的 CPU 架构：$(uname -m)" ;;
  esac
}

require_x86_64() {
  [[ "$(detect_arch)" == "x86_64" ]] || die "本安装包仅适用于 x86_64，当前架构为 $(uname -m)"
}

find_service() {
  local pattern="$1"
  systemctl list-unit-files --type=service --no-legend 2>/dev/null \
    | awk '{print $1}' | grep -E "$pattern" | head -n 1 || true
}

read_env_value() {
  local key="$1" file="${2:-${CONFIG_ROOT}/kangfu.env}" value
  value="$(sed -n "s/^${key}=//p" "${file}" | tail -n 1)"
  value="${value%\"}"; value="${value#\"}"
  printf '%s' "${value}"
}
