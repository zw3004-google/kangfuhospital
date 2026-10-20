# V1.8.1 一键升级手册

适用版本：`V1.8.1`。对比基准：`V1.8.0`。目标环境已有可用发布版本时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 修复主管医生仅保存预计出院时间时，未提交随访明细导致保存失败的问题。
- 主管医生填报字段“康复预约复诊时间”更名为“预约复诊时间（康复）”；数据字段、既有数据和企微推送逻辑不变。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.8.1` 对应的干净提交构建。
2. 升级不覆盖环境文件、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本不新增 Flyway 迁移；目标库应已完成 V1.8.0 所需的 V35、V36。

## 构建与校验

在标签 `V1.8.1` 对应的干净工作区执行：

> **版本展示强制校验**：先将 `server/pom.xml`、`web/package.json` 和本手册中的版本号同步更新为本次发布版本；前端必须通过 `pnpm build` 或 `npm run build` 构建，以注入 `package.json` 中的版本号。一键打包脚本会校验前后端版本与前端产物内嵌版本；任一不一致即终止，禁止生成升级包。

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.8.1
Get-FileHash outputs\kangfu-V1.8.1.tar.gz -Algorithm SHA256
```

构建成功后，在浏览器或使用接口确认后端 `/api/system/info` 返回 `1.8.1`，并确认前端右上角展示 `V1.8.1`；两者任一不匹配时，不得部署。

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.8.1.tar.gz.sha256
tar -xzf kangfu-V1.8.1.tar.gz
cd /opt/kangfu-V1.8.1
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## 专项验收

1. `/api/system/info` 返回版本 `1.8.1`，浏览器强制刷新后页面显示 `V1.8.1`。
2. 创建或使用仅具备“主管医生字段编辑”权限、未具备“随访员字段编辑”权限且已分配患者所属科室的账号；仅填写或修改预计出院时间后保存，应成功且不再提示“系统处理失败”。
3. 打开主管医生填报区域，确认字段显示为“预约复诊时间（康复）”；保存、查看历史记录及康复预约企微推送行为保持正常。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不新增数据库迁移；回滚仅回退应用和前端静态资源。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、部署前版本、包哈希、备份路径及验收结果。
