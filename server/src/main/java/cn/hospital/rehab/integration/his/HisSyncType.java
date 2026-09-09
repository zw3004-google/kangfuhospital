package cn.hospital.rehab.integration.his;

public enum HisSyncType {
    INPATIENT_ARREARS("BJKF_ZYCX", "ARREARS"),
    DISCHARGED_ARREARS("BDKF-cyqfcx", "ARREARS"),
    PATIENT_INFO("BDKF-ZYHZXX-xcx", "DISCHARGE");

    private final String transactionCode;
    private final String businessType;
    HisSyncType(String transactionCode, String businessType) {
        this.transactionCode = transactionCode;
        this.businessType = businessType;
    }
    public String transactionCode() { return transactionCode; }
    public String businessType() { return businessType; }
}
