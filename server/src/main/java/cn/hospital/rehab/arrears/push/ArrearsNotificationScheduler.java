package cn.hospital.rehab.arrears.push;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;

import java.util.List;
import java.util.Set;

@Component
public class ArrearsNotificationScheduler {
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
        var allPreview = notices.preview(DataScope.all(), "全院").orElse(null);
        if (allPreview == null || allPreview.departments().isEmpty()) return;

        List<Recipient> recipients = jdbc.sql("""
                SELECT DISTINCT u.id,u.wecom_user_id,u.display_name
                  FROM sys_user u
                  JOIN sys_user_role ur ON ur.user_id=u.id
                  JOIN sys_role r ON r.id=ur.role_id
                 WHERE u.enabled=true AND r.enabled=true
                   AND u.wecom_user_id IS NOT NULL AND BTRIM(u.wecom_user_id)<>''
                   AND r.role_code IN ('DEPARTMENT_DIRECTOR','ATTENDING_DOCTOR')
              ORDER BY u.id
                """).query((row, number) -> new Recipient(row.getLong("id"),
                        row.getString("wecom_user_id"), row.getString("display_name"))).list();

        for (Recipient recipient : recipients) {
            DataScope scope = dataScopes.resolveForUser(recipient.id());
            String scopeType = scopeType(scope);
            if (scopeType == null) continue;

            var preview = notices.preview(scope, scopeLabel(scope, recipient.displayName())).orElse(null);
            if (preview == null || preview.departments().isEmpty()) continue;

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
            if (scope.doctorUserId() != null) {
                jdbc.sql("""
                        INSERT INTO push_task_scope(task_id,doctor_user_id)
                        VALUES (:taskId,:doctorUserId)
                        ON CONFLICT DO NOTHING
                        """).param("taskId", taskId.get()).param("doctorUserId", scope.doctorUserId()).update();
            }
        }
    }

    private String scopeType(DataScope scope) {
        if (scope.allDepartments()) return "ALL";
        boolean departments = !scope.departmentIds().isEmpty();
        boolean doctor = scope.doctorUserId() != null;
        if (departments && doctor) return "MIXED";
        if (departments) return "DEPARTMENT";
        if (doctor) return "DOCTOR";
        return null;
    }

    private String scopeLabel(DataScope scope, String displayName) {
        if (scope.allDepartments()) return "全院";
        List<String> departmentNames = departmentNames(scope.departmentIds());
        String departmentLabel = String.join("、", departmentNames);
        if (!departmentNames.isEmpty() && scope.doctorUserId() != null) {
            return departmentLabel + "及" + displayName + "本人负责";
        }
        if (!departmentNames.isEmpty()) return departmentLabel;
        return displayName + "本人负责";
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

    private record Recipient(long id, String wecomUserId, String displayName) {}
}
