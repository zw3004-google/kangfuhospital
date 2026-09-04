package cn.hospital.rehab.arrears.importer;

import cn.hospital.rehab.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/arrears/import")
public class ArrearsImportController {
    private final ArrearsImportService service;
    private final AuditLogService audit;
    public ArrearsImportController(ArrearsImportService service,AuditLogService audit) { this.service = service;this.audit=audit; }

    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','FINANCE','OPERATIONS')")
    @PostMapping(consumes = "multipart/form-data")
    ApiResponse<ArrearsImportResult> importFile(Authentication auth,HttpServletRequest http,@RequestPart("file") MultipartFile file) {
        var result=service.importFile(file);audit.record(auth,"IMPORTING","IMPORT_BATCH",result.batchNo(),"IMPORT_ARREARS",null,result,http.getRemoteAddr());return ApiResponse.ok(result);
    }
}
