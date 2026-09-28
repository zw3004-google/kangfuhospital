# V1.6.5 一键升级手册

适用版本：`V1.6.5`。对比基准：`V1.6.4`。目标环境已部署 V1.6.4 时，仅执行随包 `upgrade.sh`，不得执行 `install.sh`。

## 本版本内容

- 特殊患者仅可维护预计出院时间和特殊原因；周转患者仅可维护预计出院时间。两类患者均可在已有实际出院时间后维护预计出院时间。
- 预出院看板不展示、也不计入周转患者。
- 日期范围筛选清空后可正常查询预出院看板以及欠费、预出院推送记录。

## 发布前置条件

1. 本次代码、发布说明及本手册必须已提交并推送；升级包必须从同名 Git 标签 `V1.6.5` 对应的干净提交构建。
2. 升级不覆盖 `/etc/kangfu/kangfu.env`、数据库密码、企业微信 Secret、HIS 参数、JRE 或 Nginx 配置。
3. 本版本不包含 Flyway 迁移；不删除、清空或改写既有业务数据。

## 构建与校验

```powershell
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
D:\projects\kangfuhospital\.tools\maven\apache-maven-3.9.16\bin\mvn.cmd -f server\pom.xml clean verify
powershell -ExecutionPolicy Bypass -File scripts\build-test-package.ps1 -Version V1.6.5
Get-FileHash outputs\kangfu-V1.6.5.tar.gz -Algorithm SHA256
```

## 服务器一键升级

```bash
cd /opt
sha256sum -c kangfu-V1.6.5.tar.gz.sha256
tar -xzf kangfu-V1.6.5.tar.gz
cd /opt/kangfu-V1.6.5
sha256sum -c checksums/SHA256SUMS
bash scripts/preflight.sh
bash scripts/upgrade.sh
bash scripts/verify.sh
```

## V1.6.5 专项验收

1. 在预出院管理的主管医生填报中验证：特殊患者为“是”时，仅预计出院时间和特殊原因可编辑；周转患者为“是”时，仅预计出院时间可编辑。
2. 对已有实际出院时间的特殊患者及周转患者，保存修改后的预计出院时间，页面提示成功且刷新后值保持一致。
3. 在统计分析—预出院看板中确认周转患者不展示、不计入分页总数；非周转且已填报预计出院时间的患者仍正常展示。
4. 在预出院看板及欠费、预出院推送记录中先选择日期范围、清空开始和结束日期、再点击查询，页面不报错且按未设置日期条件返回数据。
5. `curl -fsS http://127.0.0.1:8080/api/system/info` 返回版本 `1.6.5`；浏览器强制刷新后页面显示 `V1.6.5`。

## 回滚与记录

升级异常时执行 `bash scripts/rollback.sh` 和 `bash scripts/verify.sh`。本版本不包含数据库迁移；应用目录回滚不涉及数据库回退。部署完成后按规范记录目标环境、部署时间、版本号、Git 提交、前版本、包哈希及验收结果。