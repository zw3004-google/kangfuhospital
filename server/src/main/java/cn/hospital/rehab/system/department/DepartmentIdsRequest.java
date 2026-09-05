package cn.hospital.rehab.system.department;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record DepartmentIdsRequest(@NotEmpty Set<Long> ids) {}
