package cn.hospital.rehab.discharge.importer;

import cn.hospital.rehab.common.importing.ImportError;
import cn.hospital.rehab.common.importing.ImportValidationException;
import cn.hospital.rehab.common.importing.FailedImportBatchRecorder;
import com.alibaba.excel.EasyExcel;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.*;
import java.time.format.*;
import java.util.*;

@Service
public class DischargeImportService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<DateTimeFormatter> FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    private final JdbcClient jdbc;
    private final FailedImportBatchRecorder failedBatches;
    public DischargeImportService(JdbcClient jdbc,FailedImportBatchRecorder failedBatches) { this.jdbc = jdbc; this.failedBatches=failedBatches; }

    @Transactional
    public Result importFile(MultipartFile file) {
        validateFile(file); List<DischargeImportRow> rows = read(file);
        if (rows.size() > 1000) throw new IllegalArgumentException("单次导入最多1000条");
        if (rows.isEmpty()) throw new IllegalArgumentException("导入文件没有数据行");
        try { validateRows(rows); }
        catch (ImportValidationException exception) {
            String failedBatchNo=failedBatches.record("DISCHARGE",file.getOriginalFilename(),rows.size(),exception.getErrors());
            throw new ImportValidationException(failedBatchNo,exception.getErrors());
        }
        jdbc.sql("SELECT pg_advisory_xact_lock(hashtext('IMPORT_DISCHARGE'))").query((rs,rowNum)->true).single();
        String batchNo = "DIS-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 6);
        long batchId = jdbc.sql("INSERT INTO import_batch(batch_no,business_type,source_type,original_filename,status,total_count) VALUES (:batchNo,'DISCHARGE','EXCEL',:filename,'PROCESSING',:total) RETURNING id")
                .param("batchNo", batchNo).param("filename", file.getOriginalFilename()).param("total", rows.size()).query(Long.class).single();
        int added=0,overwritten=0,matched=0,unmatched=0,ambiguous=0;
        for(DischargeImportRow row:rows){Long id=find(row);boolean exists=id!=null;DoctorMatch doctor=matchDoctor(row.doctorEmployeeNo);if(doctor.matched())matched++;else if(doctor.ambiguous())ambiguous++;else unmatched++;if(id==null)id=insert(row,doctor);else update(id,row,doctor);upsertDischarge(id,row);if(exists)overwritten++;else added++;}
        jdbc.sql("UPDATE import_batch SET status='SUCCESS',success_count=:success,added_count=:added,overwritten_count=:overwritten,doctor_matched_count=:matched,doctor_unmatched_count=:unmatched,doctor_ambiguous_count=:ambiguous,summary_status='READY',finished_at=CURRENT_TIMESTAMP WHERE id=:id")
                .param("success",rows.size()).param("added",added).param("overwritten",overwritten).param("matched",matched).param("unmatched",unmatched).param("ambiguous",ambiguous).param("id",batchId).update();
        return new Result(batchNo,rows.size(),rows.size(),added,overwritten,0,matched,unmatched,ambiguous);
    }
    private List<DischargeImportRow> read(MultipartFile file){try{return EasyExcel.read(file.getInputStream()).head(DischargeImportRow.class).sheet().doReadSync();}catch(IOException|RuntimeException e){throw new IllegalArgumentException("Excel读取失败："+e.getMessage(),e);}}
    private Long find(DischargeImportRow r){return jdbc.sql("SELECT id FROM patient_encounter WHERE inpatient_no=:no AND admission_times=:times").param("no",r.inpatientNo.trim()).param("times",r.admissionTimes).query(Long.class).optional().orElse(null);}
    private long insert(DischargeImportRow r,DoctorMatch d){return jdbc.sql("INSERT INTO patient_encounter(inpatient_no,admission_times,patient_name,department_id,ward_name,fee_type,doctor_name_source,doctor_employee_no,doctor_user_id,doctor_match_status,admitted_at,discharged_at) VALUES (:no,:times,:name,:department,:ward,:fee,:doctor,:employee,:doctorUserId,:doctorStatus,:admitted,:discharged) RETURNING id").param("no",r.inpatientNo.trim()).param("times",r.admissionTimes).param("name",r.patientName.trim()).param("department",departmentId(r.wardName)).param("ward",trim(r.wardName)).param("fee",trim(r.feeType)).param("doctor",trim(r.doctorName)).param("employee",trim(r.doctorEmployeeNo)).param("doctorUserId",d.userId()).param("doctorStatus",d.status()).param("admitted",date(r.admittedAt)).param("discharged",date(r.actualDischargeAt)).query(Long.class).single();}
    private void update(long id,DischargeImportRow r,DoctorMatch d){jdbc.sql("UPDATE patient_encounter SET patient_name=:name,department_id=COALESCE(:department,department_id),ward_name=COALESCE(:ward,ward_name),fee_type=COALESCE(:fee,fee_type),doctor_name_source=COALESCE(:doctor,doctor_name_source),doctor_employee_no=COALESCE(:employee,doctor_employee_no),doctor_user_id=CASE WHEN :employee IS NULL THEN doctor_user_id ELSE :doctorUserId END,doctor_match_status=CASE WHEN :employee IS NULL THEN doctor_match_status ELSE :doctorStatus END,admitted_at=COALESCE(:admitted,admitted_at),discharged_at=COALESCE(:discharged,discharged_at),source_updated_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=:id").param("name",r.patientName.trim()).param("department",departmentId(r.wardName)).param("ward",trim(r.wardName)).param("fee",trim(r.feeType)).param("doctor",trim(r.doctorName)).param("employee",trim(r.doctorEmployeeNo)).param("doctorUserId",d.userId()).param("doctorStatus",d.status()).param("admitted",date(r.admittedAt)).param("discharged",date(r.actualDischargeAt)).param("id",id).update();}
    private void upsertDischarge(long id,DischargeImportRow r){jdbc.sql("INSERT INTO discharge_record(encounter_id,planned_discharge_at,actual_discharge_at,planned_discharge_updated_at) VALUES (:id,:planned,:actual,CASE WHEN CAST(:planned AS TIMESTAMPTZ) IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END) ON CONFLICT(encounter_id) DO UPDATE SET planned_discharge_at=COALESCE(EXCLUDED.planned_discharge_at,discharge_record.planned_discharge_at),planned_discharge_updated_at=CASE WHEN EXCLUDED.planned_discharge_at IS NULL THEN discharge_record.planned_discharge_updated_at ELSE CURRENT_TIMESTAMP END,actual_discharge_at=COALESCE(EXCLUDED.actual_discharge_at,discharge_record.actual_discharge_at),updated_at=CURRENT_TIMESTAMP").param("id",id).param("planned",date(r.plannedDischargeAt)).param("actual",date(r.actualDischargeAt)).update();}

    private void validateRows(List<DischargeImportRow> rows){List<ImportError> errors=new ArrayList<>();Set<String> keys=new HashSet<>();for(int i=0;i<rows.size();i++){DischargeImportRow r=rows.get(i);int n=i+2;required(errors,n,r,"住院号",r.inpatientNo);required(errors,n,r,"姓名",r.patientName);required(errors,n,r,"住院病区",r.wardName);if(r.admissionTimes==null||r.admissionTimes<1)error(errors,n,r,"住院次数",String.valueOf(r.admissionTimes),"INVALID_FORMAT","住院次数必须为正整数");if(!blank(r.inpatientNo)&&r.admissionTimes!=null&&!keys.add(r.inpatientNo.trim()+"#"+r.admissionTimes))error(errors,n,r,"住院号+住院次数",r.inpatientNo+"/"+r.admissionTimes,"DUPLICATE_KEY_IN_FILE","文件内存在重复住院记录");validateDate(errors,n,r,"入区日期",r.admittedAt);validateDate(errors,n,r,"预计出院时间",r.plannedDischargeAt);validateDate(errors,n,r,"实际出院时间",r.actualDischargeAt);if(!blank(r.wardName)&&departmentId(r.wardName)==null)error(errors,n,r,"住院病区",r.wardName,"DEPARTMENT_NOT_FOUND","科室无法匹配");}if(!errors.isEmpty())throw new ImportValidationException(errors);}
    private Long departmentId(String name){return blank(name)?null:jdbc.sql("SELECT id FROM sys_department WHERE department_name=:name AND enabled=true").param("name",name.trim()).query(Long.class).optional().orElse(null);}
    private DoctorMatch matchDoctor(String no){if(blank(no))return new DoctorMatch(null,"NOT_FOUND");List<Long> ids=jdbc.sql("SELECT id FROM sys_user WHERE employee_no=:no AND enabled=true").param("no",no.trim()).query(Long.class).list();return ids.size()==1?new DoctorMatch(ids.getFirst(),"MATCHED"):new DoctorMatch(null,ids.isEmpty()?"NOT_FOUND":"AMBIGUOUS");}
    private static void validateFile(MultipartFile f){if(f==null||f.isEmpty())throw new IllegalArgumentException("请选择Excel文件");String n=f.getOriginalFilename()==null?"":f.getOriginalFilename().toLowerCase();if(!n.endsWith(".xlsx"))throw new IllegalArgumentException("仅支持xlsx文件");}
    private static void required(List<ImportError> e,int n,DischargeImportRow r,String f,String v){if(blank(v))error(e,n,r,f,v,"MISSING_REQUIRED",f+"不能为空");}private static void validateDate(List<ImportError> e,int n,DischargeImportRow r,String f,String v){if(blank(v))return;try{date(v);}catch(IllegalArgumentException x){error(e,n,r,f,v,"INVALID_FORMAT",x.getMessage());}}private static void error(List<ImportError> e,int n,DischargeImportRow r,String f,String v,String c,String m){e.add(new ImportError(n,r.inpatientNo,r.admissionTimes,f,v,c,m));}
    private static boolean blank(String v){return v==null||v.isBlank();}private static String trim(String v){return blank(v)?null:v.trim();}private static OffsetDateTime date(String v){if(blank(v))return null;String x=v.trim();for(DateTimeFormatter f:FORMATS)try{return x.length()>10?LocalDateTime.parse(x,f).atZone(ZONE).toOffsetDateTime():LocalDate.parse(x,f).atStartOfDay(ZONE).toOffsetDateTime();}catch(DateTimeParseException ignored){}throw new IllegalArgumentException("日期格式错误："+v);}
    public record Result(String batchNo,int total,int success,int added,int overwritten,int skipped,int doctorMatched,int doctorUnmatched,int doctorAmbiguous){}private record DoctorMatch(Long userId,String status){boolean matched(){return "MATCHED".equals(status);}boolean ambiguous(){return "AMBIGUOUS".equals(status);}}
}
