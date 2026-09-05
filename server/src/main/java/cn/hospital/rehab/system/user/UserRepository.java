package cn.hospital.rehab.system.user;

import cn.hospital.rehab.system.role.Role;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<UserSummary> findPage(String keyword, Long departmentId, int limit, int offset) {
        return jdbc.sql("""
                SELECT u.*, d.department_name
                FROM sys_user u LEFT JOIN sys_department d ON d.id=u.department_id
                WHERE (:keyword='' OR u.display_name ILIKE :pattern OR u.login_name ILIKE :pattern OR u.employee_no ILIKE :pattern OR u.wecom_user_id ILIKE :pattern)
                  AND (:departmentId = 0 OR u.department_id=:departmentId)
                ORDER BY u.enabled DESC, u.created_at DESC LIMIT :limit OFFSET :offset
                """).param("keyword", clean(keyword)).param("pattern", "%" + clean(keyword) + "%")
                .param("departmentId", departmentId == null ? 0L : departmentId)
                .param("limit", limit).param("offset", offset).query(this::map).list();
    }

    public long count(String keyword, Long departmentId) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM sys_user u
                WHERE (:keyword='' OR u.display_name ILIKE :pattern OR u.login_name ILIKE :pattern OR u.employee_no ILIKE :pattern OR u.wecom_user_id ILIKE :pattern)
                  AND (:departmentId = 0 OR u.department_id=:departmentId)
                """).param("keyword", clean(keyword)).param("pattern", "%" + clean(keyword) + "%")
                .param("departmentId", departmentId == null ? 0L : departmentId).query(Long.class).single();
    }

    public List<UserSummary> findAll() {
        return jdbc.sql("""
                SELECT u.*, d.department_name FROM sys_user u
                LEFT JOIN sys_department d ON d.id=u.department_id
                ORDER BY d.department_code, u.employee_no, u.id
                """).query(this::map).list();
    }

    public boolean loginNameExists(String loginName) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM sys_user WHERE login_name=:name)")
                .param("name", loginName).query(Boolean.class).single();
    }
    public boolean employeeNoExists(String employeeNo) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM sys_user WHERE employee_no=:employeeNo)")
                .param("employeeNo", employeeNo).query(Boolean.class).single();
    }
    public long idByLoginName(String loginName) { return jdbc.sql("SELECT id FROM sys_user WHERE login_name=:name").param("name",loginName).query(Long.class).single(); }

    public Optional<UserSummary> findById(long id) {
        return jdbc.sql("""
                SELECT u.*, d.department_name FROM sys_user u
                LEFT JOIN sys_department d ON d.id=u.department_id WHERE u.id=:id
                """).param("id", id).query(this::map).optional();
    }

    public UserSummary insert(String loginName, String displayName, String passwordHash, String employeeNo, String wecomId, long departmentId) {
        long id = jdbc.sql("""
                INSERT INTO sys_user(login_name, display_name, password_hash, employee_no, wecom_user_id, department_id)
                VALUES (:loginName,:displayName,:passwordHash,:employeeNo,:wecomId,:departmentId) RETURNING id
                """).param("loginName", loginName).param("displayName", displayName).param("passwordHash", passwordHash)
                .param("employeeNo", employeeNo).param("wecomId", wecomId).param("departmentId", departmentId).query(Long.class).single();
        return findById(id).orElseThrow();
    }

    public UserSummary setEnabled(long id, boolean enabled) {
        jdbc.sql("UPDATE sys_user SET enabled=:enabled, updated_at=CURRENT_TIMESTAMP WHERE id=:id")
                .param("enabled", enabled).param("id", id).update();
        return findById(id).orElseThrow();
    }

    public void resetPassword(long id, String passwordHash) {
        jdbc.sql("""
                UPDATE sys_user SET password_hash=:hash, must_change_password=TRUE,
                  failed_login_count=0, locked_until=NULL, updated_at=CURRENT_TIMESTAMP WHERE id=:id
                """).param("hash", passwordHash).param("id", id).update();
    }
    public String passwordHash(long id) { return jdbc.sql("SELECT password_hash FROM sys_user WHERE id=:id").param("id",id).query(String.class).single(); }
    public void changePassword(long id, String hash) { jdbc.sql("UPDATE sys_user SET password_hash=:hash,must_change_password=FALSE,failed_login_count=0,locked_until=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=:id").param("hash",hash).param("id",id).update(); }
    public void recordLoginFailure(String loginName) { jdbc.sql("UPDATE sys_user SET failed_login_count=failed_login_count+1,locked_until=CASE WHEN failed_login_count+1>=5 THEN CURRENT_TIMESTAMP+INTERVAL '15 minutes' ELSE locked_until END WHERE login_name=:name").param("name",loginName).update(); }
    public void clearLoginFailures(String loginName) { jdbc.sql("UPDATE sys_user SET failed_login_count=0,locked_until=NULL WHERE login_name=:name").param("name",loginName).update(); }
    public void unlock(long id) { jdbc.sql("UPDATE sys_user SET failed_login_count=0,locked_until=NULL WHERE id=:id").param("id",id).update(); }

    public void replaceRoles(long userId, Set<Long> roleIds) {
        jdbc.sql("DELETE FROM sys_user_role WHERE user_id=:userId").param("userId", userId).update();
        for (Long roleId : roleIds) {
            jdbc.sql("INSERT INTO sys_user_role(user_id,role_id) VALUES (:userId,:roleId)")
                    .param("userId", userId).param("roleId", roleId).update();
        }
    }

    private UserSummary map(ResultSet rs, int row) throws SQLException {
        long id = rs.getLong("id");
        List<Role> roles = jdbc.sql("""
                SELECT r.id,r.role_code,r.role_name,r.built_in,r.enabled FROM sys_role r
                JOIN sys_user_role ur ON ur.role_id=r.id WHERE ur.user_id=:id ORDER BY r.id
                """).param("id", id).query((roleRs, roleRow) -> new Role(roleRs.getLong("id"), roleRs.getString("role_code"),
                roleRs.getString("role_name"), roleRs.getBoolean("built_in"), roleRs.getBoolean("enabled"))).list();
        Long departmentId = rs.getObject("department_id", Long.class);
        return new UserSummary(id, rs.getString("login_name"), rs.getString("display_name"), rs.getString("employee_no"), rs.getString("wecom_user_id"),
                departmentId, rs.getString("department_name"), rs.getBoolean("enabled"), rs.getBoolean("must_change_password"),
                rs.getObject("locked_until", java.time.OffsetDateTime.class), roles,
                rs.getObject("created_at", java.time.OffsetDateTime.class));
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
