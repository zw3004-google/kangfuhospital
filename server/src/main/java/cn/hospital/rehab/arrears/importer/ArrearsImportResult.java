package cn.hospital.rehab.arrears.importer;

public record ArrearsImportResult(String batchNo, int total, int success, int failure, int added, int overwritten, int skipped, int doctorMatched,
                                  int doctorUnmatched, int doctorAmbiguous) {
}
