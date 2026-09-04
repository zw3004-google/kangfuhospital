package cn.hospital.rehab.system.user;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.api.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import cn.hospital.rehab.common.audit.AuditLogService;import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/system/users")
public class UserController {
    private final UserService service; private final AuditLogService audit;

    public UserController(UserService service, AuditLogService audit) { this.service = service; this.audit = audit; }

    @GetMapping
    ApiResponse<PageResult<UserSummary>> list(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Long departmentId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.list(keyword, departmentId, page, pageSize));
    }

    @PreAuthorize("hasAuthority('PERM_API_USER_MANAGE')")
    @PostMapping
    ApiResponse<UserSummary> create(Authentication auth,@Valid @RequestBody CreateUserRequest request) { var u=service.create(request); audit.record(auth,"SYSTEM","USER",String.valueOf(u.id()),"CREATE",null,u); return ApiResponse.ok(u); }

    @PreAuthorize("hasAuthority('PERM_API_USER_MANAGE')")
    @PostMapping("/{id}/enable")
    ApiResponse<UserSummary> enable(Authentication auth,@PathVariable long id) { var u=service.setEnabled(id, true); audit.record(auth,"SYSTEM","USER",String.valueOf(id),"ENABLE",null,u); return ApiResponse.ok(u); }

    @PreAuthorize("hasAuthority('PERM_API_USER_MANAGE')")
    @PostMapping("/{id}/disable")
    ApiResponse<UserSummary> disable(Authentication auth,@PathVariable long id) { var u=service.setEnabled(id, false); audit.record(auth,"SYSTEM","USER",String.valueOf(id),"DISABLE",null,u); return ApiResponse.ok(u); }
    @PreAuthorize("hasAuthority('PERM_API_USER_MANAGE')")
    @PostMapping("/{id}/unlock")
    ApiResponse<Void> unlock(Authentication auth,@PathVariable long id) { service.unlock(id); audit.record(auth,"SYSTEM","USER",String.valueOf(id),"UNLOCK",null,null); return ApiResponse.ok(null); }

    @PreAuthorize("hasAuthority('PERM_API_USER_MANAGE')")
    @PostMapping("/{id}/reset-password")
    ApiResponse<Void> resetPassword(Authentication auth,@PathVariable long id) { service.resetPassword(id); audit.record(auth,"SYSTEM","USER",String.valueOf(id),"RESET_PASSWORD",null,null); return ApiResponse.ok(null); }
    @PostMapping("/me/change-password")
    ApiResponse<Void> changePassword(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody ChangePasswordRequest request) {
        service.changePasswordByLogin(principal.getUsername(), request); return ApiResponse.ok(null);
    }

    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @PutMapping("/{id}/roles")
    ApiResponse<UserSummary> assignRoles(Authentication auth,@PathVariable long id, @Valid @RequestBody AssignRolesRequest request) {
        var u=service.assignRoles(id, request); audit.record(auth,"SYSTEM","USER",String.valueOf(id),"ASSIGN_ROLES",null,u); return ApiResponse.ok(u);
    }
}
