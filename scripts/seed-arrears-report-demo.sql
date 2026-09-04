BEGIN;

DELETE FROM arrears_record
WHERE encounter_id IN (
    SELECT id FROM patient_encounter WHERE inpatient_no LIKE 'DEMO-ARR-%'
);
DELETE FROM patient_encounter WHERE inpatient_no LIKE 'DEMO-ARR-%';
DELETE FROM import_batch WHERE batch_no = 'DEMO-ARR-20260902-0900';

INSERT INTO import_batch (
    batch_no, business_type, source_type, original_filename, status,
    total_count, success_count, failure_count, added_count, overwritten_count,
    skipped_count, summary_status, started_at, finished_at
) VALUES (
    'DEMO-ARR-20260902-0900', 'ARREARS', 'EXCEL', '欠费通报演示数据.xlsx', 'SUCCESS',
    15, 15, 0, 15, 0, 0, 'READY',
    TIMESTAMPTZ '2026-09-02 08:58:00+08', TIMESTAMPTZ '2026-09-02 09:00:00+08'
);

INSERT INTO patient_encounter (
    inpatient_no, admission_times, patient_name, department_id, ward_name,
    fee_type, doctor_name_source, doctor_user_id, doctor_match_status,
    admitted_at, discharged_at, source_updated_at
)
SELECT v.inpatient_no, 1, v.patient_name, d.id, d.department_name,
       v.fee_type, v.doctor_name, u.id,
       CASE WHEN u.id IS NULL THEN 'NOT_FOUND' ELSE 'MATCHED' END,
       v.admitted_at, v.discharged_at, TIMESTAMPTZ '2026-09-02 09:00:00+08'
FROM (VALUES
    ('DEMO-ARR-001','张明远','DEV-ARR-01','职工医保','聂文斌','03563',TIMESTAMPTZ '2026-06-08 10:00:00+08',NULL),
    ('DEMO-ARR-002','李秀兰','DEV-ARR-01','居民医保','王冰芯','05225',TIMESTAMPTZ '2026-05-21 09:20:00+08',NULL),
    ('DEMO-ARR-003','周建国','DEV-ARR-01','自费','彭百成','04673',TIMESTAMPTZ '2026-04-12 14:30:00+08',TIMESTAMPTZ '2026-08-28 11:00:00+08'),
    ('DEMO-ARR-004','陈桂芳','DEV-ARR-02','职工医保','赵医生',NULL,TIMESTAMPTZ '2026-07-01 08:40:00+08',NULL),
    ('DEMO-ARR-005','王志强','DEV-ARR-02','居民医保','孙医生',NULL,TIMESTAMPTZ '2026-06-17 13:10:00+08',NULL),
    ('DEMO-ARR-006','刘春梅','DEV-ARR-02','异地医保','孙医生',NULL,TIMESTAMPTZ '2026-03-06 15:00:00+08',TIMESTAMPTZ '2026-08-20 10:00:00+08'),
    ('DEMO-ARR-007','黄德胜','DEV-ARR-03','职工医保','钱医生',NULL,TIMESTAMPTZ '2026-07-16 09:00:00+08',NULL),
    ('DEMO-ARR-008','吴美珍','DEV-ARR-03','居民医保','钱医生',NULL,TIMESTAMPTZ '2026-05-03 11:15:00+08',NULL),
    ('DEMO-ARR-009','徐海峰','DEV-ARR-03','自费','郑医生',NULL,TIMESTAMPTZ '2026-02-14 16:20:00+08',TIMESTAMPTZ '2026-08-18 09:30:00+08'),
    ('DEMO-ARR-010','孙秋月','DEV-ARR-04','职工医保','冯医生',NULL,TIMESTAMPTZ '2026-07-22 10:10:00+08',NULL),
    ('DEMO-ARR-011','马跃进','DEV-ARR-04','异地医保','冯医生',NULL,TIMESTAMPTZ '2026-04-26 12:00:00+08',TIMESTAMPTZ '2026-08-25 14:00:00+08'),
    ('DEMO-ARR-012','朱玉华','DEV-ARR-05','居民医保','褚医生',NULL,TIMESTAMPTZ '2026-06-29 08:30:00+08',NULL),
    ('DEMO-ARR-013','胡永康','DEV-ARR-05','职工医保','卫医生',NULL,TIMESTAMPTZ '2026-03-18 09:45:00+08',TIMESTAMPTZ '2026-08-12 16:00:00+08'),
    ('DEMO-ARR-014','郭小琴','DEV-ARR-06','职工医保','蒋医生',NULL,TIMESTAMPTZ '2026-07-09 14:15:00+08',NULL),
    ('DEMO-ARR-015','何长青','DEV-ARR-06','自费','沈医生',NULL,TIMESTAMPTZ '2026-01-20 11:30:00+08',TIMESTAMPTZ '2026-08-05 10:30:00+08')
) AS v(inpatient_no,patient_name,department_code,fee_type,doctor_name,employee_no,admitted_at,discharged_at)
JOIN sys_department d ON d.department_code = v.department_code
LEFT JOIN sys_user u ON u.employee_no = v.employee_no;

