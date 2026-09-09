package cn.hospital.rehab.integration.his;

import cn.hospital.rehab.arrears.importer.ArrearsImportResult;
import cn.hospital.rehab.arrears.importer.ArrearsImportRow;
import cn.hospital.rehab.arrears.importer.ArrearsImportService;
import cn.hospital.rehab.discharge.importer.DischargeImportRow;
import cn.hospital.rehab.discharge.importer.DischargeImportService;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class HisSyncCoordinator {
    private final HisGatewayClient gateway;
    private final HisProperties properties;
    private final ArrearsImportService arrearsImports;
    private final DischargeImportService dischargeImports;
    private final HisSyncBatchService batches;
    private final JdbcClient jdbc;
    private final Map<HisSyncType, AtomicBoolean> running = new EnumMap<>(HisSyncType.class);

    public HisSyncCoordinator(HisGatewayClient gateway, HisProperties properties, ArrearsImportService arrearsImports,
                              DischargeImportService dischargeImports, HisSyncBatchService batches, JdbcClient jdbc) {
        this.gateway = gateway; this.properties = properties; this.arrearsImports = arrearsImports;
        this.dischargeImports = dischargeImports; this.batches = batches; this.jdbc = jdbc;
        for (HisSyncType type : HisSyncType.values()) running.put(type, new AtomicBoolean());
    }

    public SyncResult trigger(HisSyncType type, String triggerType, Authentication authentication) {
        AtomicBoolean guard = running.get(type);
        if (!guard.compareAndSet(false, true)) throw new IllegalStateException("该类HIS同步正在运行，请稍后查看批次结果");
        Long startedBy = userId(authentication);
        batches.markAttempt(type);
        try {
            List<Map<String, Object>> sourceRows = fetchAll(type);
            SyncResult result;
            if (sourceRows.isEmpty()) {
                result = new SyncResult(batches.recordEmptySuccess(type, triggerType, startedBy),0,0,0,0,0,0,0,0);
            } else if (type == HisSyncType.PATIENT_INFO) {
                var imported = dischargeImports.importApiRows(mapPatients(sourceRows),type.transactionCode(),triggerType,startedBy);
                result = new SyncResult(imported.batchNo(),imported.total(),imported.success(),0,imported.added(),
                        imported.overwritten(),imported.skipped(),imported.doctorMatched(),imported.doctorUnmatched()+imported.doctorAmbiguous());
            } else {
                var imported = arrearsImports.importApiRows(mapArrears(sourceRows,type),type.transactionCode(),triggerType,startedBy);
                result = from(imported);
            }
            batches.markSuccess(type,result.batchNo());
            return result;
        } catch (RuntimeException exception) {
            String failedBatch = batchNo(exception);
            if (failedBatch == null) failedBatch = batches.recordGatewayFailure(type,triggerType,startedBy,exception.getMessage());
            batches.markFailure(type,failedBatch,exception.getMessage());
            if (exception instanceof cn.hospital.rehab.common.importing.ImportValidationException) throw exception;
            throw new HisSyncException(failedBatch,HisSyncBatchService.safe(exception.getMessage()),exception);
        } finally {
            guard.set(false);
        }
    }

    public boolean isRunning(HisSyncType type) { return running.get(type).get(); }

    private List<Map<String, Object>> fetchAll(HisSyncType type) {
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        int pageNumber = 1;
        for (int requested = 1; requested <= properties.getMaxPages(); requested++) {
            HisGatewayClient.Page page = gateway.fetch(type,pageNumber);
            for (Map<String,Object> row : page.rows()) {
                String no=HisFields.text(row,"住院号","inpatientNo","zyh");
                Integer times=HisFields.positiveInteger(row,"住院次数","住院次","admissionTimes","zycs");
                String key=(no==null?String.valueOf(unique.size()):no.trim())+"#"+times;
                unique.put(key,row);
            }
            if (page.rows().isEmpty() || unique.size() >= page.total() || page.rows().size() < page.size()) return new ArrayList<>(unique.values());
            pageNumber = Math.max(pageNumber + 1,page.page() + 1);
        }
        throw new HisGatewayException("HIS分页超过安全上限");
    }

    static List<ArrearsImportRow> mapArrears(List<Map<String,Object>> rows, HisSyncType type) {
        return rows.stream().map(row -> {
            ArrearsImportRow target=new ArrearsImportRow();
            target.inpatientNo=HisFields.text(row,"住院号","inpatientNo","zyh");
            target.admissionTimes=HisFields.positiveInteger(row,"住院次数","住院次","admissionTimes","zycs");
            target.patientName=HisFields.text(row,"姓名","患者姓名","patientName","xm");
            target.wardName=HisFields.text(row,"住院病区","所属科室","wardName","departmentName","ksmc");
            target.feeType=HisFields.text(row,"费别","feeType","fblx");
            target.arrearsType=type==HisSyncType.INPATIENT_ARREARS?"INPATIENT":"DISCHARGED_UNSETTLED";
            target.doctorName=HisFields.text(row,"主管医生","doctorName","ysxm");
            target.doctorEmployeeNo=HisFields.text(row,"主管医生工号","工号","doctorEmployeeNo","ysgh");
            target.admittedAt=HisFields.text(row,"入区日期","入院日期","admittedAt","rqrq");
            target.dischargedAt=HisFields.text(row,"出区日期","出院日期","dischargedAt","cqrq","cyrq");
            target.totalCost=HisFields.text(row,"总费用(元)","总费用（元）","总费用","totalCost");
            target.prepaidAmount=HisFields.text(row,type==HisSyncType.DISCHARGED_ARREARS?"可用预交金(元)":"预交金(元)","可用预交金（元）","预交金（元）","prepaidAmount");
            target.medicalInsurancePaid=HisFields.text(row,"医保支付(元)","医保支付（元）","medicalInsurancePaid");
            target.personalAccountPaid=HisFields.text(row,"个人账户支付(元)","个人账户支付（元）","personalAccountPaid");
            target.originalRequiredDeposit=HisFields.text(row,"应交押金(元)","应交押金（元）","原始应交押金（元）","requiredDeposit");
            return target;
        }).toList();
    }

    static List<DischargeImportRow> mapPatients(List<Map<String,Object>> rows) {
        return rows.stream().map(row -> {
            DischargeImportRow target=new DischargeImportRow();
            target.inpatientNo=HisFields.text(row,"住院号","inpatientNo","zyh");
            target.admissionTimes=HisFields.positiveInteger(row,"住院次数","住院次","admissionTimes","zycs");
            target.patientName=HisFields.text(row,"患者姓名","姓名","patientName","xm");
            target.gender=HisFields.text(row,"性别","患者性别","性别名称","gender","xb","xbmc");
            target.wardName=HisFields.text(row,"所属科室","住院病区","departmentName","wardName","ksmc");
            target.primaryDiagnosis=HisFields.text(row,"主诊断","主要诊断","诊断名称","primaryDiagnosis","zzd","zdmc","ryzd");
            target.doctorName=HisFields.text(row,"主管医生","doctorName","ysxm");
            target.doctorEmployeeNo=HisFields.text(row,"工号","医生工号","doctorEmployeeNo","ysgh");
            target.admittedAt=HisFields.text(row,"入院日期","入区日期","admittedAt","ryrq");
            target.actualDischargeAt=HisFields.text(row,"出院日期","实际出院时间","dischargedAt","cyrq");
            target.medicalInsuranceType=HisFields.text(row,"医保类型","医保类别","医疗保险类型","medicalInsuranceType","yblx","yblb");
            target.plannedDischargeAt=null;
            target.feeType=null;
            return target;
        }).toList();
    }

    private Long userId(Authentication authentication) {
        if (authentication == null) return null;
        return jdbc.sql("SELECT id FROM sys_user WHERE login_name=:name").param("name",authentication.getName())
                .query(Long.class).optional().orElse(null);
    }
    private static SyncResult from(ArrearsImportResult value) {
        return new SyncResult(value.batchNo(),value.total(),value.success(),value.failure(),value.added(),value.overwritten(),
                value.skipped(),value.doctorMatched(),value.doctorUnmatched()+value.doctorAmbiguous());
    }
    private static String batchNo(RuntimeException exception) {
        if (exception instanceof cn.hospital.rehab.common.importing.ImportValidationException validation)
            return validation.getBatchNo();
        return null;
    }
    public record SyncResult(String batchNo,int total,int success,int failure,int added,int overwritten,int skipped,
                             int doctorMatched,int doctorUnmatched){}
}
