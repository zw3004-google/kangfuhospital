package cn.hospital.rehab.arrears.report;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArrearsReportExportServiceTest {
    private final ArrearsReportExportService service = new ArrearsReportExportService();

    @Test
    void createsThreeSheetXlsxFromTheCurrentReportScopeAndBatch() throws Exception {
        var report = report();
        var file = service.create(report);

        assertThat(file.content()).startsWith((byte) 'P', (byte) 'K');
        assertThat(file.mediaType().toString()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(file.filename()).matches("通报报表_\\d{8}_\\d{6}\\.xlsx");
        assertThat(file.contentDisposition()).contains("%E9%80%9A%E6%8A%A5%E6%8A%A5%E8%A1%A8");
        assertThat(zipEntries(file.content())).contains("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml");
        assertThat(service.summaryRows(report)).contains(List.of("数据范围", "本科室"), List.of("最新成功批次", "ARR-20260901080000"), List.of("欠费总额（元）", "123456.78"));
        assertThat(service.departmentRows(report.ranking()).getFirst()).containsExactly("1", "神经康复一科", "2", "123456.78");
        assertThat(service.patientRows(report.patientTop10()).getFirst()).contains("在院患者", "协商中", "100000.00");
    }

    @Test
    void rejectsExportWhenThereIsNoReadyBatch() {
        var empty = new ArrearsReportController.Report(BigDecimal.ZERO, 0, "ALL", "全院", List.of(), List.of(), List.of(), null);
        assertThatThrownBy(() -> service.create(empty)).hasMessage("暂无可导出的成功批次");
    }

    private static ArrearsReportController.Report report() {
        var department = new ArrearsReportController.DepartmentStat("神经康复一科", new BigDecimal("123456.78"), 2);
        var patient = new ArrearsReportController.PatientStat(1, "ZY001", 2, "张三", "神经康复一科", "李医生",
                "INPATIENT", new BigDecimal("100000"), "NEGOTIATING");
        var batch = new ArrearsReportController.BatchSnapshot("ARR-20260901080000",
                OffsetDateTime.parse("2026-09-01T08:00:00+08:00"), "READY");
        return new ArrearsReportController.Report(new BigDecimal("123456.78"), 2, "DEPARTMENT", "本科室",
                List.of(department), List.of(department), List.of(patient), batch);
    }

    private static List<String> zipEntries(byte[] content) throws Exception {
        Path workbook = Path.of("target", "arrears-report-export-service-test.xlsx");
        Files.createDirectories(workbook.getParent());
        try {
            Files.write(workbook, content);
            try (var zip = new ZipFile(workbook.toFile())) {
                return zip.stream().map(java.util.zip.ZipEntry::getName).toList();
            }
        } finally {
            Files.deleteIfExists(workbook);
        }
    }
}
