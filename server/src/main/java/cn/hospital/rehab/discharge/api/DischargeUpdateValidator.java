package cn.hospital.rehab.discharge.api;

import java.time.OffsetDateTime;

final class DischargeUpdateValidator {
    private DischargeUpdateValidator() {}

    static void validateBefore(UpdateDischargeRequest request, DischargeSummary current) {
        if (Boolean.TRUE.equals(request.specialPatient()) && blank(request.specialReason()))
            throw new IllegalArgumentException("特殊患者必须填写特殊原因");
        if (current.actualDischargeAt()!=null && request.plannedDischargeAt()!=null &&
                !sameTime(request.plannedDischargeAt(),current.plannedDischargeAt()))
            throw new IllegalArgumentException("患者出院后不可修改预计出院时间");
    }

    static void validateAfter(DischargeSummary updated) {
        if (!updated.abnormalCodes().isEmpty() && blank(updated.abnormalReason()))
            throw new IllegalArgumentException("异常患者必须填写异常原因");
    }

    private static boolean sameTime(String requested,OffsetDateTime current){
        if(requested.isBlank())return current==null;
        try{return OffsetDateTime.parse(requested).isEqual(current);}catch(Exception ex){throw new IllegalArgumentException("预计出院时间格式不正确");}
    }
    private static boolean blank(String value){return value==null||value.isBlank();}
}
