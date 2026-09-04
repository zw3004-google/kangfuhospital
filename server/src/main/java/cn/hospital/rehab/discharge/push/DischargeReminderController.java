package cn.hospital.rehab.discharge.push;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discharge/reminders")
public class DischargeReminderController {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final JdbcClient jdbc;
    private final DischargeReminderScheduler scheduler;
    private final AuditLogService audit;

    public DischargeReminderController(JdbcClient jdbc, DischargeReminderScheduler scheduler, AuditLogService audit) {
        this.jdbc = jdbc;
        this.scheduler = scheduler;
        this.audit = audit;
    }

    @GetMapping("/preview")
    public ApiResponse<Preview> preview() {
        LocalDate day = LocalDate.now(SHANGHAI);
        long nutrition = count("SELECT COUNT(DISTINCT encounter_id) FROM discharge_nutrition_consultation WHERE deleted=false AND appointment_at::date=:day", day);
        long homeRehab = count("SELECT COUNT(DISTINCT encounter_id) FROM discharge_home_rehab_consultation WHERE deleted=false AND appointment_at::date=:day", day);
        long followUp = jdbc.sql("SELECT COUNT(DISTINCT encounter_id) FROM discharge_record WHERE actual_discharge_at::date IN (:day7,:day30,:day60)")
                .param("day7", day.minusDays(7)).param("day30", day.minusDays(30)).param("day60", day.minusDays(60))
                .query(Long.class).single();
        long unplanned = jdbc.sql("SELECT COUNT(DISTINCT encounter_id) FROM discharge_record WHERE actual_discharge_at::date=:yesterday AND (planned_discharge_at IS NULL OR planned_discharge_at::date<>actual_discharge_at::date)")
                .param("yesterday", day.minusDays(1)).query(Long.class).single();
        return ApiResponse.ok(Preview.of(day, nutrition, homeRehab, followUp, unplanned));
    }

    @PostMapping("/trigger")
    public ApiResponse<TriggerResult> trigger(Authentication auth, HttpServletRequest request) {
        LocalDate day = LocalDate.now(SHANGHAI);
        long before = taskCount(day);
        scheduler.createDailyReminders();
        long created = Math.max(0, taskCount(day) - before);
        var result = new TriggerResult(day, created, "提醒任务已生成；重复任务已自动忽略");
        audit.record(auth, "DISCHARGE", "DISCHARGE_REMINDER", day.toString(), "MANUAL_TRIGGER", null,
                Map.of("created", created), request.getRemoteAddr());
        return ApiResponse.ok(result);
    }

    private long count(String sql, LocalDate day) {
        return jdbc.sql(sql).param("day", day).query(Long.class).single();
    }

    private long taskCount(LocalDate day) {
        return jdbc.sql("SELECT COUNT(*) FROM push_task WHERE business_type='DISCHARGE' AND reminder_date=:day")
                .param("day", day).query(Long.class).single();
    }

    public record Preview(LocalDate reminderDate, long nutritionCount, long homeRehabCount,
                          long followUpCount, long unplannedCount, long totalPatients, List<ReminderItem> items) {
        static Preview of(LocalDate day, long nutrition, long homeRehab, long followUp, long unplanned) {
            return new Preview(day, nutrition, homeRehab, followUp, unplanned, nutrition + homeRehab + followUp + unplanned, List.of(
                    new ReminderItem("NUTRITION", "营养会诊", "营养科岗位人员", nutrition, "预约日期为提醒当日", "患者（姓名脱敏），住院号：****，今日需要营养会诊。"),
                    new ReminderItem("HOME", "居家康复", "居家康复科岗位人员", homeRehab, "预约日期为提醒当日", "患者（姓名脱敏），住院号：****，今日需要居家康复会诊。"),
                    new ReminderItem("FOLLOW_UP", "出院随访", "随访员岗位人员", followUp, "实际出院满7/30/60天", "患者（姓名脱敏），住院号：****，今日需要随访。"),
                    new ReminderItem("UNPLANNED", "非计划出院", "患者主管医生", unplanned, "昨日实际出院且计划缺失或日期不一致", "患者（姓名脱敏），住院号：****，非计划出院，请写明原因。")
            ));
        }
    }
    public record ReminderItem(String type, String label, String recipientScope, long patientCount,
                               String triggerBasis, String messagePreview) {}
    public record TriggerResult(LocalDate reminderDate, long createdTasks, String message) {}
}
