package cn.hospital.rehab.system.department;

import cn.hospital.rehab.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/departments")
public class DepartmentController {
    private final DepartmentService service;

    public DepartmentController(DepartmentService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<Department>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(service.list(keyword, enabled));
    }

    @PostMapping
    ApiResponse<Department> create(@Valid @RequestBody CreateDepartmentRequest request) {
        return ApiResponse.ok(service.create(request));
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
