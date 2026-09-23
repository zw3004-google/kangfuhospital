#!/usr/bin/env bash
# 受控数据维护脚本：仅清空欠费明细当前记录，不删除患者主档、导入批次、审计或推送记录。
set -Eeuo pipefail

source "$(dirname "$0")/common.sh"
require_root

CONFIRM_FLAG="--confirm-clear-arrears"
if [[ $# -ne 1 || "$1" != "${CONFIRM_FLAG}" ]]; then
  cat >&2 <<USAGE
用法：bash clear-arrears-details.sh ${CONFIRM_FLAG}

此操作会清空欠费明细中的全部当前记录（arrears_record）。
执行前会自动创建 PostgreSQL 全库备份；患者主档、导入批次、审计日志和推送记录不会删除。
仅限已获业务负责人和数据管理员书面批准的维护窗口执行。
USAGE
  exit 2
fi

DB_USERNAME="$(read_env_value DB_USERNAME)"
DB_PASSWORD="$(read_env_value DB_PASSWORD)"
[[ -n "${DB_USERNAME}" && -n "${DB_PASSWORD}" ]] || die "数据库用户名或密码为空"
command -v psql >/dev/null 2>&1 || die "缺少 psql 命令"
command -v pg_dump >/dev/null 2>&1 || die "缺少 pg_dump 命令"
PGPASSWORD="${DB_PASSWORD}" pg_isready -h 127.0.0.1 -U "${DB_USERNAME}" -d kangfu >/dev/null \
  || die "数据库不可用，未执行清空"

before_count="$(PGPASSWORD="${DB_PASSWORD}" psql -X -qAt -v ON_ERROR_STOP=1 -h 127.0.0.1 -U "${DB_USERNAME}" -d kangfu \
  -c 'SELECT count(*) FROM arrears_record')"
info "待清空欠费明细记录：${before_count} 条"

backup_output="$(bash "${PACKAGE_ROOT}/scripts/backup.sh")"
printf '%s\n' "${backup_output}"
backup_file="$(printf '%s\n' "${backup_output}" | sed -n 's/^备份完成：//p' | tail -n 1)"
[[ -n "${backup_file}" && -f "${backup_file}" ]] || die "自动备份未生成可用文件，未执行清空"

service_was_active=false
if systemctl is-active --quiet kangfu-server; then
  info "停止 kangfu-server，避免同步或人工操作并发写入"
  systemctl stop kangfu-server
  service_was_active=true
fi

restart_service() {
  if [[ "${service_was_active}" == true ]]; then
    systemctl start kangfu-server || true
  fi
}
trap restart_service EXIT

deleted_count="$(PGPASSWORD="${DB_PASSWORD}" psql -X -qAt -v ON_ERROR_STOP=1 -h 127.0.0.1 -U "${DB_USERNAME}" -d kangfu <<'SQL'
BEGIN;
LOCK TABLE arrears_record IN ACCESS EXCLUSIVE MODE;
WITH deleted AS (
  DELETE FROM arrears_record
  RETURNING 1
)
SELECT count(*) FROM deleted;
COMMIT;
SQL
)"

remaining_count="$(PGPASSWORD="${DB_PASSWORD}" psql -X -qAt -v ON_ERROR_STOP=1 -h 127.0.0.1 -U "${DB_USERNAME}" -d kangfu \
  -c 'SELECT count(*) FROM arrears_record')"
[[ "${remaining_count}" == "0" ]] || die "清空后仍有 ${remaining_count} 条欠费记录；请勿进行同步，先按备份恢复并排查"

if [[ "${service_was_active}" == true ]]; then
  systemctl start kangfu-server
  service_was_active=false
  health_deadline=$((SECONDS + 60))
  until curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; do
    (( SECONDS < health_deadline )) || die "服务重启后 60 秒内健康检查未通过；欠费数据已清空，备份为：${backup_file}"
    sleep 2
  done
fi
trap - EXIT

info "已清空 ${deleted_count} 条欠费明细记录。"
info "备份文件：${backup_file}"
info "请在系统中核对欠费明细为空；后续执行“同步在院欠费”会重新写入 HIS 返回的数据。"
