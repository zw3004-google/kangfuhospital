package cn.hospital.rehab.arrears.push;

import cn.hospital.rehab.common.security.DataScope;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ArrearsNoticeService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter NOTICE_TIME = DateTimeFormatter.ofPattern("yyyy年MM月dd日 08:00");
    public static final String SYSTEM_LINK = PushContentFormatter.SYSTEM_LINK;
    private final JdbcClient jdbc;

    public ArrearsNoticeService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<NoticePreview> preview(DataScope scope, String scopeLabel) {
        return preview(scope, scopeLabel, scope.doctorUserId() != null, OffsetDateTime.now(SHANGHAI));
    }

    public Optional<NoticePreview> preview(DataScope scope, String scopeLabel, boolean doctorOnly,
                                           OffsetDateTime cutoff) {
        List<DepartmentArrears> recipientDepartments = ranking(scope, doctorOnly);
        if (recipientDepartments.isEmpty()) return Optional.empty();
        return Optional.of(compose(null, cutoff, scopeLabel, recipientDepartments));
    }

    private List<DepartmentArrears> ranking(DataScope scope, boolean doctorOnly) {
        Set<Long> departmentIds = scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds();
        return jdbc.sql("""
                SELECT COALESCE(d.department_name,e.ward_name,'未分配') department_name,
                       SUM(ABS(a.arrears_amount)) total,COUNT(*) people,
                       SUM(ABS(a.arrears_amount)) FILTER (WHERE a.arrears_type IN ('INPATIENT','在院患者')) inpatient,
                       SUM(ABS(a.arrears_amount)) FILTER (WHERE a.arrears_type IN ('DISCHARGED_SETTLED','出院已结算')) discharged_settled,
                       SUM(ABS(a.arrears_amount)) FILTER (WHERE a.arrears_type IN ('DISCHARGED_UNSETTLED','出院未结算')) discharged_unsettled
                  FROM arrears_record a
                  JOIN patient_encounter e ON e.id=a.encounter_id
             LEFT JOIN sys_department d ON d.id=e.department_id
                 WHERE a.in_arrears=true AND a.payment_status='UNPAID'
                   AND (:allDepartments=TRUE OR
                        (:doctorOnly=TRUE AND e.department_id IN (:departmentIds) AND e.doctor_user_id=:doctorUserId) OR
                        (:doctorOnly=FALSE AND e.department_id IN (:departmentIds)))
              GROUP BY COALESCE(d.department_name,e.ward_name,'未分配'),COALESCE(d.department_code,e.ward_name,'')
              ORDER BY total DESC,COALESCE(d.department_code,e.ward_name,'')
                """).param("allDepartments", scope.allDepartments())
                .param("doctorOnly", doctorOnly).param("departmentIds", departmentIds)
                .param("doctorUserId", scope.doctorUserId())
                .query((row, number) -> new DepartmentArrears(row.getString("department_name"),
                        zero(row.getBigDecimal("total")), row.getLong("people"),
                        zero(row.getBigDecimal("inpatient")), zero(row.getBigDecimal("discharged_settled")),
                        zero(row.getBigDecimal("discharged_unsettled")))).list();
    }

    static NoticePreview compose(String batchNo, OffsetDateTime cutoff, String scopeLabel,
                                 List<DepartmentArrears> recipientDepartments) {
        BigDecimal total = recipientDepartments.stream().map(DepartmentArrears::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String content = "截止到" + cutoff.atZoneSameInstant(SHANGHAI).format(NOTICE_TIME) + "，"
                + scopeLabel + "总计欠费" + wan(total) + "万元。";
        return new NoticePreview(batchNo, cutoff, scopeLabel, total, recipientDepartments, SYSTEM_LINK,
                PushContentFormatter.withSystemLink(content));
    }

    static String wan(BigDecimal amount) { return zero(amount).divide(BigDecimal.valueOf(10_000), 2, java.math.RoundingMode.HALF_UP).toPlainString(); }
    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    public record DepartmentArrears(String department, BigDecimal total, long people, BigDecimal inpatient,
                                    BigDecimal dischargedSettled, BigDecimal dischargedUnsettled) {
        public DepartmentArrears(String department, BigDecimal total, BigDecimal inpatient,
                                 BigDecimal dischargedSettled, BigDecimal dischargedUnsettled) {
            this(department, total, 0, inpatient, dischargedSettled, dischargedUnsettled);
        }
    }
    public record NoticePreview(String batchNo, OffsetDateTime dataAsOf, String scopeLabel, BigDecimal totalAmount,
                                List<DepartmentArrears> departments, String systemLink, String content) {}
}