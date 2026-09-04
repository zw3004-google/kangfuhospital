# 腾讯云测试环境部署记录

> 更新时间：2026-09-04  
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
- 当前发布：`/opt/kangfu-test/releases/20260904-2122`
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

## 验收

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
