CREATE TABLE sys_user_department (
    user_id BIGINT NOT NULL REFERENCES sys_user(id),
    department_id BIGINT NOT NULL REFERENCES sys_department(id),
    PRIMARY KEY (user_id, department_id)
);

-- Preserve existing role-based scopes when moving the configuration to users.
INSERT INTO sys_user_department(user_id, department_id)
SELECT DISTINCT ur.user_id, rd.department_id
FROM sys_user_role ur
JOIN sys_role_department rd ON rd.role_id = ur.role_id
ON CONFLICT DO NOTHING;