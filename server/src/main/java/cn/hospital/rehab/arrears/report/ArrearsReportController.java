package cn.hospital.rehab.arrears.report;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import cn.hospital.rehab.arrears.push.ArrearsNoticeService;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/arrears/report")
public class ArrearsReportController {
    private final JdbcClient jdbc;
    private final DataScopeService scopes;
    private final ArrearsNoticeService notices;
    private final ArrearsReportExportService exporter;
    private final AuditLogService audit;

    public ArrearsReportController(JdbcClient jdbc, DataScopeService scopes, ArrearsNoticeService notices,
                                   ArrearsReportExportService exporter, AuditLogService audit) {
        this.jdbc = jdbc;
        this.scopes = scopes;
        this.notices = notices;
        this.exporter = exporter;
        this.audit = audit;
    }

    @GetMapping("/notice-preview")
    ApiResponse<ArrearsNoticeService.NoticePreview> noticePreview(Authentication auth) {
        DataScope scope = scopes.resolve(auth);
        return ApiResponse.ok(notices.preview(scope, scopeInfo(scope).label()).orElse(null));
    }

    @GetMapping("/export")
    ResponseEntity<byte[]> export(Authentication auth, HttpServletRequest request) {
        Report current = report(auth).data();
        var file = exporter.create(current);
        audit.record(auth, "ARREARS", "ARREARS_REPORT_EXPORT",
                current.latestSuccessfulBatch().batchNo(), "EXPORT_XLSX", null,
                java.util.Map.of("batchNo", current.latestSuccessfulBatch().batchNo(),
                        "scopeType", current.scopeType(), "departments", current.ranking().size(),
                        "patients", current.patientTop10().size()), request.getRemoteAddr());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, file.contentDisposition())
                .contentType(file.mediaType()).body(file.content());
    }

    @GetMapping
    ApiResponse<Report> report(Authentication auth) {
        DataScope scope = scopes.resolve(auth);
        ScopeInfo scopeInfo = scopeInfo(scope);
        SelectedBatch selectedBatch = latestReadyBatch();
        if (selectedBatch == null) {
            return ApiResponse.ok(new Report(BigDecimal.ZERO, 0, scopeInfo.type(), scopeInfo.label(),
                    List.of(), List.of(), List.of(), null));
        }

        Set<Long> departmentIds = scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds();
        var ranking = jdbc.sql("""
                SELECT COALESCE(d.department_name,e.ward_name,'未分配') department_name,
                       SUM(a.arrears_amount) amount, COUNT(*) people
                  FROM arrears_record a
                  JOIN patient_encounter e ON e.id=a.encounter_id
             LEFT JOIN sys_department d ON d.id=e.department_id
                 WHERE a.import_batch_id=:batchId
                   AND a.in_arrears=true AND a.payment_status='UNPAID'
                   AND (:allDepartments=TRUE OR e.department_id IN (:departmentIds) OR
                        (CAST(:doctorUserId AS BIGINT) IS NOT NULL AND e.doctor_user_id=:doctorUserId))
              GROUP BY COALESCE(d.department_name,e.ward_name,'未分配'),
                       COALESCE(d.department_code,e.ward_name,'')
              ORDER BY amount DESC,COALESCE(d.department_code,e.ward_name,'')
                """).param("batchId", selectedBatch.id())
                .param("allDepartments", scope.allDepartments())
                .param("departmentIds", departmentIds)
                .param("doctorUserId", scope.doctorUserId())
                .query((r, row) -> new DepartmentStat(
                        r.getString("department_name"), r.getBigDecimal("amount"), r.getLong("people"))).list();

        var patientTop10 = jdbc.sql("""
                SELECT ROW_NUMBER() OVER (
                           ORDER BY a.arrears_amount DESC,
                                    COALESCE(d.department_code,e.ward_name,''),
                                    e.inpatient_no,e.admission_times
                       ) rank,
                       e.inpatient_no,e.admission_times,e.patient_name,
                       COALESCE(d.department_name,e.ward_name,'未分配') department_name,
                       COALESCE(u.display_name,e.doctor_name_source,'未匹配') doctor_name,
                       a.arrears_type,a.arrears_amount,a.recovery_progress
                  FROM arrears_record a
                  JOIN patient_encounter e ON e.id=a.encounter_id
             LEFT JOIN sys_department d ON d.id=e.department_id
             LEFT JOIN sys_user u ON u.id=e.doctor_user_id
                 WHERE a.import_batch_id=:batchId
                   AND a.in_arrears=true AND a.payment_status='UNPAID'
                   AND (:allDepartments=TRUE OR e.department_id IN (:departmentIds) OR
                        (CAST(:doctorUserId AS BIGINT) IS NOT NULL AND e.doctor_user_id=:doctorUserId))
              ORDER BY a.arrears_amount DESC,
                       COALESCE(d.department_code,e.ward_name,''),
                       e.inpatient_no,e.admission_times
                 LIMIT 10
                """).param("batchId", selectedBatch.id())
                .param("allDepartments", scope.allDepartments())
                .param("departmentIds", departmentIds)
                .param("doctorUserId", scope.doctorUserId())
                .query((r, row) -> new PatientStat(
                        r.getLong("rank"), r.getString("inpatient_no"), r.getInt("admission_times"),
                        r.getString("patient_name"), r.getString("department_name"), r.getString("doctor_name"),
                        r.getString("arrears_type"), r.getBigDecimal("arrears_amount"),
                        r.getString("recovery_progress"))).list();

        BigDecimal total = ranking.stream().map(DepartmentStat::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long people = ranking.stream().mapToLong(DepartmentStat::people).sum();
        BatchSnapshot batch = new BatchSnapshot(selectedBatch.batchNo(), selectedBatch.dataAsOf(), selectedBatch.summaryStatus());
        return ApiResponse.ok(new Report(total, people, scopeInfo.type(), scopeInfo.label(),
                ranking.stream().limit(3).toList(), ranking, patientTop10, batch));
    }

    private SelectedBatch latestReadyBatch() {
        return jdbc.sql("""
                SELECT id,batch_no,finished_at,summary_status
                  FROM import_batch
                 WHERE business_type='ARREARS' AND status='SUCCESS' AND summary_status='READY'
              ORDER BY finished_at DESC NULLS LAST,id DESC
                 LIMIT 1
                """).query((r, row) -> new SelectedBatch(r.getLong("id"), r.getString("batch_no"),
                        r.getObject("finished_at", OffsetDateTime.class), r.getString("summary_status")))
                .optional().orElse(null);
    }

    private static ScopeInfo scopeInfo(DataScope scope) {
        if (scope.allDepartments()) return new ScopeInfo("ALL", "全院");
        if (!scope.departmentIds().isEmpty() && scope.doctorUserId() == null) return new ScopeInfo("DEPARTMENT", "本科室");
        if (scope.departmentIds().isEmpty() && scope.doctorUserId() != null) return new ScopeInfo("DOCTOR", "本人负责患者");
        return new ScopeInfo("RESTRICTED", "授权范围");
    }

    public record DepartmentStat(String departmentName, BigDecimal amount, long people) {}
    public record PatientStat(long rank, String inpatientNo, int admissionTimes, String patientName,
                              String departmentName, String doctorName, String arrearsType,
                              BigDecimal arrearsAmount, String recoveryProgress) {}
    public record BatchSnapshot(String batchNo, OffsetDateTime dataAsOf, String summaryStatus) {}
    public record Report(BigDecimal totalAmount, long people, String scopeType, String scopeLabel,
                         List<DepartmentStat> top3, List<DepartmentStat> ranking,
                         List<PatientStat> patientTop10, BatchSnapshot latestSuccessfulBatch) {}
    private record SelectedBatch(long id, String batchNo, OffsetDateTime dataAsOf, String summaryStatus) {}
    private record ScopeInfo(String type, String label) {}
}
