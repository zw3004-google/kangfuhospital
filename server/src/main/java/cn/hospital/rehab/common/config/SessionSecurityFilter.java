package cn.hospital.rehab.common.config;

import cn.hospital.rehab.common.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

/** Restores current permissions before CSRF, logout and authorization. */
public class SessionSecurityFilter extends OncePerRequestFilter {
    static final String PASSWORD_VERSION = SessionSecurityFilter.class.getName() + ".passwordVersion";
    private final DatabaseUserDetailsService users;
    private final ObjectMapper mapper;
    private final Set<String> allowedOrigins;
    public SessionSecurityFilter(DatabaseUserDetailsService users, ObjectMapper mapper, String origins) {
        this.users = users;
        this.mapper = mapper;
        this.allowedOrigins = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(SessionSecurityFilter::origin).collect(Collectors.toSet());
        if (allowedOrigins.contains("")) throw new IllegalArgumentException("Invalid app.security.allowed-origins");
    }
    static String passwordVersion(String hash) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var session = request.getSession(false);
        if (session != null && auth != null && auth.isAuthenticated()) {
            try {
                var current = users.loadUserByUsername(auth.getName());
                if (!current.isEnabled() || !current.isAccountNonLocked()
                        || !passwordVersion(current.getPassword()).equals(session.getAttribute(PASSWORD_VERSION))) {
                    session.invalidate();
                    SecurityContextHolder.clearContext();
                    reject(response, mapper, 401, "AUTH_REQUIRED", "登录已失效，请重新登录");
                    return;
                }
                var refreshed = UsernamePasswordAuthenticationToken.authenticated(current.getUsername(), null, current.getAuthorities());
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(refreshed);
                SecurityContextHolder.setContext(context);
            } catch (UsernameNotFoundException ex) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                reject(response, mapper, 401, "AUTH_REQUIRED", "登录已失效，请重新登录");
                return;
            }
        }
        if (CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request)) {
            String source = request.getHeader("Origin");
            if (source == null) source = request.getHeader("Referer");
            String target = origin(request.getRequestURL().toString());
            boolean allowed = source == null
                    ? !"cross-site".equals(request.getHeader("Sec-Fetch-Site"))
                    : (!origin(source).isEmpty() && (allowedOrigins.isEmpty()
                        ? origin(source).equals(target) : allowedOrigins.contains(origin(source))));
            // Missing source headers still require CSRF. Forwarded headers are never trusted here.
            if (!allowed) {
                reject(response, mapper, 403, "ORIGIN_DENIED", "请求来源不受信任");
                return;
            }
        }
        chain.doFilter(request, response);
    }
    private static String origin(String value) {
        try {
            var uri = URI.create(value);
            String scheme = uri.getScheme();
            if (uri.getHost() == null || uri.getUserInfo() != null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) return "";
            int port = uri.getPort() == -1 ? ("https".equalsIgnoreCase(scheme) ? 443 : 80) : uri.getPort();
            return scheme.toLowerCase(Locale.ROOT) + "://" + uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
        } catch (IllegalArgumentException ex) { return ""; }
    }
    static void reject(HttpServletResponse response, ObjectMapper mapper, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(message, Map.of("code", code)));
    }
}
