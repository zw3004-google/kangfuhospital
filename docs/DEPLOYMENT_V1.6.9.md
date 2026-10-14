# V1.6.9 一键升级手册

适用版本：`V1.6.9`。对比基准：`V1.6.8`。目标环境已部署 V1.6.8 时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- HIS“同步患者信息”新增接收并保存患者“联系电话”。
- 预出院管理“预计出院管理—患者预计出院情况”在“患者性别”和“医保类型”之间展示“联系电话”。
- 填报/编辑页“患者基本信息”新增“患者联系方式”。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.9` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本包含 Flyway V32，为 `patient_encounter` 增加可空字段 `patient_phone`。执行升级前由随包脚本自动完成数据库备份；应用目录回滚不会回退该字段。
4. 确认 HIS“同步患者信息”接口已按约定返回“联系电话”字段；不应在部署包、日志或部署记录中输出患者联系电话。

## 构建与校验

在标签 `V1.6.9` 对应的干净工作区执行：

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.9
Get-FileHash outputs\kangfu-V1.6.9.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.9.tar.gz.sha256
tar -xzf kangfu-V1.6.9.tar.gz
cd /opt/kangfu-V1.6.9
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## V1.6.9 专项验收

1. 确认后端启动日志显示 Flyway 已应用 V32，且 `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.9`；浏览器强制刷新后页面显示 `V1.6.9`。
2. 在“预出院管理—预计出院管理—患者预计出院情况”确认“联系电话”位于“患者性别”和“医保类型”之间；移动端同样显示。
3. 打开任一患者填报或编辑页，确认“患者基本信息”显示“患者联系方式”。
4. 手动执行一次“同步患者信息”，确认同步批次成功。重新查询接口返回且具有“联系电话”的患者，核对页面展示值与 HIS 一致。
5. HIS 本次未返回、或返回联系电话为空的历史患者不应被错误写入空值；如需补齐，应在 HIS 返回完整患者范围和电话号码后再次同步。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。应用目录回滚不回退 Flyway V32；若需数据库恢复，必须经业务负责人批准后使用升级前自动生成的备份按随包手册执行恢复。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、前版本、包哈希、备份路径及验收结果；不得记录患者联系方式、密码或 Secret。
