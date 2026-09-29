# V1.6.6 一键升级手册

适用版本：`V1.6.6`。对比基准：`V1.6.5`。目标环境已部署 V1.6.5 时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 同步在院欠费时，出区日期有值的患者自动更新为“出院未结算”。
- 欠费通报每天早上 8 点发送：科主任按科室权限统计全部患者；主管医生仅统计科室权限内、且主管医生为本人的患者。金额以万元展示，通报仅保留负责范围的欠费总额和统一内网链接。
- 修复院长在院患者运营报告：科室信息逐行展示，不再显示字面量 `\n`；消息只保留统一的院内系统访问链接。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.6` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本不包含 Flyway 迁移；不删除、清空或改写既有业务数据。

## 构建与校验

```powershell
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.6
Get-FileHash outputs\kangfu-V1.6.6.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.6.tar.gz.sha256
tar -xzf kangfu-V1.6.6.tar.gz
cd /opt/kangfu-V1.6.6
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## V1.6.6 专项验收

1. 在欠费管理执行“同步在院欠费”：选择一条 HIS 返回出区日期的患者，确认欠费明细类型显示为“出院未结算”；无出区日期患者仍显示“在院患者”。
2. 为具备科室权限的科主任和主管医生分别生成或等待早 8 点欠费通报：科主任总额应涵盖授权科室全部未缴欠费患者；主管医生总额仅包含授权科室内本人负责的患者；金额单位均为万元，正文不含全院科室排行。
3. 核对院长运营报告：每个病区独占一行，消息中不出现字面量 `\n`，且底部仅出现一次“详情请点击康复医院运营管理系统查看（院内内网访问）”及 `http://172.16.196.112`。
4. `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.6`；浏览器强制刷新后页面显示 `V1.6.6`。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不包含数据库迁移；应用目录回滚不涉及数据库回退。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、前版本、包哈希及验收结果。