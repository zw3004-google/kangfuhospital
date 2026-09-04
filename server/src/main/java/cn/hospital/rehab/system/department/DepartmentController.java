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

    @PostMapping("/{id}/enable")
    ApiResponse<Department> enable(@PathVariable long id) { return ApiResponse.ok(service.setEnabled(id, true)); }

    @PostMapping("/{id}/disable")
    ApiResponse<Department> disable(@PathVariable long id) { return ApiResponse.ok(service.setEnabled(id, false)); }
}
