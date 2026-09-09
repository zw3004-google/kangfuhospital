package cn.hospital.rehab.discharge.importer;

import cn.hospital.rehab.common.importing.FailedImportBatchRecorder;
import org.junit.jupiter.api.Test;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DischargeImportServiceTest {
    private final DischargeImportService service = new DischargeImportService(
            mock(JdbcClient.class), mock(FailedImportBatchRecorder.class));

    @Test
    void acceptsLegacyAndModernExcelExtensionsAndRejectsOtherFiles() {
        assertThatCode(() -> DischargeImportService.validateFile(file("legacy.xls"))).doesNotThrowAnyException();
        assertThatCode(() -> DischargeImportService.validateFile(file("modern.xlsx"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> DischargeImportService.validateFile(file("data.csv")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("仅支持.xls或.xlsx文件");
    }

    @Test
    void readsLegacyXlsAndMapsPatientProfileFieldsSeparately() throws Exception {
        byte[] content;
        try (var workbook = new HSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet=workbook.createSheet("预出院");
            var header=sheet.createRow(0);
            String[] headers={"住院号","住院次数","姓名","性别","住院病区","费别","主诊断","医保类型","预计出院时间"};
            for(int i=0;i<headers.length;i++)header.createCell(i).setCellValue(headers[i]);
            var row=sheet.createRow(1);
            Object[] values={"ZY-XLS-001",1,"测试患者","女","康复一科","自费","脑卒中恢复期","城镇职工医保","2026-09-10"};
            for(int i=0;i<values.length;i++){if(values[i] instanceof Number n)row.createCell(i).setCellValue(n.doubleValue());else row.createCell(i).setCellValue(String.valueOf(values[i]));}
            workbook.write(output);
            content=output.toByteArray();
        }
        var rows=DischargeImportService.readRows(new MockMultipartFile("file","legacy.xls","application/vnd.ms-excel",content));
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().gender).isEqualTo("女");
        assertThat(rows.getFirst().primaryDiagnosis).isEqualTo("脑卒中恢复期");
        assertThat(rows.getFirst().medicalInsuranceType).isEqualTo("城镇职工医保");
        assertThat(rows.getFirst().feeType).isEqualTo("自费");
    }

    private static MockMultipartFile file(String name) {
        return new MockMultipartFile("file",name,"application/octet-stream",new byte[]{1});
    }
}
