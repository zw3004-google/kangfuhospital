package cn.hospital.rehab.discharge.api;
import jakarta.validation.constraints.Size;
public record UpdateDischargeRequest(String plannedDischargeAt,Boolean specialPatient,@Size(max=2000)String specialReason,@Size(max=2000)String abnormalReason,Boolean followUpRequired,String followUpDay7,String followUpDay30,String followUpDay60,String outpatientAppointmentAt,Boolean outpatientArrived,String outpatientArrivalAt,@Size(max=128)String outpatientReporter,@Size(max=2000)String outpatientNoShowReason,String followUpDetailsJson) {}
