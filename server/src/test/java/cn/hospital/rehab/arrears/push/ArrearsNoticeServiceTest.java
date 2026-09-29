package cn.hospital.rehab.arrears.push;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ArrearsNoticeServiceTest {
    @Test
    void composesOnlyRecipientScopedTotalWithoutHospitalRanking() {
        var recipientDepartments = List.of(
                department("神经康复一科", "180000", 12),
                department("神经康复二科", "20000", 3));

        var preview = ArrearsNoticeService.compose(null,
                OffsetDateTime.parse("2026-09-01T08:00:00+08:00"), "神经康复一科、神经康复二科科室负责的患者",
                recipientDepartments);

        assertThat(preview.totalAmount()).isEqualByComparingTo("200000");
        assertThat(preview.departments()).containsExactlyElementsOf(recipientDepartments);
        assertThat(preview.content()).contains(
                "截止到2026年09月01日 08:00，神经康复一科、神经康复二科科室负责的患者总计欠费20.00万元。",
                "详情请点击康复医院运营管理系统查看（院内内网访问）：",
                "http://172.16.196.112")
                .doesNotContain("全院科室欠费排名", "神经康复二科：2.00万元，3人");
    }

    @Test
    void composesDoctorTitleAsDoctorThenName() {
        var preview = ArrearsNoticeService.compose(null,
                OffsetDateTime.parse("2026-09-01T08:00:00+08:00"), "神经康复一科内，主管医生张医生负责的患者",
                List.of(department("神经康复一科", "36800", 2)));

        assertThat(preview.content()).contains("神经康复一科内，主管医生张医生负责的患者总计欠费3.68万元。");
    }
    private static ArrearsNoticeService.DepartmentArrears department(String name, String amount, long people) {
        return new ArrearsNoticeService.DepartmentArrears(name, new BigDecimal(amount), people,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}