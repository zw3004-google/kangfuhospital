package cn.hospital.rehab.system.user;

import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UserIdsRequest(@NotEmpty Set<Long> ids) {}
