# V1.6.1 升级执行单

适用版本：`V1.6.1`。对比基准：`V1.6.0`。目标环境使用既有的 openEuler 22.03 LTS-SP4 x86_64 离线部署结构；本版本适用于已经部署 V1.6.0 或更早版本的服务器，已有环境仅使用随包 `upgrade.sh`。

## 发布前置条件

1. 本次代码、`docs/RELEASE_NOTES.md` 和本执行单必须已提交并推送；升级包必须由同名 Git 标签 `V1.6.1` 对应的干净提交构建。
2. 目标机沿用既有 `/etc/kangfu/kangfu.env`、PostgreSQL、JRE、Nginx 与 `kangfu-server` 服务。升级不得覆盖环境文件、密钥、密码、HIS 参数或业务数据。
3. 启动时将执行 Flyway V29，统一历史推送任务正文的院内系统访问说明。该迁移仅修改 `push_task.content` 展示正文；应用目录回滚不会回退该数据库内容。
4. 升级前确认 `/opt/kangfu/current` 指向的现有版本、数据库可用且备份目录有足够空间。

## 构建一键升级包

在标签对应的干净源码根目录执行：

```powershell
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.1
Get-FileHash outputs\kangfu-V1.6.1.tar.gz -Algorithm SHA256
```

产物：`outputs/kangfu-V1.6.1.tar.gz` 与 `outputs/kangfu-V1.6.1.tar.gz.sha256`。前端构建必须使用 `pnpm --dir web build`；页面右上角必须显示 `V1.6.1`，不得显示 `Vdev`。

## 服务器一键升级

将压缩包、`.sha256` 文件和本执行单上传到服务器 `/opt/`，以 root 执行：

```bash
cd /opt
sha256sum -c kangfu-V1.6.1.tar.gz.sha256
tar -xzf kangfu-V1.6.1.tar.gz
cd /opt/kangfu-V1.6.1
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

`upgrade.sh` 自动备份数据库、创建新发布目录、原子切换、重启服务并等待健康检查；应用验收失败时自动切回上一应用目录。不得执行 `install.sh`，不得手工覆盖 `/etc/kangfu/kangfu.env`。

## V1.6.1 专项验收

1. 用户管理：勾选 3 名用户后导出，Excel 仅包含这 3 人；不勾选时导出全部。表头包含姓名、工号、登录名、企微 ID、所属科室、角色、科室权限、状态，且不包含操作列。
2. 欠费明细：导出文件中的欠费金额为正数，和页面一致。
3. 欠费管理与预出院管理：在推送详情列表点击“推送内容详情”，长文本可完整查看；正文末尾显示统一的院内访问说明与 `http://172.16.196.112`。
4. 版本：`curl -fsS http://127.0.0.1:8080/api/system/info` 的 `data.version` 为 `1.6.1`；浏览器强制刷新后右上角显示 `V1.6.1`。

## 回滚与记录

应用异常时执行 `bash scripts/rollback.sh` 后重新执行 `bash scripts/verify.sh`。Flyway V29 已修改的推送正文不会由应用回滚自动恢复；如需数据库恢复，必须经业务负责人批准后使用随包 `restore.sh`。

部署完成后，按项目部署规范在 `docs/DEPLOYMENT_HISTORY.md` 记录目标环境、时间、版本、Git 提交、升级前版本、包 SHA-256、数据库备份路径及验收结果；不得记录密码、Secret 或患者明细。