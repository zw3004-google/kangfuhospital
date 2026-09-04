#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname "$0")/common.sh"
require_root
DB_USERNAME="$(read_env_value DB_USERNAME)"
DB_PASSWORD="$(read_env_value DB_PASSWORD)"
STAMP="$(date +%Y%m%d_%H%M%S)"
TARGET="${DATA_ROOT}/backup/kangfu_${STAMP}.dump"
PGPASSWORD="${DB_PASSWORD}" pg_dump -h 127.0.0.1 -U "${DB_USERNAME}" -d kangfu -Fc -f "${TARGET}"
chmod 0600 "${TARGET}"
find "${DATA_ROOT}/backup" -type f -name 'kangfu_*.dump' -mtime +30 -delete
echo "备份完成：${TARGET}"
