package cn.hospital.rehab.arrears.importer;

import com.alibaba.excel.EasyExcel;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import cn.hospital.rehab.common.importing.ImportError;
import cn.hospital.rehab.common.importing.ExcelSheetRows;
import cn.hospital.rehab.common.importing.ImportValidationException;
import cn.hospital.rehab.common.importing.FailedImportBatchRecorder;

@Service
public class ArrearsImportService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:m:s"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d")
    };
    private final JdbcClient jdbc;
    private final FailedImportBatchRecorder failedBatches;

    public ArrearsImportService(JdbcClient jdbc, FailedImportBatchRecorder failedBatches) { this.jdbc = jdbc; this.failedBatches = failedBatches; }

    @Transactional
    public ArrearsImportResult importFile(MultipartFile file) {
        validateFile(file);
        List<ArrearsImportRow> rows = readRows(file);
        if (rows.size() > 1000) throw new IllegalArgumentException("单次导入最多1000条数据");
        if (rows.isEmpty()) throw new IllegalArgumentException("导入文件没有数据行");
        try { validateRows(rows); }
        catch (ImportValidationException exception) {
            String failedBatchNo = failedBatches.record("ARREARS", file.getOriginalFilename(), rows.size(), exception.getErrors());
            throw new ImportValidationException(failedBatchNo, exception.getErrors());
        }
        jdbc.sql("SELECT pg_advisory_xact_lock(hashtext('IMPORT_ARREARS'))").query((rs, rowNum) -> true).single();
        String batchNo = "ARR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + UUID.randomUUID().toString().substring(0, 6);
        long batchId = jdbc.sql("""
                INSERT INTO import_batch(batch_no,business_type,source_type,original_filename,status,total_count)
                VALUES (:batchNo,'ARREARS','EXCEL',:filename,'PROCESSING',:total) RETURNING id
                """).param("batchNo", batchNo).param("filename", file.getOriginalFilename()).param("total", rows.size())
                .query(Long.class).single();
        int matched = 0, unmatched = 0, ambiguous = 0, added = 0, overwritten = 0;
        for (ArrearsImportRow row : rows) {
            DoctorMatch doctor = matchDoctor(row.doctorEmployeeNo);
            if (doctor.matched()) matched++; else if (doctor.ambiguous()) ambiguous++; else unmatched++;
            boolean exists = encounterExists(row);
            long encounterId = upsertEncounter(row, doctor);
            upsertArrears(row, encounterId, batchId);
            if (exists) overwritten++; else added++;
        }
        jdbc.sql("""
                UPDATE import_batch SET status='SUCCESS',success_count=:success,
                  added_count=:added,overwritten_count=:overwritten,summary_status='READY',
                  doctor_matched_count=:matched,doctor_unmatched_count=:unmatched,
                  doctor_ambiguous_count=:ambiguous,finished_at=CURRENT_TIMESTAMP WHERE id=:id
                """).param("success", rows.size()).param("added", added).param("overwritten", overwritten)
                .param("matched", matched).param("unmatched", unmatched)
                .param("ambiguous", ambiguous).param("id", batchId).update();
        return new ArrearsImportResult(batchNo, rows.size(), rows.size(), 0, added, overwritten, 0, matched, unmatched, ambiguous);
    }

    private boolean encounterExists(ArrearsImportRow row) {
        return jdbc.sql("SELECT EXISTS(SELECT 1 FROM patient_encounter WHERE inpatient_no=:no AND admission_times=:times)")
                .param("no", row.inpatientNo.trim()).param("times", row.admissionTimes).query(Boolean.class).single();
    }

    private long upsertEncounter(ArrearsImportRow row, DoctorMatch doctor) {
        Long departmentId = jdbc.sql("SELECT id FROM sys_department WHERE department_name=:name AND enabled=true")
                .param("name", row.wardName).query(Long.class).optional().orElse(null);
        return jdbc.sql("""
                INSERT INTO patient_encounter(inpatient_no,admission_times,patient_name,department_id,ward_name,fee_type,doctor_name_source,doctor_employee_no,doctor_user_id,doctor_match_status,admitted_at,discharged_at)
                VALUES (:no,:times,:name,:department,:ward,:fee,:doctorName,:doctorEmployeeNo,:doctorId,:doctorStatus,:admitted,:discharged)
                ON CONFLICT (inpatient_no,admission_times) DO UPDATE SET patient_name=EXCLUDED.patient_name,
                  department_id=COALESCE(EXCLUDED.department_id,patient_encounter.department_id),ward_name=COALESCE(EXCLUDED.ward_name,patient_encounter.ward_name),fee_type=COALESCE(EXCLUDED.fee_type,patient_encounter.fee_type),
                  doctor_name_source=COALESCE(EXCLUDED.doctor_name_source,patient_encounter.doctor_name_source),doctor_employee_no=COALESCE(EXCLUDED.doctor_employee_no,patient_encounter.doctor_employee_no),doctor_user_id=COALESCE(EXCLUDED.doctor_user_id,patient_encounter.doctor_user_id),
                  doctor_match_status=CASE WHEN EXCLUDED.doctor_employee_no IS NULL THEN patient_encounter.doctor_match_status ELSE EXCLUDED.doctor_match_status END,admitted_at=COALESCE(EXCLUDED.admitted_at,patient_encounter.admitted_at),discharged_at=COALESCE(EXCLUDED.discharged_at,patient_encounter.discharged_at),
                  source_updated_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP RETURNING id
                """).param("no", row.inpatientNo.trim()).param("times", row.admissionTimes).param("name", row.patientName.trim())
                .param("department", departmentId).param("ward", row.wardName).param("fee", row.feeType)
                .param("doctorName", row.doctorName).param("doctorEmployeeNo", normalizedEmployeeNo(row.doctorEmployeeNo)).param("doctorId", doctor.userId).param("doctorStatus", doctor.status)
                .param("admitted", parseDate(row.admittedAt)).param("discharged", parseDate(row.dischargedAt)).query(Long.class).single();
    }

    private void upsertArrears(ArrearsImportRow row, long encounterId, long batchId) {
        var version = jdbc.sql("SELECT c.id,c.coefficient FROM sys_fee_coefficient c JOIN sys_fee_type t ON t.id=c.fee_type_id WHERE BTRIM(t.fee_name)=:fee AND c.enabled=true")
                .param("fee", row.feeType.trim()).query((r,n) -> new CoefficientVersion(r.getLong("id"), r.getBigDecimal("coefficient"))).optional()
                .orElseThrow(() -> new IllegalArgumentException("费别未配置启用系数：" + row.feeType));
        BigDecimal coefficient = version.coefficient();
        BigDecimal original = decimal(row.originalRequiredDeposit), prepaid = decimal(row.prepaidAmount);
        BigDecimal finalDeposit = original.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
        BigDecimal difference = prepaid.subtract(finalDeposit).setScale(2, RoundingMode.HALF_UP);
        BigDecimal arrears = difference.signum() < 0 ? difference.abs() : BigDecimal.ZERO;
        jdbc.sql("""
                INSERT INTO arrears_record(encounter_id,import_batch_id,arrears_type,total_cost,prepaid_amount,medical_insurance_paid,personal_account_paid,original_required_deposit,coefficient_version_id,coefficient_snapshot,final_required_deposit,deposit_difference,in_arrears,arrears_amount)
                VALUES (:encounter,:batch,:type,:total,:prepaid,COALESCE(:insurance,0),COALESCE(:personal,0),:original,:versionId,:coefficient,:finalDeposit,:difference,:inArrears,:arrears)
                ON CONFLICT (encounter_id) DO UPDATE SET import_batch_id=EXCLUDED.import_batch_id,arrears_type=COALESCE(EXCLUDED.arrears_type,arrears_record.arrears_type),total_cost=COALESCE(EXCLUDED.total_cost,arrears_record.total_cost),prepaid_amount=EXCLUDED.prepaid_amount,medical_insurance_paid=CASE WHEN :insuranceProvided THEN EXCLUDED.medical_insurance_paid ELSE arrears_record.medical_insurance_paid END,personal_account_paid=CASE WHEN :personalProvided THEN EXCLUDED.personal_account_paid ELSE arrears_record.personal_account_paid END,original_required_deposit=EXCLUDED.original_required_deposit,coefficient_version_id=EXCLUDED.coefficient_version_id,coefficient_snapshot=EXCLUDED.coefficient_snapshot,final_required_deposit=EXCLUDED.final_required_deposit,deposit_difference=EXCLUDED.deposit_difference,in_arrears=EXCLUDED.in_arrears,arrears_amount=EXCLUDED.arrears_amount,source_updated_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP
                """).param("encounter", encounterId).param("batch", batchId).param("type", row.arrearsType)
                .param("total", nullableDecimal(row.totalCost)).param("prepaid", prepaid).param("insurance", nullableDecimal(row.medicalInsurancePaid))
                .param("personal", nullableDecimal(row.personalAccountPaid)).param("insuranceProvided", !blank(row.medicalInsurancePaid))
                .param("personalProvided", !blank(row.personalAccountPaid)).param("original", original).param("versionId", version.id()).param("coefficient", coefficient)
                .param("finalDeposit", finalDeposit).param("difference", difference).param("inArrears", difference.signum() < 0).param("arrears", arrears).update();
    }

    private DoctorMatch matchDoctor(String employeeNo) {
        if (blank(employeeNo)) return new DoctorMatch(null, "NOT_FOUND");
        List<Long> ids = jdbc.sql("SELECT id FROM sys_user WHERE employee_no=:employeeNo AND enabled=true").param("employeeNo", normalizedEmployeeNo(employeeNo)).query(Long.class).list();
        return ids.size() == 1 ? new DoctorMatch(ids.get(0), "MATCHED") : new DoctorMatch(null, ids.isEmpty() ? "NOT_FOUND" : "AMBIGUOUS");
    }
    private static String normalizedEmployeeNo(String value) { return blank(value) ? null : value.trim(); }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择Excel文件");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) throw new IllegalArgumentException("仅支持xlsx文件");
    }
    static List<ArrearsImportRow> readRows(MultipartFile file) {
        return ExcelSheetRows.read(file).stream().filter(row -> ExcelSheetRows.value(row, "住院号") != null).map(row -> {
            ArrearsImportRow result = new ArrearsImportRow();
            result.inpatientNo = ExcelSheetRows.value(row, "住院号");
            result.admissionTimes = ExcelSheetRows.integer(row, "住院次数");
            result.patientName = ExcelSheetRows.value(row, "姓名");
            result.wardName = ExcelSheetRows.value(row, "住院病区");
            result.feeType = ExcelSheetRows.value(row, "费别");
            result.arrearsType = ExcelSheetRows.value(row, "欠费类型");
            result.doctorName = ExcelSheetRows.value(row, "主管医生");
            result.doctorEmployeeNo = ExcelSheetRows.value(row, "主管医生工号", "工号");
            result.admittedAt = ExcelSheetRows.value(row, "入区日期");
            result.dischargedAt = ExcelSheetRows.value(row, "出区日期");
            result.totalCost = ExcelSheetRows.value(row, "总费用", "总费用(元)", "总费用（元）");
            result.prepaidAmount = ExcelSheetRows.value(row, "预交金（元）", "预交金(元)");
            result.medicalInsurancePaid = ExcelSheetRows.value(row, "医保支付（元）", "医保支付(元)");
            result.personalAccountPaid = ExcelSheetRows.value(row, "个人账户支付（元）", "个人账户支付(元)");
            result.originalRequiredDeposit = ExcelSheetRows.value(row, "原始应交押金（元）", "应交押金（元）", "应交押金(元)");
            return result;
        }).toList();
    }    private void validateRows(List<ArrearsImportRow> rows) {
        List<ImportError> errors = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            ArrearsImportRow row = rows.get(index);
            int excelRow = index + 2;
            required(errors, excelRow, row, "住院号", row.inpatientNo);
            if (row.admissionTimes == null || row.admissionTimes < 1) error(errors, excelRow, row, "住院次数", String.valueOf(row.admissionTimes), "INVALID_FORMAT", "住院次数必须为正整数");
            required(errors, excelRow, row, "姓名", row.patientName);
            required(errors, excelRow, row, "住院病区", row.wardName);
            required(errors, excelRow, row, "费别", row.feeType);
            required(errors, excelRow, row, "预交金（元）", row.prepaidAmount);
            required(errors, excelRow, row, "原始应交押金（元）", row.originalRequiredDeposit);
            if (!blank(row.inpatientNo) && row.admissionTimes != null && !keys.add(row.inpatientNo.trim() + "#" + row.admissionTimes))
                error(errors, excelRow, row, "住院号+住院次数", row.inpatientNo + "/" + row.admissionTimes, "DUPLICATE_KEY_IN_FILE", "文件内存在重复住院记录");
            validateDecimal(errors, excelRow, row, "预交金（元）", row.prepaidAmount, true);
            validateDecimal(errors, excelRow, row, "原始应交押金（元）", row.originalRequiredDeposit, true);
            validateDecimal(errors, excelRow, row, "总费用", row.totalCost, false);
            validateDecimal(errors, excelRow, row, "医保支付（元）", row.medicalInsurancePaid, false);
            validateDecimal(errors, excelRow, row, "个人账户支付（元）", row.personalAccountPaid, false);
            validateDate(errors, excelRow, row, "入区日期", row.admittedAt);
            validateDate(errors, excelRow, row, "出区日期", row.dischargedAt);
            if (!blank(row.wardName) && !jdbc.sql("SELECT EXISTS(SELECT 1 FROM sys_department WHERE department_name=:name AND enabled=true)").param("name", row.wardName.trim()).query(Boolean.class).single())
                error(errors, excelRow, row, "住院病区", row.wardName, "DEPARTMENT_NOT_FOUND", "科室无法匹配");
            if (!blank(row.feeType) && !jdbc.sql("SELECT EXISTS(SELECT 1 FROM sys_fee_coefficient c JOIN sys_fee_type t ON t.id=c.fee_type_id WHERE BTRIM(t.fee_name)=:fee AND c.enabled=true)").param("fee", row.feeType.trim()).query(Boolean.class).single())
                error(errors, excelRow, row, "费别", row.feeType, "FEE_COEFFICIENT_NOT_FOUND", "费别未配置启用系数");
        }
        if (!errors.isEmpty()) throw new ImportValidationException(errors);
    }
    private static void required(List<ImportError> errors, int n, ArrearsImportRow r, String field, String value) { if (blank(value)) error(errors,n,r,field,value,"MISSING_REQUIRED",field+"不能为空"); }
    private static void validateDecimal(List<ImportError> errors,int n,ArrearsImportRow r,String field,String value,boolean required){if(blank(value)){return;}try{if(decimal(value).signum()<0)throw new NumberFormatException();}catch(NumberFormatException e){error(errors,n,r,field,value,"INVALID_FORMAT",field+"必须为非负金额");}}
    private static void validateDate(List<ImportError> errors,int n,ArrearsImportRow r,String field,String value){if(blank(value))return;try{parseDate(value);}catch(IllegalArgumentException e){error(errors,n,r,field,value,"INVALID_FORMAT",e.getMessage());}}
    private static void error(List<ImportError> errors,int n,ArrearsImportRow r,String field,String value,String code,String message){errors.add(new ImportError(n,r.inpatientNo,r.admissionTimes,field,value,code,message));}
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    static BigDecimal decimal(String value) { return blank(value) ? BigDecimal.ZERO : new BigDecimal(value.trim().replace(",", "").replaceAll("\\s+", "")); }
    private static BigDecimal nullableDecimal(String value) { return blank(value) ? null : decimal(value); }
    static OffsetDateTime parseDate(String value) {
        if (blank(value)) return null;
        for (DateTimeFormatter formatter : DATE_FORMATS) try {
            if (value.trim().length() > 10) return LocalDateTime.parse(value.trim(), formatter).atZone(ZONE).toOffsetDateTime();
            return LocalDate.parse(value.trim(), formatter).atStartOfDay(ZONE).toOffsetDateTime();
        } catch (DateTimeParseException ignored) { }
        throw new IllegalArgumentException("日期格式错误：" + value);
    }
    private record DoctorMatch(Long userId, String status) {
        boolean matched() { return "MATCHED".equals(status); }
        boolean ambiguous() { return "AMBIGUOUS".equals(status); }
    }
    private record CoefficientVersion(long id, BigDecimal coefficient) {}
}
