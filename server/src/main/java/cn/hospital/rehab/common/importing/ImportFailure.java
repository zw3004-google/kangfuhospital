package cn.hospital.rehab.common.importing;

import java.util.List;

public record ImportFailure(String batchNo, List<ImportError> errors) {
}
