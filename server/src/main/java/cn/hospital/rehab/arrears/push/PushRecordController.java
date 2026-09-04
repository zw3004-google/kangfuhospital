package cn.hospital.rehab.arrears.push;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.api.PageResult;
import cn.hospital.rehab.common.audit.AuditLogService;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/arrears/push-records")
public class PushRecordController {
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100, 200);
    private static final String PUSH_TIME_SQL = "COALESCE(p.sent_at,p.scheduled_at)";
    private static final String LIST_SELECT = """
            SELECT p.id,p.business_type,p.reminder_type,p.recipient_name,p.content,p.status,
                   COALESCE((SELECT pa.trigger_type FROM push_attempt pa
                              WHERE pa.task_id=p.id ORDER BY pa.attempt_no DESC LIMIT 1),
                            p.next_trigger_type) trigger_type,
                   p.scheduled_at,p.sent_at,COALESCE(p.sent_at,p.scheduled_at) push_time,
                   p.retry_count,p.last_error
              FROM push_task p
            """;

    private final JdbcClient jdbc;
    private final AuditLogService audit;
    private final DataScopeService dataScopes;

    public PushRecordController(JdbcClient jdbc, AuditLogService audit, DataScopeService dataScopes) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.dataScopes = dataScopes;
    }

    @PreAuthorize("hasAnyAuthority('PERM_API_PUSH_RECORD_VIEW','ROLE_SYSTEM_ADMIN')")
    @GetMapping
    public ApiResponse<PageResult<PushRecord>> page(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String businessType) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        int size = PAGE_SIZES.contains(pageSize) ? pageSize : 50;
        int currentPage = Math.max(1, page);
        String business = businessType == null ? "" : businessType.trim().toUpperCase(Locale.ROOT);
        List<String> databaseStatuses = PushRecordPolicy.databaseStatuses(status);
        DataScope scope = dataScopes.resolve(auth);
        String where = where(databaseStatuses, startDate, endDate, scope);

        JdbcClient.StatementSpec list = bind(jdbc.sql(LIST_SELECT + where
                        + " ORDER BY " + PUSH_TIME_SQL + " DESC,p.id DESC LIMIT :limit OFFSET :offset"),
                business, databaseStatuses, startDate, endDate, scope)
                .param("limit", size).param("offset", (currentPage - 1) * size);
        List<PushRecord> items = list.query((row, number) -> mapRecord(row)).list();

        JdbcClient.StatementSpec count = bind(jdbc.sql("SELECT COUNT(*) FROM push_task p" + where),
                business, databaseStatuses, startDate, endDate, scope);
        long total = count.query(Long.class).single();
        return ApiResponse.ok(new PageResult<>(items, total, currentPage, size));
    }

    @PreAuthorize("hasAnyAuthority('PERM_API_PUSH_RECORD_VIEW','ROLE_SYSTEM_ADMIN')")
    @GetMapping("/{id}/attempts")
    public ApiResponse<List<Attempt>> attempts(Authentication auth, @PathVariable long id) {
        requireVisibleTask(id, dataScopes.resolve(auth));
        return ApiResponse.ok(jdbc.sql("SELECT * FROM push_attempt WHERE task_id=:id ORDER BY attempt_no")
                .param("id", id)
                .query((row, number) -> new Attempt(row.getInt("attempt_no"), row.getString("trigger_type"),
                        row.getObject("scheduled_at", OffsetDateTime.class),
                        row.getObject("attempted_at", OffsetDateTime.class), row.getString("recipient_name"),
                        row.getString("recipient_wecom_id"), row.getString("status"),
                        row.getString("error_code"), row.getString("error_message"))).list());
    }

    @PreAuthorize("hasAnyAuthority('PERM_API_PUSH_RETRY','ROLE_SYSTEM_ADMIN')")
    @PostMapping("/{id}/retry")
    @Transactional
    public ApiResponse<Void> retry(Authentication auth, HttpServletRequest http, @PathVariable long id) {
        DataScope scope = dataScopes.resolve(auth);
        PushRecord before = visibleTask(id, scope).orElseThrow(() ->
                scope.allDepartments() ? new IllegalArgumentException("推送任务不存在")
                        : new AccessDeniedException("无权重发该推送任务"));
        requireSupportedBusiness(before);
        if (!"FAILED".equals(before.status())) throw new IllegalArgumentException("仅发送失败记录允许重发");
        if (resetFailedTask(id) == 0) throw new IllegalArgumentException("任务状态已变化，请刷新后重试");
        audit.record(auth, "MESSAGING", "PUSH_TASK", String.valueOf(id), "MANUAL_RETRY",
                before, task(id), http.getRemoteAddr());
        return ApiResponse.ok(null);
    }

    @PreAuthorize("hasAnyAuthority('PERM_API_PUSH_RETRY','ROLE_SYSTEM_ADMIN')")
    @PostMapping("/retry-batch")
    @Transactional
    public ApiResponse<RetryBatchResult> retryBatch(Authentication auth, HttpServletRequest http,
                                                     @Valid @RequestBody RetryBatchRequest request) {
        DataScope scope = dataScopes.resolve(auth);
        String requestedBusiness = request.businessType().trim().toUpperCase(Locale.ROOT);
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(request.ids());
        int success = 0;
        int skipped = request.ids().size() - uniqueIds.size();
        int failed = 0;

        for (Long id : uniqueIds) {
            Optional<PushRecord> visible = visibleTask(id, scope);
            if (visible.isEmpty()) {
                failed++;
                continue;
            }
            PushRecord before = visible.get();
            if (!PushRecordPolicy.supportedBusiness(before.businessType()) || !requestedBusiness.equals(before.businessType())) {
                failed++;
                continue;
            }
            if (!"FAILED".equals(before.status())) {
                skipped++;
                continue;
            }
            if (resetFailedTask(id) == 0) {
                skipped++;
                continue;
            }
            audit.record(auth, "MESSAGING", "PUSH_TASK", String.valueOf(id),
                    "BATCH_MANUAL_RETRY", before, task(id), http.getRemoteAddr());
            success++;
        }
        return ApiResponse.ok(new RetryBatchResult(request.ids().size(), success, skipped, failed));
    }

    private int resetFailedTask(long id) {
        return jdbc.sql("""
                UPDATE push_task
                   SET status='PENDING',retry_count=0,last_error=NULL,scheduled_at=CURRENT_TIMESTAMP,
                       next_trigger_type='MANUAL',updated_at=CURRENT_TIMESTAMP
                 WHERE id=:id AND status='FAILED'
                """).param("id", id).update();
    }

    private Optional<PushRecord> visibleTask(long id, DataScope scope) {
        JdbcClient.StatementSpec query = jdbc.sql(LIST_SELECT + " WHERE p.id=:id"
                + (scope.allDepartments() ? "" : scopeWhere())).param("id", id);
        if (!scope.allDepartments()) {
            Set<Long> departmentIds = scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds();
            query = query.param("departmentIds", departmentIds)
                    .param("doctorUserId", scope.doctorUserId() == null ? -1L : scope.doctorUserId());
        }
        return query.query((row, number) -> mapRecord(row)).optional();
    }

    private void requireSupportedBusiness(PushRecord task) {
        if (!PushRecordPolicy.supportedBusiness(task.businessType())) {
            throw new IllegalArgumentException("不支持的推送业务类型");
        }
    }

    private String where(List<String> statuses, LocalDate startDate, LocalDate endDate, DataScope scope) {
        StringBuilder where = new StringBuilder(" WHERE (:business='' OR p.business_type=:business)");
        if (!statuses.isEmpty()) where.append(" AND p.status IN (:statuses)");
        if (startDate != null) {
            where.append(" AND CAST(").append(PUSH_TIME_SQL)
                    .append(" AT TIME ZONE 'Asia/Shanghai' AS DATE)>=:startDate");
        }
        if (endDate != null) {
            where.append(" AND CAST(").append(PUSH_TIME_SQL)
                    .append(" AT TIME ZONE 'Asia/Shanghai' AS DATE)<=:endDate");
        }
        if (!scope.allDepartments()) where.append(scopeWhere());
        return where.toString();
    }

    private JdbcClient.StatementSpec bind(JdbcClient.StatementSpec statement, String business,
                                           List<String> statuses, LocalDate startDate, LocalDate endDate,
                                           DataScope scope) {
        JdbcClient.StatementSpec bound = statement.param("business", business);
        if (!statuses.isEmpty()) bound = bound.param("statuses", statuses);
        if (startDate != null) bound = bound.param("startDate", startDate);
        if (endDate != null) bound = bound.param("endDate", endDate);
        if (!scope.allDepartments()) {
            Set<Long> departmentIds = scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds();
            bound = bound.param("departmentIds", departmentIds)
                    .param("doctorUserId", scope.doctorUserId() == null ? -1L : scope.doctorUserId());
        }
        return bound;
    }

    private String scopeWhere() {
        return """
                 AND p.scope_type<>'LEGACY_UNRESOLVED'
                 AND EXISTS (
                     SELECT 1 FROM push_task_scope pts
                      WHERE pts.task_id=p.id
                        AND ((pts.department_id IS NOT NULL AND pts.department_id IN (:departmentIds))
                             OR (:doctorUserId<>-1 AND pts.doctor_user_id=:doctorUserId))
                 )
                """;
    }

    private void requireVisibleTask(long id, DataScope scope) {
        if (scope.allDepartments()) {
            if (jdbc.sql("SELECT COUNT(*) FROM push_task WHERE id=:id").param("id", id)
                    .query(Long.class).single() == 0) {
                throw new IllegalArgumentException("推送任务不存在");
            }
            return;
        }
        Set<Long> departmentIds = scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds();
        long visible = jdbc.sql("SELECT COUNT(*) FROM push_task p WHERE p.id=:id" + scopeWhere())
                .param("id", id).param("departmentIds", departmentIds)
                .param("doctorUserId", scope.doctorUserId() == null ? -1L : scope.doctorUserId())
                .query(Long.class).single();
        if (visible == 0) throw new AccessDeniedException("无权访问该推送任务");
    }

    private PushRecord task(long id) {
        return jdbc.sql(LIST_SELECT + " WHERE p.id=:id").param("id", id)
                .query((row, number) -> mapRecord(row)).optional()
                .orElseThrow(() -> new IllegalArgumentException("推送任务不存在"));
    }

    private PushRecord mapRecord(java.sql.ResultSet row) throws java.sql.SQLException {
        String status = row.getString("status");
        return new PushRecord(row.getLong("id"), row.getString("business_type"),
                row.getString("reminder_type"), row.getString("recipient_name"),
                PushRecordPolicy.summarize(row.getString("content")), status,
                PushRecordPolicy.displayStatus(status),
                row.getString("trigger_type"), row.getObject("scheduled_at", OffsetDateTime.class),
                row.getObject("sent_at", OffsetDateTime.class),
                row.getObject("push_time", OffsetDateTime.class), row.getInt("retry_count"),
                row.getString("last_error"));
    }

    public record PushRecord(long id, String businessType, String reminderType, String recipientName,
                             String contentSummary, String status, String displayStatus, String triggerType,
                             OffsetDateTime scheduledAt, OffsetDateTime sentAt, OffsetDateTime pushTime,
                             int retryCount, String lastError) {}

    public record Attempt(int attemptNo, String triggerType, OffsetDateTime scheduledAt,
                          OffsetDateTime attemptedAt, String recipientName, String recipientWecomId,
                          String status, String errorCode, String errorMessage) {}

    public record RetryBatchRequest(
            @NotBlank @Pattern(regexp = "(?i)ARREARS|DISCHARGE") String businessType,
            @NotNull @Size(min = 1, max = 200) List<@NotNull @Positive Long> ids) {}

    public record RetryBatchResult(int requestedCount, int successCount, int skippedCount,
                                   int failedCount) {}
}
