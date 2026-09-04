package cn.hospital.rehab.discharge.push;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DischargeReminderPreviewTest {
    @Test
    void describesEveryFormalReminderTypeAndRecipientScope() {
        var preview = DischargeReminderController.Preview.of(LocalDate.of(2026, 9, 3), 2, 3, 4, 1);

        assertThat(preview.totalPatients()).isEqualTo(10);
        assertThat(preview.items()).extracting(DischargeReminderController.ReminderItem::type)
                .containsExactly("NUTRITION", "HOME", "FOLLOW_UP", "UNPLANNED");
        assertThat(preview.items()).allSatisfy(item -> {
            assertThat(item.recipientScope()).isNotBlank();
            assertThat(item.triggerBasis()).isNotBlank();
            assertThat(item.messagePreview()).contains("姓名脱敏").contains("住院号");
        });
        assertThat(preview.items().get(3).triggerBasis()).contains("计划缺失");
    }
}
