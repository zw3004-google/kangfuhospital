CREATE TABLE sys_fee_type (
    id          BIGSERIAL PRIMARY KEY,
    fee_code    VARCHAR(32) NOT NULL,
    fee_name    VARCHAR(64) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_fee_type_code CHECK (fee_code ~ '^[A-Z0-9]+$')
);

CREATE UNIQUE INDEX uk_fee_type_code ON sys_fee_type (UPPER(fee_code));
CREATE UNIQUE INDEX uk_fee_type_name ON sys_fee_type (BTRIM(fee_name));

INSERT INTO sys_fee_type(fee_code, fee_name)
SELECT 'LEGACY' || LPAD(ROW_NUMBER() OVER (ORDER BY fee_type)::TEXT, 3, '0'), fee_type
FROM (SELECT DISTINCT BTRIM(fee_type) fee_type FROM sys_fee_coefficient) source;

ALTER TABLE sys_fee_coefficient
    ADD COLUMN fee_type_id BIGINT REFERENCES sys_fee_type(id),
    ADD COLUMN created_by_name VARCHAR(128),
    ADD COLUMN enabled_by_name VARCHAR(128),
    ADD COLUMN disabled_by_name VARCHAR(128);

UPDATE sys_fee_coefficient coefficient
SET fee_type_id = fee.id
FROM sys_fee_type fee
WHERE fee.fee_name = BTRIM(coefficient.fee_type);

ALTER TABLE sys_fee_coefficient ALTER COLUMN fee_type_id SET NOT NULL;
DROP INDEX uk_fee_coefficient_enabled;
CREATE UNIQUE INDEX uk_fee_coefficient_enabled
    ON sys_fee_coefficient (fee_type_id) WHERE enabled = TRUE;

ALTER TABLE arrears_record
    ADD COLUMN coefficient_version_id BIGINT REFERENCES sys_fee_coefficient(id);
