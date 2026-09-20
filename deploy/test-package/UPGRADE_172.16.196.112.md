# 172.16.196.112 升级执行单

适用目标：院内服务器 `172.16.196.112`，SSH 端口 `26662`，既有应用目录 `/opt/kangfu`，systemd 服务 `kangfu-server`。该服务器已部署离线安装包；本次为**应用升级**，严禁执行 `install.sh`，严禁覆盖 `/etc/kangfu/kangfu.env`、数据库、JRE、Nginx 配置或历史发布目录。

## 随包材料

- `kangfu-VERSION.tar.gz`
- `kangfu-VERSION.tar.gz.sha256`
- 本执行单及包内 `DEPLOYMENT_GUIDE.md`

将三份材料上传到服务器 `/opt/`。部署包本身已包含后端 JAR、桌面端/H5 静态资源、升级、备份、验收和回滚脚本。

## 一键升级

以 root 登录目标服务器后执行（将 `VERSION` 替换为实际版本）：

```bash
cd /opt
sha256sum -c kangfu-VERSION.tar.gz.sha256
tar -xzf kangfu-VERSION.tar.gz
cd /opt/kangfu-VERSION
sha256sum -c checksums/SHA256SUMS
systemctl is-active --quiet kangfu-server
pg_isready -h 127.0.0.1 -p 5432
readlink -f /opt/kangfu/current
bash scripts/upgrade.sh
```

`upgrade.sh` 会自动完成：数据库备份、创建时间戳发布目录、原子切换 `/opt/kangfu/current`、重启 `kangfu-server`、最长等待 60 秒健康接口、完整验收；任一应用验收失败时自动切回上一版本。它不会修改环境文件或删除旧发布目录。

## 结果确认

```bash
bash /opt/kangfu-VERSION/scripts/verify.sh
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1/api/system/info
readlink -f /opt/kangfu/current
ls -lt /var/lib/kangfu/backup/kangfu_*.dump | head -1
```

从业务入口复核桌面端和 H5：用户管理科室权限、欠费编辑历史分行、欠费通报报表排序、导入错误详情及随访任务范围。

## 失败处理

升级脚本失败时会自动应用回滚；不要手工删除新旧发布目录。若需要人工回滚：

```bash
bash /opt/kangfu-VERSION/scripts/rollback.sh
bash /opt/kangfu-VERSION/scripts/verify.sh
```

数据库恢复会覆盖业务数据，必须经业务负责人确认后才可执行 `restore.sh`。

## 部署记录

记录升级时间、目标环境、版本/标签、Git 提交、升级前版本、压缩包 SHA-256、自动备份路径、验证结果及异常处置，并追加到 `docs/DEPLOYMENT_HISTORY.md`。不得记录密码、Secret 或患者数据。