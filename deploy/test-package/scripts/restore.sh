#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname "$0")/common.sh"
require_root
[[ $# -eq 1 && -f "$1" ]] || die "用法：bash restore.sh /var/lib/kangfu/backup/备份文件.dump"
BACKUP_FILE="$(readlink -f "$1")"
[[ "${BACKUP_FILE}" == "${DATA_ROOT}/backup/"* ]] || die "仅允许恢复 ${DATA_ROOT}/backup/ 内的备份"
DB_USERNAME="$(read_env_value DB_USERNAME)"
DB_PASSWORD="$(read_env_value DB_PASSWORD)"
systemctl stop kangfu-server
trap 'systemctl start kangfu-server' EXIT
PGPASSWORD="${DB_PASSWORD}" pg_restore -h 127.0.0.1 -U "${DB_USERNAME}" -d kangfu --clean --if-exists --no-owner "${BACKUP_FILE}"
echo "恢复完成：${BACKUP_FILE}"
