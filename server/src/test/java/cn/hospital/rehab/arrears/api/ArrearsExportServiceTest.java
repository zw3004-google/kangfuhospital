package cn.hospital.rehab.arrears.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArrearsExportServiceTest {
    private final ArrearsExportService service = new ArrearsExportService();

    @Test
    void csvUsesBomPageColumnsChineseMappingsAndInpatientPaymentRules() {
        var file = service.create("csv", List.of(record()), 1);
        String csv = new String(file.content(), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\uFEFF住院号,住院次数,姓名,住院病区");
        assertThat(csv).contains("欠费类型,主管医生,主管医生工号,入区日期");
        assertThat(csv).contains("ZY001,2,张三,康复一病区,自费,在院患者,李医生,D001,2026-08-01,未出区");
        assertThat(csv).contains("1000.00,200.00,—,—,800.00,600.00,\"家属,稍后缴费\",协商中,财务员,2026-09-01 10:20:30");
        assertThat(file.filename()).matches("欠费明细_\\d{8}_\\d{6}\\.csv");
        assertThat(file.contentDisposition()).startsWith("attachment; filename*=UTF-8''").contains("%E6%AC%A0%E8%B4%B9%E6%98%8E%E7%BB%86");
    }

    @Test
    void createsRealXlsxAndRejectsUnsupportedOrTruncatedExports() {
        var xlsx = service.create("xlsx", List.of(record()), 1);
        assertThat(xlsx.content()).startsWith((byte) 'P', (byte) 'K');
        assertThat(xlsx.mediaType().toString()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThatThrownBy(() -> service.create("pdf", List.of(record()), 1)).hasMessage("导出格式仅支持csv或xlsx");
        assertThatThrownBy(() -> service.create("csv", List.of(record()), 20001)).hasMessageContaining("超过20000条");
    }

    @Test
    void toleratesLegacyRowsWithoutArrearsTypeOrProgress() {
        var source = record();
        var legacy = new ArrearsRecordSummary(source.id(), source.inpatientNo(), source.admissionTimes(), source.patientName(),
                source.departmentName(), source.wardName(), source.feeType(), null, source.doctorName(), null, source.admittedAt(),
                source.dischargedAt(), source.totalCost(), source.prepaidAmount(), source.medicalInsurancePaid(),
                source.personalAccountPaid(), source.originalRequiredDeposit(), source.finalRequiredDeposit(), source.depositDifference(),
                source.arrearsAmount(), source.inArrears(), source.paymentStatus(), source.arrearsReason(), null,
                source.previousRecoveryProgress(), source.recoveryProgressLegacy(), source.lastOperatedBy(), source.sourceUpdatedAt());

        assertThat(service.row(legacy).get(7)).isEqualTo("—");
        assertThat(service.create("csv", List.of(legacy), 1).content()).isNotEmpty();
    }

    @Test
    void exportsDatabaseUtcTimestampsInShanghaiBusinessTime() {
        var source = record();
        var utc = new ArrearsRecordSummary(source.id(), source.inpatientNo(), source.admissionTimes(), source.patientName(),
                source.departmentName(), source.wardName(), source.feeType(), source.arrearsType(), source.doctorName(), source.doctorEmployeeNo(),
                OffsetDateTime.parse("2026-01-02T16:00:00Z"), source.dischargedAt(), source.totalCost(), source.prepaidAmount(),
                source.medicalInsurancePaid(), source.personalAccountPaid(), source.originalRequiredDeposit(), source.finalRequiredDeposit(),
                source.depositDifference(), source.arrearsAmount(), source.inArrears(), source.paymentStatus(), source.arrearsReason(),
                source.recoveryProgress(), source.previousRecoveryProgress(), source.recoveryProgressLegacy(), source.lastOperatedBy(),
                OffsetDateTime.parse("2026-09-02T11:13:39Z"));

        assertThat(service.row(utc).get(8)).isEqualTo("2026-01-03");
        assertThat(service.row(utc).get(19)).isEqualTo("2026-09-02 19:13:39");
    }

    private static ArrearsRecordSummary record() {
        return new ArrearsRecordSummary(1, "ZY001", 2, "张三", "康复科", "康复一病区", "自费", "INPATIENT", "李医生", "D001",
                OffsetDateTime.parse("2026-08-01T08:00:00+08:00"), null, new BigDecimal("1000"), new BigDecimal("200"),
                new BigDecimal("30"), new BigDecimal("20"), new BigDecimal("800"), new BigDecimal("800"), new BigDecimal("-600"),
                new BigDecimal("600"), true, "UNPAID", "家属,稍后缴费", "NEGOTIATING", "NOT_STARTED", null, "财务员",
                OffsetDateTime.parse("2026-09-01T10:20:30+08:00"));
    }
}
