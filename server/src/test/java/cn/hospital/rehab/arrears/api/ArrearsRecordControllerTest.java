package cn.hospital.rehab.arrears.api;

import cn.hospital.rehab.common.audit.AuditLogService;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

class ArrearsRecordControllerTest {
    @Test
    void pageUsesAllFiltersPagingAndResolvedDataScope() {
        var repository = mock(ArrearsRecordRepository.class);
        var scopes = mock(DataScopeService.class);
        var authentication = mock(Authentication.class);
        var scope = new DataScope(false, Set.of(12L), 34L);
        var expected = new cn.hospital.rehab.common.api.PageResult<ArrearsRecordSummary>(List.of(), 0, 2, 100);
        when(scopes.resolve(authentication)).thenReturn(scope);
        when(repository.page("李医生", 12L, "INPATIENT", "自费", "NEGOTIATING", true, scope, 2, 100)).thenReturn(expected);
        var controller = controller(repository, scopes);

        var response = controller.page(authentication, "李医生", 12L, "INPATIENT", "自费", "NEGOTIATING", true, 2, 100);

        assertThat(response.data()).isEqualTo(expected);
        verify(repository).page("李医生", 12L, "INPATIENT", "自费", "NEGOTIATING", true, scope, 2, 100);
    }

    @Test
    void summaryUsesAllFiltersAndResolvedDataScope() {
        var repository = mock(ArrearsRecordRepository.class);
        var scopes = mock(DataScopeService.class);
        var authentication = mock(Authentication.class);
        var scope = new DataScope(false, Set.of(12L), 34L);
        var expected = new ArrearsRecordSummaryStats(8, 4, 2, 2,
                new BigDecimal("1234.56"), 3, 1, OffsetDateTime.parse("2026-09-01T08:30:00+08:00"));
        when(scopes.resolve(authentication)).thenReturn(scope);
        when(repository.summary("张", 12L, "在院患者", "自费", "NOT_STARTED", true, scope))
                .thenReturn(expected);
        var controller = new ArrearsRecordController(repository, scopes, mock(AuditLogService.class), mock(ArrearsRecordHistoryService.class), mock(ArrearsExportService.class));

        var response = controller.summary(authentication, "张", 12L, "在院患者", "自费",
                "NOT_STARTED", true);

        assertThat(response.data()).isEqualTo(expected);
        verify(scopes).resolve(authentication);
        verify(repository).summary("张", 12L, "在院患者", "自费", "NOT_STARTED", true, scope);
    }

    @Test
    void historyChecksDataScopeBeforeReadingAuditRecords() {
        var repository = mock(ArrearsRecordRepository.class);
        var scopes = mock(DataScopeService.class);
        var historyService = mock(ArrearsRecordHistoryService.class);
        var authentication = mock(Authentication.class);
        var scope = new DataScope(false, Set.of(12L), 34L);
        var item = new ArrearsRecordHistory(9, "财务员", OffsetDateTime.parse("2026-09-01T09:00:00+08:00"),
                "UPDATE", "{}", "{}", "更新欠费记录");
        when(scopes.resolve(authentication)).thenReturn(scope);
        when(historyService.list(88)).thenReturn(List.of(item));
        var controller = new ArrearsRecordController(repository, scopes, mock(AuditLogService.class), historyService, mock(ArrearsExportService.class));

        var response = controller.history(authentication, 88);

        assertThat(response.data()).containsExactly(item);
        verify(repository).find(88, scope);
        verify(historyService).list(88);
    }

    @Test
    void updateUsesScopedSnapshotsOperatorAndAudit() {
        var repository = mock(ArrearsRecordRepository.class);
        var scopes = mock(DataScopeService.class);
        var audit = mock(AuditLogService.class);
        var authentication = mock(Authentication.class);
        var scope = new DataScope(false, Set.of(12L), 34L);
        var before = mock(ArrearsRecordSummary.class);
        var after = mock(ArrearsRecordSummary.class);
        var request = new UpdateArrearsRequest("unpaid", " 已联系家属 ", "negotiating");
        var http = new MockHttpServletRequest();http.setRemoteAddr("10.0.0.8");
        when(authentication.getName()).thenReturn("finance01");when(scopes.resolve(authentication)).thenReturn(scope);
        when(repository.find(88, scope)).thenReturn(before);
        when(repository.update(88, request, scope, "finance01")).thenReturn(after);
        var controller = new ArrearsRecordController(repository, scopes, audit, mock(ArrearsRecordHistoryService.class), mock(ArrearsExportService.class));

        assertThat(controller.update(authentication, http, 88, request).data()).isSameAs(after);
        verify(audit).record(authentication, "ARREARS", "ARREARS_RECORD", "88", "UPDATE", before, after, "10.0.0.8");
    }

    @Test
    void exportUsesSameFiltersScopeAndRequestedFormat() {
        var repository = mock(ArrearsRecordRepository.class);var scopes = mock(DataScopeService.class);
        var audit = mock(AuditLogService.class);var exporter = mock(ArrearsExportService.class);
        var authentication = mock(Authentication.class);var scope = new DataScope(false, Set.of(12L), 34L);
        var page = new cn.hospital.rehab.common.api.PageResult<ArrearsRecordSummary>(List.of(), 0, 1, 20000);
        var file = new ArrearsExportService.ExportFile(new byte[]{1,2}, org.springframework.http.MediaType.APPLICATION_OCTET_STREAM, "欠费明细.xlsx", "XLSX");
        when(scopes.resolve(authentication)).thenReturn(scope);
        when(repository.export("张", 12L, "INPATIENT", "自费", "NOT_STARTED", true, scope)).thenReturn(page);
        when(exporter.create("xlsx", page.items(), page.total())).thenReturn(file);
        var controller = new ArrearsRecordController(repository, scopes, audit, mock(ArrearsRecordHistoryService.class), exporter);
        var http = new MockHttpServletRequest();http.setRemoteAddr("10.0.0.9");

        var response = controller.export(authentication, http, "xlsx", "张", 12L, "INPATIENT", "自费", "NOT_STARTED", true);

        assertThat(response.getBody()).containsExactly(1,2);
        verify(exporter).create("xlsx", page.items(), page.total());
        verify(audit).record(eq(authentication), eq("ARREARS"), eq("ARREARS_EXPORT"), eq("XLSX"), eq("EXPORT"), eq(null), eq(java.util.Map.of("rows",0,"format","XLSX")), eq("10.0.0.9"));
    }

    private static ArrearsRecordController controller(ArrearsRecordRepository repository, DataScopeService scopes) {
        return new ArrearsRecordController(repository, scopes, mock(AuditLogService.class), mock(ArrearsRecordHistoryService.class), mock(ArrearsExportService.class));
    }
}
