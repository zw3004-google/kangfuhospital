UPDATE sys_permission
   SET permission_name='推送重发',
       resource_path='/api/arrears/push-records/**',
       http_method='POST',
       enabled=TRUE
 WHERE permission_code='API_PUSH_RETRY';
