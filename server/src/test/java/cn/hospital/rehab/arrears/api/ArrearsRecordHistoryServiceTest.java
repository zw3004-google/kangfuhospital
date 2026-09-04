package cn.hospital.rehab.arrears.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ArrearsRecordHistoryServiceTest {
    private final ArrearsRecordHistoryService service =
            new ArrearsRecordHistoryService(mock(org.springframework.jdbc.core.simple.JdbcClient.class), new ObjectMapper());

    @Test
    void describesChangedBusinessFields() {
        String before = "{\"arrearsReason\":\"暂时困难\",\"recoveryProgress\":\"NOT_STARTED\",\"paymentStatus\":\"UNPAID\"}";
        String after = "{\"arrearsReason\":\"已联系家属\",\"recoveryProgress\":\"NEGOTIATING\",\"paymentStatus\":\"UNPAID\"}";

        assertThat(service.describe(before, after))
                .isEqualTo("欠费原因：暂时困难 → 已联系家属；追缴进度：NOT_STARTED → NEGOTIATING");
    }

    @Test
    void fallsBackForMalformedSnapshots() {
        assertThat(service.describe("not-json", null)).isEqualTo("更新欠费记录");
    }
}
