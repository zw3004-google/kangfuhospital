# H5 CSRF 与 Session 认证整改

## 阶段 1：核查与方案

2026-09-05 改造前核查：Spring Boot 3.5.5；已有表单登录 `/api/auth/login`、Session 退出 `/api/auth/logout`；前端 Axios 使用 Cookie。后端仍启用 Basic 且关闭 CSRF，业务集成测试仍使用 Basic。未发现前端长期存储 Basic 凭据。

采用 Session 存储 Token，`GET /api/auth/csrf` 返回框架提供的 Token 和 headerName，不缓存；保持框架的 XOR/BREACH 处理。所有非安全方法包括登录、退出均校验。前端只向本站 API 携带 Token；只有明确 CSRF 拒绝可以刷新并重试一次，网络异常不自动重放。

接口范围：欠费记录编辑、出院记录及会诊增删改、导入、推送重试与提醒触发、用户启停/密码/角色、角色权限及数据范围、科室和费用系数。GET 查询不修改业务数据；导出保留必要访问审计，属于日志副作用，不作为业务写入。

## 阶段 2：CSRF 全链路

启用 Session Token、JSON 错误码、同源写入检查；统一 Axios 获取/刷新/一次重试；登录退出切换清理 Token；覆盖真实 Token 交换及业务拒绝测试。

## 阶段 3：Session 收口

关闭 Basic，迁移现有业务集成测试到真实登录 Session；登录轮换 Session ID；后端每次请求重新核对账号状态、密码指纹和权限，密码变更或禁用使旧 Session 失效，权限变更即时更新。HTTPS Cookie 使用 Secure、HttpOnly 和 SameSite。无测试环境部署。

## 验收

必须运行前端类型检查、测试、构建和后端测试。测试覆盖正确/缺失/错误 Token、登录退出和过期、Basic 拒绝、来源拒绝、权限更新、关键写接口、并发刷新和一次重试。数据库测试被跳过不算通过。真机、目标 HTTPS 代理和角色 UAT 属于阶段 4，部署前完成。

## 实施结果（2026-09-05）

阶段 1、2、3 已完成本地实现和验证，未部署测试环境。保留原有 H5 工作区改动；没有提交、推送、打标签或发布。

- 阶段 1：确认已有 Session 登录；盘点全部写接口及 Basic 调用。仓库中的正常 Basic 调用仅见于既有业务集成测试，已迁移。剩余 Basic 测试仅验证拒绝行为。
- 阶段 2：Session 存储 Token；保留 Spring 默认 XOR/BREACH 处理；新增不可缓存的 Token 接口。POST/PUT/PATCH/DELETE 包括登录退出均受保护，无业务路径豁免。Axios 仅允许本站 /api/ 请求；首次写入前懒加载 Token；登录成功后刷新，退出及 401 清理；并发共享刷新，最多重试一次，账号变化不重放。来源拒绝、权限不足、会话失效和网络结果未知不自动重试。
- 阶段 3：关闭 Basic，统一未认证 JSON 401；保留 Session ID 轮换、退出销毁和 30 分钟服务端超时。每次已登录请求读取最新用户状态和权限：禁用/锁定/删除/密码变更使旧会话失效，角色权限撤销在后续请求生效。数据库读取增加是此策略的成本，院内性能验证属于阶段 4。已经进入业务处理的并发请求不承诺被追溯撤销。
- 主动退出失败会提示错误并保留页面，不再把失败当作成功退出。
- 无数据库迁移。Cookie 保持 HttpOnly、SameSite=Strict，HTTPS 部署必须启用 Secure。

## 接口与配置约定

1. GET /api/auth/csrf：返回 data.headerName、data.token、data.username；未登录 username 为 null，并创建匿名 Session。返回 Cache-Control: no-store。
2. POST /api/auth/login：使用 username/password 表单及步骤 1 的 Token、Session Cookie。登录成功后 Session ID 和 Token 轮换，重新 GET Token。
3. 后续写请求携带 Session Cookie 与返回的 Token 请求头；退出使用受保护的 POST /api/auth/logout。
4. 错误码位于 data.code：AUTH_REQUIRED（401）、CSRF_INVALID（403）、ACCESS_DENIED（403）、ORIGIN_DENIED（403）。匿名业务写请求即使缺 Token 也返回 AUTH_REQUIRED；匿名登录缺 Token 返回 CSRF_INVALID。
5. APP_ALLOWED_ORIGINS：可选，以逗号分隔完整可信来源，例如 https://hospital.example.cn。默认按请求 URL 的协议/主机/端口同源校验。反向代理终止 HTTPS 或改写 Host 时，应显式配置外部可信来源；不能配置通配符。本实现不信任任意转发头，也不开放跨域读取。Origin 缺失时检查 Referer；二者均缺失仍强制 Token，并拒绝明确的 Sec-Fetch-Site: cross-site 写入。
6. SESSION_COOKIE_SECURE=true：正式 HTTPS 必配。当前本地 HTTP 开发保留 false。Vite 代理保留原始 Host，以兼容来源校验。
7. 非浏览器调用也必须完成 Token + Session 登录流程；不再接受 Basic。不能通过关闭 CSRF 恢复旧调用。

## 验证结果

- 后端完整测试通过，随后补充的真实 HTTP Cookie 超时测试通过；最终 Surefire 报告 29 个测试类、107 项测试，失败 0、错误 0、跳过 0。数据库使用本地 Testcontainers PostgreSQL。
- 新增 CSRF 集成测试覆盖 27 个写入口的缺失和错误 Token（54 次拒绝），比较关键业务表与审计表无变化；另覆盖登录 Token 更新、退出、Basic 拒绝、跨站来源、账号禁用、密码变更及权限撤销。
- 真实 HTTP 测试验证 HttpOnly/SameSite Cookie、Token 交换、Session 过期后写入返回 401。测试专用超时接口仅存在于测试配置，不包含在生产代码中。
- 前端类型检查通过；18 个测试文件、59 项测试全部通过，其中请求层安全测试 11 项。首次并行执行有两个既有组件测试超时；以 --maxWorkers=1 重跑全套通过，未放宽断言或超时阈值。
- 前端生产构建通过；git diff --check 通过。

## 阶段 4 进展

本地真实浏览器、移动视口、离线提示、安全响应头、跨站表单、Session 丢失跳转和 100 并发请求回归已通过，详见 [阶段 4 本地验收记录](H5-阶段4本地验收记录.md)。

目标 HTTPS/代理、Secure Cookie、真机与企业微信兼容、院内网络、角色 UAT、灰度和回滚演练仍需执行。本地验收不代表 H5 上线验收完成。
