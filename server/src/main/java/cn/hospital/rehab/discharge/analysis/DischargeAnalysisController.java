package cn.hospital.rehab.discharge.analysis;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/discharge/analysis")
public class DischargeAnalysisController {
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private final JdbcClient jdbc;
    private final DataScopeService scopes;

    public DischargeAnalysisController(JdbcClient jdbc, DataScopeService scopes) {
        this.jdbc = jdbc;
        this.scopes = scopes;
    }

    @GetMapping
    ApiResponse<Metrics> metrics(Authentication auth, @RequestParam(required = false) String month) {
        YearMonth ym = resolve(month);
        OffsetDateTime start = ym.atDay(1).atStartOfDay(SHANGHAI).toOffsetDateTime();
        OffsetDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay(SHANGHAI).toOffsetDateTime();
        DataScope scope = scopes.resolve(auth);
        long discharges = count("", start, end, scope);
        long unplanned = count("AND (r.planned_discharge_at IS NULL OR r.planned_discharge_at::date<>r.actual_discharge_at::date)", start, end, scope);
        long nutrition = consultationCount("discharge_nutrition_consultation", start, end, scope);
        long homeRehab = consultationCount("discharge_home_rehab_consultation", start, end, scope);
        long outpatient = count("AND r.outpatient_appointment_at IS NOT NULL", start, end, scope);
        OffsetDateTime now = ZonedDateTime.now(SHANGHAI).toOffsetDateTime();
        OffsetDateTime weekStart = now.toLocalDate().with(java.time.DayOfWeek.MONDAY).atStartOfDay(SHANGHAI).toOffsetDateTime();
        long nearDischarge3Day = plannedCount(now, now.plusDays(3), scope);
        long currentWeekDischarge = plannedCount(weekStart, weekStart.plusWeeks(1), scope);
        return ApiResponse.ok(new Metrics(ym.toString(), nearDischarge3Day, currentWeekDischarge,
                discharges, unplanned, nutrition, homeRehab, outpatient,
                rate(unplanned, discharges), rate(nutrition, discharges), rate(homeRehab, discharges),
                rate(outpatient, discharges), trend(ym, scope)));
    }

    private long plannedCount(OffsetDateTime start, OffsetDateTime end, DataScope scope) {
        var query = jdbc.sql("SELECT COUNT(DISTINCT r.encounter_id) FROM discharge_record r " +
                        "JOIN patient_encounter e ON e.id=r.encounter_id " +
                        "WHERE r.actual_discharge_at IS NULL AND r.planned_discharge_at>=:start " +
                        "AND r.planned_discharge_at<:end " + scopeSql())
                .param("start", start).param("end", end);
        return bindScope(query, scope).query(Long.class).single();
    }

    private long count(String extra, OffsetDateTime start, OffsetDateTime end, DataScope scope) {
        var query = jdbc.sql("SELECT COUNT(DISTINCT r.encounter_id) FROM discharge_record r " +
                        "JOIN patient_encounter e ON e.id=r.encounter_id " +
                        "WHERE r.actual_discharge_at>=:start AND r.actual_discharge_at<:end " + extra + scopeSql())
                .param("start", start).param("end", end);
        return bindScope(query, scope).query(Long.class).single();
    }

    private long consultationCount(String table, OffsetDateTime start, OffsetDateTime end, DataScope scope) {
        var query = jdbc.sql("SELECT COUNT(DISTINCT r.encounter_id) FROM " + table + " x " +
                        "JOIN discharge_record r ON r.encounter_id=x.encounter_id " +
                        "JOIN patient_encounter e ON e.id=r.encounter_id " +
                        "WHERE x.deleted=false AND r.actual_discharge_at>=:start AND r.actual_discharge_at<:end " + scopeSql())
                .param("start", start).param("end", end);
        return bindScope(query, scope).query(Long.class).single();
    }

