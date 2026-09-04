package cn.hospital.rehab.arrears.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArrearsCalculatorTest {

    @Test
    void shouldCalculateArrearsUsingFinalRequiredDeposit() {
        ArrearsCalculation result = ArrearsCalculator.calculate(
                new BigDecimal("3000"),
                new BigDecimal("5000"),
                new BigDecimal("0.80")
        );

        assertThat(result.finalRequiredDeposit()).isEqualByComparingTo("4000.00");
        assertThat(result.depositDifference()).isEqualByComparingTo("-1000.00");
        assertThat(result.inArrears()).isTrue();
        assertThat(result.arrearsAmount()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldReturnZeroArrearsWhenDifferenceIsNonNegative() {
        ArrearsCalculation result = ArrearsCalculator.calculate(
                new BigDecimal("5000"),
                new BigDecimal("5000"),
                BigDecimal.ONE
        );

        assertThat(result.inArrears()).isFalse();
        assertThat(result.arrearsAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldRejectNegativeCoefficient() {
        assertThatThrownBy(() -> ArrearsCalculator.calculate(
                BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("-1")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
