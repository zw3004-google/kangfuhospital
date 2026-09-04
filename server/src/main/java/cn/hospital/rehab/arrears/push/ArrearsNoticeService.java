package cn.hospital.rehab.arrears.push;

import cn.hospital.rehab.common.security.DataScope;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ArrearsNoticeService {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final String SYSTEM_LINK = "http://oa.kfyy.local/arrears";
    private final JdbcClient jdbc;

    public ArrearsNoticeService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<NoticePreview> preview(DataScope scope, String scopeLabel) {
        var batch = jdbc.sql("""
                SELECT id,batch_no,finished_at
                  FROM import_batch
                 WHERE business_type='ARREARS' AND status='SUCCESS' AND summary_status='READY'
              ORDER BY finished_at DESC NULLS LAST,id DESC LIMIT 1
                """).query((row, number) -> new Batch(row.getLong("id"), row.getString("batch_no"),
                        row.getObject("finished_at", OffsetDateTime.class))).optional();
        if (batch.isEmpty()) return Optional.empty();

        Set<Long> departmentIds = scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds();
        List<DepartmentArrears> ranking = jdbc.sql("""
                SELECT COALESCE(d.department_name,e.ward_name,'未分配') department_name,
                       SUM(a.arrears_amount) total,
                       SUM(a.arrears_amount) FILTER (WHERE a.arrears_type IN ('INPATIENT','在院患者')) inpatient,
                       SUM(a.arrears_amount) FILTER (WHERE a.arrears_type IN ('DISCHARGED_SETTLED','出院已结算')) discharged_settled,
                       SUM(a.arrears_amount) FILTER (WHERE a.arrears_type IN ('DISCHARGED_UNSETTLED','出院未结算')) discharged_unsettled
                  FROM arrears_record a
                  JOIN patient_encounter e ON e.id=a.encounter_id
             LEFT JOIN sys_department d ON d.id=e.department_id
                 WHERE a.import_batch_id=:batchId AND a.in_arrears=true AND a.payment_status='UNPAID'
                   AND (:allDepartments=TRUE OR e.department_id IN (:departmentIds) OR
                        (CAST(:doctorUserId AS BIGINT) IS NOT NULL AND e.doctor_user_id=:doctorUserId))
              GROUP BY COALESCE(d.department_name,e.ward_name,'未分配'),COALESCE(d.department_code,e.ward_name,'')
              ORDER BY total DESC,COALESCE(d.department_code,e.ward_name,'')
                """).param("batchId", batch.get().id())
                .param("allDepartments", scope.allDepartments())
                .param("departmentIds", departmentIds)
                .param("doctorUserId", scope.doctorUserId())
                .query((row, number) -> new DepartmentArrears(row.getString("department_name"),
                        row.getBigDecimal("total"), zero(row.getBigDecimal("inpatient")),
                        zero(row.getBigDecimal("discharged_settled")), zero(row.getBigDecimal("discharged_unsettled")))).list();
        return Optional.of(compose(batch.get().batchNo(), batch.get().dataAsOf(), scopeLabel, ranking));
    }

    static NoticePreview compose(String batchNo, OffsetDateTime dataAsOf, String scopeLabel,
                                 List<DepartmentArrears> ranking) {
        BigDecimal total = ranking.stream().map(DepartmentArrears::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        StringBuilder content = new StringBuilder("截至 ").append(dataAsOf.atZoneSameInstant(SHANGHAI).format(TIME))
                .append("，").append(scopeLabel).append("患者欠费合计 ").append(wan(total))
                .append("万 元，欠费金额科室排名如下")
                .append("（欠费金额 = 在院患者欠费 + 出院已结算患者欠费 + 出院未结算患者欠费）：\n");
        for (int index = 0; index < ranking.size(); index++) {
            DepartmentArrears item = ranking.get(index);
            content.append(index + 1).append(". ").append(item.department()).append("：")
                    .append(wan(item.total())).append("万 元（在院 ").append(wan(item.inpatient()))
                    .append("万 + 出院已结算 ").append(wan(item.dischargedSettled()))
                    .append("万 + 出院未结算 ").append(wan(item.dischargedUnsettled())).append("万）\n");
        }
        content.append("请各位科室主任、主管医生及时关注本科室欠费患者，落实催缴。\n\n")
                .append("详情请点击康复医院运营管理系统查看（院内内网访问）：\n").append(SYSTEM_LINK);
        return new NoticePreview(batchNo, dataAsOf, scopeLabel, total, ranking, SYSTEM_LINK, content.toString());
    }

    static String wan(BigDecimal amount) {
        return zero(amount).divide(BigDecimal.valueOf(10_000), 2, RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }

    private record Batch(long id, String batchNo, OffsetDateTime dataAsOf) {}
    public record DepartmentArrears(String department, BigDecimal total, BigDecimal inpatient,
                                    BigDecimal dischargedSettled, BigDecimal dischargedUnsettled) {}
    public record NoticePreview(String batchNo, OffsetDateTime dataAsOf, String scopeLabel, BigDecimal totalAmount,
                                List<DepartmentArrears> departments, String systemLink, String content) {}
}
