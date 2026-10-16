# V1.6.10 一键升级手册

适用版本：`V1.6.10`。对比基准：`V1.6.9`。目标环境已有可用发布版本时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 异常患者可正常填报和编辑其他业务信息，不再强制要求先填写异常原因。
- 预计出院管理和统计分析院后管理列表展示第7、30、60天各自的回访时间。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.10` 对应的干净提交构建。
2. 升级不覆盖环境文件、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本包含 Flyway V33。升级脚本会自动创建数据库备份；应用目录回滚不会回退该迁移。

## 构建与校验

在标签 `V1.6.10` 对应的干净工作区执行：

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.10
Get-FileHash outputs\kangfu-V1.6.10.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.10.tar.gz.sha256
tar -xzf kangfu-V1.6.10.tar.gz
cd /opt/kangfu-V1.6.10
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## 专项验收

1. `/api/system/info` 返回版本 `1.6.10`，后端启动日志显示 Flyway V33 已应用；浏览器强制刷新后页面显示 `V1.6.10`。
2. 打开一名异常患者，分别编辑可授权的门诊、营养、居家康复或随访信息并保存，确认保存成功。
3. 在“预计出院管理—患者预计出院情况”确认显示出院7天、30天、60天回访时间，且与随访员填报的对应回访时间一致。
4. 在“统计分析—业务明细—院后管理列表”确认每个第7、30、60天随访结果前均显示对应回访时间。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。应用目录回滚不回退 Flyway V33；如需数据库恢复，必须经业务负责人批准后使用升级前自动生成的备份。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、部署前版本、包哈希、备份路径及验收结果。
