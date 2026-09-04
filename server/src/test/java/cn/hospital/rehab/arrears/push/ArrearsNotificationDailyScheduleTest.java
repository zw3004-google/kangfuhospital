package cn.hospital.rehab.arrears.push;

import cn.hospital.rehab.common.security.DataScope;
import cn.hospital.rehab.common.security.DataScopeService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArrearsNotificationDailyScheduleTest {
    @Test
    void schedulesEveryDayAtEightInShanghaiAndUsesDailyWecomDeduplication() throws Exception {
        Scheduled scheduled = ArrearsNotificationScheduler.class.getDeclaredMethod("createArrearsNotice").getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("${app.messaging.arrears-cron:0 0 8 * * *}");
        assertThat(scheduled.zone()).isEqualTo("Asia/Shanghai");

        JdbcClient jdbc = mock(JdbcClient.class, RETURNS_DEEP_STUBS);
        ArrearsNoticeService notices = mock(ArrearsNoticeService.class);
        var department = new ArrearsNoticeService.DepartmentArrears("神经康复一科", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO);
        var preview = new ArrearsNoticeService.NoticePreview("ARR-1", OffsetDateTime.parse("2026-09-01T08:00:00+08:00"),
                "全院", BigDecimal.TEN, List.of(department), ArrearsNoticeService.SYSTEM_LINK, "企微通报内容");
        when(notices.preview(DataScope.all(), "全院")).thenReturn(Optional.of(preview));

        new ArrearsNotificationScheduler(jdbc, notices, mock(DataScopeService.class)).createArrearsNotice();

        verify(notices).preview(DataScope.all(), "全院");
        verify(jdbc).sql(argThat(sql -> sql.contains("u.wecom_user_id IS NOT NULL")
                && sql.contains("DEPARTMENT_DIRECTOR")
                && sql.contains("ATTENDING_DOCTOR")
                && !sql.contains("SMS")));
    }
}
