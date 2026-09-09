package cn.hospital.rehab.integration.his;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import cn.hospital.rehab.common.audit.AuditLogService;
import java.util.Map;

@Component
public class HisSyncScheduler {
    private static final Logger log=LoggerFactory.getLogger(HisSyncScheduler.class);
    private final HisSyncConfigService configs;
    private final HisSyncCoordinator coordinator;
    private final AuditLogService audit;
    public HisSyncScheduler(HisSyncConfigService configs,HisSyncCoordinator coordinator,AuditLogService audit){this.configs=configs;this.coordinator=coordinator;this.audit=audit;}

    @Scheduled(cron="0 * * * * *",zone="Asia/Shanghai")
    public void runDue() {
        for(HisSyncType type:configs.dueTypes()) try {
            var result=coordinator.trigger(type,"AUTO",null);
            audit.record(null,"HIS_SYNC",type.name(),result.batchNo(),"AUTO_TRIGGER",null,
                    Map.of("batchNo",result.batchNo(),"total",result.total(),"success",result.success()));
        } catch(RuntimeException exception){
            String reason=HisSyncBatchService.safe(exception.getMessage());
            log.warn("HIS自动同步失败 type={}, reason={}",type,reason);
            audit.record(null,"HIS_SYNC",type.name(),type.name(),"AUTO_FAILED",null,Map.of("reason",reason));
        }
    }
}
