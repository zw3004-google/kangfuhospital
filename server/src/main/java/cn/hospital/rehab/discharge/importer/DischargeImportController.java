package cn.hospital.rehab.discharge.importer;
import cn.hospital.rehab.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;import org.springframework.security.core.Authentication;import cn.hospital.rehab.common.audit.AuditLogService;import jakarta.servlet.http.HttpServletRequest;
@RestController @RequestMapping("/api/discharge/import") @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','OPERATIONS')")
public class DischargeImportController {private final DischargeImportService service;private final AuditLogService audit;public DischargeImportController(DischargeImportService service,AuditLogService audit){this.service=service;this.audit=audit;}@PostMapping(consumes="multipart/form-data") public ApiResponse<DischargeImportService.Result> importFile(Authentication auth,HttpServletRequest http,@RequestPart("file") MultipartFile file){var result=service.importFile(file);audit.record(auth,"IMPORTING","IMPORT_BATCH",result.batchNo(),"IMPORT_DISCHARGE",null,result,http.getRemoteAddr());return ApiResponse.ok(result);}}
