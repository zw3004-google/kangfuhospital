package cn.hospital.rehab.discharge.api;

public enum DischargeTimeType {
    ADMITTED,
    PLANNED_DISCHARGE,
    ACTUAL_DISCHARGE,
    OUTPATIENT,
    NUTRITION,
    HOME_REHAB,
    FOLLOW_UP;

    public static DischargeTimeType parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("时间类型不正确");
        }
    }
}
