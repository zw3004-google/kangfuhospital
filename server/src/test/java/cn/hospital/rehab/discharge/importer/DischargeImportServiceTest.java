package cn.hospital.rehab.discharge.importer;

import cn.hospital.rehab.common.importing.FailedImportBatchRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class DischargeImportServiceTest {
    private final DischargeImportService service = new DischargeImportService(
            mock(JdbcClient.class), mock(FailedImportBatchRecorder.class));

    @Test
    void rejectsLegacyXlsAndNonExcelBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.importFile(file("legacy.xls")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("仅支持xlsx文件");
        assertThatThrownBy(() -> service.importFile(file("data.csv")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("仅支持xlsx文件");
    }

    private static MockMultipartFile file(String name) {
        return new MockMultipartFile("file",name,"application/octet-stream",new byte[]{1});
    }
}
