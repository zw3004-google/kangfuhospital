# V1.6.7 一键升级手册

适用版本：`V1.6.7`。对比基准：`V1.6.6`。目标环境已部署 V1.6.6 时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 欠费报表 Excel 导入支持“追缴进度”列：该列有值时覆盖追缴进度；为空时保留系统原值。导入同时以源文件覆盖欠费类型，并将导入操作人写入对应欠费记录的操作历史。
- 同步在院欠费不再因“出区日期”有值自动变更欠费类型；接口数据仍按“在院患者”处理。
- 欠费明细移除暂未使用的“欠费状态”筛选与缴费状态快捷操作；操作历史不再展示缴费状态，仅展示欠费类型、情况说明和追缴进度。
- 欠费类型筛选固定提供“在院欠费、出院未结算、出院已结算”三项，桌面端和移动端一致。
- 院长在院患者运营报告的截止时间统一展示为完整日期时间，例如“2026年9月30日上午08:00”。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.7` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本不包含 Flyway 迁移；不删除、清空或改写既有业务数据。

## 构建与校验

在标签 `V1.6.7` 对应的干净工作区执行：

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.7
Get-FileHash outputs\kangfu-V1.6.7.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.7.tar.gz.sha256
tar -xzf kangfu-V1.6.7.tar.gz
cd /opt/kangfu-V1.6.7
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## V1.6.7 专项验收

1. 在欠费管理—欠费明细打开“全部欠费类型”，确认始终显示“在院欠费、出院未结算、出院已结算”三项；确认不再显示“欠费状态”筛选或“标记缴费/恢复未缴费”快捷按钮。
2. 导入包含“欠费类型”和“追缴进度”的欠费报表：确认两字段按源文件更新；“追缴进度”单元格为空时，确认系统原追缴进度不被覆盖。查看操作历史，确认显示导入操作人及欠费类型/追缴进度变更，不显示缴费状态。
3. 执行“同步在院欠费”，选择 HIS 返回出区日期的患者，确认其欠费类型仍为“在院患者”。
4. 生成或等待院长早 8 点运营报告，确认正文以“截止到2026年9月30日上午08:00”格式展示完整日期和时间。
5. `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.7`；浏览器强制刷新后页面显示 `V1.6.7`。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不包含数据库迁移；应用目录回滚不涉及数据库回退。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、前版本、包哈希及验收结果。
