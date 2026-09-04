package cn.hospital.rehab.arrears.push;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PushRecordPolicyTest {
    @Test
    void mapsFourBusinessStatusesAndTransitionalAliases() {
        assertThat(PushRecordPolicy.databaseStatuses("SENDING")).containsExactly("PENDING", "SENDING");
        assertThat(PushRecordPolicy.databaseStatuses("PENDING")).containsExactly("PENDING", "SENDING");
        assertThat(PushRecordPolicy.databaseStatuses("RETRYING")).containsExactly("RETRYING");
        assertThat(PushRecordPolicy.databaseStatuses("SUCCESS")).containsExactly("SENT");
        assertThat(PushRecordPolicy.databaseStatuses("SENT")).containsExactly("SENT");
        assertThat(PushRecordPolicy.databaseStatuses("FAILED")).containsExactly("FAILED");
        assertThat(PushRecordPolicy.databaseStatuses(null)).isEmpty();
        assertThatThrownBy(() -> PushRecordPolicy.databaseStatuses("CANCELLED"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("不支持的推送状态");
    }

    @Test
    void providesChineseStatusAndSafeContentSummary() {
        assertThat(PushRecordPolicy.displayStatus("PENDING")).isEqualTo("发送中");
        assertThat(PushRecordPolicy.displayStatus("SENDING")).isEqualTo("发送中");
        assertThat(PushRecordPolicy.displayStatus("RETRYING")).isEqualTo("重试中");
        assertThat(PushRecordPolicy.displayStatus("SENT")).isEqualTo("成功");
        assertThat(PushRecordPolicy.displayStatus("FAILED")).isEqualTo("失败");
        assertThat(PushRecordPolicy.displayStatus("CANCELLED")).isEqualTo("已取消");
        assertThat(PushRecordPolicy.summarize(" 第一行\n 第二行  ")).isEqualTo("第一行 第二行");
        assertThat(PushRecordPolicy.summarize(" ")).isEqualTo("—");
        assertThat(PushRecordPolicy.summarize("长".repeat(121)))
                .hasSize(121).endsWith("…");
    }

    @Test
    void onlyAllowsKnownPushBusinessTypes() {
        assertThat(PushRecordPolicy.supportedBusiness("ARREARS")).isTrue();
        assertThat(PushRecordPolicy.supportedBusiness("DISCHARGE")).isTrue();
        assertThat(PushRecordPolicy.supportedBusiness("UNKNOWN")).isFalse();
    }
}
