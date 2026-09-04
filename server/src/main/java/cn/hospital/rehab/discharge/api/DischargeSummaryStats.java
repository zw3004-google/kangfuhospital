package cn.hospital.rehab.discharge.api;

public record DischargeSummaryStats(
        long inpatientCount,
        long plannedCount,
        long nutritionPatientCount,
        long nutritionRecordCount,
        long homeRehabPatientCount,
        long homeRehabRecordCount,
        long outpatientPatientCount) {}
