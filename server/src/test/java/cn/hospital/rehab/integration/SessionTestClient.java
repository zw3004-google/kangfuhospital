package cn.hospital.rehab.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real token exchange and form authentication; does not bypass the security chain. */
final class SessionTestClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    final MockMvc mvc;
    MockHttpSession session;
    String token;
    String header;
    SessionTestClient(MockMvc mvc) throws Exception { this.mvc = mvc; refresh(); }
    void refresh() throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null && !session.isInvalid()) request.session(session);
        var result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) result.getRequest().getSession(false);
        var data = JSON.readTree(result.getResponse().getContentAsString()).path("data");
        header = data.path("headerName").asText(); token = data.path("token").asText();
    }
    SessionTestClient login(String username, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").with(auth()).param("username", username).param("password", password))
                .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) result.getRequest().getSession(false);
        refresh();
        return this;
    }
    RequestPostProcessor auth() {
        return request -> { request.setSession(session); request.addHeader(header, token); return request; };
    }
    static RequestPostProcessor login(MockMvc mvc, String username, String password) {
        try { return new SessionTestClient(mvc).login(username, password).auth(); }
        catch (Exception ex) { throw new AssertionError("Session login failed", ex); }
    }
}
