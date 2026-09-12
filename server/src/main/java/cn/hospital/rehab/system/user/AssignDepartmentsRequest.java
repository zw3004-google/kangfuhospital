package cn.hospital.rehab.system.user;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AssignDepartmentsRequest(@NotNull Set<Long> departmentIds) {}