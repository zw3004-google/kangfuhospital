WITH normalized AS (
    SELECT id,
           BTRIM(CASE
               WHEN POSITION('详情请点击康复医院运营管理系统查看（院内内网访问）：' IN COALESCE(content, '')) > 0
                   THEN SPLIT_PART(content, '详情请点击康复医院运营管理系统查看（院内内网访问）：', 1)
               ELSE COALESCE(content, '')
           END) AS body
      FROM push_task
)
UPDATE push_task p
   SET content = CASE
           WHEN n.body = '' THEN '详情请点击康复医院运营管理系统查看（院内内网访问）：' || E'\n' || 'http://172.16.196.112'
           ELSE n.body || E'\n\n' || '详情请点击康复医院运营管理系统查看（院内内网访问）：' || E'\n' || 'http://172.16.196.112'
       END,
       updated_at = CURRENT_TIMESTAMP
  FROM normalized n
 WHERE p.id = n.id;