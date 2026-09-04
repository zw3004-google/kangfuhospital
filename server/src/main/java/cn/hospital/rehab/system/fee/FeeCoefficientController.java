package cn.hospital.rehab.system.fee;

import cn.hospital.rehab.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/system/fee-coefficients")
public class FeeCoefficientController {

    private final FeeCoefficientService service;

    public FeeCoefficientController(FeeCoefficientService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<FeeCoefficient>> list(@RequestParam(required = false) String feeCode,
                                           @RequestParam(required = false) String feeType,
                                           @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(service.list(feeCode, feeType, enabled));
    }

    @PostMapping
    ApiResponse<FeeCoefficient> create(@Valid @RequestBody CreateFeeCoefficientRequest request, Authentication authentication) {
        return ApiResponse.ok(service.create(request, authentication.getName()));
    }

    @PostMapping("/{id}/enable")
    ApiResponse<FeeCoefficient> enable(@PathVariable long id, Authentication authentication) {
        return ApiResponse.ok(service.enable(id, authentication.getName()));
    }

    @PostMapping("/{id}/disable")
    ApiResponse<FeeCoefficient> disable(@PathVariable long id, Authentication authentication) {
        return ApiResponse.ok(service.disable(id, authentication.getName()));
    }
}
