package cn.hospital.rehab.discharge.api;

import java.time.OffsetDateTime;

public record DischargeRecordHistory(long id, String operatorName, OffsetDateTime operatedAt,
                                      String actionType, String beforeData, String afterData) {}
