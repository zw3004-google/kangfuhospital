package cn.hospital.rehab.common.importing;

public record ImportError(int rowNumber, String inpatientNo, Integer admissionTimes, String patientName,
                          String fieldName, String originalValue, String errorCode,
                          String message) {
}
