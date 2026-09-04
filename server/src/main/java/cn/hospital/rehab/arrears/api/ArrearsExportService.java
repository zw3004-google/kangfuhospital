package cn.hospital.rehab.arrears.api;

import com.alibaba.excel.EasyExcel;
import cn.hospital.rehab.common.api.CsvEscaper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ArrearsExportService {
    private static final ZoneId EXPORT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final List<String> HEADERS = List.of("住院号", "住院次数", "姓名", "住院病区", "费别", "欠费类型", "主管医生", "主管医生工号",
            "入区日期", "出区日期", "总费用", "预交金", "医保支付", "个人账户支付", "应交押金", "欠费金额", "欠费原因", "追缴进度", "最近操作人", "数据更新时间");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public ExportFile create(String requestedFormat, List<ArrearsRecordSummary> records, long total) {
        if (total > records.size()) throw new IllegalArgumentException("导出结果超过20000条，请缩小筛选范围后重试");
        String format = requestedFormat == null ? "csv" : requestedFormat.trim().toLowerCase();
        if (!List.of("csv", "xlsx").contains(format)) throw new IllegalArgumentException("导出格式仅支持csv或xlsx");
        List<List<String>> rows = records.stream().map(this::row).toList();
        byte[] content = "xlsx".equals(format) ? excel(rows) : csv(rows);
        String filename = "欠费明细_" + OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).format(FILE_TIME) + "." + format;
        String type = "xlsx".equals(format) ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv;charset=UTF-8";
        return new ExportFile(content, MediaType.parseMediaType(type), filename, format.toUpperCase());
    }

    List<String> row(ArrearsRecordSummary r) {
        boolean inpatient = "INPATIENT".equals(r.arrearsType()) || "在院患者".equals(r.arrearsType()) || r.dischargedAt() == null;
        return List.of(text(r.inpatientNo()), String.valueOf(r.admissionTimes()), text(r.patientName()), text(first(r.wardName(), r.departmentName())),
                text(r.feeType()), arrearsType(r.arrearsType()), text(r.doctorName()), text(r.doctorEmployeeNo()), date(r.admittedAt()), r.dischargedAt() == null ? "未出区" : date(r.dischargedAt()),
                money(r.totalCost()), money(r.prepaidAmount()), inpatient ? "—" : money(r.medicalInsurancePaid()), inpatient ? "—" : money(r.personalAccountPaid()),
                money(r.finalRequiredDeposit()), money(r.arrearsAmount()), text(r.arrearsReason()), progress(r.recoveryProgress()), text(r.lastOperatedBy()), time(r.sourceUpdatedAt()));
    }

    private byte[] csv(List<List<String>> rows) {
        StringBuilder value = new StringBuilder("\uFEFF").append(String.join(",", HEADERS)).append('\n');
        rows.forEach(row -> value.append(row.stream().map(CsvEscaper::value).collect(java.util.stream.Collectors.joining(","))).append('\n'));
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] excel(List<List<String>> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output).head(HEADERS.stream().map(List::of).toList()).sheet("欠费明细").doWrite(rows);
        return output.toByteArray();
    }

    private static String money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String date(OffsetDateTime value) { return value == null ? "—" : value.atZoneSameInstant(EXPORT_ZONE).format(DATE); }
    private static String time(OffsetDateTime value) { return value == null ? "—" : value.atZoneSameInstant(EXPORT_ZONE).format(TIME); }
    private static String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static String first(String preferred, String fallback) { return preferred == null || preferred.isBlank() ? fallback : preferred; }
    private static String arrearsType(String value) { if(value==null)return "—";return java.util.Map.of("INPATIENT", "在院患者", "DISCHARGED_UNSETTLED", "出院未结算", "DISCHARGED_SETTLED", "出院已结算").getOrDefault(value, text(value)); }
    private static String progress(String value) { if(value==null)return "—";return java.util.Map.of("NOT_STARTED", "未催缴", "NEGOTIATING", "协商中", "REFUSED", "拒绝缴费", "LEGAL_ACTION", "移交法务发起诉讼", "PAID", "已缴费").getOrDefault(value, text(value)); }
    public record ExportFile(byte[] content, MediaType mediaType, String filename, String format) {
        public String contentDisposition() { return "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"); }
    }
}
