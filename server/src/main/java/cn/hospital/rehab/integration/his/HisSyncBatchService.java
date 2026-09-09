package cn.hospital.rehab.integration.his;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class HisSyncBatchService {
    private final JdbcClient jdbc;
    public HisSyncBatchService(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String recordEmptySuccess(HisSyncType type, String triggerType, Long startedBy) {
        String batchNo = prefix(type) + "-API-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6);
        jdbc.sql("""
                INSERT INTO import_batch(batch_no,business_type,source_type,status,total_count,success_count,
                  added_count,overwritten_count,skipped_count,summary_status,finished_at,transaction_code,trigger_type,started_by)
                VALUES (:batchNo,:businessType,'API','SUCCESS',0,0,0,0,0,'READY',CURRENT_TIMESTAMP,
                  :transactionCode,:triggerType,:startedBy)
                """).param("batchNo",batchNo).param("businessType",type.businessType())
                .param("transactionCode",type.transactionCode()).param("triggerType",triggerType)
                .param("startedBy",startedBy).update();
        return batchNo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String recordGatewayFailure(HisSyncType type, String triggerType, Long startedBy, String message) {
        String batchNo = prefix(type) + "-API-FAILED-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6);
        jdbc.sql("""
                INSERT INTO import_batch(batch_no,business_type,source_type,status,total_count,failure_count,
                  error_message,summary_status,finished_at,transaction_code,trigger_type,started_by)
                VALUES (:batchNo,:businessType,'API','FAILED',0,1,:message,'FAILED',CURRENT_TIMESTAMP,
                  :transactionCode,:triggerType,:startedBy)
                """).param("batchNo",batchNo).param("businessType",type.businessType())
                .param("message",safe(message)).param("transactionCode",type.transactionCode())
                .param("triggerType",triggerType).param("startedBy",startedBy).update();
        return batchNo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAttempt(HisSyncType type) {
        jdbc.sql("UPDATE his_sync_config SET last_attempt_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE sync_type=:type")
                .param("type",type.name()).update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(HisSyncType type, String batchNo) {
        jdbc.sql("UPDATE his_sync_config SET last_success_at=CURRENT_TIMESTAMP,last_batch_no=:batch,last_error=NULL,updated_at=CURRENT_TIMESTAMP WHERE sync_type=:type")
                .param("batch",batchNo).param("type",type.name()).update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(HisSyncType type, String batchNo, String message) {
        jdbc.sql("UPDATE his_sync_config SET last_batch_no=:batch,last_error=:error,updated_at=CURRENT_TIMESTAMP WHERE sync_type=:type")
                .param("batch",batchNo).param("error",safe(message)).param("type",type.name()).update();
    }

    private static String prefix(HisSyncType type) { return type == HisSyncType.PATIENT_INFO ? "DIS" : "ARR"; }
    static String safe(String message) {
        if (message == null || message.isBlank()) return "HIS同步失败";
        String value = message.replaceAll("[\\r\\n]+", " ").replaceAll("\\b\\d{6,}\\b", "***");
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
