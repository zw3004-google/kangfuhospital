# V1.8.2 一键升级手册

适用版本：`V1.8.2`。对比基准：`V1.8.1`。目标环境已有可用发布版本时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 欠费管理—欠费明细的欠费类型筛选支持多选，默认选中在院欠费和出院未结算。
- 列表、统计与导出使用相同的多选筛选范围；已选条件直接显示名称。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.8.2` 对应的干净提交构建。
2. 升级不覆盖环境文件、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本不新增 Flyway 迁移；目标库应已完成 V1.8.0 所需的 V35、V36。

## 构建与校验

在标签 `V1.8.2` 对应的干净工作区执行：

> **版本展示强制校验**：`server/pom.xml`、`web/package.json` 和本手册中的版本号必须一致。前端必须通过 `pnpm build` 构建以注入版本号；一键打包脚本会校验前后端版本及前端产物内嵌版本，任一不一致即终止。

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.8.2
Get-FileHash outputs\kangfu-V1.8.2.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.8.2.tar.gz.sha256
tar -xzf kangfu-V1.8.2.tar.gz
cd /opt/kangfu-V1.8.2
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## 专项验收

1. `/api/system/info` 返回版本 `1.8.2`，浏览器强制刷新后页面显示 `V1.8.2`。
2. 打开“欠费管理—欠费明细”，确认筛选框默认显示“在院欠费”和“出院未结算”，不显示“+1”。
3. 选择或取消任意欠费类型后，确认列表、统计卡片与导出结果均使用相同筛选范围；清空欠费类型后应可查看全部类型。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不新增数据库迁移；回滚仅回退应用和前端静态资源。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、部署前版本、包哈希、备份路径及验收结果。
