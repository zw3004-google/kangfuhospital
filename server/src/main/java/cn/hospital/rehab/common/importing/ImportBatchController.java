package cn.hospital.rehab.common.importing;

import cn.hospital.rehab.common.api.ApiResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/import-batches")
public class ImportBatchController {
    private final JdbcClient jdbc;
    public ImportBatchController(JdbcClient jdbc){this.jdbc=jdbc;}

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','OPERATIONS','FINANCE')")
    ApiResponse<List<Batch>> list(@RequestParam(required=false) String businessType){
        String type=businessType==null?"":businessType.trim().toUpperCase();
        return ApiResponse.ok(jdbc.sql("""
                SELECT batch_no,business_type,source_type,transaction_code,trigger_type,original_filename,status,total_count,success_count,failure_count,
                added_count,overwritten_count,skipped_count,summary_status,started_at,finished_at,error_message
                FROM import_batch WHERE (:type='' OR business_type=:type) ORDER BY started_at DESC,id DESC LIMIT 200
                """).param("type",type).query((r,n)->new Batch(r.getString("batch_no"),r.getString("business_type"),
                r.getString("source_type"),r.getString("transaction_code"),r.getString("trigger_type"),
                r.getString("original_filename"),r.getString("status"),r.getInt("total_count"),r.getInt("success_count"),
                r.getInt("failure_count"),r.getInt("added_count"),r.getInt("overwritten_count"),r.getInt("skipped_count"),
                r.getString("summary_status"),r.getObject("started_at",OffsetDateTime.class),r.getObject("finished_at",OffsetDateTime.class),
                r.getString("error_message"))).list());
    }

    @GetMapping("/{batchNo}/errors")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','OPERATIONS','FINANCE')")
    ApiResponse<List<ImportError>> errors(@PathVariable String batchNo){
        return ApiResponse.ok(jdbc.sql("""
                SELECT e.row_number,e.inpatient_no,e.admission_times,e.patient_name,e.field_name,e.original_value,e.error_code,e.error_message
                FROM import_batch_error e JOIN import_batch b ON b.id=e.import_batch_id
                WHERE b.batch_no=:batchNo ORDER BY e.row_number,e.id
                """).param("batchNo",batchNo).query((r,n)->new ImportError(r.getInt("row_number"),r.getString("inpatient_no"),
                r.getObject("admission_times",Integer.class),r.getString("patient_name"),r.getString("field_name"),r.getString("original_value"),
                r.getString("error_code"),readableError(r.getString("error_code"),r.getString("field_name"),r.getString("original_value"),r.getString("error_message")))).list());
    }
    private static String readableError(String code, String field, String originalValue, String fallback) {
        String name = field == null || field.isBlank() ? "该字段" : "“" + field + "”";
        String value = originalValue == null || originalValue.isBlank() ? "" : "，当前值为“" + originalValue + "”";
        return switch (code == null ? "" : code) {
            case "MISSING_REQUIRED" -> name + "不能为空，请补充后重新导入。";
            case "INVALID_FORMAT" -> name + "格式不正确" + value + "，请按模板要求填写。";
            case "DEPARTMENT_NOT_FOUND" -> name + "中的科室“" + (originalValue == null || originalValue.isBlank() ? "空值" : originalValue) + "”未在系统启用科室中匹配到，请先维护科室信息。";
            case "DUPLICATE_KEY_IN_FILE" -> "住院号和住院次数在本次导入文件中重复，请仅保留一条患者记录。";
            default -> fallback == null || fallback.isBlank() ? "该记录校验未通过，请核对填写内容后重新导入。" : fallback;
        };
    }
    public record Batch(String batchNo,String businessType,String sourceType,String transactionCode,String triggerType,String filename,String status,int total,int success,int failure,
                        int added,int overwritten,int skipped,String summaryStatus,OffsetDateTime startedAt,
                        OffsetDateTime finishedAt,String errorMessage){}
}
