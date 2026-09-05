package cn.hospital.rehab.arrears.api;

import jakarta.validation.constraints.NotBlank;

public record UpdateArrearsRequest(@NotBlank String paymentStatus, String arrearsReason, String recoveryProgress,
        java.time.OffsetDateTime expectedUpdatedAt) {
    public UpdateArrearsRequest(String paymentStatus,String arrearsReason,String recoveryProgress) {
        this(paymentStatus,arrearsReason,recoveryProgress,null);
    }
    public UpdateArrearsRequest {
        paymentStatus = paymentStatus == null ? null : paymentStatus.trim().toUpperCase();
        recoveryProgress = recoveryProgress == null || recoveryProgress.isBlank()
                ? null : recoveryProgress.trim().toUpperCase();
        if (!"PAID".equals(paymentStatus) && !"UNPAID".equals(paymentStatus))
            throw new IllegalArgumentException("缴费状态不正确");
        if (recoveryProgress != null && !java.util.Set.of(
                "NOT_STARTED", "NEGOTIATING", "REFUSED", "LEGAL_ACTION", "PAID").contains(recoveryProgress))
            throw new IllegalArgumentException("追缴进度不正确");
        if ("PAID".equals(paymentStatus) || "PAID".equals(recoveryProgress)) {
            paymentStatus = "PAID";
            recoveryProgress = "PAID";
        }
    }
}
