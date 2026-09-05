package cn.hospital.rehab.discharge.api;

import cn.hospital.rehab.common.api.PageResult;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.discharge.domain.DischargeAbnormalCalculator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Repository
public class DischargeRepository {
    private static final Set<Integer> ALLOWED_SIZES = Set.of(20, 50, 100, 200);
    private final JdbcClient jdbc;

    public DischargeRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public PageResult<DischargeSummary> page(String keyword, Boolean discharged, int page, int size) {
        return page(keyword, discharged, DataScope.all(), page, size);
    }

    public PageResult<DischargeSummary> page(String keyword, Boolean discharged, DataScope scope, int page, int size) {
        return page(keyword,null,null,null,null,discharged,scope,page,size);
    }

    public PageResult<DischargeSummary> page(String keyword, Long departmentId, String timeType,
                                               OffsetDateTime startAt, OffsetDateTime endAt, Boolean discharged,
                                               DataScope scope, int page, int size) {
        return page(keyword,departmentId,timeType,startAt,endAt,discharged,null,scope,page,size);
    }

    public PageResult<DischargeSummary> page(String keyword, Long departmentId, String timeType,
                                               OffsetDateTime startAt, OffsetDateTime endAt, Boolean discharged,
                                               String category, DataScope scope, int page, int size) {
        return query(keyword,departmentId,timeType,startAt,endAt,discharged,category,scope,page,size,false);
    }

    public PageResult<DischargeSummary> export(String keyword, Boolean discharged, DataScope scope) {
        return query(keyword,null,null,null,null,discharged,null,scope,1,20000,true);
    }

    public PageResult<DischargeSummary> export(String keyword, Long departmentId, String timeType,
                                                OffsetDateTime startAt, OffsetDateTime endAt, Boolean discharged,
                                                DataScope scope) {
        return export(keyword,departmentId,timeType,startAt,endAt,discharged,null,scope);
    }

    public PageResult<DischargeSummary> export(String keyword, Long departmentId, String timeType,
                                                OffsetDateTime startAt, OffsetDateTime endAt, Boolean discharged,
                                                String category, DataScope scope) {
        return query(keyword,departmentId,timeType,startAt,endAt,discharged,category,scope,1,20000,true);
    }

