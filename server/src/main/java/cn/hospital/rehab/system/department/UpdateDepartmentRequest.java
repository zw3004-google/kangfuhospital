package cn.hospital.rehab.system.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Department codes are stable business identifiers and are deliberately absent. */
public record UpdateDepartmentRequest(@NotBlank @Size(max = 128) String departmentName) {
}
