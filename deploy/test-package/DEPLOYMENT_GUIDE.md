# 康复医院运营管理系统一键部署手册

本文适用于随包的 `kangfu-VERSION.tar.gz`，目标为 **openEuler 22.03 LTS-SP4 x86_64**。部署脚本以 `/opt/kangfu`、`/etc/kangfu`、`/var/log/kangfu` 和 `/var/lib/kangfu` 为固定目录，应用运行用户为 `kangfu`。

> 版本和完整性原则：只部署与 Git 标签同名、且通过 SHA-256 校验的包。禁止用同一版本号替换已有构建产物；不得把密码、企业微信 Secret、HIS 授权信息或患者数据放进部署包、命令历史或仓库。

## 1. 包内结构与一键入口

- `app/kangfu-server.jar`：Spring Boot 后端；启动时执行 Flyway 迁移。
- `app/web/`：Vue 桌面端和 H5 共用的静态资源。
- `config/`：systemd、Nginx 与环境变量模板。
- `scripts/preflight.sh`：部署前环境检查。
- `scripts/install.sh`：首次安装。
- `scripts/upgrade.sh`：升级；先备份、切换发布目录、重启并验证，失败时自动回滚应用目录。
- `scripts/verify.sh`：部署后验收。
- `scripts/rollback.sh`：仅回滚到上一套应用目录。
- `scripts/backup.sh`、`restore.sh`：数据库备份与恢复。
- `checksums/SHA256SUMS`：随包文件哈希清单。

## 2. 部署前条件

1. 使用 `root` 登录目标服务器，确认系统为 openEuler 22.03 LTS-SP4、CPU 为 x86_64、时区为 `Asia/Shanghai`，根分区至少剩余 10 GB。
2. 服务器需具备 `nginx`、PostgreSQL 17、`psql`、`pg_isready`、`openssl`、`curl`、`sudo`。若 `rpms/` 含离线依赖，安装脚本只安装目标机未安装的同名 RPM。
3. 准备 Linux x86_64 JRE 21：可使用随包 `runtime/jre-21`，或已安装系统 Java 21。
4. 放通浏览器到 Nginx 的 HTTP/HTTPS 端口；后端 8080 和数据库 5432 只能本机监听。
5. 企业微信使用前，目标机必须能解析并通过 HTTPS 访问 `qyapi.weixin.qq.com`。HIS 与企业微信敏感参数仅在服务器的环境文件填写。

## 3. 上传与完整性校验

在部署机执行（将 `VERSION` 换成实际版本）：

```bash
scp kangfu-VERSION.tar.gz root@TARGET_HOST:/opt/
ssh root@TARGET_HOST
cd /opt
tar -xzf kangfu-VERSION.tar.gz
cd kangfu-VERSION
sha256sum -c checksums/SHA256SUMS
```

所有条目必须显示 `OK`。校验失败时停止操作，重新获取发布包；不得跳过校验或手工替换包内文件。

## 3.1 前端版本注入与发布前核验

页面右上角的版本号来自前端构建常量 `__APP_VERSION__`。该常量依赖包管理器注入的 `npm_package_version`；未注入时会回退为 `dev`，页面将显示 `Vdev`，即使后端接口和业务功能已是正确版本。

构建发布包前必须在标签对应的干净源码上执行：

```bash
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
```

禁止直接执行 `vite build`、`pnpm --dir web exec vite build` 或其他绕过 `package.json` 构建脚本的 Vite 命令；这些命令可能不注入 `npm_package_version`。前端构建完成后，不得在打包前以其他命令再次覆盖 `web/dist`。

打包前应完成以下双重核验：

1. `web/package.json` 的版本号、后端构建版本和计划发布标签必须一致。
2. 从本地构建产物启动或预览后，页面右上角必须显示 `V版本号`（例如 `V1.6.0`），不得显示 `Vdev`。发现不一致时停止打包，按上述命令重新构建。

版本号错误的构建产物不得以相同版本号覆盖已发布包、Git 标签或服务器目录。若错误包已经发布，必须修复构建后创建新的修订版本、提交和同名标签，再生成新的升级包。
## 4. 首次部署（一键安装）

首次部署前，先根据实际访问地址更新 Nginx 与来源白名单模板：

```bash
cd /opt/kangfu-VERSION
sed -i 's/172\.16\.196\.111/TARGET_HOST/g' config/kangfu-nginx.conf
sed -i 's#http://172\.16\.196\.111#http://TARGET_HOST#g' config/kangfu.env.example
bash scripts/preflight.sh && bash scripts/install.sh && bash scripts/verify.sh
```

上述命令会创建数据库、首次环境文件、systemd 服务、Nginx 配置和首个发布目录；安装脚本会生成数据库密码及临时管理员密码到 `/etc/kangfu/kangfu.env`。请在受控终端安全保存，绝不可复制到工单、聊天记录或仓库。

