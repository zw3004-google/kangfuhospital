# 阶段 1：开发与测试环境基线

> 执行日期：2026-08-31  
> 状态：`VERIFIED`——Docker、PostgreSQL、Flyway、管理员初始化、固定测试数据、后端测试和前端构建均已验证。
>
> 本文记录阶段 1 的历史验收快照（Flyway V1～V9、12 个测试）。当前最新基线为阶段 4：Flyway V1～V13、23 个后端测试及前端完整联调，参见 [`../phase4/README.md`](../phase4/README.md)。

## 1. 已完成

- 项目自带 Java 21、Maven 和 pnpm 运行时可用。
- 后端 Maven 测试基线可执行。
- 前端 TypeScript 检查与 Vite 生产构建可执行。
- Testcontainers PostgreSQL 烟雾测试已配置；无 Docker 时明确跳过，不把环境故障误报为代码失败。
- 新增 Spring Boot + PostgreSQL 集成测试，用于验证全部 Flyway 迁移、管理员初始化、角色和固定测试数据。
- 新增用户工号迁移 `V9__user_employee_number.sql`，工号最长 64、必填、全局唯一。
- 主管医生数据范围由错误的企微 ID 改为用户工号。
- 欠费和预出院导入统一按工号匹配唯一启用用户。
- 新增幂等管理员初始化器，默认关闭，仅通过环境变量显式启用。
- 新增跨科室、跨医生的固定集成测试数据。
- 新增一键验证脚本 `scripts/verify-stage1.ps1`。
- Docker Desktop 4.63.0 / Engine 29.2.1 已验证；脚本会读取当前 Docker context，并为 docker-java 设置最低 API 1.44。

## 2. 本地管理员初始化

管理员初始化默认关闭，避免生产环境意外创建默认账号。本地首次启动前设置：

```powershell
$env:APP_BOOTSTRAP_ADMIN_ENABLED='true'
$env:APP_INITIAL_PASSWORD='Kfyy@2026'
$env:APP_BOOTSTRAP_ADMIN_EMPLOYEE_NO='ADMIN'
$env:APP_BOOTSTRAP_ADMIN_WECOM_USER_ID='admin'
```

启动应用后会幂等创建 `admin`、分配 `SYSTEM_ADMIN` 角色并要求首次修改密码。首次成功初始化后建议关闭 `APP_BOOTSTRAP_ADMIN_ENABLED`。生产初始密码必须通过安全渠道配置，不能沿用仓库默认值。

## 3. 固定测试数据

集成测试脚本位于 `server/src/test/resources/sql/stage1-test-data.sql`，包含：

- 两个测试科室；
- 科主任、两名主管医生、运营和财务账号；
- 两个科室各一条患者住院记录；
- 工号匹配后的医生用户关系；
- 科主任的测试科室范围。

数据仅用于 Testcontainers 临时数据库，不应加载到生产库。

## 4. 验证方式

在项目根目录执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\verify-stage1.ps1
```

已验证结果：

- `docker version` 同时显示 Client 和 Server；
- `hello-world` 正常运行；本地 PostgreSQL 17 容器健康；
- Testcontainers PostgreSQL 16 正常创建和回收；
- Flyway V1～V9 全部成功；
- 12 个测试全部通过：0 失败、0 错误、0 跳过；
- 管理员、系统管理员角色、工号和跨科室测试数据断言通过；
- 前端生产构建和类型检查通过。

## 5. Docker Desktop 兼容设置

Docker Desktop 使用 `desktop-linux` context，其真实端点为 `dockerDesktopLinuxEngine`；Testcontainers 不会自动读取 Docker CLI context。Docker Engine 29 的最低 API 是 1.44。验证脚本已自动设置，手工运行 Maven 时使用：

```powershell
$env:DOCKER_HOST='npipe:////./pipe/dockerDesktopLinuxEngine'
mvn '-Dapi.version=1.44' test
```

## 6. 已知非阻塞项

- 阶段 1 时前端主 JavaScript 约 2.1 MB；该历史问题已在阶段 4 通过路由懒加载、依赖分包和 ECharts 按需引入关闭，当前主入口约 12 KB。
- Maven 测试显示 Mockito 动态加载 agent 的未来兼容性警告；当前测试通过，可在升级测试工具链时处理。
- 当前目录无法读取有效 Git 工作树状态，阶段基线尚不能关联提交号；需由项目工作区管理方确认仓库元数据。
