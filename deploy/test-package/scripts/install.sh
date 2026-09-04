#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname "$0")/common.sh"
require_root

[[ -f /etc/openEuler-release ]] || die "未检测到 openEuler"
grep -q '22.03' /etc/openEuler-release || die "本安装包仅针对 openEuler 22.03 SP3 验证"
require_x86_64
ARCH="$(detect_arch)"
info "检测到架构 ${ARCH}"

if compgen -G "${PACKAGE_ROOT}/rpms/*.rpm" >/dev/null; then
  info "安装随包 RPM（完全离线，不访问软件源）"
  dnf install -y --disablerepo='*' "${PACKAGE_ROOT}"/rpms/*.rpm
fi

for command_name in nginx psql pg_isready openssl curl; do
  command -v "${command_name}" >/dev/null 2>&1 || die "缺少 ${command_name}，请把同架构 RPM 及完整依赖放入 rpms/ 后重试"
done

id kangfu >/dev/null 2>&1 || useradd --system --home-dir "${APP_ROOT}" --shell /sbin/nologin kangfu
install -d -o kangfu -g kangfu -m 0750 "${APP_ROOT}/releases" "${LOG_ROOT}" "${DATA_ROOT}/backup"
install -d -o root -g kangfu -m 0750 "${CONFIG_ROOT}"

RELEASE="$(date +%Y%m%d%H%M%S)"
RELEASE_DIR="${APP_ROOT}/releases/${RELEASE}"
install -d -o root -g kangfu -m 0750 "${RELEASE_DIR}/app"
cp -a "${PACKAGE_ROOT}/app/." "${RELEASE_DIR}/app/"
chown -R root:kangfu "${RELEASE_DIR}"
find "${RELEASE_DIR}" -type d -exec chmod 0750 {} +
find "${RELEASE_DIR}" -type f -exec chmod 0640 {} +
ln -sfn "${RELEASE_DIR}" "${APP_ROOT}/current"

if [[ -x "${PACKAGE_ROOT}/runtime/jre-21/bin/java" ]]; then
  info "使用随包 JRE 21"
  RUNTIME_DIR="${APP_ROOT}/runtime-x86_64"
  install -d -o root -g kangfu -m 0750 "${RUNTIME_DIR}"
  cp -a "${PACKAGE_ROOT}/runtime/jre-21/." "${RUNTIME_DIR}/"
  chown -R root:kangfu "${RUNTIME_DIR}"
  ln -sfn "${RUNTIME_DIR}" "${APP_ROOT}/runtime"
elif command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -qE 'version "21|openjdk version "21'; then
  JAVA_BIN="$(readlink -f "$(command -v java)")"
  JAVA_HOME_DIR="$(dirname "$(dirname "${JAVA_BIN}")")"
  rm -f "${APP_ROOT}/runtime"
  ln -s "${JAVA_HOME_DIR}" "${APP_ROOT}/runtime"
else
  die "缺少 Linux JRE 21。请将其解压为 runtime/jre-21，或先安装系统 Java 21"
fi

if [[ ! -f "${CONFIG_ROOT}/kangfu.env" ]]; then
  cp "${PACKAGE_ROOT}/config/kangfu.env.example" "${CONFIG_ROOT}/kangfu.env"
  DB_PASSWORD="$(openssl rand -base64 32 | tr -d '/+=' | head -c 28)"
  ADMIN_PASSWORD="KfT!$(openssl rand -hex 8)"
  sed -i "s/REPLACE_WITH_STRONG_RANDOM_PASSWORD/${DB_PASSWORD}/" "${CONFIG_ROOT}/kangfu.env"
  sed -i "s/REPLACE_WITH_TEMP_ADMIN_PASSWORD/${ADMIN_PASSWORD}/" "${CONFIG_ROOT}/kangfu.env"
  chmod 0600 "${CONFIG_ROOT}/kangfu.env"
  info "已生成数据库密码和临时管理员密码；请立即安全记录 /etc/kangfu/kangfu.env 中的值"
fi

PG_SERVICE="$(find_service '^postgresql(-[0-9]+)?\.service$')"
[[ -n "${PG_SERVICE}" ]] || die "未找到 PostgreSQL systemd 服务，请完成数据库 RPM 安装和 initdb"
systemctl enable "${PG_SERVICE}"
if ! systemctl start "${PG_SERVICE}"; then
  PG_SETUP="$(command -v postgresql-17-setup || command -v postgresql-setup || true)"
  [[ -n "${PG_SETUP}" ]] || die "PostgreSQL 启动失败且未找到数据库初始化工具"
  info "首次初始化 PostgreSQL 数据目录"
  "${PG_SETUP}" --initdb
  systemctl start "${PG_SERVICE}"
fi

DB_USERNAME="$(read_env_value DB_USERNAME)"
DB_PASSWORD="$(read_env_value DB_PASSWORD)"
[[ -n "${DB_USERNAME}" && -n "${DB_PASSWORD}" ]] || die "数据库用户名或密码为空"
sudo -u postgres psql -v ON_ERROR_STOP=1 --set=db_user="${DB_USERNAME}" --set=db_pass="${DB_PASSWORD}" --set=db_name=kangfu <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'db_user', :'db_pass')
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = :'db_user') \gexec
SELECT format('ALTER ROLE %I PASSWORD %L', :'db_user', :'db_pass') \gexec
SELECT format('CREATE DATABASE %I OWNER %I ENCODING ''UTF8'' TEMPLATE template0', :'db_name', :'db_user')
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = :'db_name') \gexec
SQL

install -m 0644 "${PACKAGE_ROOT}/config/kangfu-server.service" /etc/systemd/system/kangfu-server.service
install -m 0644 "${PACKAGE_ROOT}/config/kangfu-nginx.conf" /etc/nginx/conf.d/kangfu.conf
nginx -t
systemctl daemon-reload
systemctl enable --now nginx kangfu-server

if command -v firewall-cmd >/dev/null 2>&1; then
  firewall-cmd --permanent --add-service=http
  firewall-cmd --reload
fi

info "安装完成。请填写企微参数并执行 systemctl restart kangfu-server，然后运行 scripts/verify.sh"
