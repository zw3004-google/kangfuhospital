package cn.hospital.rehab.system.user;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AssignRolesRequest(@NotNull Set<Long> roleIds) {
}
