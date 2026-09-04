#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname "$0")/common.sh"
require_root
mapfile -t RELEASES < <(find "${APP_ROOT}/releases" -mindepth 1 -maxdepth 1 -type d | sort -r)
[[ ${#RELEASES[@]} -ge 2 ]] || die "没有可回滚的上一版本"
CURRENT="$(readlink -f "${APP_ROOT}/current")"
TARGET=""
for item in "${RELEASES[@]}"; do [[ "${item}" != "${CURRENT}" ]] && TARGET="${item}" && break; done
[[ -n "${TARGET}" ]] || die "没有可回滚版本"
ln -sfn "${TARGET}" "${APP_ROOT}/current"
systemctl restart kangfu-server
echo "已回滚到：${TARGET}"
