package cn.hospital.rehab.arrears.api;

import java.time.OffsetDateTime;
import java.util.List;

public record ArrearsRecordHistory(
        long id,
        String operatorName,
        OffsetDateTime operatedAt,
        String actionType,
        String beforeData,
        String afterData,
        String changeDescription,
        List<String> changeLines) {}
