package cn.hospital.rehab.system.department;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class DepartmentRepository {
    private final JdbcClient jdbc;

    public DepartmentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Department> findAll(String keyword, Boolean enabled) {
        return findPage(keyword, null, enabled, Integer.MAX_VALUE, 0);
    }

    public List<Department> findPage(String departmentCode, String departmentName, Boolean enabled, int limit, int offset) {
        String cleanCode = clean(departmentCode);
        String cleanName = clean(departmentName);
        return jdbc.sql("""
                SELECT * FROM sys_department
                WHERE (:departmentCode = '' OR department_code ILIKE :codePattern)
                  AND (:departmentName = '' OR department_name ILIKE :namePattern)
                  AND (:enabledFilter = 2 OR enabled = CAST(:enabled AS BOOLEAN))
                ORDER BY enabled DESC, department_code, id
                LIMIT :limit OFFSET :offset
                """).param("departmentCode", cleanCode).param("codePattern", "%" + cleanCode + "%")
                .param("departmentName", cleanName).param("namePattern", "%" + cleanName + "%")
                .param("enabledFilter", enabled == null ? 2 : (enabled ? 1 : 0))
                .param("enabled", enabled == null ? false : enabled)
                .param("limit", limit).param("offset", offset).query(DepartmentRepository::map).list();
    }

    public long count(String departmentCode, String departmentName, Boolean enabled) {
        String cleanCode = clean(departmentCode);
        String cleanName = clean(departmentName);
        return jdbc.sql("""
                SELECT COUNT(*) FROM sys_department
                WHERE (:departmentCode = '' OR department_code ILIKE :codePattern)
                  AND (:departmentName = '' OR department_name ILIKE :namePattern)
                  AND (:enabledFilter = 2 OR enabled = CAST(:enabled AS BOOLEAN))
                """).param("departmentCode", cleanCode).param("codePattern", "%" + cleanCode + "%")
                .param("departmentName", cleanName).param("namePattern", "%" + cleanName + "%")
                .param("enabledFilter", enabled == null ? 2 : (enabled ? 1 : 0))
                .param("enabled", enabled == null ? false : enabled).query(Long.class).single();
    }

    public Optional<Department> findById(long id) {
        return jdbc.sql("SELECT * FROM sys_department WHERE id=:id").param("id", id).query(DepartmentRepository::map).optional();
    }

    public Department insert(String code, String name) {
        return jdbc.sql("""
                INSERT INTO sys_department(department_code, department_name)
                VALUES (:code, :name) RETURNING *
                """).param("code", code).param("name", name).query(DepartmentRepository::map).single();
    }

    public boolean codeExists(String code) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM sys_department WHERE department_code=:code)")
                .param("code", code).query(Boolean.class).single();
    }

    public void delete(long id) {
        jdbc.sql("DELETE FROM sys_department WHERE id=:id").param("id", id).update();
    }
    public Department setEnabled(long id, boolean enabled) {
        return jdbc.sql("""
                UPDATE sys_department SET enabled=:enabled, updated_at=CURRENT_TIMESTAMP
                WHERE id=:id RETURNING *
                """).param("id", id).param("enabled", enabled).query(DepartmentRepository::map).single();
    }

    private static Department map(ResultSet rs, int row) throws SQLException {
        return new Department(rs.getLong("id"), rs.getString("department_code"), rs.getString("department_name"),
                rs.getBoolean("enabled"), rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
}
