package cn.hospital.rehab.system.user;

import cn.hospital.rehab.common.api.PageResult;
import cn.hospital.rehab.system.department.DepartmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Set;

@Service
public class UserService {
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder passwordEncoder;
    private final String initialPassword;

    public UserService(UserRepository users, DepartmentRepository departments,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.initial-password:kfyy123!}") String initialPassword) {
        this.users = users;
        this.departments = departments;
        this.passwordEncoder = passwordEncoder;
        this.initialPassword = initialPassword;
    }

    public PageResult<UserSummary> list(String keyword, Long departmentId, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safeSize = Set.of(20, 50, 100, 200).contains(pageSize) ? pageSize : 50;
        return new PageResult<>(users.findPage(keyword, departmentId, safeSize, (safePage - 1) * safeSize),
                users.count(keyword, departmentId), safePage, safeSize);
    }

    @Transactional
    public UserSummary create(CreateUserRequest request) {
        var department = departments.findById(request.departmentId())
                .orElseThrow(() -> new IllegalArgumentException("所属科室不存在"));
        if (!department.enabled()) throw new IllegalArgumentException("不能为用户分配已停用科室");
        String loginName = request.wecomUserId().trim();
        if (users.loginNameExists(loginName)) throw new IllegalArgumentException("企微ID已存在：" + loginName);
        String employeeNo = request.employeeNo().trim();
        if (users.employeeNoExists(employeeNo)) throw new IllegalArgumentException("工号已存在：" + employeeNo);
        return users.insert(loginName, request.displayName().trim(), passwordEncoder.encode(initialPassword),
                employeeNo, request.wecomUserId().trim(), request.departmentId());
    }

    @Transactional
    public UserSummary setEnabled(long id, boolean enabled) {
        UserSummary user = requireUser(id);
        return user.enabled() == enabled ? user : users.setEnabled(id, enabled);
    }
    @Transactional public void setEnabled(Set<Long> ids, boolean enabled) { for (Long id : ids) setEnabled(id, enabled); }
    @Transactional public void delete(long id) { requireUser(id); users.delete(id); }
    @Transactional public void delete(Set<Long> ids) { for (Long id : ids) delete(id); }
    public boolean isCurrentUser(long id, String loginName) { return requireUser(id).loginName().equals(loginName); }
    public boolean containsLogin(Set<Long> ids, String loginName) { return ids.stream().anyMatch(id -> isCurrentUser(id, loginName)); }
    @Transactional public void unlock(long id) { requireUser(id); users.unlock(id); }

    @Transactional
    public void resetPassword(long id) {
        requireUser(id);
        users.resetPassword(id, passwordEncoder.encode(initialPassword));
    }
    @Transactional public void changePassword(long id, ChangePasswordRequest request) {
        requireUser(id); String next=request.newPassword();
        if(next.length()<8 || !next.matches(".*[A-Za-z].*") || !next.matches(".*\\d.*")) throw new IllegalArgumentException("新密码至少8位且必须包含字母和数字");
        if(!passwordEncoder.matches(request.oldPassword(), users.passwordHash(id))) throw new IllegalArgumentException("旧密码不正确");
        users.changePassword(id,passwordEncoder.encode(next));
    }
    @Transactional public void changePasswordByLogin(String loginName, ChangePasswordRequest request) { changePassword(users.idByLoginName(loginName), request); }

    @Transactional
    public UserSummary assignRoles(long id, AssignRolesRequest request) {
        requireUser(id);
        users.replaceRoles(id, request.roleIds());
        return requireUser(id);
    }

    private UserSummary requireUser(long id) {
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

}
