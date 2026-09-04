package cn.hospital.rehab.arrears.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpdateArrearsRequestTest {
    @Test
    void normalizesCodesAndLinksPaidState() {
        var unpaid = new UpdateArrearsRequest(" unpaid ", "原因", " negotiating ");
        var paidByProgress = new UpdateArrearsRequest("unpaid", null, "paid");
        var paidByStatus = new UpdateArrearsRequest("paid", null, "refused");

        assertThat(unpaid.paymentStatus()).isEqualTo("UNPAID");
        assertThat(unpaid.recoveryProgress()).isEqualTo("NEGOTIATING");
        assertThat(paidByProgress.paymentStatus()).isEqualTo("PAID");
        assertThat(paidByProgress.recoveryProgress()).isEqualTo("PAID");
        assertThat(paidByStatus.recoveryProgress()).isEqualTo("PAID");
    }

    @Test
    void rejectsUnsupportedPaymentOrRecoveryCodes() {
        assertThatThrownBy(() -> new UpdateArrearsRequest("UNKNOWN", null, null)).hasMessage("缴费状态不正确");
        assertThatThrownBy(() -> new UpdateArrearsRequest("UNPAID", null, "UNKNOWN")).hasMessage("追缴进度不正确");
    }
}
