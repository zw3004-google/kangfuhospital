package cn.hospital.rehab.system.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Editable profile fields. Login name and WeCom ID remain immutable. */
public record UpdateUserRequest(
        @NotBlank @Size(max = 128) String displayName,
        @NotBlank @Size(max = 64) String employeeNo,
        @NotNull @Positive Long departmentId) {
}
