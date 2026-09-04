package cn.hospital.rehab.discharge.api;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DischargeRecordHistoryService {
    private final JdbcClient jdbc;

    public DischargeRecordHistoryService(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<DischargeRecordHistory> list(long recordId) {
        return jdbc.sql("SELECT id,operator_name,operated_at,action_type,before_data::text before_data," +
                        "after_data::text after_data FROM operation_audit_log " +
                        "WHERE business_type='DISCHARGE_RECORD' AND business_id=:businessId " +
                        "ORDER BY operated_at DESC,id DESC")
                .param("businessId", String.valueOf(recordId))
                .query((r, n) -> new DischargeRecordHistory(r.getLong("id"), r.getString("operator_name"),
                        r.getObject("operated_at", java.time.OffsetDateTime.class), r.getString("action_type"),
                        r.getString("before_data"), r.getString("after_data"))).list();
    }
}
