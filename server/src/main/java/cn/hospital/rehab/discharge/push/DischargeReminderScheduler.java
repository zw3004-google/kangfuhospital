package cn.hospital.rehab.discharge.push;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DischargeReminderScheduler {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final String MASKED_NAME = "CASE WHEN e.patient_name LIKE '欧阳%' THEN LEFT(e.patient_name,2)||REPEAT('*',GREATEST(CHAR_LENGTH(e.patient_name)-2,1)) ELSE LEFT(e.patient_name,1)||REPEAT('*',GREATEST(CHAR_LENGTH(e.patient_name)-1,1)) END";
    private final JdbcClient jdbc;

    public DischargeReminderScheduler(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Scheduled(cron = "${app.messaging.discharge-cron:0 0 8 * * *}", zone = "Asia/Shanghai")
    public void createDailyReminders() {
        LocalDate today = LocalDate.now(SHANGHAI);
        createConsultation("NUTRITION", today);
        createConsultation("HOME", today);
        createFollowUp(today);
        createUnplanned(today);
    }

    private void createConsultation(String type, LocalDate day) {
        String table = "NUTRITION".equals(type) ? "discharge_nutrition_consultation" : "discharge_home_rehab_consultation";
        String role = "NUTRITION".equals(type) ? "NUTRITION" : "HOME_REHAB";
        String label = "NUTRITION".equals(type) ? "营养会诊" : "居家康复会诊";
        String sql = """
                WITH message AS (
                  SELECT STRING_AGG(%s||'患者，住院号：'||e.inpatient_no||'，今日需要%s。', E'\n' ORDER BY e.inpatient_no) content
                  FROM %s c JOIN patient_encounter e ON e.id=c.encounter_id
                  WHERE c.deleted=false AND c.appointment_at::date=:day
                )
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE',:type,:day,u.wecom_user_id,u.display_name,m.content,'PENDING',CURRENT_TIMESTAMP
                FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id CROSS JOIN message m
                WHERE u.enabled=true AND r.enabled=true AND r.role_code=:role AND m.content IS NOT NULL
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME, label, table);
        jdbc.sql(sql).param("day", day).param("type", type).param("role", role).update();
    }

    private void createFollowUp(LocalDate day) {
        String sql = """
                WITH message AS (
                  SELECT STRING_AGG(%s||'患者，住院号：'||e.inpatient_no||'，今日需要随访。', E'\n' ORDER BY e.inpatient_no) content
                  FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id
                  WHERE d.actual_discharge_at::date IN (:day7,:day30,:day60)
                )
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE','FOLLOW_UP',:day,u.wecom_user_id,u.display_name,m.content,'PENDING',CURRENT_TIMESTAMP
                FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id CROSS JOIN message m
                WHERE u.enabled=true AND r.enabled=true AND r.role_code='FOLLOW_UP' AND m.content IS NOT NULL
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME);
        jdbc.sql(sql).param("day", day).param("day7", day.minusDays(7)).param("day30", day.minusDays(30))
                .param("day60", day.minusDays(60)).update();
    }

    private void createUnplanned(LocalDate day) {
        String sql = """
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE','UNPLANNED',:day,u.wecom_user_id,u.display_name,
                       STRING_AGG(%s||'患者，住院号：'||e.inpatient_no||'，非计划出院，请写明原因。', E'\n' ORDER BY e.inpatient_no),
                       'PENDING',CURRENT_TIMESTAMP
                FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id JOIN sys_user u ON u.id=e.doctor_user_id
                WHERE u.enabled=true AND d.actual_discharge_at::date=:yesterday
                  AND (d.planned_discharge_at IS NULL OR d.planned_discharge_at::date<>d.actual_discharge_at::date)
                GROUP BY u.id,u.wecom_user_id,u.display_name
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME);
        jdbc.sql(sql).param("day", day).param("yesterday", day.minusDays(1)).update();
    }
}
