package cn.hospital.rehab.arrears.push;

import cn.hospital.rehab.common.audit.AuditLogService;
import cn.hospital.rehab.common.security.DataScopeService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PushRecordControllerValidationTest {
    @Test
    void rejectsAnInvertedDateRangeBeforeQueryingData() {
        var controller = new PushRecordController(mock(JdbcClient.class), mock(AuditLogService.class),
                mock(DataScopeService.class));

        assertThatThrownBy(() -> controller.page(mock(Authentication.class), 1, 50, null,
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1), "ARREARS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("开始日期不能晚于结束日期");
    }

    @Test
    void validatesBatchBusinessTypeAndOneToTwoHundredPositiveIds() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();
            assertThat(validator.validate(new PushRecordController.RetryBatchRequest("ARREARS", List.of(1L, 2L))))
                    .isEmpty();
            assertThat(validator.validate(new PushRecordController.RetryBatchRequest("UNKNOWN", List.of(1L))))
                    .extracting(violation -> violation.getPropertyPath().toString()).contains("businessType");
            assertThat(validator.validate(new PushRecordController.RetryBatchRequest("ARREARS", List.of())))
                    .extracting(violation -> violation.getPropertyPath().toString()).contains("ids");
            assertThat(validator.validate(new PushRecordController.RetryBatchRequest("ARREARS", List.of(0L))))
                    .isNotEmpty();
            assertThat(validator.validate(new PushRecordController.RetryBatchRequest("ARREARS",
                    java.util.stream.LongStream.rangeClosed(1, 201).boxed().toList())))
                    .extracting(violation -> violation.getPropertyPath().toString()).contains("ids");
        }
    }
}
