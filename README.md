# 康复医院运营管理系统

康复医院院内运营管理系统，包含欠费管理、预出院管理和系统管理三个业务模块。

## 工程结构

- `server/`：Java 21 + Spring Boot 模块化单体后端
- `web/`：Vue 3 + TypeScript 管理端
- `deploy/`：本地开发和后续部署配置
- `docs/`：项目文档

## 代码仓库

- GitHub：<https://github.com/zw3004-google/kangfuhospital>
- 主分支：`main`
- SSH 地址：`git@github.com:zw3004-google/kangfuhospital.git`

克隆仓库：

```bash
git clone git@github.com:zw3004-google/kangfuhospital.git
```

仓库不提交本地环境变量、密钥、构建产物、依赖缓存、运行数据及临时备份。实际密码和企业微信密钥必须通过环境变量注入。

## 项目基线文档

- [架构与开发续作说明](docs/ARCHITECTURE_AND_DEVELOPMENT_CONTEXT.md)
- [阶段 0：需求与验收基线](docs/phase0/README.md)
- [阶段 0 待确认事项与签署表](docs/phase0/09-待确认事项与签署表.md)
- [阶段 1：开发与测试环境基线](docs/phase1/README.md)
- [阶段 2：权限与安全闭环](docs/phase2/README.md)
- [阶段 3：业务功能回归](docs/phase3/README.md)
- [阶段 4：前端完整联调](docs/phase4/README.md)
- [阶段 5：开发环境企业微信联调](docs/phase5/README.md)
- [阶段 6：系统测试与整改](docs/phase6/README.md)
- [通报报表原型对齐实施记录](docs/phase6/通报报表原型对齐实施记录.md)
- [Testcontainers 与 Docker Desktop 29 兼容问题处理](docs/TESTCONTAINERS_DOCKER29_TROUBLESHOOTING.md)
- [腾讯云测试环境部署记录](docs/TENCENT_CLOUD_TEST_DEPLOYMENT.md)

## 本地启动

1. 启动 Docker Desktop，并确认当前 context 为 `desktop-linux`。
2. 在 PowerShell 中执行 `. .\scripts\dev-env.ps1` 加载项目自带的 Java 21 和 Maven。
3. 执行 `.\scripts\start-database.ps1` 启动 PostgreSQL。
4. 首次本地启动按[阶段 1 说明](docs/phase1/README.md)启用管理员初始化。
5. 在 `server/` 下执行 `mvn spring-boot:run` 启动后端。
6. 在 `web/` 下执行 `pnpm.cmd dev` 启动 Vite。

默认本地端口：前端 `5173`，后端 `8080`，PostgreSQL `5432`。

> 密码及企业微信密钥必须通过环境变量注入，不得写入代码或提交到仓库。

当前完整基线验证：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\verify-stage6.ps1
```

当前状态：阶段 5 已通过并关闭；阶段 6 原有范围、各页面原型对齐及预计出院管理患者工作台补充整改均已完成。预出院左侧菜单不再独立展示会诊预约和出院随访，两类业务统一进入预计出院患者详情，并按主管医生、门诊部、营养科、居家康复科和随访员执行字段级权限控制。当前 Flyway V1～V21、后端 85 项测试、前端 33 项单元测试、类型检查和生产构建全部通过，26 项容器集成测试 0 跳过。详见 `docs/phase6/README.md`、`docs/phase6/欠费推送记录原型对齐修改计划.md`、`docs/phase6/欠费明细原型对齐修改计划.md` 和 `docs/phase6/通报报表原型对齐实施记录.md`。
