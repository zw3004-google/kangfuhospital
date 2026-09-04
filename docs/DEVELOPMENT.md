# 开发约定

## 架构

后端采用模块化单体。业务模块之间通过服务接口协作，不直接跨模块修改数据。

- `common`：公共返回结构、异常处理和审计基础
- `patient`：患者及住院主数据
- `arrears`：欠费计算、填报和报表
- `discharge`：预计出院、预约、随访和统计
- `messaging`：企业微信任务、重试及发送记录
- `system`：用户、科室、角色、权限和费别系数
- `importing`：导入批次、校验和覆盖更新

## 关键业务约束

- 一次住院以“住院号 + 住院次数”唯一识别。
- 原始应交押金来自导入或接口，最终应交押金在导入时按启用系数计算并固化。
- 重复导入覆盖源数据字段，不覆盖欠费原因、追缴进度、缴费状态等人工字段。
- 导入批次串行执行，失败批次不产生半批数据。
- 主管医生按工号匹配唯一启用用户，保留导入的原始工号、原始姓名和匹配结果。
- 所有时间按 `Asia/Shanghai` 处理，服务器同步医院 NTP。

## 已验证开发基线

- Docker Desktop 4.63.0 / Engine 29.2.1，context 为 `desktop-linux`。
- Testcontainers 统一使用 1.21.4，以兼容 Docker Engine 29 的最低 API 1.44；HTTP 400 故障见 [Testcontainers 与 Docker Desktop 29 兼容问题处理](TESTCONTAINERS_DOCKER29_TROUBLESHOOTING.md)。
- 本地 PostgreSQL 17；Testcontainers PostgreSQL 16。
- Flyway V1～V22 已在空库和 Testcontainers PostgreSQL 16 验证。
- 后端 85 个测试全部通过，Testcontainers 测试无跳过；其中 26 项 PostgreSQL/Testcontainers 集成测试全部实际执行。
- 前端 33 项单元测试、类型检查和生产构建通过。
- 阶段 2 完整复验使用 `scripts/verify-stage2.ps1`。
- 阶段 3 完整复验使用 `scripts/verify-stage3.ps1`。
- 阶段 4 完整复验使用 `scripts/verify-stage4.ps1`。
- 阶段 6 当前基线复验使用 `scripts/verify-stage6.ps1`。

## 当前开发阶段

- 阶段 0：需求与验收基线，已完成。
- 阶段 1：开发与数据库基线，已完成。
- 阶段 2：权限与安全闭环，已完成。
- 阶段 3：业务功能回归，已完成。
- 阶段 4：前端完整联调，已完成。
- 阶段 5：企业微信真实环境联调，已完成；11:00 出院提醒和欠费通告共 6 条任务均自动生成并首次发送成功。
- 阶段 6：原有系统测试、各页面原型对齐及预计出院管理患者工作台补充整改已完成。会诊预约和出院随访从左侧菜单移入预计出院患者详情；门诊跟踪、会诊、居家康复和第 7/30/60 天随访按业务角色实施字段级权限。该阶段既有全量回归基线为 Flyway V1～V21、后端 85 项、前端 33 项且容器测试 0 跳过；后续统计分析专项已把迁移基线推进至 V22，见下一项。
- 统计分析专项批次 1～7 已完成：6 张核心指标卡、4 类累计趋势、6 类分页明细、统一筛选、XLSX/CSV 导出、提醒预览/触发/审计及响应式与竞态保护均已落地。专项回归为前端 8 项、出院模块后端 26 项、PostgreSQL 集成 2 项全部通过；详见 [统计分析页面差异与修改计划](统计分析页面差异与修改计划.md#11-实施结果2026-09-03)。

主管医生工号扩展规则：列表接口直接返回 `patient_encounter.doctor_employee_no` 原始工号；关键词同时匹配主管医生姓名与工号；页面、编辑摘要、Excel 和 CSV 保持一致，空工号显示“—”；主管医生 DataScope 继续使用 `doctor_user_id`，不因展示或搜索工号而扩大权限。

2026-09-01 已按最新测试数据更新随访、营养、居家康复和非计划出院消息模板：患者姓名脱敏、携带住院号、同角色多患者按行合并；企微响应中的 `invaliduser`、`invalidparty`、`invalidtag` 现在会按失败处理。测试接收人聂文斌的企微 ID 已更新为 `niewenbin`，加入应用可见范围后真实发送成功。五名接收人均已确认收到信息。

企微出院提醒定时表达式已支持 `APP_MESSAGING_DISCHARGE_CRON` 环境变量覆盖，默认仍为每天 08:00；阶段5开发环境验收临时设为每天 11:00。

欠费通告由 `ArrearsNotificationScheduler` 调用 `ArrearsNoticeService` 动态生成，默认每天 08:00，通过 `APP_MESSAGING_ARREARS_CRON` 覆盖；时区为 `Asia/Shanghai`。仅为启用且具有有效企微 ID 的科主任和主管医生创建企业微信任务，并按提醒类型、企微 ID、日期和业务类型防止同日重复。阶段 5 的每天 11:00 配置仅为历史联调门禁，不是当前默认配置。

阶段 4 验收记录见 `docs/phase4/README.md`，完整复验使用 `scripts/verify-stage4.ps1`。

阶段 5 完成记录见 `docs/phase5/README.md`，阶段 6 当前记录见 `docs/phase6/README.md`，通报报表实现见 `docs/phase6/通报报表原型对齐实施记录.md`，统计分析批次 1～7 实施记录见 `docs/统计分析页面差异与修改计划.md` 第 11 节。

## 常见测试故障

Windows 命名管道能够连接 Docker Desktop，但 Testcontainers/docker-java 请求返回 HTTP 400 时，不要跳过集成测试。直接按 [Testcontainers 与 Docker Desktop 29 兼容问题处理](TESTCONTAINERS_DOCKER29_TROUBLESHOOTING.md) 检查版本、依赖树和 Docker API，并以 26 项专项集成测试全部执行且 0 跳过作为恢复标准。
