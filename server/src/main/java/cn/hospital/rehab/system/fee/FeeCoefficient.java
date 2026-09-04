package cn.hospital.rehab.system.fee;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FeeCoefficient(
        long id,
        long feeTypeId,
        String feeCode,
        String feeType,
        BigDecimal coefficient,
        boolean enabled,
        OffsetDateTime effectiveAt,
        OffsetDateTime disabledAt,
        OffsetDateTime createdAt,
        String createdByName,
        String enabledByName,
        String disabledByName) {
}
