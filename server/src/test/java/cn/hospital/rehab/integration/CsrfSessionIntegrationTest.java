package cn.hospital.rehab.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;

@SpringBootTest(properties = {"app.bootstrap-admin.enabled=true", "app.bootstrap-admin.employee-no=CSRF-ADMIN", "app.bootstrap-admin.wecom-user-id=csrf-admin"})
@Testcontainers
@AutoConfigureMockMvc
@Sql("/sql/stage1-test-data.sql")
@Transactional
class CsrfSessionIntegrationTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl); r.add("spring.datasource.username", POSTGRES::getUsername); r.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    private SessionTestClient admin() throws Exception { return new SessionTestClient(mvc).login("admin", "kfyy123"); }
    private Map<String, String> state() {
        Map<String, String> state = new TreeMap<>();
        for (String table : List.of("arrears_record", "discharge_record", "sys_user", "sys_role_permission", "sys_user_role", "operation_audit_log")) {
            state.put(table, jdbc.sql("SELECT COALESCE(string_agg(row_to_json(t)::text, ',' ORDER BY row_to_json(t)::text), '') FROM " + table + " t").query(String.class).single());
        }
        return state;
    }
    @Test void missingAndInvalidTokensRejectEveryWriteFamilyWithoutMutation() throws Exception {
        var client = admin();
        var before = state();
        String[][] endpoints = {
            {"PUT", "/api/arrears/records/1"}, {"PUT", "/api/discharge/records/1"},
            {"POST", "/api/discharge/consultations"}, {"PUT", "/api/discharge/consultations/1"}, {"DELETE", "/api/discharge/consultations/1"},
            {"POST", "/api/arrears/import"}, {"POST", "/api/discharge/import"},
            {"POST", "/api/arrears/push-records/1/retry"}, {"POST", "/api/arrears/push-records/retry-batch"},
            {"POST", "/api/discharge/reminders/trigger"}, {"POST", "/api/system/users"},
            {"POST", "/api/system/users/1/disable"}, {"POST", "/api/system/users/1/enable"},
            {"POST", "/api/system/users/1/reset-password"}, {"POST", "/api/system/users/1/unlock"},
            {"POST", "/api/system/users/me/change-password"}, {"PUT", "/api/system/users/1/roles"},
            {"PUT", "/api/system/permissions/roles/1"}, {"PUT", "/api/system/permissions/roles/1/scope"},
            {"PUT", "/api/system/permissions/roles/1/departments"},
            {"POST", "/api/system/departments"}, {"POST", "/api/system/departments/1/enable"}, {"POST", "/api/system/departments/1/disable"},
            {"POST", "/api/system/fee-coefficients"}, {"POST", "/api/system/fee-coefficients/1/enable"}, {"POST", "/api/system/fee-coefficients/1/disable"},
            {"POST", "/api/auth/logout"}
        };
        for (var endpoint : endpoints) {
            for (boolean wrong : List.of(false, true)) {
                var request = request(org.springframework.http.HttpMethod.valueOf(endpoint[0]), endpoint[1]).session(client.session);
                if (wrong) request.header(client.header, "incorrect");
                mvc.perform(request).andExpect(status().isForbidden()).andExpect(jsonPath("$.data.code").value("CSRF_INVALID"));
            }
        }
        assertThat(state()).isEqualTo(before);
    }
    @Test void loginRequiresTokenAndOldPreLoginTokenIsRejectedAfterAuthentication() throws Exception {
        var client = new SessionTestClient(mvc);
        mvc.perform(post("/api/auth/login").session(client.session).param("username", "admin").param("password", "kfyy123"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.data.code").value("CSRF_INVALID"));
        String old = client.token;
        client.login("admin", "kfyy123");
        mvc.perform(post("/api/auth/logout").session(client.session).header(client.header, old))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").with(client.auth())).andExpect(status().isOk());
        mvc.perform(put("/api/arrears/records/1").session(client.session).header(client.header, client.token))
                .andExpect(status().isUnauthorized());
    }
    @Test void queriesNeedNoTokenAndDoNotChangeBusinessState() throws Exception {
        var client = admin(); var before = state();
        for (String path : List.of("/api/arrears/records", "/api/discharge/records", "/api/system/me"))
            mvc.perform(get(path).session(client.session)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/csrf").session(client.session)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        assertThat(state()).isEqualTo(before);
    }
    @Test void untrustedOriginsAndCrossSiteFormsCannotLogoutEvenWithToken() throws Exception {
        var client = admin();
        for (String source : List.of("https://evil.example", "http://evil.localhost", "null", "http://localhost:9999"))
            mvc.perform(post("/api/auth/logout").with(client.auth()).header("Origin", source))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.data.code").value("ORIGIN_DENIED"));
        mvc.perform(post("/api/auth/logout").with(client.auth()).header("Referer", "https://evil.example/page")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").with(client.auth()).header("Sec-Fetch-Site", "cross-site")).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").with(client.auth()).header("Origin", "http://localhost")).andExpect(status().isOk());
    }
    @Test void validBasicCredentialsNeverAuthenticateOrRestoreExpiredSession() throws Exception {
        var client = admin(); client.session.invalidate();
        mvc.perform(get("/api/system/me").with(httpBasic("admin", "kfyy123")))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(put("/api/arrears/records/1").with(httpBasic("admin", "kfyy123")).header(client.header, client.token))
                .andExpect(status().isUnauthorized());
    }
    @Test void disablingAccountInvalidatesExistingSession() throws Exception {
        var client = admin();
        jdbc.sql("UPDATE sys_user SET enabled=false WHERE login_name='admin'").update();
        mvc.perform(get("/api/system/me").session(client.session)).andExpect(status().isUnauthorized());
        assertThat(client.session.isInvalid()).isTrue();
    }
    @Test void passwordChangeInvalidatesExistingSession() throws Exception {
        var client = admin();
        jdbc.sql("UPDATE sys_user SET password_hash='changed-hash' WHERE login_name='admin'").update();
        mvc.perform(put("/api/arrears/records/1").with(client.auth())).andExpect(status().isUnauthorized());
        assertThat(client.session.isInvalid()).isTrue();
    }
    @Test void roleRevocationTakesEffectWithoutReloginEvenWithValidToken() throws Exception {
        var client = admin();
        jdbc.sql("DELETE FROM sys_user_role WHERE user_id=(SELECT id FROM sys_user WHERE login_name='admin')").update();
        mvc.perform(put("/api/arrears/records/1").with(client.auth())).andExpect(status().isForbidden()).andExpect(jsonPath("$.data.code").value("ACCESS_DENIED"));
        mvc.perform(get("/api/system/me").session(client.session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authorities", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("ROLE_SYSTEM_ADMIN"))));
    }
}
