package cn.hospital.rehab.integration.his;

import cn.hospital.rehab.arrears.importer.ArrearsImportService;
import cn.hospital.rehab.discharge.importer.DischargeImportService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HisSyncCoordinatorPaginationTest {
    @Test
    void continuesWhenGatewayTotalIsOnlyCurrentPageCount() {
        var gateway=mock(HisGatewayClient.class);
        var properties=new HisProperties();
        properties.setMaxPages(10);
        when(gateway.fetch(HisSyncType.PATIENT_INFO,1)).thenReturn(page(1,1,"A001"));
        when(gateway.fetch(HisSyncType.PATIENT_INFO,2)).thenReturn(page(2,1,"A002"));
        when(gateway.fetch(HisSyncType.PATIENT_INFO,3)).thenReturn(new HisGatewayClient.Page(List.of(),0,3,1));
        var coordinator=new HisSyncCoordinator(gateway,properties,mock(ArrearsImportService.class),
                mock(DischargeImportService.class),mock(HisSyncBatchService.class),mock(JdbcClient.class));

        assertThat(coordinator.fetchAll(HisSyncType.PATIENT_INFO))
                .extracting(row->row.get("住院号"))
                .containsExactly("A001","A002");
    }

    @Test
    void mapsSecondaryDiagnosisFromHisPatientInfo() {
        var rows = HisSyncCoordinator.mapPatients(List.of(Map.of(
                "住院号", "A003", "住院次数", 1, "主诊断", "脑卒中", "次要诊断", "高血压")));

        assertThat(rows.getFirst().primaryDiagnosis).isEqualTo("脑卒中");
        assertThat(rows.getFirst().secondaryDiagnosis).isEqualTo("高血压");
    }
    private static HisGatewayClient.Page page(int page,int size,String inpatientNo) {
        return new HisGatewayClient.Page(List.of(Map.of("住院号",inpatientNo,"住院次数",1)),1,page,size);
    }
}
