#!/usr/bin/env bash
set -Eeuo pipefail
source "$(dirname "$0")/common.sh"

failed=0
pass() { echo "[通过] $1"; }
fail() { echo "[失败] $1"; failed=1; }
grep -q '22.03' /etc/openEuler-release 2>/dev/null && pass "openEuler 22.03" || fail "不是 openEuler 22.03"
[[ "$(uname -m)" == "x86_64" ]] && pass "x86_64 架构" || fail "架构不是 x86_64"
[[ "$(timedatectl show -p Timezone --value 2>/dev/null)" == "Asia/Shanghai" ]] && pass "时区 Asia/Shanghai" || fail "时区不是 Asia/Shanghai"
for name in nginx psql pg_isready openssl curl sudo; do
  command -v "$name" >/dev/null 2>&1 && pass "已安装 $name" || fail "缺少 $name"
done
if [[ -x "${PACKAGE_ROOT}/runtime/jre-21/bin/java" ]]; then
  pass "随包 Linux x86_64 JRE 21"
elif command -v java >/dev/null 2>&1 && java -version 2>&1 | grep -qE 'version "21|openjdk version "21'; then
  pass "系统 Java 21"
else
  fail "缺少 Linux x86_64 JRE 21"
fi
getent hosts qyapi.weixin.qq.com >/dev/null 2>&1 && pass "企业微信域名可解析" || fail "企业微信域名无法解析"
curl -fsS --connect-timeout 8 -o /dev/null https://qyapi.weixin.qq.com/cgi-bin/gettoken && pass "企业微信 HTTPS 出站可达" || fail "企业微信 HTTPS 出站不可达"
df -Pk / | awk 'NR==2 {exit !($4>=10485760)}' && pass "系统盘可用空间至少 10 GB" || fail "系统盘可用空间不足 10 GB"
[[ $failed -eq 0 ]] || exit 1
echo "部署前检查全部通过"
