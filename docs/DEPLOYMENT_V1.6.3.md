# V1.6.3 一键升级手册

适用版本：`V1.6.3`。对比基准：`V1.6.2`。目标环境沿用既有 openEuler 离线部署结构；已部署 V1.6.2 的服务器仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 预出院统计分析的预出院看板改为展示次要诊断。
- 预出院看板按预计出院时间由早到晚展示。
- 业务明细新增“特殊患者看板”，仅显示特殊患者，并展示特殊原因。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.3` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本无新增数据库迁移；目标服务器应已包含 V1.6.2 的 Flyway V30（`patient_encounter.secondary_diagnosis`）。

## 构建与校验

```powershell
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.3
Get-FileHash outputs\kangfu-V1.6.3.tar.gz -Algorithm SHA256
```

前端必须通过 `pnpm --dir web build` 构建；打包前确认页面显示 `V1.6.3`，不得显示 `Vdev`。

## 服务器一键升级

将 `kangfu-V1.6.3.tar.gz` 与 `kangfu-V1.6.3.tar.gz.sha256` 上传到服务器 `/opt`，然后以具备 sudo 权限的账号执行：

```bash
cd /opt
sha256sum -c kangfu-V1.6.3.tar.gz.sha256
tar -xzf kangfu-V1.6.3.tar.gz
cd /opt/kangfu-V1.6.3
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

`upgrade.sh` 会先备份当前应用，再切换版本并重启服务；健康检查或校验失败时自动回滚应用目录。

## V1.6.3 专项验收

1. 在“预出院管理—统计分析—预出院看板”中，诊断列标题为“次要诊断”，内容与患者预计出院情况中的次要诊断一致。
2. 任选两个预计出院时间不同的患者，较早的日期应始终排在较晚日期之前；翻页后仍遵循同一排序。
3. 打开“特殊患者看板”，仅出现“是否特殊患者=是”的患者；列表不显示预计出院时间，且显示“是否特殊患者：是”和特殊原因。
4. `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.3`，浏览器强制刷新后页面显示 `V1.6.3`。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不包含数据库变更；如需恢复应用版本，按既有发布目录回滚即可。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、前版本、包哈希及验收结果。