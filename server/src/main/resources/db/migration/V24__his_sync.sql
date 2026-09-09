ALTER TABLE patient_encounter ADD COLUMN medical_insurance_type VARCHAR(128);

ALTER TABLE import_batch ADD COLUMN transaction_code VARCHAR(64), ADD COLUMN trigger_type VARCHAR(16);

CREATE TABLE his_sync_config (
    sync_type VARCHAR(32) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    daily_time TIME NOT NULL DEFAULT TIME '02:00:00',
    last_attempt_at TIMESTAMPTZ,
    last_success_at TIMESTAMPTZ,
    last_batch_no VARCHAR(64),
    last_error TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_his_sync_type CHECK (sync_type IN ('INPATIENT_ARREARS','DISCHARGED_ARREARS','PATIENT_INFO'))
);

INSERT INTO his_sync_config(sync_type) VALUES ('INPATIENT_ARREARS'), ('DISCHARGED_ARREARS'), ('PATIENT_INFO') ON CONFLICT DO NOTHING;

INSERT INTO sys_permission(permission_code,permission_name,permission_type,resource_path,http_method) VALUES
    ('API_HIS_SYNC_TRIGGER','HIS手动同步','API','/api/integration/his-sync/*/trigger','POST'),
    ('API_HIS_SYNC_CONFIG','HIS同步配置','API','/api/integration/his-sync/configs','*')
ON CONFLICT(permission_code) DO UPDATE SET permission_name=EXCLUDED.permission_name, permission_type=EXCLUDED.permission_type, resource_path=EXCLUDED.resource_path, http_method=EXCLUDED.http_method, enabled=TRUE;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p WHERE r.role_code='OPERATIONS' AND p.permission_code='API_HIS_SYNC_TRIGGER' ON CONFLICT DO NOTHING;

INSERT INTO sys_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sys_role r CROSS JOIN sys_permission p WHERE r.role_code='SYSTEM_ADMIN' AND p.permission_code IN ('API_HIS_SYNC_TRIGGER','API_HIS_SYNC_CONFIG') ON CONFLICT DO NOTHING;
