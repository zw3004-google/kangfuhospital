package cn.hospital.rehab.common.config;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final JdbcClient jdbc;
    public DatabaseUserDetailsService(JdbcClient jdbc){this.jdbc=jdbc;}
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return jdbc.sql("SELECT u.login_name,u.password_hash,u.enabled,u.locked_until,COALESCE(string_agg(DISTINCT r.role_code,','),'USER') roles,COALESCE(string_agg(DISTINCT p.permission_code,','),'') permissions FROM sys_user u LEFT JOIN sys_user_role ur ON ur.user_id=u.id LEFT JOIN sys_role r ON r.id=ur.role_id AND r.enabled=true LEFT JOIN sys_role_permission rp ON rp.role_id=r.id LEFT JOIN sys_permission p ON p.id=rp.permission_id AND p.enabled=true WHERE u.login_name=:name GROUP BY u.id")
                .param("name", username.trim()).query((r,n)->{
                    java.time.OffsetDateTime locked=r.getObject("locked_until",java.time.OffsetDateTime.class);
                    boolean accountNonLocked=locked==null||locked.isBefore(java.time.OffsetDateTime.now());
                    String roles=r.getString("roles");
                    var builder=User.withUsername(r.getString("login_name")).password(r.getString("password_hash"))
                            .disabled(!r.getBoolean("enabled")).accountLocked(!accountNonLocked);
                    java.util.List<String> authorities = new java.util.ArrayList<>();
                    java.util.Arrays.stream(roles.split(",")).filter(v -> !v.isBlank())
                            .map(v -> "ROLE_" + v).forEach(authorities::add);
                    String permissions=r.getString("permissions");
                    if(permissions!=null&&!permissions.isBlank()) java.util.Arrays.stream(permissions.split(","))
                            .filter(v -> !v.isBlank()).map(v -> "PERM_" + v).forEach(authorities::add);
                    return builder.authorities(authorities.toArray(String[]::new)).build();
                }).optional().orElseThrow(()->new UsernameNotFoundException("用户不存在"));
    }
}
