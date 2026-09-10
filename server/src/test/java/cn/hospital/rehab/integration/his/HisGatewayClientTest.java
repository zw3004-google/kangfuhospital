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
    void buildsWilinkRequestEnvelope() {
        var properties = new HisProperties();
        properties.setOrganizationCode("994717");
        properties.setApplicationId("WiNEX");
        properties.setLicenseId("license-value");
        properties.setPageSize(20);
        var request = new HisGatewayClient(properties,new ObjectMapper())
                .request(HisSyncType.PATIENT_INFO,2);

        var head=request.path("Request").path("Head");
        var body=request.path("Request").path("Body");
        assertThat(head.path("TranCode").asText()).isEqualTo("BDKF-ZYHZXX-xcx");
        assertThat(head.path("OrgId").asText()).isEqualTo("994717");
        assertThat(head.path("AppId").asText()).isEqualTo("WiNEX");
        assertThat(head.path("RecAppId").asText()).isEqualTo("HIS");
        assertThat(head.path("MessageId").asText()).isNotBlank();
        assertThat(body.path("size").asInt()).isEqualTo(20);
        assertThat(body.path("page").asText()).isEqualTo("2");
        assertThat(request.has("TransactionCode")).isFalse();
    }

    @Test
    void parsesActualWilinkSuccessResponse() {
        var page=client.parse("""
                {"Response":{"Head":{"AckCode":"100","AckMessage":"调用成功"},
                "Body":{"data":{"content":[{"住院号":"A001","住院次数":1}],
                "total":1,"size":20,"page":"1"}}}}
                """,1,200);
        assertThat(page.rows()).hasSize(1);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.page()).isEqualTo(1);
    }

    @Test
    void rejectsBusinessFailureWithoutLeakingPayload() {
        assertThatThrownBy(()->client.parse("""
                {"AckCode":"E01","AckMessage":"机构不可用"}
                """,1,100))
                .isInstanceOf(HisGatewayException.class).hasMessageContaining("E01").hasMessageContaining("机构不可用");
    }

    @Test
    void rejectsFlatGatewayFailure() {
        assertThatThrownBy(()->client.parse("""
                {"code":3,"message":"API接口未找到！","timestamp":"2026-09-10 16:00:00"}
                """,1,100))
                .isInstanceOf(HisGatewayException.class).hasMessageContaining("3").hasMessageContaining("API接口未找到");
    }

    @Test
    void mapsPatientInfoWithoutPlannedDischarge() {
        var rows=HisSyncCoordinator.mapPatients(List.of(Map.of(
                "住院号","A001","住院次数","2","患者姓名","张某","所属科室","康复一科",
                "预计出院时间","2026-09-10","医保类型","职工医保")));
        assertThat(rows.getFirst().plannedDischargeAt).isNull();
        assertThat(rows.getFirst().medicalInsuranceType).isEqualTo("职工医保");
    }

    @Test
    void mapsInpatientInterfaceArrearsAmount() {
        var rows=HisSyncCoordinator.mapArrears(List.of(Map.of(
                "住院号","A001","住院次数",1,"姓名","测试患者","住院病区","测试科室",
                "欠费金额(元)","123.45")),HisSyncType.INPATIENT_ARREARS);

        assertThat(rows.getFirst().interfaceArrearsAmount).isEqualTo("123.45");
    }
}
