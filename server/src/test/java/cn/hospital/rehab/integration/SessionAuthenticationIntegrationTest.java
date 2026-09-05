package cn.hospital.rehab.integration;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.bootstrap-admin.enabled=true",
        "app.bootstrap-admin.employee-no=ADMIN-H5-TEST",
        "app.bootstrap-admin.wecom-user-id=admin-h5-test"
})
@Testcontainers(disabledWithoutDocker = true)
@org.springframework.context.annotation.Import(SessionAuthenticationIntegrationTest.TimeoutConfig.class)
@AutoConfigureMockMvc
class SessionAuthenticationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kangfu_h5_test").withUsername("test").withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @org.springframework.boot.test.web.server.LocalServerPort int port;

    @org.springframework.boot.test.context.TestConfiguration
    static class TimeoutConfig {
        @org.springframework.context.annotation.Bean TimeoutController timeoutController() { return new TimeoutController(); }
    }
    @org.springframework.web.bind.annotation.RestController
    static class TimeoutController {
        @org.springframework.web.bind.annotation.PostMapping("/api/test/session-timeout")
        void shorten(jakarta.servlet.http.HttpServletRequest request) { request.getSession().setMaxInactiveInterval(1); }
    }
    @Test
    void actualCookieSessionExpiresAndCannotWrite() throws Exception {
        var cookies = new java.net.CookieManager(null, java.net.CookiePolicy.ACCEPT_ALL);
        var client = java.net.http.HttpClient.newBuilder().cookieHandler(cookies).build();
        String base = "http://localhost:" + port;
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var first = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/auth/csrf")).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(first.statusCode()).isEqualTo(200);
        assertThat(first.headers().firstValue("set-cookie").orElseThrow()).contains("HttpOnly", "SameSite=Strict");
        var token = json.readTree(first.body()).path("data");
        var login = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/auth/login"))
                .header(token.path("headerName").asText(), token.path("token").asText())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("username=admin&password=kfyy123!")).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(200);
        var next = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/auth/csrf")).GET().build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        token = json.readTree(next.body()).path("data");
        var shortened = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/test/session-timeout"))
                .header(token.path("headerName").asText(), token.path("token").asText()).POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(shortened.statusCode()).isEqualTo(200);
        Thread.sleep(1500);
        var expired = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/api/arrears/records/1"))
                .header(token.path("headerName").asText(), token.path("token").asText()).PUT(java.net.http.HttpRequest.BodyPublishers.ofString("{}" )).build(), java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(expired.statusCode()).isEqualTo(401);
        assertThat(json.readTree(expired.body()).path("data").path("code").asText()).isEqualTo("AUTH_REQUIRED");
    }


    @Test
    void browserCanLoginUseSessionAndLogoutWithoutPersistingBasicCredentials() throws Exception {
        var client = new SessionTestClient(mvc);
        String oldId = client.session.getId();
        var login = mvc.perform(post("/api/auth/login").with(client.auth())
                        .param("username", "admin")
                        .param("password", "kfyy123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNotEqualTo(oldId);
        client.session = session;
        client.refresh();

        mvc.perform(get("/api/system/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginName").value("admin"));

        var logout = mvc.perform(post("/api/auth/logout").with(client.auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        assertThat(logout.getResponse().getCookies())
                .extracting(Cookie::getName)
                .contains("JSESSIONID");

        mvc.perform(get("/api/system/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responsesIncludeMobileSecurityHeaders() throws Exception {
        mvc.perform(get("/api/system/info"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
                .andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
    }

    @Test
    void invalidCredentialsReturnConsistentJsonError() throws Exception {
        mvc.perform(post("/api/auth/login").with(new SessionTestClient(mvc).auth())
                        .param("username", "admin")
                        .param("password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("账号或密码错误，或账号已被锁定"));
    }
}
