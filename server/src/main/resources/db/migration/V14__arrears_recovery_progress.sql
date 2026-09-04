ALTER TABLE arrears_record
    ADD COLUMN recovery_progress_legacy TEXT,
    ADD COLUMN previous_recovery_progress VARCHAR(32);

UPDATE arrears_record
SET recovery_progress_legacy = recovery_progress;

UPDATE arrears_record
SET recovery_progress = CASE
    WHEN payment_status = 'PAID' THEN 'PAID'
    WHEN recovery_progress IS NULL OR BTRIM(recovery_progress) = '' THEN 'NOT_STARTED'
    WHEN UPPER(BTRIM(recovery_progress)) IN ('NOT_STARTED', 'NEGOTIATING', 'REFUSED', 'LEGAL_ACTION', 'PAID')
        THEN UPPER(BTRIM(recovery_progress))
    WHEN BTRIM(recovery_progress) IN ('未催缴', '未开始') THEN 'NOT_STARTED'
    WHEN BTRIM(recovery_progress) IN ('协商中', '跟进中') THEN 'NEGOTIATING'
    WHEN BTRIM(recovery_progress) = '拒绝缴费' THEN 'REFUSED'
    WHEN BTRIM(recovery_progress) IN ('移交法务', '移交法务发起诉讼') THEN 'LEGAL_ACTION'
    WHEN BTRIM(recovery_progress) = '已缴费' THEN 'PAID'
    ELSE 'NOT_STARTED'
END;

UPDATE arrears_record
SET payment_status = 'PAID'
WHERE recovery_progress = 'PAID';

UPDATE arrears_record
SET previous_recovery_progress = CASE
    WHEN recovery_progress = 'PAID' THEN 'NOT_STARTED'
    ELSE recovery_progress
END;

ALTER TABLE arrears_record
    ALTER COLUMN recovery_progress SET DEFAULT 'NOT_STARTED',
    ALTER COLUMN recovery_progress SET NOT NULL,
    ALTER COLUMN previous_recovery_progress SET DEFAULT 'NOT_STARTED',
    ALTER COLUMN previous_recovery_progress SET NOT NULL,
    ADD CONSTRAINT ck_recovery_progress CHECK (
        recovery_progress IN ('NOT_STARTED', 'NEGOTIATING', 'REFUSED', 'LEGAL_ACTION', 'PAID')
    ),
    ADD CONSTRAINT ck_previous_recovery_progress CHECK (
        previous_recovery_progress IN ('NOT_STARTED', 'NEGOTIATING', 'REFUSED', 'LEGAL_ACTION')
    ),
    ADD CONSTRAINT ck_payment_recovery_consistency CHECK (
        (payment_status = 'PAID' AND recovery_progress = 'PAID')
        OR (payment_status = 'UNPAID' AND recovery_progress <> 'PAID')
    );

COMMENT ON COLUMN arrears_record.recovery_progress_legacy IS
    'V14迁移前的追缴进度原始文本；无法识别的值保留在此供人工核对';
COMMENT ON COLUMN arrears_record.previous_recovery_progress IS
    '最近一次非已缴费追缴进度，用于恢复未缴费状态';
