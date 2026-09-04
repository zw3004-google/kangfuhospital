package cn.hospital.rehab.discharge.consult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ConsultationRequest(@NotBlank String appointmentAt,@Size(max=128) String executorName,@Size(max=1000) String executionResult) {}
