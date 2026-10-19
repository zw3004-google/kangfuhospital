# V1.8.0 一键升级手册

适用版本：`V1.8.0`。对比基准：`V1.7.0`。目标环境已有可用发布版本时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 主管医生填报将原字段调整为“预约复诊时间（医疗）”，并新增“康复预约复诊时间”。
- 新增“康复医生填报”模块，可维护康复预约、预约时间、执行人及执行结果。
- 统计分析“业务明细”新增位于营养会诊之前的“康复预约”表，支持按康复预约时间筛选、展示和导出。
- 用户管理新增“康复医师”角色及对应字段权限；新增或编辑用户时，所属科室支持按名称或编码模糊搜索。
- 每天 08:00 向全部启用且已配置企微 ID 的康复医师角色用户推送当日康复预约复诊；当天 08:00 后新增当天康复预约复诊时间时立即推送。重复保存同一时间不重复推送。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.8.0` 对应的干净提交构建。
2. 升级不覆盖环境文件、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本包含 Flyway V35、V36。升级脚本会自动创建数据库备份；应用目录回滚不会回退这些迁移。

## 构建与校验

在标签 `V1.8.0` 对应的干净工作区执行：

> **版本展示强制校验**：先将 `server/pom.xml`、`web/package.json` 和本手册中的版本号同步更新为本次发布版本；前端必须通过 `pnpm build` 或 `npm run build` 构建，以注入 `package.json` 中的版本号。一键打包脚本会校验前后端版本与前端产物内嵌版本；任一不一致即终止，禁止生成升级包。

```powershell
Push-Location web
pnpm install --frozen-lockfile
pnpm build
Pop-Location
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.8.0
Get-FileHash outputs\kangfu-V1.8.0.tar.gz -Algorithm SHA256
```

构建成功后，在浏览器或使用接口确认后端 `/api/system/info` 返回 `1.8.0`，并确认前端右上角展示 `V1.8.0`；两者任一不匹配时，不得部署。

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.8.0.tar.gz.sha256
tar -xzf kangfu-V1.8.0.tar.gz
cd /opt/kangfu-V1.8.0
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## 专项验收

1. `/api/system/info` 返回版本 `1.8.0`，后端启动日志显示 Flyway V35、V36 已应用；浏览器强制刷新后页面显示 `V1.8.0`。
2. 在预计出院管理填报/编辑页，确认“预约复诊时间（医疗）”和“康复预约复诊时间”均可独立保存；进入页面不再出现“会诊类型不正确”。
3. 使用具有康复医师字段权限的账号维护“康复医生填报”模块，确认康复预约、预约时间、执行人和执行结果可正常保存。
4. 在统计分析“业务明细”确认“康复预约”表位于“营养会诊”之前；按康复预约时间筛选、列表展示及导出数据一致。
5. 为一名启用且已配置企微 ID 的用户分配“康复医师”角色。确认当日 08:00 收到当日康复预约复诊汇总；当天 08:00 后设置当天康复预约复诊时间时即时创建推送任务；重复保存相同时间不新增任务，改为新时间时按相同规则重新推送。
6. 新增或编辑用户时，在所属科室输入科室名称或编码的一部分，确认可筛选并选择匹配科室。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。应用目录回滚不回退 Flyway V35、V36；如需数据库恢复，必须经业务负责人批准后使用升级前自动生成的备份。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、部署前版本、包哈希、备份路径及验收结果。
