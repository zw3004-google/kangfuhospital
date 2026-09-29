package cn.hospital.rehab.arrears.push;

import cn.hospital.rehab.common.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArrearsNotificationDailyScheduleTest {
    @Test
    void schedulesEveryDayAtEightInShanghaiAndFindsEligibleWecomRecipients() throws Exception {
        Scheduled scheduled = ArrearsNotificationScheduler.class.getDeclaredMethod("createArrearsNotice").getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("${app.messaging.arrears-cron:0 0 8 * * *}");
        assertThat(scheduled.zone()).isEqualTo("Asia/Shanghai");

        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        new ArrearsNotificationScheduler(jdbc, mock(ArrearsNoticeService.class), mock(DataScopeService.class))
                .createArrearsNotice();

        verify(jdbc).sql(argThat(sql -> sql.contains("u.wecom_user_id IS NOT NULL")
                && sql.contains("DEPARTMENT_DIRECTOR")
                && sql.contains("ATTENDING_DOCTOR")
                && sql.contains("BOOL_OR")
                && !sql.contains("SMS")));
    }
}