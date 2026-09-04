package cn.hospital.rehab.system.fee;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FeeCoefficientServiceTest {

    private final FeeCoefficientRepository repository = mock(FeeCoefficientRepository.class);
    private final FeeCoefficientService service = new FeeCoefficientService(repository);

    @Test
    void enablingVersionDisablesPreviousVersionOfSameFeeType() {
        FeeCoefficient disabled = coefficient(2L, 20L, "YB01", "医保", false);
        FeeCoefficient enabled = coefficient(2L, 20L, "YB01", "医保", true);
        when(repository.findById(2L)).thenReturn(Optional.of(disabled));
        when(repository.enable(2L, "admin")).thenReturn(enabled);

        assertThat(service.enable(2L, "admin").enabled()).isTrue();
        verify(repository).disableEnabledVersion(20L, "admin");
        verify(repository).enable(2L, "admin");
    }

    @Test
    void enablingActiveVersionIsIdempotent() {
        FeeCoefficient active = coefficient(1L, 10L, "ZF01", "自费", true);
        when(repository.findById(1L)).thenReturn(Optional.of(active));

        assertThat(service.enable(1L, "admin")).isSameAs(active);
        verify(repository, never()).disableEnabledVersion(anyLong(), anyString());
        verify(repository, never()).enable(anyLong(), anyString());
    }

    private FeeCoefficient coefficient(long id, long feeTypeId, String feeCode, String feeType, boolean enabled) {
        return new FeeCoefficient(id, feeTypeId, feeCode, feeType, BigDecimal.ONE, enabled,
                OffsetDateTime.now(), null, OffsetDateTime.now(), "admin", enabled ? "admin" : null, null);
    }
}