安装后必须编辑服务器环境文件，再重启和验收：

```bash
vi /etc/kangfu/kangfu.env
chmod 600 /etc/kangfu/kangfu.env
systemctl restart kangfu-server
bash /opt/kangfu-VERSION/scripts/verify.sh
```

必须核对：

- `APP_ALLOWED_ORIGINS` 为真实访问来源，例如 `https://hospital.example.com`；多个来源以逗号分隔，不包含路径且不使用通配符。
- HTTPS 入口必须设置 `SESSION_COOKIE_SECURE=true`，HTTP 临时测试入口才可为 `false`。
- 首次管理员创建完成并改密后，设置 `APP_BOOTSTRAP_ADMIN_ENABLED=false` 并重启服务。
- 在目标机填写 `WECOM_*` 和 `HIS_*`；Secret、密码和授权标识不得写入包或代码。

## 5. 已有环境升级（一键升级）

升级不会覆盖 `/etc/kangfu/kangfu.env`。先核对新版本是否增加配置项，并确认当前版本、备份空间和包完整性：

```bash
readlink -f /opt/kangfu/current
bash /opt/kangfu-VERSION/scripts/preflight.sh
bash /opt/kangfu-VERSION/scripts/upgrade.sh
```

`upgrade.sh` 会依次完成数据库备份、创建带时间戳的新发布目录、原子切换 `current`、重启 `kangfu-server`、运行 `verify.sh`。后端启动时自动执行只增不减的 Flyway 迁移；迁移不可由应用目录回滚替代。

## 6. 部署后验收

```bash
bash /opt/kangfu-VERSION/scripts/verify.sh
systemctl status kangfu-server --no-pager
systemctl status nginx --no-pager
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1/api/system/info
journalctl -u kangfu-server -n 200 --no-pager
```

除脚本检查外，应从真实浏览器入口验证：登录与 CSRF、桌面端和 H5、欠费查询/编辑、预出院同步、角色与科室数据范围、企业微信任务创建与失败记录。确认 8080 未对外监听，且非可信 Origin 被拒绝。

版本验收必须同时通过：

- `/api/system/info` 响应中的 `data.version` 与本次 `VERSION` 一致，证明后端版本正确。
- 浏览器强制刷新后，页面右上角显示 `V版本号`，证明前端静态资源正确。

两项任一不一致，均不得将部署标记为完成；应保留现场信息并按新的修订版本重新发布，不得替换同版本产物。

## 7. 自动回滚与人工回滚

若升级后的应用或健康检查失败，`upgrade.sh` 会自动切回上一套应用目录。人工回滚仅用于应用问题：

```bash
bash /opt/kangfu-VERSION/scripts/rollback.sh
bash /opt/kangfu-VERSION/scripts/verify.sh
```

数据库迁移或业务数据问题必须先停止业务、确认影响范围和最近备份，再按批准的恢复方案执行：

```bash
ls -l /var/lib/kangfu/backup/
bash /opt/kangfu-VERSION/scripts/restore.sh /var/lib/kangfu/backup/FILE.dump
```

`restore.sh` 会停止服务并覆盖数据库内容；这是高风险操作，未经业务负责人确认不得执行。应用目录回滚不会自动回退 Flyway 结构或业务数据。

## 8. 常见故障

- `preflight.sh` 提示企微不可达：检查 DNS、网关、防火墙或代理；不要把 Secret 写入诊断命令。
- 登录返回 `ORIGIN_DENIED`：核对 `APP_ALLOWED_ORIGINS` 的协议、主机和端口，修改后重启后端。
- 登录返回 `CSRF_INVALID`：先请求 `/api/auth/csrf`，在相同会话中携带返回 Token；不要关闭 CSRF 防护。
- 服务未启动：执行 `journalctl -u kangfu-server -n 200 --no-pager`，检查 `/etc/kangfu/kangfu.env` 权限为 600、数据库可用和 Java 21。
- 首页 502/500：执行 `nginx -t`、检查 Nginx 服务、`/opt/kangfu/current` 链接及目录属组/权限；不要直接删除旧发布目录。
- 功能已更新但页面显示 `Vdev`：前端静态资源很可能由直接调用 Vite 的命令构建，导致 `npm_package_version` 未注入。不要在服务器手工修改版本号；从标签对应源码使用 `pnpm --dir web build` 重新构建，并以新的修订版本重新生成、校验和部署升级包。

## 9. 部署记录

每次部署或回滚必须记录：目标环境、时间、版本和 Git 提交、部署前版本、包 SHA-256、数据库备份路径、执行人、验证结果和异常处置。记录追加到项目 `docs/DEPLOYMENT_HISTORY.md`，不得记录密码、Secret 或患者明细。