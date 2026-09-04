package cn.hospital.rehab.system.user;

import cn.hospital.rehab.system.role.Role;

import java.time.OffsetDateTime;
import java.util.List;

public record UserSummary(long id, String loginName, String displayName, String employeeNo, String wecomUserId,
                          Long departmentId, String departmentName, boolean enabled,
                          boolean mustChangePassword, OffsetDateTime lockedUntil,
                          List<Role> roles, OffsetDateTime createdAt) {
}
