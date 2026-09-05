package cn.hospital.rehab.system.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent bootstrap for an empty local/test database. Disabled by default. */
@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcClient jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String password;
    private final String employeeNo;
    private final String wecomUserId;

    public AdminBootstrap(JdbcClient jdbc, PasswordEncoder passwordEncoder,
                          @Value("${app.security.initial-password:kfyy123!}") String password,
                          @Value("${app.bootstrap-admin.employee-no:ADMIN}") String employeeNo,
                          @Value("${app.bootstrap-admin.wecom-user-id:admin}") String wecomUserId) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
        this.employeeNo = employeeNo.trim();
        this.wecomUserId = wecomUserId.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (employeeNo.isBlank() || wecomUserId.isBlank()) {
            throw new IllegalStateException("管理员工号和企微ID不能为空");
        }
        Long userId = jdbc.sql("SELECT id FROM sys_user WHERE login_name='admin'")
                .query(Long.class).optional().orElseGet(this::createAdmin);
        jdbc.sql("""
                INSERT INTO sys_user_role(user_id,role_id)
                SELECT :userId,id FROM sys_role WHERE role_code='SYSTEM_ADMIN'
                ON CONFLICT DO NOTHING
                """).param("userId", userId).update();
    }

    private long createAdmin() {
        return jdbc.sql("""
                INSERT INTO sys_user(login_name,display_name,password_hash,employee_no,wecom_user_id,
                                     enabled,must_change_password)
                VALUES ('admin','系统管理员',:passwordHash,:employeeNo,:wecomUserId,TRUE,TRUE)
                RETURNING id
                """).param("passwordHash", passwordEncoder.encode(password))
                .param("employeeNo", employeeNo).param("wecomUserId", wecomUserId)
                .query(Long.class).single();
    }
}
