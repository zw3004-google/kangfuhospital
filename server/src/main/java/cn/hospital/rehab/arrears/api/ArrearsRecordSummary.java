package cn.hospital.rehab.arrears.api;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ArrearsRecordSummary(long id, String inpatientNo, int admissionTimes, String patientName,
        String departmentName, String wardName, String feeType, String arrearsType, String doctorName,
        String doctorEmployeeNo,
        OffsetDateTime admittedAt, OffsetDateTime dischargedAt, BigDecimal totalCost, BigDecimal prepaidAmount,
        BigDecimal medicalInsurancePaid, BigDecimal personalAccountPaid,
        BigDecimal originalRequiredDeposit, BigDecimal finalRequiredDeposit, BigDecimal depositDifference,
        BigDecimal arrearsAmount, boolean inArrears, String paymentStatus, String arrearsReason,
        String recoveryProgress, String previousRecoveryProgress, String recoveryProgressLegacy,
        String lastOperatedBy, OffsetDateTime sourceUpdatedAt, OffsetDateTime updatedAt) {
    public ArrearsRecordSummary(long id, String inpatientNo, int admissionTimes, String patientName,
            String departmentName, String wardName, String feeType, String arrearsType, String doctorName,
            String doctorEmployeeNo, OffsetDateTime admittedAt, OffsetDateTime dischargedAt, BigDecimal totalCost,
            BigDecimal prepaidAmount, BigDecimal medicalInsurancePaid, BigDecimal personalAccountPaid,
            BigDecimal originalRequiredDeposit, BigDecimal finalRequiredDeposit, BigDecimal depositDifference,
            BigDecimal arrearsAmount, boolean inArrears, String paymentStatus, String arrearsReason,
            String recoveryProgress, String previousRecoveryProgress, String recoveryProgressLegacy,
            String lastOperatedBy, OffsetDateTime sourceUpdatedAt) {
        this(id,inpatientNo,admissionTimes,patientName,departmentName,wardName,feeType,arrearsType,doctorName,
                doctorEmployeeNo,admittedAt,dischargedAt,totalCost,prepaidAmount,medicalInsurancePaid,
                personalAccountPaid,originalRequiredDeposit,finalRequiredDeposit,depositDifference,arrearsAmount,
                inArrears,paymentStatus,arrearsReason,recoveryProgress,previousRecoveryProgress,
                recoveryProgressLegacy,lastOperatedBy,sourceUpdatedAt,sourceUpdatedAt);
    }
}
