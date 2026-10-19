# V1.7.0 一键升级手册

适用版本：`V1.7.0`。对比基准：`V1.6.10`。目标环境已有可用发布版本时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 异常患者可正常保存其他可编辑业务信息；异常原因为可选填写项。
- 预计出院管理和统计分析院后管理列表展示第 7、30、60 天各自的回访时间，并支持按对应回访时间筛选。
- 预约复诊消息接收人改为“用户管理—分配科室”中包含“门诊部”的启用且已配置企微 ID 的用户。
- 每日 08:00 汇总推送前一天新设或修改且当前仍有效的预约复诊；当天 08:00 后设置当天复诊时间时即时推送。重复保存相同时间不重复发送。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.7.0` 对应的干净提交构建。
2. 升级不覆盖环境文件、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本包含 Flyway V34。升级脚本会自动创建数据库备份；应用目录回滚不会回退该迁移。

## 构建与校验

在标签 `V1.7.0` 对应的干净工作区执行：

> **版本展示强制校验（适用于后续每个升级包）**：先将 `server/pom.xml`、`web/package.json` 和本手册中的版本号同步更新为本次发布版本；前端必须通过 `pnpm build` 或 `npm run build` 构建，以注入 `package.json` 中的版本号。一键打包脚本还会校验前后端版本与前端产物内嵌版本；任一不一致即终止，禁止生成升级包。

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.7.0
Get-FileHash outputs\kangfu-V1.7.0.tar.gz -Algorithm SHA256
```

构建成功后，在浏览器或使用接口确认后端 `/api/system/info` 返回 `1.7.0`，并确认前端右上角展示 `V1.7.0`；两者任一不匹配时，不得部署。

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.7.0.tar.gz.sha256
tar -xzf kangfu-V1.7.0.tar.gz
cd /opt/kangfu-V1.7.0
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## 专项验收

1. `/api/system/info` 返回版本 `1.7.0`，后端启动日志显示 Flyway V34 已应用；浏览器强制刷新后页面显示 `V1.7.0`。
2. 在“用户管理—分配科室”为一名启用、已配置企微 ID 的用户分配“门诊部”；未分配门诊部的用户不得成为预约复诊通知接收人。
3. 保存前一天设置或修改的预约复诊时间后，次日 08:00 检查门诊部用户收到汇总消息，内容含脱敏姓名、住院号和当前预约复诊时间。
4. 在当天 08:00 后设置当天复诊时间，确认保存后创建即时推送任务；重复保存同一时间不新增任务。修改为新时间后，按时间规则重新创建即时或次日汇总任务。
5. 确认异常患者可保存门诊、营养、居家康复或随访信息；确认第 7、30、60 天回访时间展示及筛选正确。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。应用目录回滚不回退 Flyway V34；如需数据库恢复，必须经业务负责人批准后使用升级前自动生成的备份。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、部署前版本、包哈希、备份路径及验收结果。
