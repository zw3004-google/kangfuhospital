package cn.hospital.rehab.discharge.api;

import java.time.OffsetDateTime;
import java.util.List;

public record DischargeSummary(
        long id,
        long encounterId,
        String inpatientNo,
        int admissionTimes,
        String patientName,
        String gender,
        String departmentName,
        String primaryDiagnosis,
        String doctorName,
        OffsetDateTime admittedAt,
        OffsetDateTime plannedDischargeAt,
        OffsetDateTime actualDischargeAt,
        OffsetDateTime latestOutpatientAppointmentAt,
        Boolean outpatientArrived,
        OffsetDateTime outpatientArrivalAt,
        String outpatientReporter,
        String outpatientNoShowReason,
        OffsetDateTime latestNutritionAppointmentAt,
        OffsetDateTime latestHomeRehabAppointmentAt,
        OffsetDateTime latestFollowUpAt,
        String status,
        List<String> abnormalCodes,
        String abnormalReason,
        Boolean specialPatient,
        String specialReason,
        Boolean followUpRequired,
        String followUpDay7,
        String followUpDay30,
        String followUpDay60,
        String followUpDetailsJson,
        OffsetDateTime updatedAt) {
    public DischargeSummary(long id,long encounterId,String inpatientNo,int admissionTimes,String patientName,String gender,
            String departmentName,String primaryDiagnosis,String doctorName,OffsetDateTime admittedAt,OffsetDateTime plannedDischargeAt,
            OffsetDateTime actualDischargeAt,OffsetDateTime latestOutpatientAppointmentAt,Boolean outpatientArrived,
            OffsetDateTime outpatientArrivalAt,String outpatientReporter,String outpatientNoShowReason,
            OffsetDateTime latestNutritionAppointmentAt,OffsetDateTime latestHomeRehabAppointmentAt,OffsetDateTime latestFollowUpAt,
            String status,List<String> abnormalCodes,String abnormalReason,Boolean specialPatient,String specialReason,
            Boolean followUpRequired,String followUpDay7,String followUpDay30,String followUpDay60,String followUpDetailsJson) {
        this(id,encounterId,inpatientNo,admissionTimes,patientName,gender,departmentName,primaryDiagnosis,doctorName,admittedAt,
                plannedDischargeAt,actualDischargeAt,latestOutpatientAppointmentAt,outpatientArrived,outpatientArrivalAt,
                outpatientReporter,outpatientNoShowReason,latestNutritionAppointmentAt,latestHomeRehabAppointmentAt,
                latestFollowUpAt,status,abnormalCodes,abnormalReason,specialPatient,specialReason,followUpRequired,
                followUpDay7,followUpDay30,followUpDay60,followUpDetailsJson,null);
    }
}
