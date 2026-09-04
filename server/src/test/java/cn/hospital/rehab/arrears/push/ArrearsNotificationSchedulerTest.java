package cn.hospital.rehab.arrears.push;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class ArrearsNotificationSchedulerTest {
    @Test void formatsYuanAsWanWithTwoDecimals() {
        assertThat(ArrearsNoticeService.wan(new BigDecimal("1076500"))).isEqualTo("107.65");
        assertThat(ArrearsNoticeService.wan(BigDecimal.ZERO)).isEqualTo("0.00");
    }
}
