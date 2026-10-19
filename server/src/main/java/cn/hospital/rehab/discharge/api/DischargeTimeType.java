package cn.hospital.rehab.discharge.api;

public enum DischargeTimeType {
    ADMITTED,
    PLANNED_DISCHARGE,
    ACTUAL_DISCHARGE,
    OUTPATIENT,
    NUTRITION,
    REHAB_DOCTOR,
    HOME_REHAB,
    FOLLOW_UP,
    FOLLOW_UP_DAY7,
    FOLLOW_UP_DAY30,
    FOLLOW_UP_DAY60;

    public static DischargeTimeType parse(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("时间类型不正确");
        }
    }
}
