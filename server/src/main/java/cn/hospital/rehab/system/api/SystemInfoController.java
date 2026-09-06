package cn.hospital.rehab.system.api;

import cn.hospital.rehab.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemInfoController {

    @GetMapping("/info")
    public ApiResponse<Map<String, String>> info() {
        return ApiResponse.ok(Map.of(
                "name", "康复医院运营管理系统",
                "version", "1.1.2"
        ));
    }
}
