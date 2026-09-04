package cn.hospital.rehab.discharge.api;
import cn.hospital.rehab.common.security.DataScopeService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/discharge/records")
public class DischargeExportController {
 private final DischargeRepository repository; private final DataScopeService scopes;private final AuditLogService audit; private final DischargeExportService exportService;
 public DischargeExportController(DischargeRepository repository, DataScopeService scopes,AuditLogService audit,DischargeExportService exportService){this.repository=repository;this.scopes=scopes;this.audit=audit;this.exportService=exportService;}
 @GetMapping("/export")
 public ResponseEntity<byte[]> export(Authentication auth,HttpServletRequest http,@RequestParam(required=false)String keyword,@RequestParam(required=false)Long departmentId,@RequestParam(required=false)String timeType,@RequestParam(required=false)java.time.OffsetDateTime startAt,@RequestParam(required=false)java.time.OffsetDateTime endAt,@RequestParam(required=false)Boolean discharged,@RequestParam(required=false)String category,@RequestParam(required=false)String format){
  var page=repository.export(keyword,departmentId,timeType,startAt,endAt,discharged,category,scopes.resolve(auth));
  var file=exportService.create(format,page.items(),page.total());
  audit.record(auth,"DISCHARGE","DISCHARGE_EXPORT",file.format(),"EXPORT",null,java.util.Map.of("rows",page.items().size(),"format",file.format()),http.getRemoteAddr());
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,file.contentDisposition()).contentType(file.mediaType()).body(file.content());
 }
}
