#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname "$0")/common.sh"
require_root
bash "${PACKAGE_ROOT}/scripts/backup.sh"
RELEASE="$(date +%Y%m%d%H%M%S)"
TARGET="${APP_ROOT}/releases/${RELEASE}"
install -d -o root -g kangfu -m 0750 "${TARGET}/app"
cp -a "${PACKAGE_ROOT}/app/." "${TARGET}/app/"
chown -R root:kangfu "${TARGET}"
ln -sfn "${TARGET}" "${APP_ROOT}/current"
systemctl restart kangfu-server
bash "${PACKAGE_ROOT}/scripts/verify.sh" || { bash "${PACKAGE_ROOT}/scripts/rollback.sh"; die "升级验证失败，已自动回滚应用版本；数据库如需回退请按手册恢复备份"; }
echo "升级完成：${RELEASE}"
