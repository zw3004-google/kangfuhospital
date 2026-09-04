#!/usr/bin/env bash
set -Eeuo pipefail

PG_VERSION="${PG_VERSION:-17.11}"
OUT_DIR="${OUT_DIR:-/out}"
TOPDIR=/root/rpmbuild

dnf install -y rpm-build gcc make readline-devel zlib-devel openssl-devel perl bison flex curl tar gzip
mkdir -p "${TOPDIR}"/{BUILD,BUILDROOT,RPMS,SOURCES,SPECS,SRPMS} "${OUT_DIR}"
curl -fL --retry 3 "https://ftp.postgresql.org/pub/source/v${PG_VERSION}/postgresql-${PG_VERSION}.tar.gz" -o "${TOPDIR}/SOURCES/postgresql-${PG_VERSION}.tar.gz"

cat >"${TOPDIR}/SOURCES/postgresql-17.service" <<'EOF'
[Unit]
Description=PostgreSQL 17 database server (Kangfu offline package)
After=network.target

[Service]
Type=notify
User=postgres
Group=postgres
Environment=PGDATA=/var/lib/pgsql/17/data
ExecStart=/opt/postgresql-17/bin/postgres -D ${PGDATA}
ExecReload=/bin/kill -HUP $MAINPID
KillMode=mixed
KillSignal=SIGINT
TimeoutSec=0

[Install]
WantedBy=multi-user.target
EOF

cat >"${TOPDIR}/SOURCES/postgresql-17-setup" <<'EOF'
#!/usr/bin/env bash
set -Eeuo pipefail
case "${1:-}" in initdb|--initdb) ;; *) echo "用法: postgresql-17-setup initdb" >&2; exit 2;; esac
PGDATA=/var/lib/pgsql/17/data
install -d -o postgres -g postgres -m 0700 "${PGDATA}"
if [[ ! -f "${PGDATA}/PG_VERSION" ]]; then
  runuser -u postgres -- /opt/postgresql-17/bin/initdb -D "${PGDATA}" --encoding=UTF8 --locale=C.UTF-8
fi
EOF

cat >"${TOPDIR}/SPECS/kangfu-postgresql17.spec" <<EOF
%global debug_package %{nil}
Name:           kangfu-postgresql17
Version:        ${PG_VERSION}
Release:        1.oe2203sp3
Summary:        PostgreSQL 17 server and client for Kangfu Hospital
License:        PostgreSQL
URL:            https://www.postgresql.org/
Source0:        postgresql-${PG_VERSION}.tar.gz
Source1:        postgresql-17.service
Source2:        postgresql-17-setup
BuildRequires:  gcc make readline-devel zlib-devel openssl-devel perl bison flex
Requires:       readline zlib openssl-libs systemd shadow-utils util-linux

%description
Native PostgreSQL 17 server and client built on openEuler 22.03 SP3 x86_64.

%prep
%setup -q -n postgresql-${PG_VERSION}

%build
./configure --prefix=/opt/postgresql-17 --with-openssl --without-icu
make %{?_smp_mflags}

%install
rm -rf %{buildroot}
make install DESTDIR=%{buildroot}
install -D -m 0644 %{SOURCE1} %{buildroot}/usr/lib/systemd/system/postgresql-17.service
install -D -m 0755 %{SOURCE2} %{buildroot}/usr/bin/postgresql-17-setup
mkdir -p %{buildroot}/var/lib/pgsql/17/data %{buildroot}/usr/local/bin
for c in psql pg_dump pg_restore pg_isready createdb dropdb createuser dropuser; do ln -s /opt/postgresql-17/bin/\$c %{buildroot}/usr/local/bin/\$c; done

%pre
getent group postgres >/dev/null || groupadd -r postgres
getent passwd postgres >/dev/null || useradd -r -g postgres -d /var/lib/pgsql -s /sbin/nologin postgres

%post
systemctl daemon-reload >/dev/null 2>&1 || :

%preun
if [ \$1 -eq 0 ]; then systemctl disable --now postgresql-17.service >/dev/null 2>&1 || :; fi

%postun
systemctl daemon-reload >/dev/null 2>&1 || :

%files
%license COPYRIGHT
/opt/postgresql-17
/usr/bin/postgresql-17-setup
/usr/lib/systemd/system/postgresql-17.service
/usr/local/bin/*
%dir %attr(0700,postgres,postgres) /var/lib/pgsql/17/data

%changelog
* Fri Sep 04 2026 Kangfu Deployment <ops@localhost> - ${PG_VERSION}-1
- Native offline build for openEuler 22.03 SP3 x86_64
EOF

rpmbuild --define "_topdir ${TOPDIR}" -bb "${TOPDIR}/SPECS/kangfu-postgresql17.spec"
cp -v "${TOPDIR}"/RPMS/x86_64/*.rpm "${OUT_DIR}/"
sha256sum "${OUT_DIR}"/*.rpm
