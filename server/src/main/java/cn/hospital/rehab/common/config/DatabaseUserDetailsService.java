package cn.hospital.rehab.common.config;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final JdbcClient jdbc;

    public DatabaseUserDetailsService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserRecord user = jdbc.sql("""
                        SELECT u.login_name, u.password_hash, u.enabled, u.locked_until,
                               COALESCE(string_agg(DISTINCT r.role_code, ','), 'USER') roles,
                               COALESCE(string_agg(DISTINCT p.permission_code, ','), '') permissions
                          FROM sys_user u
                          LEFT JOIN sys_user_role ur ON ur.user_id = u.id
                          LEFT JOIN sys_role r ON r.id = ur.role_id AND r.enabled = true
                          LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
                          LEFT JOIN sys_permission p ON p.id = rp.permission_id AND p.enabled = true
                         WHERE u.login_name = :name
                         GROUP BY u.id
                        """)
                .param("name", username.trim())
                .query((result, rowNum) -> new UserRecord(
                        result.getString("login_name"),
                        result.getString("password_hash"),
                        result.getBoolean("enabled"),
                        result.getObject("locked_until", OffsetDateTime.class),
                        result.getString("roles"),
                        result.getString("permissions")))
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        List<String> roleCodes = split(user.roles());
        List<String> permissionCodes = roleCodes.contains("SYSTEM_ADMIN")
                ? jdbc.sql("SELECT permission_code FROM sys_permission WHERE enabled = true ORDER BY id")
                    .query(String.class).list()
                : split(user.permissions());

        List<String> authorities = new ArrayList<>();
        roleCodes.stream().map(code -> "ROLE_" + code).forEach(authorities::add);
        permissionCodes.stream().map(code -> "PERM_" + code).forEach(authorities::add);

        boolean accountNonLocked = user.lockedUntil() == null || user.lockedUntil().isBefore(OffsetDateTime.now());
        return User.withUsername(user.loginName())
                .password(user.passwordHash())
                .disabled(!user.enabled())
                .accountLocked(!accountNonLocked)
                .authorities(authorities.toArray(String[]::new))
                .build();
    }

    private static List<String> split(String values) {
        if (values == null || values.isBlank()) return List.of();
        return Arrays.stream(values.split(",")).filter(value -> !value.isBlank()).toList();
    }

    private record UserRecord(String loginName, String passwordHash, boolean enabled,
                              OffsetDateTime lockedUntil, String roles, String permissions) {}
}
