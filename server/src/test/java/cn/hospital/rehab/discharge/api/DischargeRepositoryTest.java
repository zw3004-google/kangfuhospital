package cn.hospital.rehab.discharge.api;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DischargeRepositoryTest {

    @Test
    void parsesAllDistinctAbnormalCodesInStoredOrder() {
        assertThat(DischargeRepository.parseAbnormalCodes("DATE_MISMATCH, LATE_PLAN,DATE_MISMATCH"))
                .containsExactly("DATE_MISMATCH", "LATE_PLAN");
    }

    @Test
    void returnsEmptyCodesForMissingLegacyValue() {
        assertThat(DischargeRepository.parseAbnormalCodes(null)).isEmpty();
        assertThat(DischargeRepository.parseAbnormalCodes("  ")).isEmpty();
    }

    @Test
    void mapsProfileAndLatestBusinessTimesIntoSummary() throws Exception {
        ResultSet row = mock(ResultSet.class);
        OffsetDateTime admitted = at(1);
        OffsetDateTime planned = at(2);
        OffsetDateTime outpatient = at(3);
        OffsetDateTime nutrition = at(4);
        OffsetDateTime home = at(5);
        OffsetDateTime followUp = at(6);
        when(row.getLong("id")).thenReturn(11L);
        when(row.getLong("encounter_id")).thenReturn(22L);
        when(row.getInt("admission_times")).thenReturn(2);
        when(row.getString("inpatient_no")).thenReturn("ZY001");
        when(row.getString("patient_name")).thenReturn("测试患者");
        when(row.getString("gender")).thenReturn("女");
        when(row.getString("department_name")).thenReturn("康复一科");
        when(row.getString("primary_diagnosis")).thenReturn("脑卒中恢复期");
        when(row.getString("doctor_name_source")).thenReturn("测试医生");
        when(row.getString("abnormal_codes")).thenReturn("DATE_MISMATCH,LATE_PLAN");
        when(row.getObject("admitted_at", OffsetDateTime.class)).thenReturn(admitted);
        when(row.getObject("planned_discharge_at", OffsetDateTime.class)).thenReturn(planned);
        when(row.getObject("outpatient_appointment_at", OffsetDateTime.class)).thenReturn(outpatient);
        when(row.getObject("latest_nutrition_appointment_at", OffsetDateTime.class)).thenReturn(nutrition);
        when(row.getObject("latest_home_rehab_appointment_at", OffsetDateTime.class)).thenReturn(home);
        when(row.getObject("latest_follow_up_at", OffsetDateTime.class)).thenReturn(followUp);

        DischargeSummary result = new DischargeRepository(mock(JdbcClient.class)).map(row, 0);

        assertThat(result.gender()).isEqualTo("女");
        assertThat(result.primaryDiagnosis()).isEqualTo("脑卒中恢复期");
        assertThat(result.admittedAt()).isEqualTo(admitted);
        assertThat(result.latestOutpatientAppointmentAt()).isEqualTo(outpatient);
        assertThat(result.latestNutritionAppointmentAt()).isEqualTo(nutrition);
        assertThat(result.latestHomeRehabAppointmentAt()).isEqualTo(home);
        assertThat(result.latestFollowUpAt()).isEqualTo(followUp);
        assertThat(result.abnormalCodes()).containsExactly("DATE_MISMATCH", "LATE_PLAN");
        assertThat(result.status()).isEqualTo("已填报");
    }

    private static OffsetDateTime at(int day) {
        return OffsetDateTime.of(2026, 9, day, 8, 0, 0, 0, ZoneOffset.ofHours(8));
    }
}
