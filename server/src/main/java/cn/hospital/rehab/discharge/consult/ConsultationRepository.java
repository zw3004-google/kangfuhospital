package cn.hospital.rehab.discharge.consult;

import cn.hospital.rehab.common.security.DataScope;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Repository
public class ConsultationRepository {
    private final JdbcClient jdbc;

    public ConsultationRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<ConsultationRecord> list(long encounterId, String type, DataScope scope) {
        var query = jdbc.sql("SELECT c.*,e.inpatient_no,e.patient_name FROM " + table(type) +
                        " c JOIN patient_encounter e ON e.id=c.encounter_id WHERE c.encounter_id=:encounter " +
                        "AND c.deleted=false" + scopeSql() + " ORDER BY c.appointment_at")
                .param("encounter", encounterId);
        return bindScope(query, scope).query(this::map).list();
    }

    public ConsultationRecord create(long encounterId, String type, OffsetDateTime at,String executorName,String executionResult, DataScope scope) {
        assertEncounterVisible(encounterId, scope);
        long id = jdbc.sql("INSERT INTO " + table(type) + "(encounter_id,appointment_at,executor_name,execution_result) VALUES (:encounter,:at,:executor,:result) RETURNING id")
                .param("encounter", encounterId).param("at", at).param("executor",clean(executorName)).param("result",clean(executionResult)).query(Long.class).single();
        return find(id, type, scope);
    }

    public ConsultationRecord update(long id, String type, OffsetDateTime at,String executorName,String executionResult, DataScope scope) {
        var query = jdbc.sql("UPDATE " + table(type) + " c SET appointment_at=:at,executor_name=:executor,execution_result=:result,updated_at=CURRENT_TIMESTAMP " +
                        "FROM patient_encounter e WHERE c.encounter_id=e.id AND c.id=:id AND c.deleted=false" + scopeSql())
                .param("at", at).param("executor",clean(executorName)).param("result",clean(executionResult)).param("id", id);
        if (bindScope(query, scope).update() == 0) throw denied();
        return find(id, type, scope);
    }

    public void delete(long id, String type, DataScope scope) {
        var query = jdbc.sql("UPDATE " + table(type) + " c SET deleted=true,updated_at=CURRENT_TIMESTAMP " +
                        "FROM patient_encounter e WHERE c.encounter_id=e.id AND c.id=:id AND c.deleted=false" + scopeSql())
                .param("id", id);
        if (bindScope(query, scope).update() == 0) throw denied();
    }

    private void assertEncounterVisible(long encounterId, DataScope scope) {
        var query = jdbc.sql("SELECT COUNT(*) FROM patient_encounter e WHERE e.id=:id" + scopeSql()).param("id", encounterId);
        if (bindScope(query, scope).query(Long.class).single() == 0) throw denied();
    }

    public ConsultationRecord find(long id, String type, DataScope scope) {
        var query = jdbc.sql("SELECT c.*,e.inpatient_no,e.patient_name FROM " + table(type) +
                        " c JOIN patient_encounter e ON e.id=c.encounter_id WHERE c.id=:id" + scopeSql()).param("id", id);
        return bindScope(query, scope).query(this::map).optional().orElseThrow(ConsultationRepository::denied);
    }

    private ConsultationRecord map(java.sql.ResultSet r, int row) throws java.sql.SQLException {
        return new ConsultationRecord(r.getLong("id"), r.getLong("encounter_id"), r.getString("inpatient_no"),
                r.getString("patient_name"), r.getObject("appointment_at", OffsetDateTime.class),r.getString("executor_name"),r.getString("execution_result"), r.getBoolean("deleted"));
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

    private static AccessDeniedException denied() { return new AccessDeniedException("无权访问该患者数据"); }
    private static String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private static String table(String type) {
        if ("NUTRITION".equalsIgnoreCase(type)) return "discharge_nutrition_consultation";
        if ("HOME".equalsIgnoreCase(type)) return "discharge_home_rehab_consultation";
        throw new IllegalArgumentException("会诊类型不正确");
    }
}
