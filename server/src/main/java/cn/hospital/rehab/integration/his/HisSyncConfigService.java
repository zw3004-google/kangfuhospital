package cn.hospital.rehab.integration.his;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class HisSyncConfigService {
    private final JdbcClient jdbc;
    public HisSyncConfigService(JdbcClient jdbc) { this.jdbc=jdbc; }

    public List<Config> list(HisSyncCoordinator coordinator) {
        return jdbc.sql("""
                SELECT sync_type,enabled,daily_time,last_attempt_at,last_success_at,last_batch_no,last_error,updated_at
                FROM his_sync_config ORDER BY sync_type
                """).query((r,n)->new Config(HisSyncType.valueOf(r.getString("sync_type")),r.getBoolean("enabled"),
                r.getObject("daily_time",LocalTime.class),r.getObject("last_attempt_at",OffsetDateTime.class),
                r.getObject("last_success_at",OffsetDateTime.class),r.getString("last_batch_no"),r.getString("last_error"),
                coordinator.isRunning(HisSyncType.valueOf(r.getString("sync_type"))),r.getObject("updated_at",OffsetDateTime.class))).list();
    }

    @Transactional
    public Config update(HisSyncType type, boolean enabled, LocalTime dailyTime, HisSyncCoordinator coordinator) {
        if (dailyTime==null) throw new IllegalArgumentException("每日同步时间不能为空");
        jdbc.sql("UPDATE his_sync_config SET enabled=:enabled,daily_time=:time,updated_at=CURRENT_TIMESTAMP WHERE sync_type=:type")
                .param("enabled",enabled).param("time",dailyTime).param("type",type.name()).update();
        return list(coordinator).stream().filter(item->item.syncType()==type).findFirst().orElseThrow();
    }

    public List<HisSyncType> dueTypes() {
        return jdbc.sql("""
                SELECT sync_type FROM his_sync_config WHERE enabled=true AND daily_time<=LOCALTIME
                  AND (last_attempt_at IS NULL OR last_attempt_at::date<CURRENT_DATE) ORDER BY sync_type
                """).query(String.class).list().stream().map(HisSyncType::valueOf).toList();
    }

    public record Config(HisSyncType syncType,boolean enabled,LocalTime dailyTime,OffsetDateTime lastAttemptAt,
                         OffsetDateTime lastSuccessAt,String lastBatchNo,String lastError,boolean running,OffsetDateTime updatedAt){}
}
