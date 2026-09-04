package cn.hospital.rehab.arrears.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArrearsRecordHistoryService {
    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    public ArrearsRecordHistoryService(JdbcClient jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public List<ArrearsRecordHistory> list(long recordId) {
        return jdbc.sql("SELECT id,operator_name,operated_at,action_type,before_data::text before_data," +
                        "after_data::text after_data FROM operation_audit_log " +
                        "WHERE business_type='ARREARS_RECORD' AND business_id=:businessId " +
                        "ORDER BY operated_at DESC,id DESC")
                .param("businessId", String.valueOf(recordId))
                .query((r, n) -> {
                    String before = r.getString("before_data");
                    String after = r.getString("after_data");
                    return new ArrearsRecordHistory(r.getLong("id"), r.getString("operator_name"),
                            r.getObject("operated_at", java.time.OffsetDateTime.class),
                            r.getString("action_type"), before, after, describe(before, after));
                }).list();
    }

    String describe(String before, String after) {
        try {
            JsonNode oldValue = mapper.readTree(before);
            JsonNode newValue = mapper.readTree(after);
            List<String> changes = new ArrayList<>();
            addChange(changes, "欠费原因", oldValue, newValue, "arrearsReason");
            addChange(changes, "追缴进度", oldValue, newValue, "recoveryProgress");
            addChange(changes, "缴费状态", oldValue, newValue, "paymentStatus");
            return changes.isEmpty() ? "更新欠费记录" : String.join("；", changes);
        } catch (Exception ignored) {
            return "更新欠费记录";
        }
    }

    private static void addChange(List<String> changes, String label, JsonNode before, JsonNode after, String field) {
        String oldValue = value(before, field);
        String newValue = value(after, field);
        if (!oldValue.equals(newValue)) changes.add(label + "：" + display(oldValue) + " → " + display(newValue));
    }

    private static String value(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }

    private static String display(String value) {
        return value.isBlank() ? "（空）" : value;
    }
}
