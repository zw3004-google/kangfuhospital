package cn.hospital.rehab.arrears.domain;

import java.math.BigDecimal;

public record ArrearsCalculation(
        BigDecimal originalRequiredDeposit,
        BigDecimal coefficient,
        BigDecimal finalRequiredDeposit,
        BigDecimal depositDifference,
        boolean inArrears,
        BigDecimal arrearsAmount
) {
}
