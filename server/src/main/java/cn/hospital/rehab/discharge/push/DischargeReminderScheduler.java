package cn.hospital.rehab.discharge.push;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class DischargeReminderScheduler {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final LocalTime DAILY_REMINDER_TIME = LocalTime.of(8, 0);
    private static final OffsetDateTime PUSH_GO_LIVE_AT = OffsetDateTime.parse("2026-10-01T00:00:00+08:00");
    private static final String MASKED_NAME = "CASE WHEN e.patient_name LIKE '欧阳%' THEN LEFT(e.patient_name,2)||REPEAT('*',GREATEST(CHAR_LENGTH(e.patient_name)-2,1)) ELSE LEFT(e.patient_name,1)||REPEAT('*',GREATEST(CHAR_LENGTH(e.patient_name)-1,1)) END";
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public DischargeReminderScheduler(JdbcClient jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    @Scheduled(cron = "${app.messaging.discharge-cron:0 0 8 * * *}", zone = "Asia/Shanghai")
    public void createDailyReminders() {
        LocalDate today = LocalDate.now(SHANGHAI);
        createConsultation("NUTRITION", today);
        createConsultation("HOME", today);
        createFollowUp(today);
        createAbnormalReport(today);
    }

    private void createConsultation(String type, LocalDate day) {
        String table = "NUTRITION".equals(type) ? "discharge_nutrition_consultation" : "discharge_home_rehab_consultation";
        String role = "NUTRITION".equals(type) ? "NUTRITION" : "HOME_REHAB";
        String label = "NUTRITION".equals(type) ? "营养会诊" : "居家康复会诊";
        String sql = """
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE',:type,:day,u.wecom_user_id,u.display_name,m.content,'PENDING',CURRENT_TIMESTAMP
                FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
                CROSS JOIN LATERAL (
                  SELECT STRING_AGG(%s||'患者，住院号：'||e.inpatient_no||'，今日需要%s。', E'\n' ORDER BY e.inpatient_no) content
                  FROM %s c JOIN patient_encounter e ON e.id=c.encounter_id
                  WHERE c.deleted=false AND c.appointment_at::date=:day
                    AND c.reported_at>=:goLiveAt
                    AND EXISTS (
                      SELECT 1 FROM sys_user_department ud
                      JOIN sys_department access_department ON access_department.id=ud.department_id AND access_department.enabled=true
                      WHERE ud.user_id=u.id AND ud.department_id=e.department_id
                    )
                ) m
                WHERE u.enabled=true AND r.enabled=true AND r.role_code=:role AND m.content IS NOT NULL
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME, label, table);
        jdbc.sql(sql).param("day", day).param("type", type).param("role", role).param("goLiveAt", PUSH_GO_LIVE_AT).update();
    }
    public void createLateConsultationReminder(String type, long consultationId, OffsetDateTime appointmentAt) {
        if (!isAfterDailyCutoff(appointmentAt)) return;
        String table = "NUTRITION".equalsIgnoreCase(type) ? "discharge_nutrition_consultation" : "discharge_home_rehab_consultation";
        String role = "NUTRITION".equalsIgnoreCase(type) ? "NUTRITION" : "HOME_REHAB";
        String label = "NUTRITION".equalsIgnoreCase(type) ? "营养会诊" : "居家康复会诊";
        String reminderType = ("NUTRITION".equalsIgnoreCase(type) ? "NUTRITION_LATE_" : "HOME_LATE_") + consultationId;
        String sql = """
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE',:reminderType,:day,u.wecom_user_id,u.display_name,
                       %s||'患者，住院号：'||e.inpatient_no||'，今日需要%s。','PENDING',CURRENT_TIMESTAMP
                FROM %s c JOIN patient_encounter e ON e.id=c.encounter_id
                JOIN sys_user u ON u.enabled=true
                JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
                WHERE c.id=:consultationId AND c.deleted=false AND c.reported_at>=:goLiveAt
                  AND r.enabled=true AND r.role_code=:role
                  AND EXISTS (
                    SELECT 1 FROM sys_user_department ud
                    JOIN sys_department access_department ON access_department.id=ud.department_id AND access_department.enabled=true
                    WHERE ud.user_id=u.id AND ud.department_id=e.department_id
                  )
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME, label, table);
        jdbc.sql(sql).param("reminderType", reminderType).param("day", LocalDate.now(SHANGHAI))
                .param("consultationId", consultationId).param("role", role).param("goLiveAt", PUSH_GO_LIVE_AT).update();
    }

    private void createFollowUp(LocalDate day) {
        String sql = """
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE','FOLLOW_UP',:day,u.wecom_user_id,u.display_name,m.content,'PENDING',CURRENT_TIMESTAMP
                FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
                CROSS JOIN LATERAL (
                  SELECT STRING_AGG(%s||'患者，住院号：'||e.inpatient_no||'，今日需要随访。', E'\n' ORDER BY e.inpatient_no) content
                  FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id
                  WHERE d.actual_discharge_at::date IN (:day7,:day30,:day60)
                      AND d.actual_discharge_at>=:goLiveAt
                    AND EXISTS (
                      SELECT 1 FROM sys_user_department ud
                      JOIN sys_department access_department ON access_department.id=ud.department_id AND access_department.enabled=true
                      WHERE ud.user_id=u.id AND ud.department_id=e.department_id
                    )
                ) m
                WHERE u.enabled=true AND r.enabled=true AND r.role_code='FOLLOW_UP' AND m.content IS NOT NULL
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME);
        jdbc.sql(sql).param("day", day).param("day7", day.minusDays(7)).param("day30", day.minusDays(30))
                .param("day60", day.minusDays(60)).param("goLiveAt", PUSH_GO_LIVE_AT).update();
    }

    public void createLateFollowUpReminders(long dischargeRecordId, String detailsJson) {
        if (detailsJson == null || detailsJson.isBlank()) return;
        try {
            JsonNode details = json.readTree(detailsJson);
            if (!details.isArray()) return;
            for (JsonNode detail : details) {
                int day = detail.path("day").asInt();
                String followUpAt = detail.path("followUpAt").asText("");
                if (day <= 0 || followUpAt.isBlank()) continue;
                if (isAfterDailyCutoff(OffsetDateTime.parse(followUpAt))) createLateFollowUpReminder(dischargeRecordId, day);
            }
        } catch (Exception ignored) {
            // Malformed legacy JSON must not block a successful patient-record save.
        }
    }

    private void createLateFollowUpReminder(long dischargeRecordId, int followUpDay) {
        String sql = """
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE',:reminderType,:day,u.wecom_user_id,u.display_name,
                       %s||'患者，住院号：'||e.inpatient_no||'，今日需要随访。','PENDING',CURRENT_TIMESTAMP
                FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id
                JOIN sys_user u ON u.enabled=true
                JOIN sys_user_role ur ON ur.user_id=u.id JOIN sys_role r ON r.id=ur.role_id
                WHERE d.id=:recordId AND d.actual_discharge_at>=:goLiveAt
                  AND r.enabled=true AND r.role_code='FOLLOW_UP'
                  AND EXISTS (
                    SELECT 1 FROM sys_user_department ud
                    JOIN sys_department access_department ON access_department.id=ud.department_id AND access_department.enabled=true
                    WHERE ud.user_id=u.id AND ud.department_id=e.department_id
                  )
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME);
        jdbc.sql(sql).param("reminderType", "FOLLOW_UP_LATE_" + dischargeRecordId + "_" + followUpDay)
                .param("day", LocalDate.now(SHANGHAI)).param("recordId", dischargeRecordId).param("goLiveAt", PUSH_GO_LIVE_AT).update();
    }

    private static boolean isAfterDailyCutoff(OffsetDateTime appointmentAt) {
        ZonedDateTime now = ZonedDateTime.now(SHANGHAI);
        return appointmentAt.atZoneSameInstant(SHANGHAI).toLocalDate().equals(now.toLocalDate())
                && !now.toLocalTime().isBefore(DAILY_REMINDER_TIME);
    }

    private void createAbnormalReport(LocalDate day) {
        OffsetDateTime cutoff = day.atTime(DAILY_REMINDER_TIME).atZone(SHANGHAI).toOffsetDateTime();
        String sql = """
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_wecom_id,recipient_name,content,status,scheduled_at)
                SELECT 'DISCHARGE','ABNORMAL_REPORT',:day,u.wecom_user_id,u.display_name,m.content,'PENDING',CURRENT_TIMESTAMP
                FROM sys_user u
                JOIN sys_user_role ur ON ur.user_id=u.id
                JOIN sys_role r ON r.id=ur.role_id
                CROSS JOIN LATERAL (
                    SELECT STRING_AGG(%s||'患者，住院号：'||e.inpatient_no||'，填报异常：'||
                        REPLACE(REPLACE(REPLACE(d.abnormal_codes,'MISSING_PLAN','未填报预计出院时间'),
                        'DATE_MISMATCH','预计与实际出院日期不一致'),'LATE_PLAN','出院前12小时内填报')||'。请及时处理。',
                        E'\n' ORDER BY e.inpatient_no) AS content
                    FROM discharge_record d
                    JOIN patient_encounter e ON e.id=d.encounter_id
                    WHERE d.actual_discharge_at>=:goLiveAt
                      AND COALESCE(d.abnormal_codes,'')<>''
                      AND d.updated_at<=:cutoff
                      AND EXISTS (
                          SELECT 1
                          FROM sys_user_department ud
                          JOIN sys_department access_department ON access_department.id=ud.department_id AND access_department.enabled=true
                          WHERE ud.user_id=u.id AND ud.department_id=e.department_id
                      )
                ) m
                WHERE u.enabled=true AND r.enabled=true AND r.role_code='ATTENDING_DOCTOR' AND m.content IS NOT NULL
                ON CONFLICT DO NOTHING
                """.formatted(MASKED_NAME);
        jdbc.sql(sql).param("day", day).param("cutoff", cutoff).param("goLiveAt", PUSH_GO_LIVE_AT).update();
    }
}