INSERT INTO arrears_record (
    encounter_id, import_batch_id, arrears_type, total_cost, prepaid_amount,
    medical_insurance_paid, personal_account_paid, original_required_deposit,
    coefficient_snapshot, final_required_deposit, deposit_difference,
    in_arrears, arrears_amount, payment_status, arrears_reason,
    recovery_progress, previous_recovery_progress, source_updated_at
)
SELECT e.id, b.id, v.arrears_type, v.total_cost, v.prepaid_amount,
       v.medical_paid, v.account_paid, v.original_deposit,
       1.0000, v.final_deposit, v.final_deposit - v.prepaid_amount,
       TRUE, v.arrears_amount, 'UNPAID', v.reason,
       v.progress, v.progress, TIMESTAMPTZ '2026-09-02 09:00:00+08'
FROM (VALUES
    ('DEMO-ARR-001','INPATIENT',168000.00,20000.00,48000.00, 5000.00,80000.00,80000.00,82000.00,'NOT_STARTED','入院押金不足，尚未联系家属'),
    ('DEMO-ARR-002','INPATIENT',142000.00,30000.00,52000.00, 4000.00,70000.00,70000.00,58000.00,'NEGOTIATING','家属承诺本周分批补缴'),
    ('DEMO-ARR-003','DISCHARGED_UNSETTLED',105000.00,25000.00,36000.00,2000.00,65000.00,65000.00,40000.00,'LEGAL_ACTION','多次催缴未果，已移交法务'),
    ('DEMO-ARR-004','INPATIENT',130000.00,25000.00,42000.00,3000.00,68000.00,68000.00,63000.00,'NEGOTIATING','正在与家属协商补缴计划'),
    ('DEMO-ARR-005','INPATIENT',118000.00,22000.00,39000.00,2000.00,62000.00,62000.00,49000.00,'NOT_STARTED','新产生欠费，待主管医生联系'),
    ('DEMO-ARR-006','DISCHARGED_SETTLED',88000.00,18000.00,28000.00,1000.00,52000.00,52000.00,23000.00,'REFUSED','患者对结算金额有异议'),
    ('DEMO-ARR-007','INPATIENT',99000.00,26000.00,33000.00,2000.00,55000.00,55000.00,41000.00,'NEGOTIATING','已联系单位协助处理'),
    ('DEMO-ARR-008','INPATIENT',84000.00,24000.00,30000.00,1000.00,50000.00,50000.00,30000.00,'NOT_STARTED','待核实医保支付到账情况'),
    ('DEMO-ARR-009','DISCHARGED_UNSETTLED',76000.00,17000.00,24000.00,1000.00,46000.00,46000.00,21000.00,'LEGAL_ACTION','拒接电话，准备发送律师函'),
    ('DEMO-ARR-010','INPATIENT',72000.00,18000.00,25000.00,1000.00,40000.00,40000.00,32000.00,'NEGOTIATING','计划三日内补缴'),
    ('DEMO-ARR-011','DISCHARGED_SETTLED',61000.00,15000.00,21000.00,1000.00,36000.00,36000.00,26000.00,'REFUSED','对自费项目存在异议'),
    ('DEMO-ARR-012','INPATIENT',57000.00,16000.00,19000.00,1000.00,34000.00,34000.00,22000.00,'NOT_STARTED','今日进入欠费名单'),
    ('DEMO-ARR-013','DISCHARGED_UNSETTLED',48000.00,13000.00,16000.00,1000.00,30000.00,30000.00,14000.00,'NEGOTIATING','家属申请延期一周'),
    ('DEMO-ARR-014','INPATIENT',43000.00,12000.00,15000.00,1000.00,26000.00,26000.00,15000.00,'NOT_STARTED','待首次催缴'),
    ('DEMO-ARR-015','DISCHARGED_SETTLED',35000.00,10000.00,12000.00,1000.00,22000.00,22000.00, 6000.00,'LEGAL_ACTION','长期欠费，已进入法务流程')
) AS v(inpatient_no,arrears_type,total_cost,prepaid_amount,medical_paid,account_paid,
       original_deposit,final_deposit,arrears_amount,progress,reason)
JOIN patient_encounter e ON e.inpatient_no = v.inpatient_no AND e.admission_times = 1
JOIN import_batch b ON b.batch_no = 'DEMO-ARR-20260902-0900';

COMMIT;
