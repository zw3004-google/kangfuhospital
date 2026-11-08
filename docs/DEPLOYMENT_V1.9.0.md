# V1.9.0 一键升级手册

适用版本：`V1.9.0`。对比基准：`V1.8.2`。目标环境已有可用发布版本时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 预计出院时间改为可选项，清空保存后会写入空值。
- 在院欠费接口按完整快照同步：覆盖同一住院号和住院次数的数据，新增接口新增项，删除接口中不再存在的在院欠费项；出院未结算记录在接口返回在院状态后会改回在院欠费。
- 欠费明细支持单条和批量删除；统计卡片、通报报表及通报内容统一统计当前仍欠费且未缴费的在院欠费、出院未结算患者。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.9.0` 对应的干净提交构建。
2. 升级不覆盖环境文件、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本不新增 Flyway 迁移。
4. **执行升级和首次在院欠费同步前必须完成数据库备份。**接口 `BJKF_ZYCX` 必须返回完整、未截断的在院患者快照；若无法确认返回完整性，不得执行同步。

## 构建与校验

在标签 `V1.9.0` 对应的干净工作区执行：

> **版本展示强制校验**：`server/pom.xml`、`web/package.json` 和本手册中的版本号必须一致。前端必须通过 `pnpm build` 构建以注入版本号；一键打包脚本会校验前后端版本及前端产物内嵌版本，任一不一致即终止。

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.9.0
Get-FileHash outputs\kangfu-V1.9.0.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.9.0.tar.gz.sha256
tar -xzf kangfu-V1.9.0.tar.gz
cd /opt/kangfu-V1.9.0
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## 专项验收

1. `/api/system/info` 返回版本 `1.9.0`，浏览器强制刷新后页面显示 `V1.9.0`。
2. 清空一条患者的预计出院时间并保存，重新打开后该字段应为空。
3. 在备份库或测试数据中执行一次完整在院欠费同步，核对覆盖、新增、类型回切和接口缺失删除均符合预期；接口不完整时不得执行该操作。
4. 欠费明细可执行单条及批量删除；欠费患者数、欠费金额合计、通报报表与通报内容的金额范围一致。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不新增数据库迁移，但同步在院欠费删除的数据不能通过应用目录回滚恢复；如需恢复，须使用升级前数据库备份。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、部署前版本、包哈希、备份路径及验收结果。
