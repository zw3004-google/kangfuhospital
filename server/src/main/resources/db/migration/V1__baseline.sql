CREATE TABLE sys_department (
    id                  BIGSERIAL PRIMARY KEY,
    department_code     VARCHAR(64) NOT NULL UNIQUE,
    department_name     VARCHAR(128) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user (
    id                  BIGSERIAL PRIMARY KEY,
    login_name          VARCHAR(128) NOT NULL UNIQUE,
    display_name        VARCHAR(128) NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    wecom_user_id       VARCHAR(128) NOT NULL UNIQUE,
    department_id       BIGINT REFERENCES sys_department(id),
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_count  INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
    id                  BIGSERIAL PRIMARY KEY,
    role_code           VARCHAR(64) NOT NULL UNIQUE,
    role_name           VARCHAR(128) NOT NULL,
    built_in            BOOLEAN NOT NULL DEFAULT FALSE,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_user_role (
    user_id             BIGINT NOT NULL REFERENCES sys_user(id),
    role_id             BIGINT NOT NULL REFERENCES sys_role(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE sys_role_department (
    role_id             BIGINT NOT NULL REFERENCES sys_role(id),
    department_id       BIGINT NOT NULL REFERENCES sys_department(id),
    PRIMARY KEY (role_id, department_id)
);

CREATE TABLE sys_fee_coefficient (
    id                  BIGSERIAL PRIMARY KEY,
    fee_type            VARCHAR(64) NOT NULL,
    coefficient         NUMERIC(10, 4) NOT NULL CHECK (coefficient >= 0),
    enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    effective_at        TIMESTAMPTZ,
    disabled_at         TIMESTAMPTZ,
    created_by          BIGINT REFERENCES sys_user(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_fee_coefficient_enabled
    ON sys_fee_coefficient (fee_type) WHERE enabled = TRUE;

CREATE TABLE patient_encounter (
    id                  BIGSERIAL PRIMARY KEY,
    inpatient_no        VARCHAR(64) NOT NULL,
    admission_times     INTEGER NOT NULL CHECK (admission_times > 0),
    patient_name        VARCHAR(128) NOT NULL,
    department_id       BIGINT REFERENCES sys_department(id),
    ward_name           VARCHAR(128),
    fee_type            VARCHAR(64),
    doctor_name_source  VARCHAR(128),
    doctor_user_id      BIGINT REFERENCES sys_user(id),
    doctor_match_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    admitted_at         TIMESTAMPTZ,
    discharged_at       TIMESTAMPTZ,
    source_updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_patient_encounter UNIQUE (inpatient_no, admission_times),
    CONSTRAINT ck_doctor_match_status CHECK (
        doctor_match_status IN ('MATCHED', 'NOT_FOUND', 'AMBIGUOUS', 'PENDING')
    )
);

CREATE INDEX idx_patient_encounter_department ON patient_encounter(department_id);
CREATE INDEX idx_patient_encounter_admitted_at ON patient_encounter(admitted_at DESC);

CREATE TABLE import_batch (
    id                  BIGSERIAL PRIMARY KEY,
    batch_no            VARCHAR(64) NOT NULL UNIQUE,
    business_type       VARCHAR(32) NOT NULL,
    source_type         VARCHAR(16) NOT NULL DEFAULT 'EXCEL',
    original_filename   VARCHAR(255),
    status              VARCHAR(24) NOT NULL,
    total_count         INTEGER NOT NULL DEFAULT 0,
    success_count       INTEGER NOT NULL DEFAULT 0,
    failure_count       INTEGER NOT NULL DEFAULT 0,
    doctor_matched_count INTEGER NOT NULL DEFAULT 0,
    doctor_unmatched_count INTEGER NOT NULL DEFAULT 0,
    doctor_ambiguous_count INTEGER NOT NULL DEFAULT 0,
    error_message       TEXT,
    started_by          BIGINT REFERENCES sys_user(id),
    started_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at         TIMESTAMPTZ,
    CONSTRAINT ck_import_status CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'))
);

CREATE TABLE arrears_record (
    id                      BIGSERIAL PRIMARY KEY,
    encounter_id            BIGINT NOT NULL UNIQUE REFERENCES patient_encounter(id),
    import_batch_id         BIGINT REFERENCES import_batch(id),
    arrears_type             VARCHAR(64),
    total_cost               NUMERIC(16, 2),
    prepaid_amount           NUMERIC(16, 2) NOT NULL DEFAULT 0,
    medical_insurance_paid   NUMERIC(16, 2) NOT NULL DEFAULT 0,
    personal_account_paid    NUMERIC(16, 2) NOT NULL DEFAULT 0,
    original_required_deposit NUMERIC(16, 2) NOT NULL DEFAULT 0,
    coefficient_snapshot     NUMERIC(10, 4) NOT NULL,
    final_required_deposit   NUMERIC(16, 2) NOT NULL,
    deposit_difference       NUMERIC(16, 2) NOT NULL,
    in_arrears               BOOLEAN NOT NULL,
    arrears_amount           NUMERIC(16, 2) NOT NULL DEFAULT 0,
    payment_status           VARCHAR(16) NOT NULL DEFAULT 'UNPAID',
    arrears_reason           TEXT,
    recovery_progress        TEXT,
    last_operated_by         BIGINT REFERENCES sys_user(id),
    source_updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_payment_status CHECK (payment_status IN ('UNPAID', 'PAID')),
    CONSTRAINT ck_arrears_amount CHECK (arrears_amount >= 0)
);

CREATE INDEX idx_arrears_current_report
    ON arrears_record(in_arrears, payment_status) WHERE in_arrears = TRUE AND payment_status = 'UNPAID';

CREATE TABLE operation_audit_log (
    id                  BIGSERIAL PRIMARY KEY,
    module_code         VARCHAR(64) NOT NULL,
    business_type       VARCHAR(64) NOT NULL,
    business_id         VARCHAR(64) NOT NULL,
    action_type         VARCHAR(32) NOT NULL,
    operator_id         BIGINT REFERENCES sys_user(id),
    operator_name       VARCHAR(128),
    before_data         JSONB,
    after_data          JSONB,
    operated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    client_ip           VARCHAR(64)
);

CREATE INDEX idx_audit_business ON operation_audit_log(business_type, business_id, operated_at DESC);

INSERT INTO sys_role(role_code, role_name, built_in) VALUES
    ('OPERATIONS', '运营部', TRUE),
    ('ATTENDING_DOCTOR', '主管医生', TRUE),
    ('OUTPATIENT', '门诊部', TRUE),
    ('NUTRITION', '营养科', TRUE),
    ('HOME_REHAB', '居家康复科', TRUE),
    ('SYSTEM_ADMIN', '系统管理员', TRUE),
    ('FINANCE', '财务科', TRUE),
    ('LEGAL', '法务部', TRUE),
    ('MEDICAL_INSURANCE', '医保办', TRUE),
    ('DEPARTMENT_DIRECTOR', '科主任', TRUE),
    ('BED_MANAGER', '床位管理员', TRUE),
    ('FOLLOW_UP', '随访员', TRUE);
