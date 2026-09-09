package cn.hospital.rehab.integration.his;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HisGatewayClientTest {
    private final HisGatewayClient client=new HisGatewayClient(new HisProperties(),new ObjectMapper());

    @Test
    void parsesNestedPagedRows() {
        var page=client.parse("""
                {"AckCode":"0","Data":{"total":2,"page":1,"size":100,
                "rows":[{"住院号":"A001","住院次数":1},{"住院号":"A002","住院次数":2}]}}
                """,1,100);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.rows()).hasSize(2);
        assertThat(page.rows().getFirst()).containsEntry("住院号","A001");
    }

    @Test
    void rejectsBusinessFailureWithoutLeakingPayload() {
        assertThatThrownBy(()->client.parse("""
                {"AckCode":"E01","AckMessage":"机构不可用"}
                """,1,100))
                .isInstanceOf(HisGatewayException.class).hasMessageContaining("E01").hasMessageContaining("机构不可用");
    }

    @Test
    void mapsPatientInfoWithoutPlannedDischarge() {
        var rows=HisSyncCoordinator.mapPatients(List.of(Map.of(
                "住院号","A001","住院次数","2","患者姓名","张某","所属科室","康复一科",
                "预计出院时间","2026-09-10","医保类型","职工医保")));
        assertThat(rows.getFirst().plannedDischargeAt).isNull();
        assertThat(rows.getFirst().medicalInsuranceType).isEqualTo("职工医保");
    }
}
