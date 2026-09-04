package cn.hospital.rehab.discharge.consult;
import java.time.OffsetDateTime;
public record ConsultationRecord(long id,long encounterId,String inpatientNo,String patientName,OffsetDateTime appointmentAt,String executorName,String executionResult,boolean deleted) {}
