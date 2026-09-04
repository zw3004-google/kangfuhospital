package cn.hospital.rehab.arrears.api;

import java.time.OffsetDateTime;

public record ArrearsRecordHistory(
        long id,
        String operatorName,
        OffsetDateTime operatedAt,
        String actionType,
        String beforeData,
        String afterData,
        String changeDescription) {}