    private PageResult<DischargeSummary> query(String keyword, Long departmentId, String timeType,
                                                OffsetDateTime startAt, OffsetDateTime endAt, Boolean discharged,
                                                String category, DataScope scope, int page, int size, boolean export) {
        String value = normalizeKeyword(keyword);
        DischargeTimeType parsedTimeType=DischargeTimeType.parse(timeType);
        validateRange(startAt,endAt);
        int safePage = safePage(page);
        int safeSize = export ? 20000 : safeSize(size);
        int filter = discharged == null ? 2 : (discharged ? 1 : 0);
        String parsedCategory = normalizeCategory(category);
        String scopeWhere = buildScopeWhere(scope);

        var itemsQuery = jdbc.sql(buildPageSql(scopeWhere))
                .param("keyword", value).param("pattern", "%" + value + "%")
                .param("departmentId",departmentId==null?0:departmentId)
                .param("timeType",parsedTimeType==null?"":parsedTimeType.name())
                .param("startAt",startAt).param("endAt",endAt)
                .param("dischargedFilter", filter).param("category",parsedCategory).param("limit", safeSize)
                .param("offset", (safePage - 1) * safeSize);
        bindScope(itemsQuery, scope);
        var items = itemsQuery.query(this::map).list();

        var countQuery = jdbc.sql(buildCountSql(scopeWhere))
                .param("keyword", value).param("pattern", "%" + value + "%")
                .param("departmentId",departmentId==null?0:departmentId)
                .param("timeType",parsedTimeType==null?"":parsedTimeType.name())
                .param("startAt",startAt).param("endAt",endAt)
                .param("dischargedFilter", filter).param("category",parsedCategory);
        bindScope(countQuery, scope);
        long total = countQuery.query(Long.class).single();
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public DischargeSummary update(long id, UpdateDischargeRequest request) {
        return update(id, request, DataScope.all());
    }

    @Transactional
    public DischargeSummary update(long id, UpdateDischargeRequest request, DataScope scope) {
        DischargeSummary current=find(id,scope);
        DischargeUpdateValidator.validateBefore(request,current);
        var query = jdbc.sql("""
                UPDATE discharge_record d SET planned_discharge_at=COALESCE(:planned,d.planned_discharge_at),
                planned_discharge_updated_at=CASE WHEN CAST(:planned AS TIMESTAMPTZ) IS NULL THEN d.planned_discharge_updated_at ELSE CURRENT_TIMESTAMP END,
                is_special_patient=COALESCE(:special,d.is_special_patient),
                special_reason=COALESCE(:specialReason,d.special_reason), abnormal_reason=COALESCE(:abnormalReason,d.abnormal_reason),
                follow_up_required=COALESCE(:followUp,d.follow_up_required), follow_up_day7=COALESCE(:day7,d.follow_up_day7),
                follow_up_day30=COALESCE(:day30,d.follow_up_day30), follow_up_day60=COALESCE(:day60,d.follow_up_day60),
                outpatient_appointment_at=COALESCE(:outpatient,d.outpatient_appointment_at),
                outpatient_arrived=COALESCE(:outpatientArrived,d.outpatient_arrived),
                outpatient_arrival_at=COALESCE(:outpatientArrivalAt,d.outpatient_arrival_at),
                outpatient_reporter=COALESCE(:outpatientReporter,d.outpatient_reporter),
                outpatient_no_show_reason=COALESCE(:outpatientNoShowReason,d.outpatient_no_show_reason),
                follow_up_details=COALESCE(CAST(:followUpDetails AS JSONB),d.follow_up_details),
                updated_at=CURRENT_TIMESTAMP FROM patient_encounter e
                WHERE d.encounter_id=e.id AND d.id=:id
                  AND (CAST(:expectedUpdatedAt AS TIMESTAMPTZ) IS NULL OR d.updated_at=:expectedUpdatedAt)
                """ + buildScopeWhere(scope))
                .param("planned", parseDate(request.plannedDischargeAt()))
                .param("special", request.specialPatient())
                .param("specialReason", request.specialReason()).param("abnormalReason", request.abnormalReason())
                .param("followUp", request.followUpRequired()).param("day7", request.followUpDay7())
                .param("day30", request.followUpDay30()).param("day60", request.followUpDay60())
                .param("outpatient", parseDate(request.outpatientAppointmentAt())).param("outpatientArrived",request.outpatientArrived())
                .param("outpatientArrivalAt",parseDate(request.outpatientArrivalAt())).param("outpatientReporter",request.outpatientReporter())
                .param("outpatientNoShowReason",request.outpatientNoShowReason()).param("followUpDetails",request.followUpDetailsJson())
                .param("expectedUpdatedAt",request.expectedUpdatedAt()).param("id", id);
        bindScope(query, scope);
        if (query.update() == 0) throw new cn.hospital.rehab.common.api.ConcurrentUpdateException("记录已被其他人员更新，请刷新后核对再保存");
        recalculateAbnormalCodes(id);
        DischargeSummary updated=findById(id);
        DischargeUpdateValidator.validateAfter(updated);
        return updated;
    }

    private String buildPageSql(String scope) {
        return """
                SELECT d.*, e.id AS encounter_id, e.inpatient_no, e.admission_times, e.patient_name,
                       e.gender, e.primary_diagnosis, e.admitted_at, e.doctor_name_source, dpt.department_name,
                       (SELECT MAX(c.appointment_at) FROM discharge_nutrition_consultation c
                         WHERE c.encounter_id=e.id AND c.deleted=false) AS latest_nutrition_appointment_at,
                       (SELECT MAX(c.appointment_at) FROM discharge_home_rehab_consultation c
                         WHERE c.encounter_id=e.id AND c.deleted=false) AS latest_home_rehab_appointment_at,
                       GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) AS latest_follow_up_at
                  FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id
             LEFT JOIN sys_department dpt ON dpt.id=e.department_id
                 WHERE (:keyword='' OR e.inpatient_no ILIKE :pattern OR e.patient_name ILIKE :pattern OR e.doctor_name_source ILIKE :pattern)
                   AND (:departmentId=0 OR e.department_id=:departmentId)
                   AND (:dischargedFilter=2 OR (d.actual_discharge_at IS NOT NULL)=(:dischargedFilter=1))
                   AND (:category='' OR
                        (:category='BOARD' AND d.actual_discharge_at IS NULL) OR
                        (:category='FOLLOW_UP' AND d.actual_discharge_at IS NOT NULL) OR
                        (:category='NUTRITION' AND EXISTS(SELECT 1 FROM discharge_nutrition_consultation cn WHERE cn.encounter_id=e.id AND cn.deleted=false)) OR
                        (:category='HOME_REHAB' AND EXISTS(SELECT 1 FROM discharge_home_rehab_consultation ch WHERE ch.encounter_id=e.id AND ch.deleted=false)) OR
                        (:category='OUTPATIENT' AND d.outpatient_appointment_at IS NOT NULL) OR
                        (:category='ABNORMAL' AND COALESCE(d.abnormal_codes,'')<>''))
                   AND (:timeType='' OR
                        (:timeType='ADMITTED' AND e.admitted_at >= COALESCE(:startAt,e.admitted_at) AND e.admitted_at < COALESCE(:endAt,e.admitted_at + INTERVAL '1 microsecond')) OR
                        (:timeType='PLANNED_DISCHARGE' AND d.planned_discharge_at >= COALESCE(:startAt,d.planned_discharge_at) AND d.planned_discharge_at < COALESCE(:endAt,d.planned_discharge_at + INTERVAL '1 microsecond')) OR
                        (:timeType='ACTUAL_DISCHARGE' AND d.actual_discharge_at >= COALESCE(:startAt,d.actual_discharge_at) AND d.actual_discharge_at < COALESCE(:endAt,d.actual_discharge_at + INTERVAL '1 microsecond')) OR
                        (:timeType='OUTPATIENT' AND d.outpatient_appointment_at >= COALESCE(:startAt,d.outpatient_appointment_at) AND d.outpatient_appointment_at < COALESCE(:endAt,d.outpatient_appointment_at + INTERVAL '1 microsecond')) OR
                        (:timeType='NUTRITION' AND EXISTS(SELECT 1 FROM discharge_nutrition_consultation n WHERE n.encounter_id=e.id AND n.deleted=false AND n.appointment_at >= COALESCE(:startAt,n.appointment_at) AND n.appointment_at < COALESCE(:endAt,n.appointment_at + INTERVAL '1 microsecond'))) OR
                        (:timeType='HOME_REHAB' AND EXISTS(SELECT 1 FROM discharge_home_rehab_consultation h WHERE h.encounter_id=e.id AND h.deleted=false AND h.appointment_at >= COALESCE(:startAt,h.appointment_at) AND h.appointment_at < COALESCE(:endAt,h.appointment_at + INTERVAL '1 microsecond'))) OR
                        (:timeType='FOLLOW_UP' AND GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) >= COALESCE(:startAt,GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at)) AND GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) < COALESCE(:endAt,GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) + INTERVAL '1 microsecond')))
                """ + scope + " ORDER BY e.admitted_at DESC NULLS LAST LIMIT :limit OFFSET :offset";
    }

    private String buildCountSql(String scope) {
        return """
                SELECT COUNT(*) FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id
                 WHERE (:keyword='' OR e.inpatient_no ILIKE :pattern OR e.patient_name ILIKE :pattern OR e.doctor_name_source ILIKE :pattern)
                   AND (:departmentId=0 OR e.department_id=:departmentId)
                   AND (:dischargedFilter=2 OR (d.actual_discharge_at IS NOT NULL)=(:dischargedFilter=1))
                   AND (:category='' OR
                        (:category='BOARD' AND d.actual_discharge_at IS NULL) OR
                        (:category='FOLLOW_UP' AND d.actual_discharge_at IS NOT NULL) OR
                        (:category='NUTRITION' AND EXISTS(SELECT 1 FROM discharge_nutrition_consultation cn WHERE cn.encounter_id=e.id AND cn.deleted=false)) OR
                        (:category='HOME_REHAB' AND EXISTS(SELECT 1 FROM discharge_home_rehab_consultation ch WHERE ch.encounter_id=e.id AND ch.deleted=false)) OR
                        (:category='OUTPATIENT' AND d.outpatient_appointment_at IS NOT NULL) OR
                        (:category='ABNORMAL' AND COALESCE(d.abnormal_codes,'')<>''))
                   AND (:timeType='' OR
                        (:timeType='ADMITTED' AND e.admitted_at >= COALESCE(:startAt,e.admitted_at) AND e.admitted_at < COALESCE(:endAt,e.admitted_at + INTERVAL '1 microsecond')) OR
                        (:timeType='PLANNED_DISCHARGE' AND d.planned_discharge_at >= COALESCE(:startAt,d.planned_discharge_at) AND d.planned_discharge_at < COALESCE(:endAt,d.planned_discharge_at + INTERVAL '1 microsecond')) OR
                        (:timeType='ACTUAL_DISCHARGE' AND d.actual_discharge_at >= COALESCE(:startAt,d.actual_discharge_at) AND d.actual_discharge_at < COALESCE(:endAt,d.actual_discharge_at + INTERVAL '1 microsecond')) OR
                        (:timeType='OUTPATIENT' AND d.outpatient_appointment_at >= COALESCE(:startAt,d.outpatient_appointment_at) AND d.outpatient_appointment_at < COALESCE(:endAt,d.outpatient_appointment_at + INTERVAL '1 microsecond')) OR
                        (:timeType='NUTRITION' AND EXISTS(SELECT 1 FROM discharge_nutrition_consultation n WHERE n.encounter_id=e.id AND n.deleted=false AND n.appointment_at >= COALESCE(:startAt,n.appointment_at) AND n.appointment_at < COALESCE(:endAt,n.appointment_at + INTERVAL '1 microsecond'))) OR
                        (:timeType='HOME_REHAB' AND EXISTS(SELECT 1 FROM discharge_home_rehab_consultation h WHERE h.encounter_id=e.id AND h.deleted=false AND h.appointment_at >= COALESCE(:startAt,h.appointment_at) AND h.appointment_at < COALESCE(:endAt,h.appointment_at + INTERVAL '1 microsecond'))) OR
                        (:timeType='FOLLOW_UP' AND GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) >= COALESCE(:startAt,GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at)) AND GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) < COALESCE(:endAt,GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) + INTERVAL '1 microsecond')))
                """ + scope;
    }

    private String buildScopeWhere(DataScope scope) {
        return """
                   AND (:allDepartments=TRUE OR e.department_id IN (:departmentIds) OR
                        (CAST(:doctorUserId AS BIGINT) IS NOT NULL AND e.doctor_user_id=:doctorUserId))
                """;
    }

    private void bindScope(JdbcClient.StatementSpec query, DataScope scope) {
        query.param("allDepartments", scope.allDepartments()).param("doctorUserId", scope.doctorUserId())
                .param("departmentIds", scope.departmentIds().isEmpty() ? Set.of(-1L) : scope.departmentIds());
    }

    private DischargeSummary findById(long id) {
        return jdbc.sql(buildFindSql()+" WHERE d.id=:id")
                .param("id", id).query(this::map).single();
    }

    public DischargeSummary find(long id,DataScope scope) {
        var query=jdbc.sql(buildFindSql()+" WHERE d.id=:id"+buildScopeWhere(scope)).param("id",id);
        bindScope(query,scope);return query.query(this::map).optional().orElseThrow(()->new AccessDeniedException("无权访问该患者数据"));
    }

    private String buildFindSql() {
        return """
                SELECT d.*,e.id AS encounter_id,e.inpatient_no,e.admission_times,e.patient_name,
                       e.gender,e.primary_diagnosis,e.admitted_at,e.doctor_name_source,dpt.department_name,
                       (SELECT MAX(c.appointment_at) FROM discharge_nutrition_consultation c
                         WHERE c.encounter_id=e.id AND c.deleted=false) AS latest_nutrition_appointment_at,
                       (SELECT MAX(c.appointment_at) FROM discharge_home_rehab_consultation c
                         WHERE c.encounter_id=e.id AND c.deleted=false) AS latest_home_rehab_appointment_at,
                       GREATEST(d.follow_up_day7_at,d.follow_up_day30_at,d.follow_up_day60_at) AS latest_follow_up_at
                  FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id
             LEFT JOIN sys_department dpt ON dpt.id=e.department_id
                """;
    }

    private void recalculateAbnormalCodes(long id) {
        var values = jdbc.sql("SELECT planned_discharge_at,actual_discharge_at,planned_discharge_updated_at FROM discharge_record WHERE id=:id")
                .param("id", id).query((r,n) -> new OffsetDateTime[]{r.getObject(1,OffsetDateTime.class),r.getObject(2,OffsetDateTime.class),r.getObject(3,OffsetDateTime.class)}).single();
        jdbc.sql("UPDATE discharge_record SET abnormal_codes=:codes WHERE id=:id")
                .param("codes", String.join(",", DischargeAbnormalCalculator.calculate(values[0],values[1],values[2]))).param("id", id).update();
    }

    DischargeSummary map(ResultSet r, int row) throws SQLException {
        OffsetDateTime planned=r.getObject("planned_discharge_at",OffsetDateTime.class), actual=r.getObject("actual_discharge_at",OffsetDateTime.class);
        return new DischargeSummary(r.getLong("id"),r.getLong("encounter_id"),r.getString("inpatient_no"),r.getInt("admission_times"),r.getString("patient_name"),r.getString("gender"),r.getString("department_name"),r.getString("primary_diagnosis"),r.getString("doctor_name_source"),r.getObject("admitted_at",OffsetDateTime.class),planned,actual,r.getObject("outpatient_appointment_at",OffsetDateTime.class),(Boolean)r.getObject("outpatient_arrived"),r.getObject("outpatient_arrival_at",OffsetDateTime.class),r.getString("outpatient_reporter"),r.getString("outpatient_no_show_reason"),r.getObject("latest_nutrition_appointment_at",OffsetDateTime.class),r.getObject("latest_home_rehab_appointment_at",OffsetDateTime.class),r.getObject("latest_follow_up_at",OffsetDateTime.class),planned==null?"未填报":actual==null?"已填报":"已出院",parseAbnormalCodes(r.getString("abnormal_codes")),r.getString("abnormal_reason"),r.getBoolean("is_special_patient"),r.getString("special_reason"),(Boolean)r.getObject("follow_up_required"),r.getString("follow_up_day7"),r.getString("follow_up_day30"),r.getString("follow_up_day60"),r.getString("follow_up_details"),r.getObject("updated_at",OffsetDateTime.class));
    }

    static List<String> parseAbnormalCodes(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
                .map(String::trim).filter(code -> !code.isEmpty()).distinct().toList();
    }

    public DischargeSummaryStats summary(String keyword,Long departmentId,String timeType,OffsetDateTime startAt,
                                         OffsetDateTime endAt,Boolean discharged,DataScope scope) {
        String value=normalizeKeyword(keyword);DischargeTimeType parsed=DischargeTimeType.parse(timeType);validateRange(startAt,endAt);
        String countSql=buildCountSql(buildScopeWhere(scope));
        String filtered="SELECT d.*,e.inpatient_no,e.patient_name,e.doctor_name_source,e.department_id FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id"+countSql.substring(countSql.indexOf(" WHERE "));
        String sql="WITH filtered AS ("+filtered+") SELECT COUNT(*) FILTER (WHERE actual_discharge_at IS NULL) inpatient_count,COUNT(*) FILTER (WHERE planned_discharge_at IS NOT NULL) planned_count,(SELECT COUNT(DISTINCT n.encounter_id) FROM discharge_nutrition_consultation n JOIN filtered f ON f.encounter_id=n.encounter_id WHERE n.deleted=false) nutrition_patient_count,(SELECT COUNT(*) FROM discharge_nutrition_consultation n JOIN filtered f ON f.encounter_id=n.encounter_id WHERE n.deleted=false) nutrition_record_count,(SELECT COUNT(DISTINCT h.encounter_id) FROM discharge_home_rehab_consultation h JOIN filtered f ON f.encounter_id=h.encounter_id WHERE h.deleted=false) home_patient_count,(SELECT COUNT(*) FROM discharge_home_rehab_consultation h JOIN filtered f ON f.encounter_id=h.encounter_id WHERE h.deleted=false) home_record_count,COUNT(*) FILTER (WHERE outpatient_appointment_at IS NOT NULL) outpatient_patient_count FROM filtered";
        var q=jdbc.sql(sql).param("keyword",value).param("pattern","%"+value+"%").param("departmentId",departmentId==null?0:departmentId).param("timeType",parsed==null?"":parsed.name()).param("startAt",startAt).param("endAt",endAt).param("dischargedFilter",discharged==null?2:(discharged?1:0)).param("category","");bindScope(q,scope);
        return q.query((r,n)->new DischargeSummaryStats(r.getLong("inpatient_count"),r.getLong("planned_count"),r.getLong("nutrition_patient_count"),r.getLong("nutrition_record_count"),r.getLong("home_patient_count"),r.getLong("home_record_count"),r.getLong("outpatient_patient_count"))).single();
    }

    public List<DischargeDepartmentOption> filterOptions(DataScope scope) {
        var query=jdbc.sql("SELECT DISTINCT dpt.id,dpt.department_name FROM discharge_record d JOIN patient_encounter e ON e.id=d.encounter_id JOIN sys_department dpt ON dpt.id=e.department_id WHERE dpt.enabled=true"+buildScopeWhere(scope)+" ORDER BY dpt.department_name");
        bindScope(query,scope);
        return query.query((r,n)->new DischargeDepartmentOption(r.getLong("id"),r.getString("department_name"))).list();
    }

    private static void validateRange(OffsetDateTime startAt,OffsetDateTime endAt){if(startAt!=null&&endAt!=null&&!startAt.isBefore(endAt))throw new IllegalArgumentException("开始时间必须早于结束时间");}

    private static String normalizeKeyword(String keyword){return keyword==null?"":keyword.trim();}
    private static String normalizeCategory(String category){
        if(category==null||category.isBlank())return "";
        String value=category.trim().toUpperCase();
        if(!Set.of("BOARD","FOLLOW_UP","NUTRITION","HOME_REHAB","OUTPATIENT","ABNORMAL").contains(value))
            throw new IllegalArgumentException("不支持的统计明细类型");
        return value;
    }
    private static int safePage(int page){return Math.max(1,page);}
    private static int safeSize(int size){return ALLOWED_SIZES.contains(size)?size:50;}
    private static OffsetDateTime parseDate(String value){return value==null||value.isBlank()?null:OffsetDateTime.parse(value);}
}
