package cn.hospital.rehab.system.fee;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFeeCoefficientRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "^[A-Za-z0-9]+$", message = "费别编码仅支持字母和数字") String feeCode,
        @NotBlank @Size(max = 64) String feeType,
        @NotNull @DecimalMin("0.0000") @Digits(integer = 6, fraction = 4) BigDecimal coefficient) {
}
