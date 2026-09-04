package cn.hospital.rehab.arrears.report;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
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
public class ArrearsReportExportService {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public ExportFile create(ArrearsReportController.Report report) {
        if (report.latestSuccessfulBatch() == null) throw new IllegalArgumentException("暂无可导出的成功批次");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ExcelWriter writer = EasyExcel.write(output).build()) {
            WriteSheet summary = EasyExcel.writerSheet("报表汇总").head(head("项目", "内容")).build();
            writer.write(summaryRows(report), summary);
            WriteSheet departments = EasyExcel.writerSheet("科室排行").head(head("排名", "科室", "欠费人数", "欠费金额（元）")).build();
            writer.write(departmentRows(report.ranking()), departments);
            WriteSheet patients = EasyExcel.writerSheet("患者Top10").head(head("排名", "住院号", "住院次数", "姓名", "住院病区", "主管医生", "欠费类型", "欠费金额（元）", "追缴进度")).build();
            writer.write(patientRows(report.patientTop10()), patients);
        }
        String filename = "通报报表_" + OffsetDateTime.now(ZoneId.of("Asia/Shanghai")).format(FILE_TIME) + ".xlsx";
        return new ExportFile(output.toByteArray(), XLSX, filename);
    }

    List<List<String>> summaryRows(ArrearsReportController.Report report) {
        var batch = report.latestSuccessfulBatch();
        return List.of(List.of("数据范围", text(report.scopeLabel())), List.of("数据截至时间", time(batch.dataAsOf())),
                List.of("最新成功批次", text(batch.batchNo())), List.of("汇总状态", status(batch.summaryStatus())),
                List.of("欠费总额（元）", money(report.totalAmount())), List.of("欠费人数", String.valueOf(report.people())));
    }

    List<List<String>> departmentRows(List<ArrearsReportController.DepartmentStat> ranking) {
        return java.util.stream.IntStream.range(0, ranking.size()).mapToObj(index -> {
            var item = ranking.get(index);
            return List.of(String.valueOf(index + 1), text(item.departmentName()), String.valueOf(item.people()), money(item.amount()));
        }).toList();
    }

    List<List<String>> patientRows(List<ArrearsReportController.PatientStat> patients) {
        return patients.stream().map(item -> List.of(String.valueOf(item.rank()), text(item.inpatientNo()),
                String.valueOf(item.admissionTimes()), text(item.patientName()), text(item.departmentName()),
                text(item.doctorName()), arrearsType(item.arrearsType()), money(item.arrearsAmount()),
                progress(item.recoveryProgress()))).toList();
    }

    private static List<List<String>> head(String... values) { return java.util.Arrays.stream(values).map(List::of).toList(); }
    private static String money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private static String time(OffsetDateTime value) { return value == null ? "—" : value.format(TIME); }
    private static String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static String status(String value) { return "READY".equals(value) ? "更新完成" : text(value); }
    private static String arrearsType(String value) { return value == null ? "—" : java.util.Map.of("INPATIENT", "在院患者", "DISCHARGED_UNSETTLED", "出院未结算", "DISCHARGED_SETTLED", "出院已结算").getOrDefault(value, text(value)); }
    private static String progress(String value) { return value == null ? "—" : java.util.Map.of("NOT_STARTED", "未催缴", "NEGOTIATING", "协商中", "REFUSED", "拒绝缴费", "LEGAL_ACTION", "移交法务发起诉讼", "PAID", "已缴费").getOrDefault(value, text(value)); }

    public record ExportFile(byte[] content, MediaType mediaType, String filename) {
        public String contentDisposition() { return "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"); }
    }
}
