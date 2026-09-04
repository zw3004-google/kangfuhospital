package cn.hospital.rehab.arrears.push;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArrearsNoticeServiceTest {
    @Test
    void composesStructuredPreviewAndWecomTextFromTheSameData() {
        var departments = List.of(
                new ArrearsNoticeService.DepartmentArrears("神经康复一科", amount("180000"), amount("100000"), amount("30000"), amount("50000")),
                new ArrearsNoticeService.DepartmentArrears("重症康复科", amount("20000"), amount("20000"), BigDecimal.ZERO, BigDecimal.ZERO));

        var preview = ArrearsNoticeService.compose("ARR-20260901080000",
                OffsetDateTime.parse("2026-09-01T08:00:00+08:00"), "全院", departments);

        assertThat(preview.batchNo()).isEqualTo("ARR-20260901080000");
        assertThat(preview.totalAmount()).isEqualByComparingTo("200000");
        assertThat(preview.departments()).containsExactlyElementsOf(departments);
        assertThat(preview.systemLink()).isEqualTo("http://oa.kfyy.local/arrears");
        assertThat(preview.content()).contains(
                "截至 2026-09-01 08:00，全院患者欠费合计 20.00万 元",
                "1. 神经康复一科：18.00万 元（在院 10.00万 + 出院已结算 3.00万 + 出院未结算 5.00万）",
                "2. 重症康复科：2.00万 元（在院 2.00万 + 出院已结算 0.00万 + 出院未结算 0.00万）",
                preview.systemLink());
    }

    @Test
    void composesAnEmptyButReadablePreviewWhenTheBatchHasNoArrears() {
        var preview = ArrearsNoticeService.compose("ARR-EMPTY",
                OffsetDateTime.parse("2026-09-01T08:00:00+08:00"), "本科室", List.of());

        assertThat(preview.totalAmount()).isZero();
        assertThat(preview.departments()).isEmpty();
        assertThat(preview.content()).contains("本科室患者欠费合计 0.00万 元");
    }

    private static BigDecimal amount(String value) { return new BigDecimal(value); }
}
