package cn.hospital.rehab.discharge.domain;
import org.junit.jupiter.api.Test;import java.time.*;import static org.assertj.core.api.Assertions.assertThat;
class DischargeAbnormalCalculatorTest {
 private final ZoneOffset offset=ZoneOffset.ofHours(8);private final OffsetDateTime actual=OffsetDateTime.of(2026,8,30,10,0,0,0,offset);
 @Test void noActualDischargeHasNoAbnormal(){assertThat(DischargeAbnormalCalculator.calculate(null,null,null)).isEmpty();}
 @Test void missingPlanIsAbnormal(){assertThat(DischargeAbnormalCalculator.calculate(null,actual,null)).containsExactly("MISSING_PLAN");}
 @Test void latePlanAndDateMismatchCanCoexist(){var plan=OffsetDateTime.of(2026,8,29,12,0,0,0,offset);var updated=OffsetDateTime.of(2026,8,30,13,0,0,0,offset);assertThat(DischargeAbnormalCalculator.calculate(plan,actual,updated)).containsExactly("DATE_MISMATCH","LATE_PLAN");}
}
