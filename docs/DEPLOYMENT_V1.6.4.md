# V1.6.4 一键升级手册

适用版本：`V1.6.4`。对比基准：`V1.6.3`。目标环境沿用既有 openEuler 离线部署结构；已部署 V1.6.3 的服务器仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 新增“院长”角色和每日早 8 点全院在院患者运营报告企微推送。
- 主管医生填报新增周转患者标识，选择“是”后禁用其余业务填报，不要求填写原因。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.4` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 启动时执行 Flyway V31：为 `discharge_record` 新增默认值为 false 的 `is_turnover_patient` 字段，并创建内置 `PRESIDENT`（院长）角色；不删除或重写既有患者数据。

## 构建与校验

```powershell
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.4
Get-FileHash outputs\kangfu-V1.6.4.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.4.tar.gz.sha256
tar -xzf kangfu-V1.6.4.tar.gz
cd /opt/kangfu-V1.6.4
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## V1.6.4 专项验收

1. 在“系统管理—用户管理”的角色分配窗口可见“院长”角色；为启用用户分配该角色并配置企微 ID。
2. 每日早 8 点检查预出院推送记录：每名院长各有一条 `PRESIDENT_OPERATION_REPORT` 任务，正文包含全院在院患者总数、全部病区人数和 `http://172.16.196.112`。
3. 在主管医生填报中选择“患者是否为周转患者：是”，不要求原因，预计出院、复诊、营养会诊、居家康复及随访区域不可编辑；不得同时保存为特殊患者。
4. `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.4`，浏览器强制刷新后页面显示 `V1.6.4`。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。应用目录回滚不会回退 Flyway V31；如需数据库回退，必须先取得业务负责人批准。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、前版本、包哈希及验收结果。