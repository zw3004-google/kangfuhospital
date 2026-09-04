package cn.hospital.rehab.discharge.api;

import cn.hospital.rehab.common.audit.AuditLogService;
import cn.hospital.rehab.common.api.PageResult;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DischargeExportControllerTest {
    @Test
    void exportsListColumnsWithAllCurrentFiltersAndScope() {
        DischargeRepository repository=mock(DischargeRepository.class);
        DataScopeService scopes=mock(DataScopeService.class);
        AuditLogService audit=mock(AuditLogService.class);
        Authentication auth=mock(Authentication.class);
        HttpServletRequest request=mock(HttpServletRequest.class);
        DataScope scope=DataScope.all();
        OffsetDateTime start=OffsetDateTime.parse("2026-09-01T00:00:00+08:00");
        OffsetDateTime end=OffsetDateTime.parse("2026-10-01T00:00:00+08:00");
        DischargeSummary row=new DischargeSummary(1,2,"ZY001",1,"张三","男","康复一科","脑卒中",
                "李医生",start,end,null,null,null,null,null,null,null,null,null,"已填报",List.of(),null,false,null,true,null,null,null,null);
        when(scopes.resolve(auth)).thenReturn(scope);
        when(repository.export("张",12L,"OUTPATIENT",start,end,false,"OUTPATIENT",scope))
                .thenReturn(new PageResult<>(List.of(row),1,1,20000));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        var response=new DischargeExportController(repository,scopes,audit,new DischargeExportService())
                .export(auth,request,"张",12L,"OUTPATIENT",start,end,false,"OUTPATIENT",null);
        String csv=new String(response.getBody(), StandardCharsets.UTF_8);

        verify(repository).export("张",12L,"OUTPATIENT",start,end,false,"OUTPATIENT",scope);
        assertThat(csv).startsWith("\uFEFF患者姓名,患者性别,住院号,住院次数,所属科室,入院时间,主诊断,主管医生");
        assertThat(csv).contains("预约营养会诊时间,预约居家康复时间,最近随访时间,状态,异常编码,异常原因");
        assertThat(csv).contains("张三,男,ZY001,1,康复一科");
        assertThat(response.getHeaders().getContentDisposition().toString()).contains("UTF-8");
        @SuppressWarnings("unchecked")
        var details = (org.mockito.ArgumentCaptor<Map<String, Object>>) (org.mockito.ArgumentCaptor<?>) org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(audit).record(eq(auth),eq("DISCHARGE"),eq("DISCHARGE_EXPORT"),eq("CSV"),eq("EXPORT"),isNull(),details.capture(),eq("127.0.0.1"));
        assertThat(details.getValue()).containsEntry("rows", 1).containsEntry("format", "CSV");
    }
}
