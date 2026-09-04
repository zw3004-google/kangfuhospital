package cn.hospital.rehab.discharge.api;

import cn.hospital.rehab.common.api.PageResult;
import cn.hospital.rehab.common.audit.AuditLogService;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import cn.hospital.rehab.common.security.FieldPermissionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DischargeControllerTest {
    private final DischargeRepository repository = mock(DischargeRepository.class);
    private final DataScopeService scopes = mock(DataScopeService.class);
    private final DischargeRecordHistoryService history = mock(DischargeRecordHistoryService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final FieldPermissionService fields = mock(FieldPermissionService.class);
    private final DataScope scope = new DataScope(false, Set.of(12L), 34L);
    private final DischargeController controller = new DischargeController(repository, scopes,
            mock(AuditLogService.class), history, fields);

    @Test
    void pagePassesAllFiltersAndResolvedScope() {
        OffsetDateTime start = OffsetDateTime.parse("2026-09-01T00:00:00+08:00");
        OffsetDateTime end = OffsetDateTime.parse("2026-10-01T00:00:00+08:00");
        PageResult<DischargeSummary> expected = new PageResult<>(List.of(), 0, 2, 100);
        when(scopes.resolve(authentication)).thenReturn(scope);
        when(repository.page("李医生", 12L, "NUTRITION", start, end, false, "ABNORMAL", scope, 2, 100))
                .thenReturn(expected);

        assertThat(controller.page(authentication,"李医生",12L,"NUTRITION",start,end,false,"ABNORMAL",2,100).data())
                .isSameAs(expected);
    }

    @Test
    void summaryUsesSameFiltersAndScope() {
        when(scopes.resolve(authentication)).thenReturn(scope);
        DischargeSummaryStats expected = new DischargeSummaryStats(10,8,3,4,2,3,5);
        when(repository.summary("张",12L,null,null,null,null,scope)).thenReturn(expected);

        assertThat(controller.summary(authentication,"张",12L,null,null,null,null).data()).isSameAs(expected);
    }

    @Test
    void detailIsDataScoped() {
        when(scopes.resolve(authentication)).thenReturn(scope);
        DischargeSummary expected = mock(DischargeSummary.class);
        when(repository.find(88L,scope)).thenReturn(expected);

        assertThat(controller.detail(authentication,88L).data()).isSameAs(expected);
        verify(repository).find(88L,scope);
    }

    @Test
    void historyChecksRecordScopeBeforeReadingAuditData() {
        when(scopes.resolve(authentication)).thenReturn(scope);
        DischargeRecordHistory item = new DischargeRecordHistory(1,"doctor01",
                OffsetDateTime.parse("2026-09-03T08:00:00+08:00"),"UPDATE","{}","{}");
        when(history.list(88L)).thenReturn(List.of(item));

        assertThat(controller.history(authentication,88L).data()).containsExactly(item);
        verify(repository).find(88L,scope);
        verify(history).list(88L);
    }

    @Test
    void filterOptionsAreLimitedByResolvedScope() {
        when(scopes.resolve(authentication)).thenReturn(scope);
        var expected=List.of(new DischargeDepartmentOption(12L,"康复一科"));
        when(repository.filterOptions(scope)).thenReturn(expected);

        assertThat(controller.filterOptions(authentication).data()).isEqualTo(expected);
        verify(repository).filterOptions(scope);
    }

    @Test
    void updateChecksEachSubmittedBusinessFieldPermission() {
        when(scopes.resolve(authentication)).thenReturn(scope);
        DischargeSummary current=mock(DischargeSummary.class);
        when(repository.find(88L,scope)).thenReturn(current);
        UpdateDischargeRequest request=new UpdateDischargeRequest("2026-09-10T08:00:00+08:00",false,null,null,true,"良好",null,null,"2026-09-20T08:00:00+08:00",false,null,"门诊员甲","未到诊","[]");
        when(repository.update(88L,request,scope)).thenReturn(current);

        controller.update(authentication,mock(HttpServletRequest.class),88L,request);

        verify(fields).require(authentication,"FIELD_ATTENDING_DOCTOR");
        verify(fields).require(authentication,"FIELD_OUTPATIENT");
        verify(fields).require(authentication,"FIELD_FOLLOW_UP");
    }
}
