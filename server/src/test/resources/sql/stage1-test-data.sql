INSERT INTO sys_department(department_code,department_name,enabled) VALUES
 ('TEST-A','测试康复一科',TRUE),
 ('TEST-B','测试康复二科',TRUE)
ON CONFLICT(department_code) DO UPDATE SET department_name=EXCLUDED.department_name,enabled=TRUE;

INSERT INTO sys_user(login_name,display_name,password_hash,employee_no,wecom_user_id,department_id,enabled,must_change_password)
SELECT v.login_name,v.display_name,a.password_hash,v.employee_no,v.wecom_user_id,d.id,TRUE,TRUE
FROM (VALUES
 ('test_director','测试科主任','T-DIRECTOR','test-director','TEST-A'),
 ('test_doctor_a','测试医生甲','T-DOCTOR-A','test-doctor-a','TEST-A'),
 ('test_doctor_b','测试医生乙','T-DOCTOR-B','test-doctor-b','TEST-B'),
 ('test_operations','测试运营','T-OPERATIONS','test-operations','TEST-A'),
 ('test_finance','测试财务','T-FINANCE','test-finance','TEST-A')
) AS v(login_name,display_name,employee_no,wecom_user_id,department_code)
JOIN sys_department d ON d.department_code=v.department_code
CROSS JOIN (SELECT password_hash FROM sys_user WHERE login_name='admin') a
ON CONFLICT(login_name) DO NOTHING;

INSERT INTO sys_user_role(user_id,role_id)
SELECT u.id,r.id FROM (VALUES
 ('test_director','DEPARTMENT_DIRECTOR'),
 ('test_doctor_a','ATTENDING_DOCTOR'),
 ('test_doctor_b','ATTENDING_DOCTOR'),
 ('test_operations','OPERATIONS'),
 ('test_finance','FINANCE')
) v(login_name,role_code)
JOIN sys_user u ON u.login_name=v.login_name
JOIN sys_role r ON r.role_code=v.role_code
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_department(role_id,department_id)
SELECT r.id,d.id FROM sys_role r CROSS JOIN sys_department d
WHERE r.role_code='DEPARTMENT_DIRECTOR' AND d.department_code='TEST-A'
ON CONFLICT DO NOTHING;

INSERT INTO patient_encounter(inpatient_no,admission_times,patient_name,department_id,ward_name,fee_type,
                              doctor_name_source,doctor_employee_no,doctor_user_id,doctor_match_status,admitted_at)
SELECT v.inpatient_no,1,v.patient_name,d.id,d.department_name,'TEST',u.display_name,u.employee_no,u.id,'MATCHED',
       TIMESTAMPTZ '2026-08-01 08:00:00+08'
FROM (VALUES
 ('TEST-0001','测试患者甲','TEST-A','test_doctor_a'),
 ('TEST-0002','测试患者乙','TEST-B','test_doctor_b')
) v(inpatient_no,patient_name,department_code,doctor_login)
JOIN sys_department d ON d.department_code=v.department_code
JOIN sys_user u ON u.login_name=v.doctor_login
ON CONFLICT(inpatient_no,admission_times) DO NOTHING;

INSERT INTO import_batch(batch_no,business_type,source_type,original_filename,status,total_count,success_count,
                         summary_status,started_at,finished_at)
VALUES ('TEST-ARREARS-READY','ARREARS','EXCEL','stage1-test.xlsx','SUCCESS',2,2,'READY',
        TIMESTAMPTZ '2026-08-03 07:59:00+08',TIMESTAMPTZ '2026-08-03 08:00:00+08')
ON CONFLICT(batch_no) DO UPDATE SET status='SUCCESS',summary_status='READY',finished_at=EXCLUDED.finished_at;

INSERT INTO arrears_record(encounter_id,import_batch_id,arrears_type,original_required_deposit,coefficient_snapshot,
                           final_required_deposit,deposit_difference,in_arrears,arrears_amount,payment_status)
SELECT e.id,b.id,'TEST',1000,1,1000,-100,TRUE,100,'UNPAID'
FROM patient_encounter e
CROSS JOIN import_batch b
WHERE e.inpatient_no IN ('TEST-0001','TEST-0002') AND b.batch_no='TEST-ARREARS-READY'
ON CONFLICT(encounter_id) DO UPDATE SET import_batch_id=EXCLUDED.import_batch_id,
    in_arrears=TRUE,arrears_amount=100,payment_status='UNPAID',recovery_progress='NOT_STARTED';

INSERT INTO discharge_record(encounter_id,planned_discharge_at,actual_discharge_at,planned_discharge_updated_at)
SELECT e.id,TIMESTAMPTZ '2026-08-20 08:00:00+08',TIMESTAMPTZ '2026-08-20 10:00:00+08',
       TIMESTAMPTZ '2026-08-02 08:00:00+08'
FROM patient_encounter e WHERE e.inpatient_no IN ('TEST-0001','TEST-0002')
ON CONFLICT(encounter_id) DO NOTHING;
