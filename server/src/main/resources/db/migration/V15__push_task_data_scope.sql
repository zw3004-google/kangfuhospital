ALTER TABLE push_task
    ADD COLUMN recipient_user_id BIGINT REFERENCES sys_user(id),
    ADD COLUMN scope_type VARCHAR(24) NOT NULL DEFAULT 'LEGACY_UNRESOLVED';

ALTER TABLE push_task
    ADD CONSTRAINT ck_push_task_scope_type
        CHECK (scope_type IN ('ALL', 'DEPARTMENT', 'DOCTOR', 'MIXED', 'LEGACY_UNRESOLVED'));

UPDATE push_task p
   SET recipient_user_id = u.id
  FROM sys_user u
 WHERE p.recipient_user_id IS NULL
   AND p.recipient_wecom_id IS NOT NULL
   AND BTRIM(p.recipient_wecom_id) <> ''
   AND u.wecom_user_id = p.recipient_wecom_id;

CREATE TABLE push_task_scope (
    task_id         BIGINT NOT NULL REFERENCES push_task(id) ON DELETE CASCADE,
    department_id   BIGINT REFERENCES sys_department(id),
    doctor_user_id  BIGINT REFERENCES sys_user(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_push_task_scope_target CHECK (
        (department_id IS NOT NULL AND doctor_user_id IS NULL)
        OR (department_id IS NULL AND doctor_user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_push_task_scope_department
    ON push_task_scope(task_id, department_id)
    WHERE department_id IS NOT NULL;

CREATE UNIQUE INDEX uk_push_task_scope_doctor
    ON push_task_scope(task_id, doctor_user_id)
    WHERE doctor_user_id IS NOT NULL;

CREATE INDEX idx_push_task_recipient_user
    ON push_task(recipient_user_id, business_type, scheduled_at DESC);

CREATE INDEX idx_push_task_scope_department
    ON push_task_scope(department_id, task_id)
    WHERE department_id IS NOT NULL;

CREATE INDEX idx_push_task_scope_doctor
    ON push_task_scope(doctor_user_id, task_id)
    WHERE doctor_user_id IS NOT NULL;

COMMENT ON COLUMN push_task.recipient_user_id IS '接收系统用户；历史任务仅在企微 ID 可唯一匹配时回填';
COMMENT ON COLUMN push_task.scope_type IS '任务正文固化时的数据范围：ALL/DEPARTMENT/DOCTOR/MIXED；LEGACY_UNRESOLVED 表示历史范围无法可靠还原';
COMMENT ON TABLE push_task_scope IS '推送任务固化的科室或主管医生数据范围；一条任务可包含多个范围目标';
