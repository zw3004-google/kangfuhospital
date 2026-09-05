package cn.hospital.rehab.system.permission;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.api.ConcurrentUpdateException;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/system/permissions")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class PermissionController {
    private final JdbcClient jdbc;
    private final AuditLogService audit;

    public PermissionController(JdbcClient jdbc, AuditLogService audit) { this.jdbc=jdbc; this.audit=audit; }

    @GetMapping
    public ApiResponse<List<Permission>> list() {
        return ApiResponse.ok(jdbc.sql("SELECT id,permission_code,permission_name,permission_type,resource_path,http_method,parent_id,enabled FROM sys_permission WHERE enabled=true ORDER BY permission_type,id")
                .query((r,n)->new Permission(r.getLong("id"),r.getString("permission_code"),r.getString("permission_name"),r.getString("permission_type"),r.getString("resource_path"),r.getString("http_method"),r.getObject("parent_id",Long.class),r.getBoolean("enabled"))).list());
    }

    @GetMapping("/roles/{roleId}")
    public ApiResponse<List<Long>> rolePermissions(@PathVariable long roleId) { ensureRole(roleId); return ApiResponse.ok(permissionIds(roleId)); }

    @Transactional
    @PutMapping("/roles/{roleId}")
    public ApiResponse<Void> assign(Authentication auth,@PathVariable long roleId,@Valid @RequestBody PermissionIds req) {
        ensureRole(roleId);
        List<Long> before=permissionIds(roleId);
        ensureUnchanged(before,req.expectedPermissionIds());
        jdbc.sql("DELETE FROM sys_role_permission WHERE role_id=:id").param("id",roleId).update();
        for(Long id:new HashSet<>(req.permissionIds())) jdbc.sql("INSERT INTO sys_role_permission(role_id,permission_id) SELECT :role,:permission WHERE EXISTS(SELECT 1 FROM sys_permission WHERE id=:permission)").param("role",roleId).param("permission",id).update();
        audit.record(auth,"SYSTEM","ROLE",String.valueOf(roleId),"ASSIGN_PERMISSIONS",before,req.permissionIds());
        return ApiResponse.ok(null);
    }

    @Transactional
    @PutMapping("/roles/{roleId}/scope")
    public ApiResponse<Void> assignScope(Authentication auth,@PathVariable long roleId,@Valid @RequestBody RoleScope req) {
        ensureRole(roleId);
        List<Long> beforePermissions=permissionIds(roleId), beforeDepartments=departmentIds(roleId);
        ensureUnchanged(beforePermissions,req.expectedPermissionIds());
        ensureUnchanged(beforeDepartments,req.expectedDepartmentIds());
        for(Long id:req.departmentIds()) if(jdbc.sql("SELECT COUNT(*) FROM sys_department WHERE id=:id AND enabled=true").param("id",id).query(Long.class).single()==0) throw new IllegalArgumentException("科室不存在或已停用："+id);
        jdbc.sql("DELETE FROM sys_role_permission WHERE role_id=:id").param("id",roleId).update();
        for(Long id:new HashSet<>(req.permissionIds())) jdbc.sql("INSERT INTO sys_role_permission(role_id,permission_id) SELECT :role,:permission WHERE EXISTS(SELECT 1 FROM sys_permission WHERE id=:permission)").param("role",roleId).param("permission",id).update();
        jdbc.sql("DELETE FROM sys_role_department WHERE role_id=:id").param("id",roleId).update();
        for(Long id:new HashSet<>(req.departmentIds())) jdbc.sql("INSERT INTO sys_role_department(role_id,department_id) VALUES(:role,:dept)").param("role",roleId).param("dept",id).update();
        audit.record(auth,"SYSTEM","ROLE",String.valueOf(roleId),"ASSIGN_SCOPE",Map.of("permissionIds",beforePermissions,"departmentIds",beforeDepartments),Map.of("permissionIds",req.permissionIds(),"departmentIds",req.departmentIds()));
        return ApiResponse.ok(null);
    }

    @GetMapping("/roles/{roleId}/departments")
    public ApiResponse<List<Long>> roleDepartments(@PathVariable long roleId) { ensureRole(roleId); return ApiResponse.ok(departmentIds(roleId)); }

    @Transactional
    @PutMapping("/roles/{roleId}/departments")
    public ApiResponse<Void> assignDepartments(Authentication auth,@PathVariable long roleId,@Valid @RequestBody DepartmentIds req) {
        ensureRole(roleId);
        List<Long> before=departmentIds(roleId);
        ensureUnchanged(before,req.expectedDepartmentIds());
        for(Long id:req.departmentIds()) if(jdbc.sql("SELECT COUNT(*) FROM sys_department WHERE id=:id AND enabled=true").param("id",id).query(Long.class).single()==0) throw new IllegalArgumentException("科室不存在或已停用："+id);
        jdbc.sql("DELETE FROM sys_role_department WHERE role_id=:id").param("id",roleId).update();
        for(Long id:new HashSet<>(req.departmentIds())) jdbc.sql("INSERT INTO sys_role_department(role_id,department_id) VALUES(:role,:dept)").param("role",roleId).param("dept",id).update();
        audit.record(auth,"SYSTEM","ROLE",String.valueOf(roleId),"ASSIGN_DEPARTMENTS",before,req.departmentIds());
        return ApiResponse.ok(null);
    }

    private List<Long> permissionIds(long roleId) { return jdbc.sql("SELECT permission_id FROM sys_role_permission WHERE role_id=:id ORDER BY permission_id").param("id",roleId).query(Long.class).list(); }
    private List<Long> departmentIds(long roleId) { return jdbc.sql("SELECT department_id FROM sys_role_department WHERE role_id=:id ORDER BY department_id").param("id",roleId).query(Long.class).list(); }
    private void ensureUnchanged(List<Long> current,List<Long> expected) {
        if(expected!=null && !new HashSet<>(current).equals(new HashSet<>(expected))) throw new ConcurrentUpdateException("权限范围已被其他管理员修改，请刷新后重新确认");
    }
    private void ensureRole(long id) { if(jdbc.sql("SELECT COUNT(*) FROM sys_role WHERE id=:id AND enabled=true").param("id",id).query(Long.class).single()==0) throw new IllegalArgumentException("角色不存在或已停用"); }

    public record Permission(long id,String permissionCode,String permissionName,String permissionType,String resourcePath,String httpMethod,Long parentId,boolean enabled) {}
    public record RoleScope(List<Long> permissionIds,List<Long> departmentIds,List<Long> expectedPermissionIds,List<Long> expectedDepartmentIds) {}
    public record PermissionIds(List<Long> permissionIds,List<Long> expectedPermissionIds) { public PermissionIds(List<Long> permissionIds){this(permissionIds,null);} }
    public record DepartmentIds(List<Long> departmentIds,List<Long> expectedDepartmentIds) { public DepartmentIds(List<Long> departmentIds){this(departmentIds,null);} }
}
