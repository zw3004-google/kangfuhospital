# Testcontainers 与 Docker Desktop 29 兼容问题处理

## 快速结论

当 Testcontainers 在 Windows 上通过 Docker Desktop 命名管道连接 Docker Engine 29，并在 `/info` 等请求处返回 HTTP 400 时，先检查 Testcontainers 版本。本项目的永久解决方案是统一升级到 **Testcontainers 1.21.4 或更高兼容版本**；当前版本固定在 `server/pom.xml` 的 `testcontainers.version` 属性中。

不要仅凭 Maven 的 `BUILD SUCCESS` 判定问题已解决。验收必须同时满足：26 项 PostgreSQL/Testcontainers 集成测试全部执行，且 `Failures: 0`、`Errors: 0`、`Skipped: 0`。

## 典型现象

- Docker Desktop 正常运行，`docker version`、`docker ps` 可用。
- Testcontainers 能找到 `npipe:////./pipe/docker_engine`，但 docker-java 探测 `/info` 时收到 HTTP 400。
- 集成测试可能因 Docker 环境不可用而被整体跳过，导致表面显示 `BUILD SUCCESS`。
- 单独设置 `DOCKER_HOST` 或 `DOCKER_API_VERSION=1.44` 仍可能无效，因为旧版 docker-java 的 API 协商发生在 Java 客户端内部。

## 根因

Docker Engine 29 将守护进程支持的最低 API 提高到了 1.44。旧版 Testcontainers/docker-java 可能仍以更低 API 版本发起请求，命名管道本身虽然连通，请求却会被守护进程以 HTTP 400 拒绝。

本项目曾显式使用 Testcontainers 1.20.6，并出现依赖版本不一致。现已把 `junit-jupiter` 与 `postgresql` 两个模块统一绑定到 `testcontainers.version=1.21.4`，避免模块之间漂移。

## 项目内处理步骤

1. 启动 Docker Desktop，确认使用 Linux containers。
2. 在项目根目录加载工具链：

   ```powershell
   . .\scripts\dev-env.ps1
   ```

3. 确认 Docker 版本和最低 API：

   ```powershell
   docker version
   docker context show
   ```

4. 在 `server` 目录运行 26 项专项集成测试：

   ```powershell
   Set-Location .\server
   mvn '-Dtest=ApplicationPostgresIntegrationTest,PostgresTestcontainersSmokeTest' test
   ```

5. 检查结果必须包含：

   ```text
   Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
   BUILD SUCCESS
   ```

6. 再运行完整回归：

   ```powershell
   Set-Location ..
   .\scripts\verify-stage6.ps1
   ```

## 再次发生时的排查顺序

1. 查看日志中的 `Testcontainers version`，必须不低于项目锁定版本。
2. 用 `mvn dependency:tree -Dincludes=org.testcontainers,com.github.docker-java` 检查是否混入旧版本。
3. 用 `docker version` 确认客户端、服务端及服务端最低 API。
4. 确认 Docker Desktop 已启动、context 为 `desktop-linux`，且 `docker ps` 可正常访问。
5. 清理测试报告后重新运行专项测试，防止旧报告或测试跳过造成误判。
6. 只有出现 26 项实际执行且 0 跳过，才算恢复。

## 旧分支临时兜底

若历史分支暂时无法升级 Testcontainers，可在 `server/src/test/resources/docker-java.properties` 中设置：

```properties
api.version=1.44
```

这只是临时兼容措施，应优先升级 Testcontainers。不要把 `DOCKER_API_VERSION` 当作本项目的永久修复，也不要通过禁用或跳过容器测试绕过问题。

## 本次验证记录

- Docker Desktop Engine：29.2.1，Server API 1.53，最低 API 1.44。
- Testcontainers：1.21.4。
- `ApplicationPostgresIntegrationTest`：25 项通过。
- `PostgresTestcontainersSmokeTest`：1 项通过。
- 合计：26 项通过，0 失败、0 错误、0 跳过。
- 完整后端回归：82 项通过，0 失败、0 错误、0 跳过。

## 参考资料

- [Docker Engine API 版本矩阵](https://docs.docker.com/reference/api/engine/)
- [Docker Engine 29 发布说明](https://docs.docker.com/engine/release-notes/29/)
- [Testcontainers Java：Docker 29 API 兼容问题 #11211](https://github.com/testcontainers/testcontainers-java/issues/11211)
