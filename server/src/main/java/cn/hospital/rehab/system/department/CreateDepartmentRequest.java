package cn.hospital.rehab.system.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9_-]+", message = "科室编码只能包含字母、数字、下划线和短横线") String departmentCode,
        @NotBlank @Size(max = 128) String departmentName) {
}
