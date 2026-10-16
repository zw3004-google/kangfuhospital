-- 将既有随访员填报明细中的回访时间同步到列表筛选字段。
UPDATE discharge_record
SET follow_up_day7_at = NULLIF(jsonb_path_query_first(follow_up_details, '$[*] ? (@.day == 7).followUpAt') #>> '{}', '')::TIMESTAMPTZ,
    follow_up_day30_at = NULLIF(jsonb_path_query_first(follow_up_details, '$[*] ? (@.day == 30).followUpAt') #>> '{}', '')::TIMESTAMPTZ,
    follow_up_day60_at = NULLIF(jsonb_path_query_first(follow_up_details, '$[*] ? (@.day == 60).followUpAt') #>> '{}', '')::TIMESTAMPTZ
WHERE follow_up_details IS NOT NULL;
