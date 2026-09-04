package cn.hospital.rehab.arrears.importer;

import cn.hospital.rehab.common.importing.FailedImportBatchRecorder;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ArrearsImportServiceTest {
    @Test
    void acceptsExcelSlashDateWithoutZeroPadding() {
        assertThat(ArrearsImportService.parseDate("2026/1/3"))
                .isEqualTo(OffsetDateTime.parse("2026-01-03T00:00:00+08:00"));
    }

    @Test
    void readsRowsFromEveryWorksheet() {
        var first = row("IN001", "在院患者");
        var second = row("OUT001", "出院未结算");
        var output = new ByteArrayOutputStream();
        try (ExcelWriter writer = EasyExcel.write(output, ArrearsImportRow.class).build()) {
            WriteSheet inpatient = EasyExcel.writerSheet("在院欠费").build();
            WriteSheet discharged = EasyExcel.writerSheet("出院欠费").build();
            writer.write(List.of(first), inpatient);
            writer.write(List.of(second), discharged);
        }
        var file = new MockMultipartFile("file", "arrears.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());

        assertThat(ArrearsImportService.readRows(file))
                .extracting(row -> row.inpatientNo)
                .containsExactly("IN001", "OUT001");
    }

    @Test
    void acceptsAccountingFormattedZero() {
        assertThat(ArrearsImportService.decimal("- 0")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void rejectsLegacyXlsFilesBeforeReadingWorkbook() {
        var service = new ArrearsImportService(mock(org.springframework.jdbc.core.simple.JdbcClient.class),
                mock(FailedImportBatchRecorder.class));
        var file = new MockMultipartFile("file", "arrears.xls", "application/vnd.ms-excel", new byte[]{1});

        assertThatThrownBy(() -> service.importFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("仅支持xlsx文件");
    }

    private static ArrearsImportRow row(String inpatientNo, String arrearsType) {
        var row = new ArrearsImportRow();
        row.inpatientNo = inpatientNo;
        row.admissionTimes = 1;
        row.patientName = "测试患者";
        row.wardName = "测试病区";
        row.feeType = "测试费别";
        row.arrearsType = arrearsType;
        row.doctorName = "测试医生";
        row.doctorEmployeeNo = "D001";
        row.admittedAt = "2026/1/3";
        row.totalCost = "100";
        row.prepaidAmount = "10";
        row.originalRequiredDeposit = "100";
        return row;
    }
}
