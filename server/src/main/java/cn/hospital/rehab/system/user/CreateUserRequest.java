package cn.hospital.rehab.system.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 128) String displayName,
        @NotBlank @Size(max = 64) String employeeNo,
        @NotBlank @Size(max = 128) String wecomUserId,
        @NotNull @Positive Long departmentId) {
}
