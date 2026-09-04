package cn.hospital.rehab.discharge.api;

import cn.hospital.rehab.common.api.CsvEscaper;
import com.alibaba.excel.EasyExcel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DischargeExportService {
    private static final ZoneId EXPORT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final List<String> HEADERS = List.of(
            "患者姓名", "患者性别", "住院号", "住院次数", "所属科室", "入院时间", "主诊断", "主管医生",
            "预约复诊时间", "复诊是否到诊", "复诊到诊时间", "复诊填报人", "未到诊原因", "预计出院时间", "实际出院时间",
            "预约营养会诊时间", "预约居家康复时间", "最近随访时间", "状态", "异常编码", "异常原因",
            "特殊患者", "特殊患者原因", "需要随访", "7天随访", "30天随访", "60天随访", "随访详情"
    );

    public ExportFile create(String requestedFormat, List<DischargeSummary> records, long total) {
        if (total > records.size()) throw new IllegalArgumentException("导出结果超过20000条，请缩小筛选范围后重试");
        String format = requestedFormat == null || requestedFormat.isBlank() ? "csv" : requestedFormat.trim().toLowerCase();
        if (!List.of("csv", "xlsx").contains(format)) throw new IllegalArgumentException("导出格式仅支持csv或xlsx");
        List<List<String>> rows = records.stream().map(this::row).toList();
        byte[] content = "xlsx".equals(format) ? excel(rows) : csv(rows);
        String filename = "出院统计明细_" + OffsetDateTime.now(EXPORT_ZONE).format(FILE_TIME) + "." + format;
        String type = "xlsx".equals(format) ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv;charset=UTF-8";
        return new ExportFile(content, MediaType.parseMediaType(type), filename, format.toUpperCase());
    }

    List<String> row(DischargeSummary r) {
        return List.of(text(r.patientName()), text(r.gender()), text(r.inpatientNo()), String.valueOf(r.admissionTimes()),
                text(r.departmentName()), time(r.admittedAt()), text(r.primaryDiagnosis()), text(r.doctorName()),
                time(r.latestOutpatientAppointmentAt()), yesNo(r.outpatientArrived()), time(r.outpatientArrivalAt()),
                text(r.outpatientReporter()), text(r.outpatientNoShowReason()), time(r.plannedDischargeAt()), time(r.actualDischargeAt()),
                time(r.latestNutritionAppointmentAt()), time(r.latestHomeRehabAppointmentAt()), time(r.latestFollowUpAt()),
                text(r.status()), r.abnormalCodes() == null || r.abnormalCodes().isEmpty() ? "—" : String.join("、", r.abnormalCodes()),
                text(r.abnormalReason()), yesNo(r.specialPatient()), text(r.specialReason()), yesNo(r.followUpRequired()),
                followUp(r.followUpDay7()), followUp(r.followUpDay30()), followUp(r.followUpDay60()), text(r.followUpDetailsJson()));
    }

    private byte[] csv(List<List<String>> rows) {
        StringBuilder value = new StringBuilder("\uFEFF").append(String.join(",", HEADERS)).append('\n');
        rows.forEach(row -> value.append(row.stream().map(CsvEscaper::value).collect(Collectors.joining(","))).append('\n'));
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] excel(List<List<String>> rows) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output).head(HEADERS.stream().map(List::of).toList()).sheet("出院统计明细").doWrite(rows);
        return output.toByteArray();
    }

    private static String time(OffsetDateTime value) { return value == null ? "—" : value.atZoneSameInstant(EXPORT_ZONE).format(TIME); }
    private static String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static String yesNo(Boolean value) { return value == null ? "—" : value ? "是" : "否"; }
    private static String followUp(String value) { return value == null ? "—" : Map.of("PENDING", "待随访", "COMPLETED", "已完成", "MISSED", "已逾期").getOrDefault(value, value); }

    public record ExportFile(byte[] content, MediaType mediaType, String filename, String format) {
        public String contentDisposition() {
            return "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        }
    }
}
