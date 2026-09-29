package cn.hospital.rehab.arrears.push;

import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Component
public class ArrearsNotificationScheduler {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final JdbcClient jdbc;
    private final ArrearsNoticeService notices;
    private final DataScopeService dataScopes;

    public ArrearsNotificationScheduler(JdbcClient jdbc, ArrearsNoticeService notices, DataScopeService dataScopes) {
        this.jdbc = jdbc;
        this.notices = notices;
        this.dataScopes = dataScopes;
    }

    @Scheduled(cron = "${app.messaging.arrears-cron:0 0 8 * * *}", zone = "Asia/Shanghai")
    @Transactional
    public void createArrearsNotice() {
        OffsetDateTime cutoff = LocalDate.now(SHANGHAI).atTime(8, 0).atZone(SHANGHAI).toOffsetDateTime();

        List<Recipient> recipients = jdbc.sql("""
                SELECT u.id,u.wecom_user_id,u.display_name,
                       BOOL_OR(r.role_code='DEPARTMENT_DIRECTOR') department_director
                  FROM sys_user u
                  JOIN sys_user_role ur ON ur.user_id=u.id
                  JOIN sys_role r ON r.id=ur.role_id
                 WHERE u.enabled=true AND r.enabled=true
                   AND u.wecom_user_id IS NOT NULL AND BTRIM(u.wecom_user_id)<>''
                   AND r.role_code IN ('DEPARTMENT_DIRECTOR','ATTENDING_DOCTOR')
              GROUP BY u.id,u.wecom_user_id,u.display_name
              ORDER BY u.id
                """).query((row, number) -> new Recipient(row.getLong("id"),
                        row.getString("wecom_user_id"), row.getString("display_name"),
                        row.getBoolean("department_director"))).list();

        for (Recipient recipient : recipients) {
            DataScope scope = dataScopes.resolveForUser(recipient.id());
            boolean doctorOnly = !recipient.departmentDirector();
            String scopeType = scopeType(scope, doctorOnly);
            if (scopeType == null) continue;

            var preview = notices.preview(scope, scopeLabel(scope, recipient, doctorOnly), doctorOnly, cutoff).orElse(null);
            if (preview == null) continue;

            var taskId = jdbc.sql("""
                    INSERT INTO push_task(
                        business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,
                        recipient_user_id,scope_type,content,status,scheduled_at)
                    VALUES ('ARREARS','ARREARS_NOTICE',CURRENT_DATE,:wecomId,:displayName,
                            :userId,:scopeType,:content,'PENDING',CURRENT_TIMESTAMP)
                    ON CONFLICT (reminder_type,recipient_wecom_id,reminder_date,business_type) DO NOTHING
                    RETURNING id
                    """).param("wecomId", recipient.wecomUserId())
                    .param("displayName", recipient.displayName())
                    .param("userId", recipient.id())
                    .param("scopeType", scopeType)
                    .param("content", preview.content())
                    .query(Long.class).optional();
            if (taskId.isEmpty()) continue;

            for (Long departmentId : scope.departmentIds()) {
                jdbc.sql("""
                        INSERT INTO push_task_scope(task_id,department_id)
                        VALUES (:taskId,:departmentId)
                        ON CONFLICT DO NOTHING
                        """).param("taskId", taskId.get()).param("departmentId", departmentId).update();
            }
            if (doctorOnly && scope.doctorUserId() != null) {
                jdbc.sql("""
                        INSERT INTO push_task_scope(task_id,doctor_user_id)
                        VALUES (:taskId,:doctorUserId)
                        ON CONFLICT DO NOTHING
                        """).param("taskId", taskId.get()).param("doctorUserId", scope.doctorUserId()).update();
            }
        }
    }

    private String scopeType(DataScope scope, boolean doctorOnly) {
        if (scope.allDepartments()) return doctorOnly ? "DOCTOR" : "ALL";
        if (doctorOnly) return !scope.departmentIds().isEmpty() && scope.doctorUserId() != null ? "MIXED" : null;
        return scope.departmentIds().isEmpty() ? null : "DEPARTMENT";
    }

    private String scopeLabel(DataScope scope, Recipient recipient, boolean doctorOnly) {
        List<String> departmentNames = departmentNames(scope.departmentIds());
        String departments = scope.allDepartments() ? "全院" : (departmentNames.isEmpty() ? "授权" : String.join("、", departmentNames));
        return doctorOnly ? departments + "内，主管医生" + recipient.displayName() + "负责的患者"
                : departments + "科室负责的患者";
    }

    private List<String> departmentNames(Set<Long> departmentIds) {
        if (departmentIds.isEmpty()) return List.of();
        return jdbc.sql("""
                SELECT department_name
                  FROM sys_department
                 WHERE id IN (:departmentIds) AND enabled=true
              ORDER BY department_code,id
                """).param("departmentIds", departmentIds).query(String.class).list();
    }

    private record Recipient(long id, String wecomUserId, String displayName, boolean departmentDirector) {}
}