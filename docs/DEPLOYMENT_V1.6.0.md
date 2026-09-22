# V1.6.0 部署执行单

适用版本：`V1.6.0`。对比基准：`V1.5.1`。目标环境使用既有的 openEuler 22.03 LTS-SP4 x86_64 离线部署结构；首次安装使用随包 `install.sh`，已有环境仅使用 `upgrade.sh`。

## 发布前置条件

1. 将本次代码、`docs/RELEASE_NOTES.md` 和本执行单提交并推送；创建并推送同名 Git 标签 `V1.6.0`。
2. 必须从标签对应的干净提交执行测试和构建；不得将环境文件、Secret、密码、患者数据或构建缓存加入提交和发布包。
3. 核对目标机环境文件与服务名。腾讯云测试环境使用 `/etc/kangfu-test/kangfu.env` 和 `kangfu-test`；院内既有环境使用 `/etc/kangfu/kangfu.env` 和 `kangfu-server`。升级不得覆盖环境文件。
4. V1.6.0 启动时将执行 Flyway V28，为两张会诊表增加可空的 `reported_at` 字段和索引。该迁移不回填历史记录，因此历史会诊数据不会因本次上线被推送。

## 构建部署包

在标签对应的干净源码根目录执行：

```powershell
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
C:\path\to\apache-maven-3.9.11\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.0
Get-FileHash outputs\kangfu-V1.6.0.tar.gz -Algorithm SHA256
```

产物为 `outputs/kangfu-V1.6.0.tar.gz`。同时生成 SHA-256 值并与发布记录一同归档。

## 已有环境一键升级

上传压缩包和其 SHA-256 文件到目标服务器 `/opt/` 后，执行：

```bash
cd /opt
sha256sum -c kangfu-V1.6.0.tar.gz.sha256
tar -xzf kangfu-V1.6.0.tar.gz
cd /opt/kangfu-V1.6.0
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

`upgrade.sh` 会备份数据库、切换应用目录、重启服务，并在验证失败时自动回滚应用目录。Flyway 数据库迁移不会随应用回滚自动回退。

## V1.6.0 专项验收

- 用户管理：角色与科室权限组合筛选、科室权限搜索正常。
- 欠费管理：欠费金额正数展示；欠费明细、Top3、科室排行和患者 Top10 为降序，Top10 排名为 1 至 10。
- 预计出院：病区分布图能按全量同步患者数据展示。
- 企业微信：验证营养、居家康复、随访和主管医生仅接收已授权病区患者；确认 2026-10-01 前的历史会诊和实际出院记录不创建推送任务，边界当天的合格记录可创建任务。

## 部署记录与回滚

在 `docs/DEPLOYMENT_HISTORY.md` 记录目标环境、时间、版本、标签提交、部署前版本、包 SHA-256、数据库备份路径和验收结果。应用异常时运行随包 `scripts/rollback.sh`；如需恢复数据库，必须经业务负责人批准后使用 `scripts/restore.sh`。