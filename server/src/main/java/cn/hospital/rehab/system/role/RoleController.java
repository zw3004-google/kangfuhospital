package cn.hospital.rehab.system.role;

import cn.hospital.rehab.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
public class RoleController {
    private final RoleRepository repository;

    public RoleController(RoleRepository repository) { this.repository = repository; }

    @GetMapping
    ApiResponse<List<Role>> list() { return ApiResponse.ok(repository.findAll()); }
}
