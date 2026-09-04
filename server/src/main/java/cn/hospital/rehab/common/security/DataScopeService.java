package cn.hospital.rehab.common.security;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class DataScopeService {
    private static final Set<String> FULL_ACCESS_ROLES = Set.of(
            "ROLE_SYSTEM_ADMIN", "ROLE_OPERATIONS", "ROLE_FINANCE");
    private final JdbcClient jdbc;

    public DataScopeService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public DataScope resolve(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("未登录");
        }
        long userId = jdbc.sql("SELECT id FROM sys_user WHERE login_name=:name AND enabled=true")
                .param("name", auth.getName()).query(Long.class).single();
        return resolveForUser(userId);
    }

    public DataScope resolveForUser(long userId) {
        Set<String> authorities = new HashSet<>(jdbc.sql("""
                SELECT 'ROLE_' || r.role_code
                  FROM sys_user_role ur
                  JOIN sys_role r ON r.id=ur.role_id
                  JOIN sys_user u ON u.id=ur.user_id
                 WHERE ur.user_id=:userId AND u.enabled=true AND r.enabled=true
                """).param("userId", userId).query(String.class).list());
        if (authorities.stream().anyMatch(FULL_ACCESS_ROLES::contains)) return DataScope.all();

        Set<Long> departments = new HashSet<>(jdbc.sql("""
                SELECT DISTINCT rd.department_id
                  FROM sys_user_role ur
                  JOIN sys_role r ON r.id=ur.role_id
                  JOIN sys_role_department rd ON rd.role_id=ur.role_id
                 WHERE ur.user_id=:userId AND r.enabled=true
                """).param("userId", userId).query(Long.class).list());
        Long doctorUserId = authorities.contains("ROLE_ATTENDING_DOCTOR") ? userId : null;
        return new DataScope(false, departments, doctorUserId);
    }
}
