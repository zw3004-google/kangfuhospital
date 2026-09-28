package cn.hospital.rehab.discharge.push;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DischargeReminderPreviewTest {
    @Test
    void formatsPresidentOperationReportWithEveryDepartmentAndSystemLink() {
        String report = DischargeReminderScheduler.presidentOperationReport(LocalDate.of(2026, 9, 28), List.of(
                new DischargeReminderScheduler.DepartmentPatientCount("骨与关节病运动康复病房", 26),
                new DischargeReminderScheduler.DepartmentPatientCount("神经重症康复病房", 33)));

        assertThat(report).startsWith("截止到0928上午8点，在院患者一共59人，其中：")
                .contains("骨与关节病运动康复病房：26人")
                .contains("神经重症康复病房：33人")
                .endsWith("详情请登录康复医院运营管理系统查看：http://172.16.196.112");
    }
    @Test
    void describesEveryFormalReminderTypeAndRecipientScope() {
        var preview = DischargeReminderController.Preview.of(LocalDate.of(2026, 9, 3), 2, 3, 4, 1);

        assertThat(preview.totalPatients()).isEqualTo(10);
        assertThat(preview.items()).extracting(DischargeReminderController.ReminderItem::type)
                .containsExactly("NUTRITION", "HOME", "FOLLOW_UP", "ABNORMAL_REPORT");
        assertThat(preview.items()).allSatisfy(item -> {
            assertThat(item.recipientScope()).isNotBlank();
            assertThat(item.triggerBasis()).isNotBlank();
            assertThat(item.messagePreview()).contains("姓名脱敏").contains("住院号");
        });
        assertThat(preview.items().get(3).triggerBasis()).contains("填报异常");
    }
}
