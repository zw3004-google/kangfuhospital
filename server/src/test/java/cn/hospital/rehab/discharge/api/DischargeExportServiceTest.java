package cn.hospital.rehab.discharge.api;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DischargeExportServiceTest {
    private final DischargeExportService service = new DischargeExportService();

    @Test
    void createsCsvWithAllBusinessFieldsAndShanghaiTime() {
        var file = service.create("csv", List.of(summary()), 1);
        String csv = new String(file.content(), StandardCharsets.UTF_8);

        assertThat(file.mediaType().toString()).isEqualTo("text/csv;charset=UTF-8");
        assertThat(file.filename()).startsWith("出院统计明细_").endsWith(".csv");
        assertThat(csv).startsWith("\uFEFF患者姓名,患者性别,住院号,住院次数");
        assertThat(csv).contains("特殊患者,特殊患者原因,需要随访,7天随访,30天随访,60天随访,随访详情");
        assertThat(csv).contains("张三,男,ZY001,2,康复一科,2026-09-01 08:00:00");
        assertThat(csv).contains("是,已完成,待随访,已逾期");
    }

    @Test
    void createsRealXlsxWorkbook() {
        var file = service.create("XLSX", List.of(summary()), 1);

        assertThat(file.mediaType().toString()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(file.filename()).endsWith(".xlsx");
        assertThat(file.content()).startsWith((byte) 'P', (byte) 'K');
    }

    @Test
    void rejectsUnsupportedFormatAndTruncatedResult() {
        assertThatThrownBy(() -> service.create("pdf", List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("导出格式仅支持csv或xlsx");
        assertThatThrownBy(() -> service.create("xlsx", List.of(summary()), 20_001))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("导出结果超过20000条，请缩小筛选范围后重试");
    }

    private static DischargeSummary summary() {
        OffsetDateTime admitted = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        return new DischargeSummary(1, 2, "ZY001", 2, "张三", "男", "康复一科", "脑卒中", "李医生",
                admitted, admitted.plusDays(2), null, admitted.plusDays(7), true, admitted.plusDays(7).plusHours(1), "王护士", null,
                admitted.plusDays(3), admitted.plusDays(4), admitted.plusDays(5), "已填报", List.of("未计划出院"), "临时出院",
                true, "重点关注", true, "COMPLETED", "PENDING", "MISSED", "{\"note\":\"稳定\"}");
    }
}
