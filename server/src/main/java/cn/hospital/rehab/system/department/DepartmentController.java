package cn.hospital.rehab.system.department;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.api.PageResult;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/departments")
public class DepartmentController {
    private final DepartmentService service;
    private final AuditLogService audit;

    public DepartmentController(DepartmentService service, AuditLogService audit) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping
    ApiResponse<PageResult<Department>> list(@RequestParam(required = false) String departmentCode,
                                             @RequestParam(required = false) String departmentName,
                                             @RequestParam(required = false) Boolean enabled,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.list(departmentCode, departmentName, enabled, page, pageSize));
    }

    @PostMapping
    ApiResponse<Department> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.ok(service.create(request));
    }

    @PutMapping("/{id}")
    ApiResponse<Department> update(Authentication auth, @PathVariable long id, @Valid @RequestBody UpdateDepartmentRequest request) {
        var before = service.get(id);
        var updated = service.update(id, request);
        audit.record(auth, "SYSTEM", "DEPARTMENT", String.valueOf(id), "UPDATE", before, updated);
        return ApiResponse.ok(updated);
    }

    @PostMapping("/batch/enable")
    ApiResponse<Void> batchEnable(@Valid @RequestBody DepartmentIdsRequest request) { service.setEnabled(request.ids(), true); return ApiResponse.ok(null); }
    @PostMapping("/batch/disable")
    ApiResponse<Void> batchDisable(@Valid @RequestBody DepartmentIdsRequest request) { service.setEnabled(request.ids(), false); return ApiResponse.ok(null); }
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable long id) { service.delete(id); return ApiResponse.ok(null); }
    @DeleteMapping("/batch")
    ApiResponse<Void> batchDelete(@Valid @RequestBody DepartmentIdsRequest request) { service.delete(request.ids()); return ApiResponse.ok(null); }
    @PostMapping("/{id}/enable")
    ApiResponse<Department> enable(@PathVariable long id) { return ApiResponse.ok(service.setEnabled(id, true)); }

    @PostMapping("/{id}/disable")
    ApiResponse<Department> disable(@PathVariable long id) { return ApiResponse.ok(service.setEnabled(id, false)); }
}