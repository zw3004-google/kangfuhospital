# V1.6.8 一键升级手册

适用版本：`V1.6.8`。对比基准：`V1.6.7`。目标环境已部署 V1.6.7 时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 修复预出院管理“统计分析—业务明细—复诊预约”未显示已设置预约复诊时间的问题。
- 统计分析页面桌面端和移动端均改为显示接口返回的最近预约复诊时间。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.8` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本不包含 Flyway 迁移；不删除、清空或改写既有业务数据。

## 构建与校验

在标签 `V1.6.8` 对应的干净工作区执行：

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.8
Get-FileHash outputs\kangfu-V1.6.8.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.8.tar.gz.sha256
tar -xzf kangfu-V1.6.8.tar.gz
cd /opt/kangfu-V1.6.8
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## V1.6.8 专项验收

1. 使用主管医生账号在预出院管理中为患者设置“预约复诊时间”并保存。
2. 打开“统计分析—业务明细—复诊预约”，确认该患者“复诊预约时间”显示为已设置的日期和时间，而不是“—”。
3. 使用移动端宽度打开同一页签，确认“复诊预约”字段同样显示该预约时间。
4. `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.8`；浏览器强制刷新后页面显示 `V1.6.8`。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不包含数据库迁移；应用目录回滚不涉及数据库回退。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、前版本、包哈希及验收结果。
