package cn.hospital.rehab.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import cn.hospital.rehab.arrears.importer.ArrearsImportRow;
import cn.hospital.rehab.arrears.importer.ArrearsImportService;
import cn.hospital.rehab.discharge.importer.DischargeImportRow;
import cn.hospital.rehab.discharge.importer.DischargeImportService;
import cn.hospital.rehab.common.importing.ImportValidationException;
import cn.hospital.rehab.arrears.push.PushTaskDispatcher;
import com.alibaba.excel.EasyExcel;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest(properties = {
        "app.bootstrap-admin.enabled=true",
        "app.bootstrap-admin.employee-no=ADMIN-TEST",
        "app.bootstrap-admin.wecom-user-id=admin-test"
})
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
@Sql("/sql/stage1-test-data.sql")
class ApplicationPostgresIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kangfu_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor sessionAuth(String username, String password) { return SessionTestClient.login(mvc, username, password); }

    @Autowired JdbcClient jdbc;
    @Autowired MockMvc mvc;
    @Autowired ArrearsImportService arrearsImportService;
    @Autowired DischargeImportService dischargeImportService;
    @Autowired PushTaskDispatcher pushTaskDispatcher;

    @Test
    @Transactional
    void dischargeCareRecordsSupportPatientScopedCrudAndFollowUp() throws Exception {
        long encounterId=jdbc.sql("SELECT id FROM patient_encounter WHERE inpatient_no='TEST-0001'").query(Long.class).single();
        long dischargeId=id("discharge_record","TEST-0001");
        String appointment=OffsetDateTime.now().plusDays(2).withNano(0).toString();
        String created=mvc.perform(post("/api/discharge/consultations").param("encounterId",String.valueOf(encounterId)).param("type","NUTRITION").with(sessionAuth("admin","kfyy123!")).contentType("application/json").content("{\"appointmentAt\":\""+appointment+"\",\"executorName\":\"营养师甲\",\"executionResult\":\"待执行\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.executorName").value("营养师甲")).andReturn().getResponse().getContentAsString();
        long consultationId=new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).path("data").path("id").asLong();
        mvc.perform(get("/api/discharge/consultations").param("encounterId",String.valueOf(encounterId)).param("type","NUTRITION").with(sessionAuth("admin","kfyy123!"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mvc.perform(delete("/api/discharge/consultations/{id}",consultationId).param("type","NUTRITION").with(sessionAuth("admin","kfyy123!"))).andExpect(status().isOk());
        assertThat(jdbc.sql("SELECT deleted FROM discharge_nutrition_consultation WHERE id=:id").param("id",consultationId).query(Boolean.class).single()).isTrue();
        mvc.perform(put("/api/discharge/records/{id}",dischargeId).with(sessionAuth("admin","kfyy123!")).contentType("application/json").content("{\"followUpRequired\":true,\"followUpDay7\":\"恢复良好\",\"abnormalReason\":\"已核对\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.followUpDay7").value("恢复良好"));
    }

    @Test
    void flywayCreatesSchemaAndBootstrapsAdminIdempotently() {
        assertThat(jdbc.sql("SELECT COUNT(*) FROM flyway_schema_history WHERE success=true")
                .query(Long.class).single()).isGreaterThanOrEqualTo(10);
        assertThat(jdbc.sql("SELECT employee_no FROM sys_user WHERE login_name='admin'")
                .query(String.class).single()).isEqualTo("ADMIN-TEST");
        assertThat(jdbc.sql("""
                SELECT COUNT(*) FROM sys_user_role ur
                JOIN sys_user u ON u.id=ur.user_id
                JOIN sys_role r ON r.id=ur.role_id
                WHERE u.login_name='admin' AND r.role_code='SYSTEM_ADMIN'
                """).query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM sys_user WHERE employee_no LIKE 'T-%'")
                .query(Long.class).single()).isEqualTo(5);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM patient_encounter WHERE doctor_match_status='MATCHED'")
                .query(Long.class).single()).isEqualTo(2);
    }

    @Test
    void systemAdministratorAutomaticallyHasEveryEnabledPermission() throws Exception {
        mvc.perform(get("/api/system/me").with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorities", org.hamcrest.Matchers.hasItem("PERM_API_USER_MANAGE")))
                .andExpect(jsonPath("$.data.authorities", org.hamcrest.Matchers.hasItem("PERM_API_ROLE_MANAGE")));
    }

    @Test
    void fullAccessRolesCanQueryAllDepartments() throws Exception {
        for (String user : new String[]{"admin", "test_operations", "test_finance"}) {
            mvc.perform(get("/api/discharge/records").with(sessionAuth(user, "kfyy123!")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2));
            mvc.perform(get("/api/arrears/records").with(sessionAuth(user, "kfyy123!")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(2))
                    .andExpect(jsonPath("$.data.items[0].admittedAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.items[0].prepaidAmount").value(0))
                    .andExpect(jsonPath("$.data.items[0].medicalInsurancePaid").value(0))
                    .andExpect(jsonPath("$.data.items[0].personalAccountPaid").value(0));
        }
    }

    @Test
    void invalidBasicCredentialsReturnJsonWithoutBrowserChallenge() throws Exception {
        mvc.perform(get("/api/system/me").with(httpBasic("missing-user", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", nullValue()))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("登录已失效，请重新登录"));
    }

    @Test
    void departmentDirectorQueryAndExportAreLimitedToAssignedDepartment() throws Exception {
        mvc.perform(get("/api/discharge/records").with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].inpatientNo").value("TEST-0001"));
        mvc.perform(get("/api/discharge/records/export").with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("TEST-0001")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TEST-0002"))));
        mvc.perform(get("/api/arrears/records/export").with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("TEST-0001")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TEST-0002"))));
    }

    @Test
    void attendingDoctorQueryAndExportAreLimitedByDoctorUserId() throws Exception {
        mvc.perform(get("/api/discharge/records").with(sessionAuth("test_doctor_a", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].inpatientNo").value("TEST-0001"));
        mvc.perform(get("/api/arrears/records/export").with(sessionAuth("test_doctor_a", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("TEST-0001")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TEST-0002"))));
    }

    @Test
    void arrearsDoctorEmployeeNumberIsReturnedSearchableExportedAndScoped() throws Exception {
        mvc.perform(get("/api/arrears/records").param("keyword", "T-DOCTOR-A")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].inpatientNo").value("TEST-0001"))
                .andExpect(jsonPath("$.data.items[0].doctorName").value("测试医生甲"))
                .andExpect(jsonPath("$.data.items[0].doctorEmployeeNo").value("T-DOCTOR-A"));

        mvc.perform(get("/api/arrears/records").param("keyword", "T-DOCTOR-A")
                        .with(sessionAuth("test_doctor_b", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mvc.perform(get("/api/arrears/records/export").param("keyword", "T-DOCTOR-A")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("主管医生工号")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("T-DOCTOR-A")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("T-DOCTOR-B"))));
    }

    @Test
    @Transactional
    void scopedRolesRequireFieldPermissionToModifyVisibleDischargeRecords() throws Exception {
        long dischargeId = id("discharge_record", "TEST-0001");
        long arrearsId = id("arrears_record", "TEST-0001");
        mvc.perform(put("/api/discharge/records/{id}", dischargeId)
                        .with(sessionAuth("test_director", "kfyy123!")).contentType("application/json")
                        .content("{\"specialPatient\":false,\"abnormalReason\":\"已核对\"}"))
                .andExpect(status().isForbidden());
        jdbc.sql("""
                INSERT INTO sys_role_permission(role_id,permission_id)
                SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p
                WHERE r.role_code='DEPARTMENT_DIRECTOR' AND p.permission_code='FIELD_ATTENDING_DOCTOR'
                ON CONFLICT DO NOTHING
                """).update();
        mvc.perform(put("/api/discharge/records/{id}", dischargeId)
                        .with(sessionAuth("test_director", "kfyy123!")).contentType("application/json")
                        .content("{\"specialPatient\":false,\"abnormalReason\":\"已核对\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/arrears/records/{id}", arrearsId)
                        .with(sessionAuth("test_doctor_a", "kfyy123!")).contentType("application/json")
                        .content("{\"paymentStatus\":\"UNPAID\",\"arrearsReason\":\"测试原因\",\"recoveryProgress\":\"NEGOTIATING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.recoveryProgress").value("NEGOTIATING"))
                .andExpect(jsonPath("$.data.lastOperatedBy").value("测试医生甲"));
        mvc.perform(put("/api/arrears/records/{id}", arrearsId)
                        .with(sessionAuth("test_doctor_a", "kfyy123!")).contentType("application/json")
                        .content("{\"paymentStatus\":\"PAID\",\"arrearsReason\":\"测试原因\",\"recoveryProgress\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PAID"))
                .andExpect(jsonPath("$.data.recoveryProgress").value("PAID"))
                .andExpect(jsonPath("$.data.previousRecoveryProgress").value("NEGOTIATING"));
        mvc.perform(put("/api/arrears/records/{id}", arrearsId)
                        .with(sessionAuth("test_doctor_a", "kfyy123!")).contentType("application/json")
                        .content("{\"paymentStatus\":\"UNPAID\",\"arrearsReason\":\"测试原因\",\"recoveryProgress\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
                .andExpect(jsonPath("$.data.recoveryProgress").value("NEGOTIATING"));
        assertThat(jdbc.sql("SELECT u.login_name FROM arrears_record a JOIN sys_user u ON u.id=a.last_operated_by WHERE a.id=:id")
                .param("id", arrearsId).query(String.class).single()).isEqualTo("test_doctor_a");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM operation_audit_log WHERE business_type='ARREARS_RECORD' AND business_id=:id AND before_data IS NOT NULL AND after_data IS NOT NULL AND client_ip IS NOT NULL").param("id",String.valueOf(arrearsId)).query(Long.class).single()).isGreaterThanOrEqualTo(1);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM operation_audit_log WHERE business_type='DISCHARGE_RECORD' AND business_id=:id AND before_data IS NOT NULL AND after_data IS NOT NULL AND client_ip IS NOT NULL").param("id",String.valueOf(dischargeId)).query(Long.class).single()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void crossDepartmentAndCrossDoctorModificationsReturn403() throws Exception {
        long dischargeId = id("discharge_record", "TEST-0002");
        long arrearsId = id("arrears_record", "TEST-0002");
        mvc.perform(put("/api/discharge/records/{id}", dischargeId)
                        .with(sessionAuth("test_director", "kfyy123!")).contentType("application/json")
                        .content("{\"specialPatient\":false}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/arrears/records/{id}", arrearsId)
                        .with(sessionAuth("test_doctor_a", "kfyy123!")).contentType("application/json")
                        .content("{\"paymentStatus\":\"UNPAID\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void arrearsUpdateRejectsUnknownRecoveryProgress() throws Exception {
        long arrearsId = id("arrears_record", "TEST-0001");
        mvc.perform(put("/api/arrears/records/{id}", arrearsId)
                        .with(sessionAuth("test_doctor_a", "kfyy123!")).contentType("application/json")
                        .content("{\"paymentStatus\":\"UNPAID\",\"recoveryProgress\":\"随意填写\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsAndAnalysisUseTheSameDataScope() throws Exception {
        mvc.perform(get("/api/arrears/report").with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.people").value(1))
                .andExpect(jsonPath("$.data.scopeType").value("DEPARTMENT"))
                .andExpect(jsonPath("$.data.scopeLabel").value("本科室"))
                .andExpect(jsonPath("$.data.latestSuccessfulBatch.batchNo").value("TEST-ARREARS-READY"))
                .andExpect(jsonPath("$.data.latestSuccessfulBatch.summaryStatus").value("READY"))
                .andExpect(jsonPath("$.data.patientTop10.length()").value(1))
                .andExpect(jsonPath("$.data.patientTop10[0].rank").value(1))
                .andExpect(jsonPath("$.data.patientTop10[0].inpatientNo").value("TEST-0001"));
        mvc.perform(get("/api/arrears/report").with(sessionAuth("test_doctor_a", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.people").value(1))
                .andExpect(jsonPath("$.data.scopeType").value("DOCTOR"))
                .andExpect(jsonPath("$.data.patientTop10[0].doctorName").value("测试医生甲"));
        mvc.perform(get("/api/arrears/report").with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.people").value(2))
                .andExpect(jsonPath("$.data.scopeType").value("ALL"))
                .andExpect(jsonPath("$.data.ranking.length()").value(2))
                .andExpect(jsonPath("$.data.patientTop10.length()").value(2));
        mvc.perform(get("/api/discharge/analysis").param("month", "2026-08")
                        .with(sessionAuth("test_doctor_a", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.dischargeCount").value(1));
    }

    @Test
    @Transactional
    void dischargeAnalysisReturnsFourDeduplicatedCumulativeRates() throws Exception {
        long encounterA = jdbc.sql("SELECT id FROM patient_encounter WHERE inpatient_no='TEST-0001'")
                .query(Long.class).single();
        long encounterB = jdbc.sql("SELECT id FROM patient_encounter WHERE inpatient_no='TEST-0002'")
                .query(Long.class).single();
        jdbc.sql("UPDATE discharge_record SET outpatient_appointment_at=TIMESTAMPTZ '2026-09-01 09:00:00+08' WHERE encounter_id=:id")
                .param("id", encounterA).update();
        jdbc.sql("INSERT INTO discharge_nutrition_consultation(encounter_id,appointment_at) VALUES(:id,TIMESTAMPTZ '2026-07-01 09:00:00+08'),(:id,TIMESTAMPTZ '2026-09-01 09:00:00+08')")
                .param("id", encounterA).update();
        jdbc.sql("INSERT INTO discharge_home_rehab_consultation(encounter_id,appointment_at,deleted) VALUES(:a,TIMESTAMPTZ '2026-08-21 09:00:00+08',false),(:b,TIMESTAMPTZ '2026-08-21 09:00:00+08',true)")
                .param("a", encounterA).param("b", encounterB).update();

        mvc.perform(get("/api/discharge/analysis").param("month", "2026-08")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dischargeCount").value(2))
                .andExpect(jsonPath("$.data.nearDischarge3DayCount").isNumber())
                .andExpect(jsonPath("$.data.currentWeekDischargeCount").isNumber())
                .andExpect(jsonPath("$.data.nutritionCount").value(1))
                .andExpect(jsonPath("$.data.homeRehabCount").value(1))
                .andExpect(jsonPath("$.data.outpatientCount").value(1))
                .andExpect(jsonPath("$.data.nutritionRate").value(50.0))
                .andExpect(jsonPath("$.data.homeRehabRate").value(50.0))
                .andExpect(jsonPath("$.data.outpatientRate").value(50.0))
                .andExpect(jsonPath("$.data.trend.length()").value(31))
                .andExpect(jsonPath("$.data.trend[18].dischargeCount").value(0))
                .andExpect(jsonPath("$.data.trend[18].outpatientRate").value(nullValue()))
                .andExpect(jsonPath("$.data.trend[19].dischargeCount").value(2))
                .andExpect(jsonPath("$.data.trend[19].nutritionCount").value(1))
                .andExpect(jsonPath("$.data.trend[19].homeRehabCount").value(1))
                .andExpect(jsonPath("$.data.trend[19].outpatientCount").value(1))
                .andExpect(jsonPath("$.data.trend[19].nutritionRate").value(50.0))
                .andExpect(jsonPath("$.data.trend[19].homeRehabRate").value(50.0))
                .andExpect(jsonPath("$.data.trend[19].outpatientRate").value(50.0));

        mvc.perform(get("/api/discharge/records").param("category", "NUTRITION")
                        .param("page", "1").param("pageSize", "20").with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.items[0].inpatientNo").value("TEST-0001"));
        mvc.perform(get("/api/discharge/records").param("category", "HOME_REHAB")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/discharge/records").param("category", "OUTPATIENT")
                        .with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].inpatientNo").value("TEST-0001"));
        mvc.perform(get("/api/discharge/records").param("category", "OUTPATIENT")
                        .with(sessionAuth("test_doctor_b", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        mvc.perform(get("/api/discharge/records").param("category", "NUTRITION")
                        .param("timeType", "NUTRITION")
                        .param("startAt", "2026-09-01T00:00:00+08:00")
                        .param("endAt", "2026-09-02T00:00:00+08:00")
                        .param("keyword", "TEST-0001")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/discharge/records/export").param("category", "OUTPATIENT")
                        .param("keyword", "TEST-").with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TEST-0001")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("TEST-0002"))));
    }

    @Test
    void dischargeAnalysisDetailsRejectUnknownCategory() throws Exception {
        mvc.perform(get("/api/discharge/records").param("category", "UNKNOWN")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void operationsCanPreviewAndTriggerDischargeRemindersWithAuditTrail() throws Exception {
        jdbc.sql("""
                UPDATE discharge_record d SET planned_discharge_at=NULL, actual_discharge_at=(CURRENT_DATE - 1) + TIME '10:00'
                FROM patient_encounter e WHERE e.id=d.encounter_id AND e.inpatient_no='TEST-0001'
                """).update();
        mvc.perform(get("/api/discharge/reminders/preview").with(sessionAuth("test_operations", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reminderDate").isNotEmpty())
                .andExpect(jsonPath("$.data.totalPatients").isNumber())
                .andExpect(jsonPath("$.data.items.length()").value(4))
                .andExpect(jsonPath("$.data.items[3].recipientScope").value("患者主管医生"))
                .andExpect(jsonPath("$.data.items[3].triggerBasis").value(org.hamcrest.Matchers.containsString("计划缺失")));
        mvc.perform(post("/api/discharge/reminders/trigger").with(sessionAuth("test_operations", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reminderDate").isNotEmpty())
                .andExpect(jsonPath("$.data.createdTasks").isNumber())
                .andExpect(jsonPath("$.data.message").value("提醒任务已生成；重复任务已自动忽略"));
        assertThat(jdbc.sql("SELECT COUNT(*) FROM operation_audit_log WHERE business_type='DISCHARGE_REMINDER' AND action_type='MANUAL_TRIGGER' AND operator_name='test_operations'")
                .query(Long.class).single()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM push_task WHERE business_type='DISCHARGE' AND reminder_type='UNPLANNED' AND recipient_wecom_id='test-doctor-a' AND reminder_date=CURRENT_DATE")
                .query(Long.class).single()).isEqualTo(1);
        mvc.perform(post("/api/discharge/reminders/trigger").with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dischargeAnalysisUsesNullRatesWhenTheMonthHasNoDischarges() throws Exception {
        mvc.perform(get("/api/discharge/analysis").param("month", "2025-01")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dischargeCount").value(0))
                .andExpect(jsonPath("$.data.unplannedRate").value(nullValue()))
                .andExpect(jsonPath("$.data.nutritionRate").value(nullValue()))
                .andExpect(jsonPath("$.data.homeRehabRate").value(nullValue()))
                .andExpect(jsonPath("$.data.outpatientRate").value(nullValue()))
                .andExpect(jsonPath("$.data.trend[0].outpatientRate").value(nullValue()));
    }

    @Test
    @Transactional
    void reportUsesLatestReadyBatchAndExcludesPaidPatients() throws Exception {
        jdbc.sql("""
                INSERT INTO import_batch(batch_no,business_type,status,total_count,success_count,summary_status,finished_at)
                VALUES ('TEST-ARREARS-PENDING','ARREARS','SUCCESS',1,1,'PENDING',
                        TIMESTAMPTZ '2099-01-01 08:00:00+08')
                """).update();
        jdbc.sql("""
                UPDATE arrears_record
                   SET payment_status='PAID',recovery_progress='PAID'
                 WHERE encounter_id=(SELECT id FROM patient_encounter WHERE inpatient_no='TEST-0002')
                """).update();

        mvc.perform(get("/api/arrears/report").with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestSuccessfulBatch.batchNo").value("TEST-ARREARS-READY"))
                .andExpect(jsonPath("$.data.people").value(1))
                .andExpect(jsonPath("$.data.patientTop10.length()").value(1))
                .andExpect(jsonPath("$.data.patientTop10[0].inpatientNo").value("TEST-0001"));
    }

    @Test
    void attendingDoctorCannotImportDischargeData() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/discharge/import")
                        .file("file", new byte[]{1}).with(sessionAuth("test_doctor_a", "kfyy123!")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dischargeImportRejectsDuplicateKeysBeforeWritingAnything() {
        DischargeImportRow first = dischargeRow("PHASE3-DUP");
        DischargeImportRow duplicate = dischargeRow("PHASE3-DUP");
        long before = countEncounter("PHASE3-DUP");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> dischargeImportService.importFile(
                        workbook("duplicate.xlsx", DischargeImportRow.class, List.of(first, duplicate))))
                .isInstanceOfSatisfying(ImportValidationException.class, exception ->
                {
                    assertThat(exception.getErrors()).anyMatch(error -> "DUPLICATE_KEY_IN_FILE".equals(error.errorCode()));
                    assertThat(exception.getBatchNo()).startsWith("DIS-FAILED-");
                    assertThat(jdbc.sql("SELECT COUNT(*) FROM import_batch_error e JOIN import_batch b ON b.id=e.import_batch_id WHERE b.batch_no=:batch AND e.error_code='DUPLICATE_KEY_IN_FILE'").param("batch",exception.getBatchNo()).query(Long.class).single()).isEqualTo(1);
                });
        assertThat(countEncounter("PHASE3-DUP")).isEqualTo(before);
    }

    @Test
    void auditEndpointUsesSecuredSystemPath() throws Exception {
        mvc.perform(get("/api/system/audit-logs").with(sessionAuth("admin","kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(get("/api/audit/logs").with(sessionAuth("admin","kfyy123!")))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void pushFailureCreatesAttemptAndManualRetryKeepsHistory() throws Exception {
        long task=jdbc.sql("INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_name,content,status,scheduled_at) VALUES('ARREARS','PHASE3_TEST',CURRENT_DATE,'测试接收人','测试消息','PENDING',CURRENT_TIMESTAMP) RETURNING id").query(Long.class).single();
        pushTaskDispatcher.dispatch();
        assertThat(jdbc.sql("SELECT status FROM push_task WHERE id=:id").param("id",task).query(String.class).single()).isEqualTo("RETRYING");
        assertThat(jdbc.sql("SELECT trigger_type||'|'||error_code FROM push_attempt WHERE task_id=:id AND attempt_no=1").param("id",task).query(String.class).single()).isEqualTo("AUTOMATIC|CONFIG_MISSING");
        jdbc.sql("UPDATE push_task SET status='FAILED' WHERE id=:id").param("id",task).update();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/arrears/push-records/{id}/retry",task).with(sessionAuth("admin","kfyy123!"))).andExpect(status().isOk());
        pushTaskDispatcher.dispatch();
        assertThat(jdbc.sql("SELECT trigger_type FROM push_attempt WHERE task_id=:id AND attempt_no=2").param("id",task).query(String.class).single()).isEqualTo("MANUAL");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM operation_audit_log WHERE business_type='PUSH_TASK' AND business_id=:id AND action_type='MANUAL_RETRY'").param("id",String.valueOf(task)).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @Transactional
    void pushRecordsRespectDepartmentScopeAndHideLegacyUnresolvedTasks() throws Exception {
        long departmentA = jdbc.sql("SELECT id FROM sys_department WHERE department_code='TEST-A'")
                .query(Long.class).single();
        long departmentB = jdbc.sql("SELECT id FROM sys_department WHERE department_code='TEST-B'")
                .query(Long.class).single();
        long visibleTask = scopedPushTask("PHASE7_SCOPE_A", "DEPARTMENT", "FAILED");
        long hiddenTask = scopedPushTask("PHASE7_SCOPE_B", "DEPARTMENT", "FAILED");
        scopedPushTask("PHASE7_LEGACY", "LEGACY_UNRESOLVED", "FAILED");
        jdbc.sql("INSERT INTO push_task_scope(task_id,department_id) VALUES(:task,:department)")
                .param("task", visibleTask).param("department", departmentA).update();
        jdbc.sql("INSERT INTO push_task_scope(task_id,department_id) VALUES(:task,:department)")
                .param("task", hiddenTask).param("department", departmentB).update();

        mvc.perform(get("/api/arrears/push-records").param("businessType", "ARREARS")
                        .with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(visibleTask));
        mvc.perform(get("/api/arrears/push-records/{id}/attempts", hiddenTask)
                        .with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/arrears/push-records").param("businessType", "ARREARS")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3));
    }

    @Test
    @Transactional
    void pushBatchRetryHandlesDuplicatesAndNonFailedTasksWithoutBroadeningSelection() throws Exception {
        long firstFailed = scopedPushTask("PHASE7_BATCH_1", "ALL", "FAILED");
        long secondFailed = scopedPushTask("PHASE7_BATCH_2", "ALL", "FAILED");
        long sent = scopedPushTask("PHASE7_BATCH_SENT", "ALL", "SENT");
        long unselected = scopedPushTask("PHASE7_BATCH_UNSELECTED", "ALL", "FAILED");

        mvc.perform(post("/api/arrears/push-records/retry-batch")
                        .with(sessionAuth("admin", "kfyy123!"))
                        .contentType("application/json")
                        .content("{\"businessType\":\"ARREARS\",\"ids\":[" + firstFailed + "," + secondFailed
                                + "," + sent + "," + firstFailed + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedCount").value(4))
                .andExpect(jsonPath("$.data.successCount").value(2))
                .andExpect(jsonPath("$.data.skippedCount").value(2))
                .andExpect(jsonPath("$.data.failedCount").value(0));

        assertThat(jdbc.sql("SELECT status FROM push_task WHERE id=:id").param("id", firstFailed)
                .query(String.class).single()).isEqualTo("PENDING");
        assertThat(jdbc.sql("SELECT status FROM push_task WHERE id=:id").param("id", secondFailed)
                .query(String.class).single()).isEqualTo("PENDING");
        assertThat(jdbc.sql("SELECT status FROM push_task WHERE id=:id").param("id", sent)
                .query(String.class).single()).isEqualTo("SENT");
        assertThat(jdbc.sql("SELECT status FROM push_task WHERE id=:id").param("id", unselected)
                .query(String.class).single()).isEqualTo("FAILED");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM operation_audit_log WHERE action_type='BATCH_MANUAL_RETRY' AND business_id IN (:first,:second)")
                .param("first", String.valueOf(firstFailed)).param("second", String.valueOf(secondFailed))
                .query(Long.class).single()).isEqualTo(2);
    }

    @Test
    @Transactional
    void scopedViewerCannotRetryPushRecordsWithoutRetryPermission() throws Exception {
        long task = scopedPushTask("PHASE7_PERMISSION", "DEPARTMENT", "FAILED");
        long departmentA = jdbc.sql("SELECT id FROM sys_department WHERE department_code='TEST-A'")
                .query(Long.class).single();
        jdbc.sql("INSERT INTO push_task_scope(task_id,department_id) VALUES(:task,:department)")
                .param("task", task).param("department", departmentA).update();

        mvc.perform(post("/api/arrears/push-records/{id}/retry", task)
                        .with(sessionAuth("test_director", "kfyy123!")))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    void arrearsRepeatedImportPreservesManualFieldsAndReportsOverwrite() {
        ensureFeeCoefficient("PHASE3", "PHASE3", "1.5");
        ArrearsImportRow row = arrearsRow("PHASE3-ARR");
        var first = arrearsImportService.importFile(workbook("arrears-first.xlsx", ArrearsImportRow.class, List.of(row)));
        assertThat(first.added()).isEqualTo(1);
        jdbc.sql("UPDATE arrears_record SET payment_status='PAID',arrears_reason='人工原因',recovery_progress='PAID',previous_recovery_progress='NEGOTIATING' WHERE encounter_id=(SELECT id FROM patient_encounter WHERE inpatient_no='PHASE3-ARR' AND admission_times=1)").update();
        row.patientName = "阶段三患者更新"; row.totalCost = null;
        var second = arrearsImportService.importFile(workbook("arrears-second.xlsx", ArrearsImportRow.class, List.of(row)));
        assertThat(second.overwritten()).isEqualTo(1);
        var preserved = jdbc.sql("SELECT payment_status||'|'||arrears_reason||'|'||recovery_progress FROM arrears_record WHERE encounter_id=(SELECT id FROM patient_encounter WHERE inpatient_no='PHASE3-ARR' AND admission_times=1)").query(String.class).single();
        assertThat(preserved).isEqualTo("PAID|人工原因|PAID");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM import_batch WHERE business_type='ARREARS' AND status='SUCCESS' AND summary_status='READY' AND batch_no IN (:a,:b)").param("a",first.batchNo()).param("b",second.batchNo()).query(Long.class).single()).isEqualTo(2);
    }

    @Test
    @Transactional
    void arrearsImportCovers999To1001BoundariesAndLargeQueries() throws Exception {
        ensureFeeCoefficient("PHASE6", "PHASE6", "1.0");
        List<ArrearsImportRow> belowLimitRows = IntStream.rangeClosed(1, 999)
                .mapToObj(i -> arrearsCapacityRow("S6-ARR-999-" + i)).toList();
        var belowLimit = arrearsImportService.importFile(workbook("arrears-999.xlsx", ArrearsImportRow.class, belowLimitRows));
        assertThat(belowLimit.total()).isEqualTo(999);
        assertThat(belowLimit.success()).isEqualTo(999);

        List<ArrearsImportRow> acceptedRows = IntStream.rangeClosed(1, 1000)
                .mapToObj(i -> arrearsCapacityRow("S6-ARR-" + i)).toList();
        long startedAt = System.nanoTime();
        var accepted = arrearsImportService.importFile(workbook("arrears-1000.xlsx", ArrearsImportRow.class, acceptedRows));
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        assertThat(accepted.total()).isEqualTo(1000);
        assertThat(accepted.success()).isEqualTo(1000);
        assertThat(accepted.added()).isEqualTo(1000);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM patient_encounter WHERE inpatient_no LIKE 'S6-ARR-%'").query(Long.class).single()).isEqualTo(1999);

        long queryStartedAt = System.nanoTime();
        mvc.perform(get("/api/arrears/records").param("page", "1").param("pageSize", "200")
                        .param("keyword", "S6-ARR-").with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(1999));
        long queryMillis = (System.nanoTime() - queryStartedAt) / 1_000_000;
        long exportStartedAt = System.nanoTime();
        mvc.perform(get("/api/arrears/records/export").param("keyword", "S6-ARR-")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("S6-ARR-1000")));
        long exportMillis = (System.nanoTime() - exportStartedAt) / 1_000_000;
        long reportStartedAt = System.nanoTime();
        mvc.perform(get("/api/arrears/report").with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latestSuccessfulBatch.batchNo").value(accepted.batchNo()))
                .andExpect(jsonPath("$.data.people").value(1000))
                .andExpect(jsonPath("$.data.patientTop10.length()").value(10));
        long reportMillis = (System.nanoTime() - reportStartedAt) / 1_000_000;

        List<ArrearsImportRow> rejectedRows = IntStream.rangeClosed(1, 1001)
                .mapToObj(i -> arrearsCapacityRow("S6-ARR-REJECT-" + i)).toList();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> arrearsImportService.importFile(
                        workbook("arrears-1001.xlsx", ArrearsImportRow.class, rejectedRows)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最多1000条");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM patient_encounter WHERE inpatient_no LIKE 'S6-ARR-REJECT-%'").query(Long.class).single()).isZero();
        System.out.printf("PHASE6_METRIC arrears_import_1000_ms=%d arrears_query_1999_ms=%d arrears_export_1999_ms=%d arrears_report_1999_ms=%d%n", elapsedMillis, queryMillis, exportMillis, reportMillis);
    }

    @Test
    @Transactional
    void dischargeImportCovers4999To5001BoundariesAndLargeQueries() throws Exception {
        List<DischargeImportRow> belowLimitRows = IntStream.rangeClosed(1, 4999)
                .mapToObj(i -> dischargeRow("S6-DIS-4999-" + i)).toList();
        var belowLimit = dischargeImportService.importFile(workbook("discharge-4999.xlsx", DischargeImportRow.class, belowLimitRows));
        assertThat(belowLimit.total()).isEqualTo(4999);
        assertThat(belowLimit.success()).isEqualTo(4999);

        List<DischargeImportRow> acceptedRows = IntStream.rangeClosed(1, 5000)
                .mapToObj(i -> dischargeRow("S6-DIS-" + i)).toList();
        long startedAt = System.nanoTime();
        var accepted = dischargeImportService.importFile(workbook("discharge-5000.xlsx", DischargeImportRow.class, acceptedRows));
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        assertThat(accepted.total()).isEqualTo(5000);
        assertThat(accepted.success()).isEqualTo(5000);
        assertThat(accepted.added()).isEqualTo(5000);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM patient_encounter WHERE inpatient_no LIKE 'S6-DIS-%'").query(Long.class).single()).isEqualTo(9999);

        long queryStartedAt = System.nanoTime();
        mvc.perform(get("/api/discharge/records").param("page", "1").param("pageSize", "200")
                        .param("keyword", "S6-DIS-").with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(9999));
        long queryMillis = (System.nanoTime() - queryStartedAt) / 1_000_000;
        long exportStartedAt = System.nanoTime();
        mvc.perform(get("/api/discharge/records/export").param("keyword", "S6-DIS-")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("S6-DIS-1000")));
        long exportMillis = (System.nanoTime() - exportStartedAt) / 1_000_000;
        long analysisStartedAt = System.nanoTime();
        mvc.perform(get("/api/discharge/analysis").param("month", "2026-09")
                        .with(sessionAuth("admin", "kfyy123!")))
                .andExpect(status().isOk());
        long analysisMillis = (System.nanoTime() - analysisStartedAt) / 1_000_000;

        List<DischargeImportRow> rejectedRows = IntStream.rangeClosed(1, 5001)
                .mapToObj(i -> dischargeRow("S6-DIS-REJECT-" + i)).toList();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> dischargeImportService.importFile(
                        workbook("discharge-5001.xlsx", DischargeImportRow.class, rejectedRows)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("最多5000条");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM patient_encounter WHERE inpatient_no LIKE 'S6-DIS-REJECT-%'").query(Long.class).single()).isZero();
        System.out.printf("PHASE6_METRIC discharge_import_5000_ms=%d discharge_query_9999_ms=%d discharge_export_9999_ms=%d discharge_analysis_9999_ms=%d%n", elapsedMillis, queryMillis, exportMillis, analysisMillis);
    }

    @Test
    void oneHundredConcurrentAuthenticatedCoreQueriesSucceed() throws Exception {
        var executor = Executors.newFixedThreadPool(20);
        try {
            List<Callable<Integer>> requests = IntStream.range(0, 100)
                    .mapToObj(i -> (Callable<Integer>) () -> mvc.perform(get(i % 2 == 0 ? "/api/arrears/records" : "/api/discharge/records")
                                    .param("page", "1").param("pageSize", "20")
                                    .with(sessionAuth("admin", "kfyy123!")))
                            .andReturn().getResponse().getStatus())
                    .toList();
            long startedAt = System.nanoTime();
            var results = executor.invokeAll(requests);
            long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
            for (var result : results) assertThat(result.get()).isEqualTo(200);
            System.out.printf("PHASE6_METRIC authenticated_core_queries_100_ms=%d%n", elapsedMillis);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @Transactional
    void fiftyRepeatedImportsPreserveManualFieldsAndComplete() {
        ensureFeeCoefficient("PHASE6", "PHASE6", "1.0");
        ArrearsImportRow row = arrearsCapacityRow("S6-REPEAT-50");
        var file = workbook("arrears-repeat-50.xlsx", ArrearsImportRow.class, List.of(row));
        var first = arrearsImportService.importFile(file);
        jdbc.sql("UPDATE arrears_record SET payment_status='PAID',arrears_reason='容量测试人工原因',recovery_progress='PAID',previous_recovery_progress='LEGAL_ACTION' WHERE encounter_id=(SELECT id FROM patient_encounter WHERE inpatient_no='S6-REPEAT-50' AND admission_times=1)").update();
        long startedAt = System.nanoTime();
        for (int i = 1; i < 50; i++) {
            row.patientName = "阶段六重复患者" + i;
            var result = arrearsImportService.importFile(file);
            assertThat(result.overwritten()).isEqualTo(1);
        }
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        assertThat(first.added()).isEqualTo(1);
        assertThat(jdbc.sql("SELECT payment_status||'|'||arrears_reason||'|'||recovery_progress FROM arrears_record WHERE encounter_id=(SELECT id FROM patient_encounter WHERE inpatient_no='S6-REPEAT-50' AND admission_times=1)").query(String.class).single())
                .isEqualTo("PAID|容量测试人工原因|PAID");
        assertThat(jdbc.sql("SELECT COUNT(*) FROM import_batch WHERE business_type='ARREARS' AND original_filename='arrears-repeat-50.xlsx' AND status='SUCCESS'").query(Long.class).single()).isEqualTo(50);
        System.out.printf("PHASE6_METRIC arrears_repeated_imports_49_ms=%d%n", elapsedMillis);
    }

    @Test
    @Transactional
    void oneThousandPushTasksAreSafelyProcessedInBatchesOfOneHundred() {
        jdbc.sql("""
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_name,content,status,scheduled_at)
                SELECT 'ARREARS','PHASE6_CAPACITY',CURRENT_DATE,'容量接收人','容量消息','PENDING',CURRENT_TIMESTAMP
                  FROM generate_series(1,1000)
                """).update();
        long startedAt = System.nanoTime();
        pushTaskDispatcher.dispatch();
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        assertThat(jdbc.sql("SELECT COUNT(*) FROM push_task WHERE reminder_type='PHASE6_CAPACITY' AND status='RETRYING'").query(Long.class).single()).isEqualTo(100);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM push_task WHERE reminder_type='PHASE6_CAPACITY' AND status='PENDING'").query(Long.class).single()).isEqualTo(900);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM push_attempt a JOIN push_task t ON t.id=a.task_id WHERE t.reminder_type='PHASE6_CAPACITY' AND a.attempt_no=1 AND a.error_code='CONFIG_MISSING'").query(Long.class).single()).isEqualTo(100);
        System.out.printf("PHASE6_METRIC push_queue_1000_first_batch_ms=%d%n", elapsedMillis);
    }

    private void ensureFeeCoefficient(String code, String name, String coefficient) {
        long feeTypeId = jdbc.sql("INSERT INTO sys_fee_type(fee_code,fee_name) VALUES(:code,:name) ON CONFLICT DO NOTHING RETURNING id")
                .param("code", code).param("name", name).query(Long.class).optional()
                .orElseGet(() -> jdbc.sql("SELECT id FROM sys_fee_type WHERE fee_code=:code").param("code", code).query(Long.class).single());
        jdbc.sql("INSERT INTO sys_fee_coefficient(fee_type_id,fee_type,coefficient,enabled) SELECT :typeId,:name,CAST(:coefficient AS numeric),TRUE WHERE NOT EXISTS(SELECT 1 FROM sys_fee_coefficient WHERE fee_type_id=:typeId AND enabled=true)")
                .param("typeId", feeTypeId).param("name", name).param("coefficient", coefficient).update();
    }

    private long scopedPushTask(String reminderType, String scopeType, String status) {
        return jdbc.sql("""
                INSERT INTO push_task(business_type,reminder_type,reminder_date,recipient_name,content,
                                      scope_type,status,scheduled_at)
                VALUES('ARREARS',:reminderType,CURRENT_DATE,'阶段七接收人','阶段七推送内容',
                       :scopeType,:status,CURRENT_TIMESTAMP)
                RETURNING id
                """).param("reminderType", reminderType).param("scopeType", scopeType)
                .param("status", status).query(Long.class).single();
    }

    private static DischargeImportRow dischargeRow(String no) { DischargeImportRow r=new DischargeImportRow();r.inpatientNo=no;r.admissionTimes=1;r.patientName="阶段三患者";r.wardName="测试康复一科";r.feeType="TEST";return r; }
    private static ArrearsImportRow arrearsRow(String no) { ArrearsImportRow r=new ArrearsImportRow();r.inpatientNo=no;r.admissionTimes=1;r.patientName="阶段三患者";r.wardName="测试康复一科";r.feeType="PHASE3";r.prepaidAmount="100";r.originalRequiredDeposit="200";r.totalCost="500";return r; }
    private static ArrearsImportRow arrearsCapacityRow(String no) { ArrearsImportRow r=arrearsRow(no);r.patientName="阶段六容量患者";r.feeType="PHASE6";return r; }
    private static <T> MockMultipartFile workbook(String name,Class<T> type,List<T> rows){
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        if(type==DischargeImportRow.class){
            List<List<String>> head=List.of("住院号","住院次数","姓名","住院病区","费别","主管医生","主管医生工号","入区日期","预计出院时间","实际出院时间").stream().map(List::of).toList();
            List<List<Object>> data=rows.stream().map(value->{DischargeImportRow r=(DischargeImportRow)value;return List.<Object>of(r.inpatientNo,r.admissionTimes,r.patientName,r.wardName,r.feeType==null?"":r.feeType,r.doctorName==null?"":r.doctorName,r.doctorEmployeeNo==null?"":r.doctorEmployeeNo,r.admittedAt==null?"":r.admittedAt,r.plannedDischargeAt==null?"":r.plannedDischargeAt,r.actualDischargeAt==null?"":r.actualDischargeAt);}).toList();
            EasyExcel.write(out).head(head).sheet().doWrite(data);
        }else{
            List<List<String>> head=List.of("住院号","住院次数","姓名","住院病区","费别","欠费类型","主管医生","主管医生工号","入区日期","出区日期","总费用","预交金（元）","医保支付（元）","个人账户支付（元）","原始应交押金（元）").stream().map(List::of).toList();
            List<List<Object>> data=rows.stream().map(value->{ArrearsImportRow r=(ArrearsImportRow)value;return List.<Object>of(r.inpatientNo,r.admissionTimes,r.patientName,r.wardName,r.feeType,r.arrearsType==null?"":r.arrearsType,r.doctorName==null?"":r.doctorName,r.doctorEmployeeNo==null?"":r.doctorEmployeeNo,r.admittedAt==null?"":r.admittedAt,r.dischargedAt==null?"":r.dischargedAt,r.totalCost==null?"":r.totalCost,r.prepaidAmount,r.medicalInsurancePaid==null?"":r.medicalInsurancePaid,r.personalAccountPaid==null?"":r.personalAccountPaid,r.originalRequiredDeposit);}).toList();
            EasyExcel.write(out).head(head).sheet().doWrite(data);
        }
        return new MockMultipartFile("file",name,"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",out.toByteArray());
    }
    private long countEncounter(String no){return jdbc.sql("SELECT COUNT(*) FROM patient_encounter WHERE inpatient_no=:no").param("no",no).query(Long.class).single();}

    private long id(String table, String inpatientNo) {
        return jdbc.sql("SELECT r.id FROM " + table + " r JOIN patient_encounter e ON e.id=r.encounter_id WHERE e.inpatient_no=:no")
                .param("no", inpatientNo).query(Long.class).single();
    }
}
