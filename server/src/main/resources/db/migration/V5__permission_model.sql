CREATE TABLE sys_permission (
    id BIGSERIAL PRIMARY KEY,
    permission_code VARCHAR(128) NOT NULL UNIQUE,
    permission_name VARCHAR(128) NOT NULL,
    permission_type VARCHAR(16) NOT NULL,
    resource_path VARCHAR(255),
    http_method VARCHAR(16),
    parent_id BIGINT REFERENCES sys_permission(id),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE sys_role_permission (
    role_id BIGINT NOT NULL REFERENCES sys_role(id),
    permission_id BIGINT NOT NULL REFERENCES sys_permission(id),
    PRIMARY KEY(role_id, permission_id)
);
CREATE INDEX idx_sys_user_locked_until ON sys_user(locked_until);
INSERT INTO sys_permission(permission_code,permission_name,permission_type,resource_path,http_method) VALUES
 ('MENU_ARREARS','欠费管理','MENU','/arrears',NULL),
 ('MENU_DISCHARGE','预出院管理','MENU','/discharge',NULL),
 ('MENU_SYSTEM','系统管理','MENU','/system',NULL),
 ('API_USER_MANAGE','用户管理','API','/api/system/users','*'),
 ('API_ROLE_MANAGE','角色权限管理','API','/api/system/roles','*'),
 ('API_ARREARS_EDIT','欠费编辑','API','/api/arrears/records/*','PUT'),
 ('API_DISCHARGE_EDIT','预出院填报','API','/api/discharge/records/*','PUT'),
 ('API_PUSH_RETRY','推送重发','API','/api/arrears/push-records/*/retry','POST')
ON CONFLICT(permission_code) DO NOTHING;
