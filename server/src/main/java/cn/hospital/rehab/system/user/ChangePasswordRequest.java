package cn.hospital.rehab.system.user;
import jakarta.validation.constraints.NotBlank;
public record ChangePasswordRequest(@NotBlank String oldPassword,@NotBlank String newPassword) {}
