package cn.hospital.rehab.arrears.push;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class WeComClientTest {
    @Test void rejectsInvalidUserEvenWhenErrcodeIsZero() {
        var result = WeComClient.interpret(Map.of("errcode", 0, "errmsg", "ok", "invaliduser", "missing-user"));
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("INVALIDUSER");
    }

    @Test void acceptsCleanSuccessfulResponse() {
        assertThat(WeComClient.interpret(Map.of("errcode", 0, "errmsg", "ok", "msgid", "test")).success()).isTrue();
    }
}
