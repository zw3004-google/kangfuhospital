package cn.hospital.rehab.discharge.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DischargeTimeTypeTest {
    @Test
    void supportsAnalysisListBusinessDates() {
        assertThat(DischargeTimeType.parse("admitted")).isEqualTo(DischargeTimeType.ADMITTED);
        assertThat(DischargeTimeType.parse("planned_discharge")).isEqualTo(DischargeTimeType.PLANNED_DISCHARGE);
        assertThat(DischargeTimeType.parse("actual_discharge")).isEqualTo(DischargeTimeType.ACTUAL_DISCHARGE);
        assertThat(DischargeTimeType.parse("follow_up_day7")).isEqualTo(DischargeTimeType.FOLLOW_UP_DAY7);
        assertThat(DischargeTimeType.parse("follow_up_day30")).isEqualTo(DischargeTimeType.FOLLOW_UP_DAY30);
        assertThat(DischargeTimeType.parse("follow_up_day60")).isEqualTo(DischargeTimeType.FOLLOW_UP_DAY60);
    }
    @Test void acceptsCaseInsensitiveValues(){assertThat(DischargeTimeType.parse(" nutrition ")).isEqualTo(DischargeTimeType.NUTRITION);}
    @Test void emptyValueMeansNoTimeFilter(){assertThat(DischargeTimeType.parse(" ")).isNull();}
    @Test void rejectsUnknownValue(){assertThatThrownBy(()->DischargeTimeType.parse("OTHER")).isInstanceOf(IllegalArgumentException.class).hasMessage("时间类型不正确");}
}
