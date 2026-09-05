# 腾讯云测试环境部署记录

> 更新时间：2026-09-05
> 用途：功能测试与演示，不作为生产环境。

## 访问与管理员

- HTTPS：<https://kangfu.pkucarewiki.cn>
- HTTP 自动跳转 HTTPS
- 公网 IP：`170.106.110.189`
- 初始管理员：`admin`

`APP_INITIAL_PASSWORD` 是首次创建 `admin` 时使用的临时密码。实际值仅保存在服务器 `/etc/kangfu-test/kangfu.env`，不得提交 Git。首次登录后必须修改密码。初始化已经完成并设置 `APP_BOOTSTRAP_ADMIN_ENABLED=false`，以后修改该配置值不会改变数据库中的管理员密码。

授权管理员查看临时密码：

~~~powershell
ssh -i "$env:USERPROFILE\.ssh\id_ed25519" root@170.106.110.189 "grep '^APP_INITIAL_PASSWORD=' /etc/kangfu-test/kangfu.env"
~~~

## 部署结构与端口

~~~text
Internet -> Nginx :80/:443
             ├─ Vue: /opt/kangfu-test/current/app/web
             └─ /api/ -> 127.0.0.1:18080 -> Spring Boot
                                              └─ 127.0.0.1:55432 -> PostgreSQL 17
~~~

- 康复医院后端：`127.0.0.1:18080`
- 康复医院 PostgreSQL：`127.0.0.1:55432`
- AITS 保持使用 `127.0.0.1:3100`
- `doc.pkucarewiki.cn` 原配置未覆盖
- 后端和数据库均不能从公网直接访问

## 服务器调整

- Outline、Outline PostgreSQL 和 Redis 已停止，容器、数据卷和数据均保留。
- 已创建 2 GiB `/swapfile` 并写入 `/etc/fstab`。
- fstab 备份：`/etc/fstab.codex-backup-20260904`
- Java：`-Xms256m -Xmx512m`，`MemoryHigh=640M`，`MemoryMax=768M`
- PostgreSQL 容器内存上限：512 MiB

## 运行资源

- 应用：`/opt/kangfu-test`
- 当前发布：`v1.0.0`，`/opt/kangfu-test/releases/v1.0.0`；源码提交 `7b391296c7c73eb72a271eb84ce4a118e17abe38`。详见 [部署历史](DEPLOYMENT_HISTORY.md)。
- JRE：`/opt/kangfu-test/runtime`
- 环境配置：`/etc/kangfu-test/kangfu.env`
- 数据库配置：`/etc/kangfu-test/postgres.env`
- 日志：`/var/log/kangfu-test`
- 服务：`kangfu-test.service`
- 数据库容器：`kangfu-postgres-test`
- 数据卷：`kangfu-postgres-test-data`
- Nginx：`/etc/nginx/conf.d/kangfu-test.conf`
- Nginx 备份：`/etc/nginx/conf.d/kangfu-test.conf.backup-20260904-2147`

配置文件权限须保持为 `600`。不得记录或提交企业微信 Secret、数据库密码及实际临时密码。

## HTTPS

- 证书：`/etc/letsencrypt/live/kangfu.pkucarewiki.cn/`
- 当前有效期至 2026-12-03
- 自动续期：`certbot-renew.timer`
- HTTP 以 301 跳转 HTTPS

## 首次部署验收（历史记录；当前版本结果见部署历史）

- 后端测试 94 项通过，前端测试 44 项通过
- Flyway 23 个迁移成功
- HTTPS 首页返回 200
- 管理员认证返回 200，未认证 API 返回 401
- TLS 主机名验证通过
- 后端及数据库公网直连不可达

## 运维命令

~~~bash
systemctl status kangfu-test
systemctl restart kangfu-test
journalctl -u kangfu-test -f
docker ps --filter name=kangfu-postgres-test
docker logs -f kangfu-postgres-test
nginx -t
systemctl reload nginx
free -h
swapon --show
~~~

停止与启动：

~~~bash
systemctl stop kangfu-test
docker stop kangfu-postgres-test

docker start kangfu-postgres-test
systemctl start kangfu-test
~~~

禁止执行 `docker compose down -v` 或 `docker volume rm kangfu-postgres-test-data` 等删除数据库数据的命令。

## HTTPS 可信来源配置与升级检查

`/etc/kangfu-test/kangfu.env` 必须包含浏览器实际访问来源：

```ini
APP_ALLOWED_ORIGINS=https://kangfu.pkucarewiki.cn
SESSION_COOKIE_SECURE=true
```

Nginx 在 443 端口终止 TLS，再通过 HTTP 转发到后端。来源白名单为空时，后端按自身请求 URL 校验同源，浏览器 HTTPS 来源与后端 HTTP 协议不一致，会返回 HTTP 403、`ORIGIN_DENIED`（“请求来源不受信任”）。来源校验不信任任意转发头，不能仅依赖 `X-Forwarded-Proto`。

来源须包含协议、主机及实际非默认端口，不包含路径；多个来源用逗号分隔，不使用通配符。本环境 HTTP 自动跳转 HTTPS，只配置上述 HTTPS 来源。修改后执行 `systemctl restart kangfu-test`。

每次升级应核对现有环境文件中的新增配置项。除首页和健康检查外，还须验证：

1. `GET /api/auth/csrf` 返回 200，保留会话 Cookie 和返回的 Token。
2. 使用同一会话、返回的 CSRF 请求头及真实 HTTPS Origin 提交登录。空账号验证应返回认证失败 401，不能返回 `ORIGIN_DENIED` 或 `CSRF_INVALID`；真实账号登录须另行验收。
3. 使用非可信 Origin 和有效 Token 提交请求，仍返回 403 `ORIGIN_DENIED`。
4. 使用可信 Origin 但不带 Token，仍返回 403 `CSRF_INVALID`。

不能通过关闭来源校验或 CSRF 防护解决此问题。离线包使用不同的环境文件路径和服务名，见 `deploy/test-package/README.txt`。

## 2026-09-05 登录来源配置修复记录

- 环境：腾讯云测试环境 `https://kangfu.pkucarewiki.cn`。
- 时间：2026-09-05 14:25–14:26（Asia/Shanghai）。
- 原因：新版本启用来源校验后，服务器环境文件和运行进程均缺少 `APP_ALLOWED_ORIGINS`，导致 HTTPS 登录被拒绝；HTTP Origin 对照请求可通过来源校验。
- 处理：备份环境文件，补充上述 HTTPS 来源，保留文件权限 `600`，重启 `kangfu-test`，确认运行进程已加载该值。
- 备份：`/etc/kangfu-test/kangfu.env.backup-origin-20260905-142544`，仅保留在服务器，不提交仓库。
- 发布目录：修复前后均为 `/opt/kangfu-test/releases/20260905-141150`。仅修改环境配置，未替换应用构建产物、未修改数据库；现有版本号与 Git 提交编号本次未核实，不以目录时间戳代替版本号。
- 结果：服务 active，健康接口 `UP`，HTTPS 首页和 CSRF 接口均为 200；可信来源携带有效 Token 的空账号登录返回认证失败 401，非可信来源返回 403 `ORIGIN_DENIED`，缺少 Token 返回 403 `CSRF_INVALID`。未使用真实账号进行完整登录验收。上文原验收列表为首次部署历史记录。
- 回退：如需撤销本次配置，用上述备份恢复环境文件，保持权限 `600` 后重启服务；撤销后原 HTTPS 来源错误将重新出现。
