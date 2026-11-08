package cn.hospital.rehab.arrears.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DeleteArrearsRecordsRequest(@NotEmpty @Size(max = 200) List<Long> ids) {}
