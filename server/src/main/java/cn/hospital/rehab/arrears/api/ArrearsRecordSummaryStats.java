package cn.hospital.rehab.arrears.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ArrearsRecordSummaryStats(
        long totalPeople,
        long inpatientPeople,
        long dischargedUnsettledPeople,
        long dischargedSettledPeople,
        BigDecimal totalAmount,
        long uncollectedPeople,
        long legalPeople,
        OffsetDateTime sourceUpdatedAt) {}
