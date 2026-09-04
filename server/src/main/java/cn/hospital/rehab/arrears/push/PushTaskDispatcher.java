package cn.hospital.rehab.arrears.push;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PushTaskDispatcher {
    private final JdbcClient jdbc; private final WeComClient weCom;
    public PushTaskDispatcher(JdbcClient jdbc,WeComClient weCom){this.jdbc=jdbc;this.weCom=weCom;}

    @Scheduled(fixedDelay=60000)
    public void dispatch(){for(Task task:claim())send(task);}

    @Transactional
    List<Task> claim(){return jdbc.sql("""
            WITH candidates AS (
              SELECT id FROM push_task WHERE status IN ('PENDING','RETRYING') AND scheduled_at<=CURRENT_TIMESTAMP
              ORDER BY scheduled_at,id FOR UPDATE SKIP LOCKED LIMIT 100
            )
            UPDATE push_task p SET status='SENDING',updated_at=CURRENT_TIMESTAMP FROM candidates c WHERE p.id=c.id
            RETURNING p.id,p.recipient_wecom_id,p.recipient_name,p.content,p.retry_count,p.scheduled_at,p.next_trigger_type,
              (SELECT COALESCE(MAX(a.attempt_no),0)+1 FROM push_attempt a WHERE a.task_id=p.id) attempt_no
            """).query((r,n)->new Task(r.getLong("id"),r.getString("recipient_wecom_id"),r.getString("recipient_name"),
            r.getString("content"),r.getInt("retry_count"),r.getObject("scheduled_at",OffsetDateTime.class),r.getString("next_trigger_type"),r.getInt("attempt_no"))).list();}

    private void send(Task task){
        int cycleAttempt=task.retry()+1;int attempt=task.attemptNo();WeComClient.SendResult result=weCom.send(task.userId(),task.content());
        jdbc.sql("""
                INSERT INTO push_attempt(task_id,attempt_no,trigger_type,scheduled_at,recipient_wecom_id,recipient_name,status,error_code,error_message)
                VALUES (:task,:attempt,:trigger,:scheduled,:recipientId,:recipientName,:status,:errorCode,:error)
                """).param("task",task.id()).param("attempt",attempt).param("trigger",task.triggerType())
                .param("scheduled",task.scheduledAt()).param("recipientId",task.userId()).param("recipientName",task.recipientName())
                .param("status",result.success()?"SENT":"FAILED").param("errorCode",result.errorCode()).param("error",result.error()).update();
        if(result.success())jdbc.sql("UPDATE push_task SET status='SENT',sent_at=CURRENT_TIMESTAMP,retry_count=:retry,last_error=NULL,next_trigger_type='AUTOMATIC',updated_at=CURRENT_TIMESTAMP WHERE id=:id").param("retry",cycleAttempt).param("id",task.id()).update();
        else if(cycleAttempt<4)jdbc.sql("UPDATE push_task SET status='RETRYING',retry_count=:retry,scheduled_at=CURRENT_TIMESTAMP + CASE :retry WHEN 1 THEN INTERVAL '1 minute' WHEN 2 THEN INTERVAL '5 minutes' ELSE INTERVAL '15 minutes' END,last_error=:error,next_trigger_type='AUTOMATIC',updated_at=CURRENT_TIMESTAMP WHERE id=:id").param("retry",cycleAttempt).param("error",result.error()).param("id",task.id()).update();
        else jdbc.sql("UPDATE push_task SET status='FAILED',retry_count=:retry,last_error=:error,next_trigger_type='AUTOMATIC',updated_at=CURRENT_TIMESTAMP WHERE id=:id").param("retry",cycleAttempt).param("error",result.error()).param("id",task.id()).update();
    }
    record Task(long id,String userId,String recipientName,String content,int retry,OffsetDateTime scheduledAt,String triggerType,int attemptNo){}
}
