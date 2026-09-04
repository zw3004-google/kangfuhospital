package cn.hospital.rehab.common.importing;

import java.util.List;

public class ImportValidationException extends IllegalArgumentException {
    private final List<ImportError> errors;
    private final String batchNo;

    public ImportValidationException(List<ImportError> errors) {
        this(null, errors);
    }

    public ImportValidationException(String batchNo, List<ImportError> errors) {
        super("导入校验失败，共 " + errors.size() + " 项错误");
        this.batchNo = batchNo;
        this.errors = List.copyOf(errors);
    }

    public List<ImportError> getErrors() {
        return errors;
    }

    public String getBatchNo() { return batchNo; }
}
