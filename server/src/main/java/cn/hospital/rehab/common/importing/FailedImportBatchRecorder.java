package cn.hospital.rehab.common.importing;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class FailedImportBatchRecorder {
    private final JdbcClient jdbc;

    public FailedImportBatchRecorder(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String record(String businessType, String filename, int total, List<ImportError> errors) {
        String prefix = "ARREARS".equals(businessType) ? "ARR" : "DIS";
        String batchNo = prefix + "-FAILED-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6);
        long id = jdbc.sql("""
                INSERT INTO import_batch(batch_no,business_type,source_type,original_filename,status,total_count,
                failure_count,error_message,summary_status,finished_at)
                VALUES (:batchNo,:businessType,'EXCEL',:filename,'FAILED',:total,:failures,:message,'FAILED',CURRENT_TIMESTAMP)
                RETURNING id
                """).param("batchNo",batchNo).param("businessType",businessType).param("filename",filename)
                .param("total",total).param("failures",errors.size()).param("message","导入校验失败，共 "+errors.size()+" 项错误")
                .query(Long.class).single();
        for (ImportError error : errors) jdbc.sql("""
                INSERT INTO import_batch_error(import_batch_id,row_number,inpatient_no,admission_times,field_name,
                original_value,error_code,error_message)
                VALUES (:batch,:row,:no,:times,:field,:value,:code,:message)
                """).param("batch",id).param("row",error.rowNumber()).param("no",error.inpatientNo())
                .param("times",error.admissionTimes()).param("field",error.fieldName()).param("value",error.originalValue())
                .param("code",error.errorCode()).param("message",error.message()).update();
        return batchNo;
    }
}
