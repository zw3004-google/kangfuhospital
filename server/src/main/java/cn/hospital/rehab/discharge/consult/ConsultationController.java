package cn.hospital.rehab.discharge.consult;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.security.DataScopeService;
import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.FieldPermissionService;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/discharge/consultations")
public class ConsultationController {
    private final ConsultationRepository repository;
    private final DataScopeService scopes;
    private final AuditLogService audit;
    private final FieldPermissionService fields;

    public ConsultationController(ConsultationRepository repository, DataScopeService scopes,AuditLogService audit,FieldPermissionService fields) {
        this.repository = repository;
        this.scopes = scopes;
        this.audit=audit;
        this.fields=fields;
    }

    @GetMapping
    public ApiResponse<List<ConsultationRecord>> list(Authentication auth, @RequestParam long encounterId,
                                                       @RequestParam String type) {
        return ApiResponse.ok(repository.list(encounterId, type, scopes.resolve(auth)));
    }

    @PostMapping
    public ApiResponse<ConsultationRecord> create(Authentication auth,HttpServletRequest http, @RequestParam long encounterId,
                                                   @RequestParam String type, @Valid @RequestBody ConsultationRequest request) {
        requireField(auth,type);
        var result=repository.create(encounterId,type,parse(request.appointmentAt()),request.executorName(),request.executionResult(),scopes.resolve(auth));audit.record(auth,"DISCHARGE","CONSULTATION",String.valueOf(result.id()),"CREATE",null,result,http.getRemoteAddr());return ApiResponse.ok(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<ConsultationRecord> update(Authentication auth,HttpServletRequest http, @PathVariable long id,
                                                   @RequestParam String type, @Valid @RequestBody ConsultationRequest request) {
        requireField(auth,type);
        DataScope scope=scopes.resolve(auth);var before=repository.find(id,type,scope);var after=repository.update(id,type,parse(request.appointmentAt()),request.executorName(),request.executionResult(),scope);audit.record(auth,"DISCHARGE","CONSULTATION",String.valueOf(id),"UPDATE",before,after,http.getRemoteAddr());return ApiResponse.ok(after);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth,HttpServletRequest http, @PathVariable long id, @RequestParam String type) {
        requireField(auth,type);
        DataScope scope=scopes.resolve(auth);var before=repository.find(id,type,scope);repository.delete(id,type,scope);audit.record(auth,"DISCHARGE","CONSULTATION",String.valueOf(id),"DELETE",before,null,http.getRemoteAddr());
        return ApiResponse.ok(null);
    }

    private static OffsetDateTime parse(String value) {
        OffsetDateTime result;
        try { result=OffsetDateTime.parse(value); }
        catch (Exception exception) { throw new IllegalArgumentException("预约时间应为ISO-8601格式"); }
        if(!result.isAfter(OffsetDateTime.now()))throw new IllegalArgumentException("预约时间不得早于当前时间");
        return result;
    }

    private void requireField(Authentication auth,String type) {
        fields.require(auth,"NUTRITION".equalsIgnoreCase(type)?"FIELD_NUTRITION":"FIELD_HOME_REHAB");
    }
}
