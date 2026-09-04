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
                SELECT batch_no,business_type,original_filename,status,total_count,success_count,failure_count,
                added_count,overwritten_count,skipped_count,summary_status,started_at,finished_at,error_message
                FROM import_batch WHERE (:type='' OR business_type=:type) ORDER BY started_at DESC,id DESC LIMIT 200
                """).param("type",type).query((r,n)->new Batch(r.getString("batch_no"),r.getString("business_type"),
                r.getString("original_filename"),r.getString("status"),r.getInt("total_count"),r.getInt("success_count"),
                r.getInt("failure_count"),r.getInt("added_count"),r.getInt("overwritten_count"),r.getInt("skipped_count"),
                r.getString("summary_status"),r.getObject("started_at",OffsetDateTime.class),r.getObject("finished_at",OffsetDateTime.class),
                r.getString("error_message"))).list());
    }

    @GetMapping("/{batchNo}/errors")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','OPERATIONS','FINANCE')")
    ApiResponse<List<ImportError>> errors(@PathVariable String batchNo){
        return ApiResponse.ok(jdbc.sql("""
                SELECT e.row_number,e.inpatient_no,e.admission_times,e.field_name,e.original_value,e.error_code,e.error_message
                FROM import_batch_error e JOIN import_batch b ON b.id=e.import_batch_id
                WHERE b.batch_no=:batchNo ORDER BY e.row_number,e.id
                """).param("batchNo",batchNo).query((r,n)->new ImportError(r.getInt("row_number"),r.getString("inpatient_no"),
                r.getObject("admission_times",Integer.class),r.getString("field_name"),r.getString("original_value"),
                r.getString("error_code"),r.getString("error_message"))).list());
    }
    public record Batch(String batchNo,String businessType,String filename,String status,int total,int success,int failure,
                        int added,int overwritten,int skipped,String summaryStatus,OffsetDateTime startedAt,
                        OffsetDateTime finishedAt,String errorMessage){}
}
