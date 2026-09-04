package cn.hospital.rehab.arrears.importer;

import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ArrearsImportControllerTest {
    @Test
    void returnsCompleteImportCountsAndRecordsAudit() {
        var service = mock(ArrearsImportService.class);
        var audit = mock(AuditLogService.class);
        var auth = mock(Authentication.class);
        var request = mock(HttpServletRequest.class);
        var file = new MockMultipartFile("file", "arrears.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1});
        var result = new ArrearsImportResult("ARR-001", 8, 8, 0, 3, 5, 0, 6, 1, 1);
        when(service.importFile(file)).thenReturn(result);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        var controller = new ArrearsImportController(service, audit);

        var response = controller.importFile(auth, request, file);

        assertThat(response.data()).isEqualTo(result);
        assertThat(response.data().failure()).isZero();
        verify(audit).record(auth, "IMPORTING", "IMPORT_BATCH", "ARR-001", "IMPORT_ARREARS",
                null, result, "127.0.0.1");
    }
}
