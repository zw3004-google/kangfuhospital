INSERT INTO sys_permission(permission_code,permission_name,permission_type,resource_path,http_method) VALUES
 ('FIELD_ARREARS_REASON','欠费原因编辑','FIELD',NULL,NULL),
 ('FIELD_RECOVERY_PROGRESS','追缴进度编辑','FIELD',NULL,NULL),
 ('FIELD_NUTRITION','营养科字段编辑','FIELD',NULL,NULL),
 ('FIELD_HOME_REHAB','居家康复字段编辑','FIELD',NULL,NULL),
 ('FIELD_NURSING','护理字段编辑','FIELD',NULL,NULL),
 ('FIELD_FOLLOW_UP','随访字段编辑','FIELD',NULL,NULL),
 ('FIELD_ATTENDING_DOCTOR','主管医生字段编辑','FIELD',NULL,NULL)
ON CONFLICT(permission_code) DO NOTHING;
