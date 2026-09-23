# V1.6.2 升级执行单

适用版本：`V1.6.2`。对比基准：`V1.6.1`。目标环境沿用既有 openEuler 离线部署结构；已有环境仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 发布前置条件

1. 本次代码、发布说明及本执行单已提交并推送，升级包必须由同名 Git 标签 `V1.6.2` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 启动时执行 Flyway V30，向 `patient_encounter` 增加可空的 `secondary_diagnosis` 字段；不回填或删除既有数据。

## 构建与校验

```powershell
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.2
Get-FileHash outputs\kangfu-V1.6.2.tar.gz -Algorithm SHA256
```

前端必须使用 `pnpm --dir web build`，并在打包前确认页面版本为 `V1.6.2`，不得显示 `Vdev`。

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.2.tar.gz.sha256
tar -xzf kangfu-V1.6.2.tar.gz
cd /opt/kangfu-V1.6.2
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## V1.6.2 专项验收

1. 点击“同步患者信息”后，HIS 返回的次要诊断写入患者记录；再次同步且该字段为空时，既有次要诊断保持不变。
2. 预计出院管理列表中“次要诊断”紧随“主诊断”显示；移动端和患者详情一致。
3. 导出预出院数据时，表头包含“主诊断、次要诊断”。
4. `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.2`，浏览器强制刷新后显示 `V1.6.2`。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 与 `bash scripts/verify.sh`。应用目录回滚不会回退 Flyway V30；数据库恢复必须经业务负责人批准。部署完成后按规范记录版本、提交、前版本、包哈希、备份路径和验收结果。