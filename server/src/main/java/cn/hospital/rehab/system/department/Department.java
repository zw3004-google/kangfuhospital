package cn.hospital.rehab.system.department;

import java.time.OffsetDateTime;

public record Department(long id, String departmentCode, String departmentName, boolean enabled,
                         OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
