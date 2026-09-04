package cn.hospital.rehab.arrears.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class ArrearsCalculator {

    private ArrearsCalculator() {
    }

    public static ArrearsCalculation calculate(
            BigDecimal prepaidAmount,
            BigDecimal originalRequiredDeposit,
            BigDecimal coefficient
    ) {
        Objects.requireNonNull(prepaidAmount, "预交金不能为空");
        Objects.requireNonNull(originalRequiredDeposit, "原始应交押金不能为空");
        Objects.requireNonNull(coefficient, "费别系数不能为空");
        if (coefficient.signum() < 0) {
            throw new IllegalArgumentException("费别系数不能小于0");
        }

        BigDecimal finalRequiredDeposit = originalRequiredDeposit
                .multiply(coefficient)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal difference = prepaidAmount
                .subtract(finalRequiredDeposit)
                .setScale(2, RoundingMode.HALF_UP);
        boolean inArrears = difference.signum() < 0;
        BigDecimal arrearsAmount = inArrears ? difference.abs() : BigDecimal.ZERO.setScale(2);

        return new ArrearsCalculation(
                originalRequiredDeposit.setScale(2, RoundingMode.HALF_UP),
                coefficient,
                finalRequiredDeposit,
                difference,
                inArrears,
                arrearsAmount
        );
    }
}
