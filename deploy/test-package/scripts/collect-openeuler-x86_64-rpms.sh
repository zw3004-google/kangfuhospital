#!/usr/bin/env bash
set -Eeuo pipefail

[[ "$(uname -m)" == "x86_64" ]] || { echo "只能在 x86_64 临时机执行" >&2; exit 1; }
grep -q '22.03 (LTS-SP4)' /etc/openEuler-release || { echo "只能在 openEuler 22.03 LTS-SP4 临时机执行" >&2; exit 1; }
command -v dnf >/dev/null || { echo "缺少 dnf" >&2; exit 1; }
dnf -y install dnf-plugins-core
DEST="${1:-$PWD/rpms}"
mkdir -p "$DEST"
dnf download --resolve --alldeps --archlist=x86_64,noarch --destdir "$DEST" nginx
echo "Nginx 及其 SP4 依赖已下载到 $DEST"
echo "PostgreSQL 17 请从医院批准的 SP4 软件源下载服务端、客户端及依赖后放入同一目录。"
echo "不要将 glibc、systemd、openssl、rpm、dnf 等已安装系统基础包放入离线包。"