    private List<Trend> trend(YearMonth ym, DataScope scope) {
        var query = jdbc.sql("""
                SELECT series_date::date metric_date,
                       COUNT(DISTINCT r.encounter_id) discharges,
                       COUNT(DISTINCT r.encounter_id) FILTER(WHERE r.planned_discharge_at IS NULL OR r.planned_discharge_at::date<>r.actual_discharge_at::date) unplanned,
                       COUNT(DISTINCT r.encounter_id) FILTER(WHERE EXISTS (
                           SELECT 1 FROM discharge_nutrition_consultation n WHERE n.encounter_id=r.encounter_id AND n.deleted=false)) nutrition,
                       COUNT(DISTINCT r.encounter_id) FILTER(WHERE EXISTS (
                           SELECT 1 FROM discharge_home_rehab_consultation h WHERE h.encounter_id=r.encounter_id AND h.deleted=false)) home_rehab,
                       COUNT(DISTINCT r.encounter_id) FILTER(WHERE r.outpatient_appointment_at IS NOT NULL) outpatient
                  FROM generate_series(:start::date,:end::date-1,interval '1 day') AS dates(series_date)
             LEFT JOIN (discharge_record r JOIN patient_encounter e ON e.id=r.encounter_id)
                    ON r.actual_discharge_at>=:monthStart AND r.actual_discharge_at<series_date+interval '1 day'
                   AND (:allDepartments=TRUE OR e.department_id IN (:departmentIds) OR
                        (CAST(:doctorUserId AS BIGINT) IS NOT NULL AND e.doctor_user_id=:doctorUserId))
              GROUP BY series_date ORDER BY series_date
                """).param("start", ym.atDay(1)).param("end", ym.plusMonths(1).atDay(1))
                .param("monthStart", ym.atDay(1).atStartOfDay(SHANGHAI).toOffsetDateTime());
        return bindScope(query, scope).query((r, row) -> {
            long discharges = r.getLong("discharges");
            long unplanned = r.getLong("unplanned");
            long nutrition = r.getLong("nutrition");
            long homeRehab = r.getLong("home_rehab");
            long outpatient = r.getLong("outpatient");
            return new Trend(r.getObject("metric_date", LocalDate.class), discharges, unplanned, nutrition,
                    homeRehab, outpatient, rate(unplanned, discharges), rate(nutrition, discharges),
                    rate(homeRehab, discharges), rate(outpatient, discharges));
        }).list();
    }

    private static String scopeSql() {
        return " AND (:allDepartments=TRUE OR e.department_id IN (:departmentIds) OR " +
                "(CAST(:doctorUserId AS BIGINT) IS NOT NULL AND e.doctor_user_id=:doctorUserId))";
    }

    private static JdbcClient.StatementSpec bindScope(JdbcClient.StatementSpec query, DataScope scope) {
        return query.param("allDepartments", scope.allDepartments())
                .param("departmentIds", scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds())
                .param("doctorUserId", scope.doctorUserId());
    }

    private static BigDecimal rate(long numerator, long denominator) {
        return denominator == 0 ? null : BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(2, RoundingMode.HALF_UP);
    }

    private static YearMonth resolve(String month) {
        return month == null || month.isBlank() ? YearMonth.now(SHANGHAI) : YearMonth.parse(month);
    }

    public record Trend(LocalDate day, long dischargeCount, long unplannedCount, long nutritionCount,
                        long homeRehabCount, long outpatientCount, BigDecimal unplannedRate,
                        BigDecimal nutritionRate, BigDecimal homeRehabRate, BigDecimal outpatientRate) {}
    public record Metrics(String month, long nearDischarge3DayCount, long currentWeekDischargeCount,
                          long dischargeCount, long unplannedCount, long nutritionCount,
                          long homeRehabCount, long outpatientCount, BigDecimal unplannedRate,
                          BigDecimal nutritionRate, BigDecimal homeRehabRate, BigDecimal outpatientRate,
                          List<Trend> trend) {}
}
