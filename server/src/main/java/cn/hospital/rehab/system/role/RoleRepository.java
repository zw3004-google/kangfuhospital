package cn.hospital.rehab.system.role;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RoleRepository {
    private final JdbcClient jdbc;

    public RoleRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Role> findAll() {
        return jdbc.sql("SELECT id, role_code, role_name, built_in, enabled FROM sys_role ORDER BY id")
                .query((rs, row) -> new Role(rs.getLong("id"), rs.getString("role_code"), rs.getString("role_name"),
                        rs.getBoolean("built_in"), rs.getBoolean("enabled"))).list();
    }
}
