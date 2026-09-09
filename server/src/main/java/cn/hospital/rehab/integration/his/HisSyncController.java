package cn.hospital.rehab.integration.his;

import cn.hospital.rehab.common.api.ApiResponse;
import cn.hospital.rehab.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integration/his-sync")
public class HisSyncController {
    private final HisSyncCoordinator coordinator;
    private final HisSyncConfigService configs;
    private final AuditLogService audit;
    public HisSyncController(HisSyncCoordinator coordinator, HisSyncConfigService configs, AuditLogService audit) {
        this.coordinator=coordinator; this.configs=configs; this.audit=audit;
    }

    @PostMapping("/{type}/trigger")
    @PreAuthorize("hasAnyAuthority('PERM_API_HIS_SYNC_TRIGGER','ROLE_SYSTEM_ADMIN')")
    public ApiResponse<HisSyncCoordinator.SyncResult> trigger(@PathVariable HisSyncType type, Authentication authentication,
                                                               HttpServletRequest request) {
        HisSyncCoordinator.SyncResult result=coordinator.trigger(type,"MANUAL",authentication);
        audit.record(authentication,"HIS_SYNC",type.name(),result.batchNo(),"TRIGGER",null,
                Map.of("batchNo",result.batchNo(),"total",result.total(),"success",result.success()),request.getRemoteAddr());
        return ApiResponse.ok(result);
    }

    @GetMapping("/configs")
    @PreAuthorize("hasAnyAuthority('PERM_API_HIS_SYNC_CONFIG','ROLE_SYSTEM_ADMIN')")
    public ApiResponse<List<HisSyncConfigService.Config>> configs() { return ApiResponse.ok(configs.list(coordinator)); }

    @PutMapping("/configs/{type}")
    @PreAuthorize("hasAnyAuthority('PERM_API_HIS_SYNC_CONFIG','ROLE_SYSTEM_ADMIN')")
    public ApiResponse<HisSyncConfigService.Config> update(@PathVariable HisSyncType type,@RequestBody UpdateConfig request,
                                                            Authentication authentication,HttpServletRequest httpRequest) {
        var result=configs.update(type,request.enabled(),request.dailyTime(),coordinator);
        audit.record(authentication,"HIS_SYNC_CONFIG",type.name(),type.name(),"UPDATE",null,result,httpRequest.getRemoteAddr());
        return ApiResponse.ok(result);
    }
    public record UpdateConfig(boolean enabled, LocalTime dailyTime){}
}
