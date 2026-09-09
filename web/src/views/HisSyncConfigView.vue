<script setup lang="ts">
import {onMounted,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {http,type ApiResponse} from '../api/http'

type SyncType='INPATIENT_ARREARS'|'DISCHARGED_ARREARS'|'PATIENT_INFO'
interface Config{syncType:SyncType;enabled:boolean;dailyTime:string;lastAttemptAt:string|null;lastSuccessAt:string|null;lastBatchNo:string|null;lastError:string|null;running:boolean;updatedAt:string}
const labels:Record<SyncType,string>={INPATIENT_ARREARS:'在院患者欠费',DISCHARGED_ARREARS:'出院患者欠费',PATIENT_INFO:'患者信息'}
const rows=ref<Config[]>([]),loading=ref(false),saving=ref<SyncType>()
const fmt=(value:string|null)=>value?new Date(value).toLocaleString('zh-CN',{hour12:false}):'—'
const load=async()=>{loading.value=true;try{rows.value=(await http.get<ApiResponse<Config[]>>('/integration/his-sync/configs')).data.data}catch(e){ElMessage.error(e instanceof Error?e.message:'同步配置加载失败')}finally{loading.value=false}}
const save=async(row:Config)=>{saving.value=row.syncType;try{const value=(await http.put<ApiResponse<Config>>(`/integration/his-sync/configs/${row.syncType}`,{enabled:row.enabled,dailyTime:row.dailyTime})).data.data;Object.assign(row,value);ElMessage.success('同步配置已保存')}catch(e){ElMessage.error(e instanceof Error?e.message:'同步配置保存失败');await load()}finally{saving.value=undefined}}
onMounted(load)
</script>
<template>
<section class="page-card his-sync-config-page">
<div class="page-heading"><div><h2>HIS 同步配置</h2><p>三类数据独立启停；修改保存后无需重启应用。</p></div><el-button plain @click="load">刷新</el-button></div>
<el-alert title="HIS 网关地址、机构号、应用标识和授权标识由部署环境变量提供，本页面不展示或保存敏感信息。" type="info" :closable="false" show-icon class="import-feedback"/>
<el-table v-loading="loading" :data="rows" stripe class="desktop-only">
<el-table-column label="数据类型" min-width="170"><template #default="s"><strong>{{labels[s.row.syncType as SyncType]}}</strong></template></el-table-column>
<el-table-column label="自动同步" width="120"><template #default="s"><el-switch v-model="s.row.enabled"/></template></el-table-column>
<el-table-column label="每日时间" width="180"><template #default="s"><el-time-picker v-model="s.row.dailyTime" value-format="HH:mm:ss" format="HH:mm" :clearable="false"/></template></el-table-column>
<el-table-column label="运行状态" width="110"><template #default="s"><el-tag :type="s.row.running?'warning':'info'">{{s.row.running?'运行中':'空闲'}}</el-tag></template></el-table-column>
<el-table-column label="最近运行" width="180"><template #default="s">{{fmt(s.row.lastAttemptAt)}}</template></el-table-column>
<el-table-column label="最近成功" width="180"><template #default="s">{{fmt(s.row.lastSuccessAt)}}</template></el-table-column>
<el-table-column label="最近批次" min-width="190"><template #default="s">{{s.row.lastBatchNo||'—'}}</template></el-table-column>
<el-table-column label="错误摘要" min-width="220" show-overflow-tooltip><template #default="s"><span :class="{'danger-text':s.row.lastError}">{{s.row.lastError||'—'}}</span></template></el-table-column>
<el-table-column label="操作" width="100" fixed="right"><template #default="s"><el-button type="primary" link :loading="saving===s.row.syncType" @click="save(s.row)">保存</el-button></template></el-table-column>
</el-table>
<div v-loading="loading" class="mobile-only his-sync-mobile-list">
<el-empty v-if="!loading&&!rows.length" description="暂无同步配置"/>
<article v-for="row in rows" :key="row.syncType" class="his-sync-mobile-card">
<header><strong>{{labels[row.syncType]}}</strong><el-tag :type="row.running?'warning':'info'">{{row.running?'运行中':'空闲'}}</el-tag></header>
<div class="his-sync-mobile-controls"><label><span>自动同步</span><el-switch v-model="row.enabled"/></label><label><span>每日同步时间</span><el-time-picker v-model="row.dailyTime" value-format="HH:mm:ss" format="HH:mm" :clearable="false"/></label></div>
<dl><div><dt>最近运行</dt><dd>{{fmt(row.lastAttemptAt)}}</dd></div><div><dt>最近成功</dt><dd>{{fmt(row.lastSuccessAt)}}</dd></div><div class="wide"><dt>最近批次</dt><dd>{{row.lastBatchNo||'—'}}</dd></div><div v-if="row.lastError" class="wide error"><dt>错误摘要</dt><dd>{{row.lastError}}</dd></div></dl>
<footer><el-button type="primary" :loading="saving===row.syncType" @click="save(row)">保存配置</el-button></footer>
</article>
</div>
</section>
</template>
