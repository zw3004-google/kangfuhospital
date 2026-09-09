<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'
interface Batch{batchNo:string;businessType:string;sourceType:'EXCEL'|'API';transactionCode:string|null;triggerType:string|null;filename:string|null;status:string;total:number;success:number;failure:number;added:number;overwritten:number;skipped:number;summaryStatus:string|null;startedAt:string;finishedAt:string|null;errorMessage:string|null} interface ImportError{rowNumber:number;inpatientNo:string|null;admissionTimes:number|null;fieldName:string|null;originalValue:string|null;errorCode:string;errorMessage:string}
const route=useRoute(),loading=ref(false),rows=ref<Batch[]>([]),errors=ref<ImportError[]>([]),dialog=ref(false),current=ref<Batch|null>(null),businessType=computed(()=>String(route.meta.businessType||''))
const fmt=(v:string|null)=>v?new Date(v).toLocaleString('zh-CN',{hour12:false}):'—'
const sourceLabel=(row:Batch)=>row.sourceType==='API'?'接口同步':'Excel 导入'
const sourceDetail=(row:Batch)=>row.sourceType==='API'?`${row.transactionCode||'—'} · ${row.triggerType==='AUTO'?'自动':'手动'}`:(row.filename||'—')
const load=async()=>{loading.value=true;try{rows.value=(await http.get<ApiResponse<Batch[]>>('/import-batches',{params:{businessType:businessType.value}})).data.data}catch(e){ElMessage.error(e instanceof Error?e.message:'加载失败')}finally{loading.value=false}}
const showErrors=async(row:Batch)=>{current.value=row;dialog.value=true;errors.value=[];try{errors.value=(await http.get<ApiResponse<ImportError[]>>(`/import-batches/${encodeURIComponent(row.batchNo)}/errors`)).data.data}catch(e){ElMessage.error(e instanceof Error?e.message:'加载失败')}}
watch(businessType,load,{immediate:true})
</script>
<template>
<section class="page-card import-batch-page">
<div class="page-heading"><div><h2>导入批次</h2><p>查看 Excel 导入和接口同步的处理结果；失败批次可展开定位到原始记录和字段。</p></div><el-button :loading="loading" @click="load">刷新</el-button></div>
<el-table v-loading="loading" :data="rows" stripe class="data-table desktop-only">
<el-table-column prop="batchNo" label="批次号" min-width="170"/>
<el-table-column label="来源" width="110"><template #default="s"><el-tag size="small" :type="s.row.sourceType==='API'?'success':'info'">{{sourceLabel(s.row)}}</el-tag></template></el-table-column>
<el-table-column label="交易/文件" min-width="180" show-overflow-tooltip><template #default="s">{{sourceDetail(s.row)}}</template></el-table-column>
<el-table-column prop="status" label="状态" width="100"/>
<el-table-column label="总数/成功/失败" width="150"><template #default="s">{{s.row.total}} / {{s.row.success}} / <span :class="{'danger-text':s.row.failure}">{{s.row.failure}}</span></template></el-table-column>
<el-table-column label="新增/覆盖/跳过" width="150"><template #default="s">{{s.row.added}} / {{s.row.overwritten}} / {{s.row.skipped}}</template></el-table-column>
<el-table-column label="开始时间" width="180"><template #default="s">{{fmt(s.row.startedAt)}}</template></el-table-column>
<el-table-column prop="errorMessage" label="批次错误" min-width="180" show-overflow-tooltip/>
<el-table-column label="操作" width="100"><template #default="s"><el-button v-if="s.row.failure||s.row.status==='FAILED'" link type="primary" @click="showErrors(s.row)">错误详情</el-button></template></el-table-column>
</el-table>
<div v-loading="loading" class="mobile-only import-batch-mobile-list">
<el-empty v-if="!loading&&!rows.length" description="暂无导入或同步批次"/>
<article v-for="row in rows" :key="row.batchNo" class="import-batch-mobile-card">
<header><strong>{{row.batchNo}}</strong><el-tag size="small" :type="row.sourceType==='API'?'success':'info'">{{sourceLabel(row)}}</el-tag></header>
<dl><div class="wide"><dt>交易/文件</dt><dd>{{sourceDetail(row)}}</dd></div><div><dt>状态</dt><dd>{{row.status}}</dd></div><div><dt>开始时间</dt><dd>{{fmt(row.startedAt)}}</dd></div><div><dt>总数 / 成功 / 失败</dt><dd>{{row.total}} / {{row.success}} / <span :class="{'danger-text':row.failure}">{{row.failure}}</span></dd></div><div><dt>新增 / 覆盖 / 跳过</dt><dd>{{row.added}} / {{row.overwritten}} / {{row.skipped}}</dd></div><div v-if="row.errorMessage" class="wide error"><dt>批次错误</dt><dd>{{row.errorMessage}}</dd></div></dl>
<footer v-if="row.failure||row.status==='FAILED'"><el-button type="primary" @click="showErrors(row)">错误详情</el-button></footer>
</article>
</div>
<el-dialog v-model="dialog" :title="`导入错误 · ${current?.batchNo||''}`" width="78%" class="mobile-full-dialog">
<el-empty v-if="!errors.length" description="该批次没有逐条错误"/>
<el-table v-else :data="errors" max-height="520" class="desktop-only"><el-table-column prop="rowNumber" label="行号" width="70"/><el-table-column prop="inpatientNo" label="住院号" width="120"/><el-table-column prop="admissionTimes" label="住院次数" width="90"/><el-table-column prop="fieldName" label="字段" width="130"/><el-table-column prop="originalValue" label="原值" min-width="130" show-overflow-tooltip/><el-table-column prop="errorCode" label="错误码" width="150"/><el-table-column prop="errorMessage" label="错误说明" min-width="220"/></el-table>
<div v-if="errors.length" class="mobile-only import-error-mobile-list"><article v-for="item in errors" :key="`${item.rowNumber}-${item.errorCode}-${item.fieldName||''}`" class="import-error-mobile-card"><header><strong>第 {{item.rowNumber}} 条 · {{item.errorCode}}</strong></header><dl><div><dt>住院号</dt><dd>{{item.inpatientNo||'—'}}</dd></div><div><dt>住院次数</dt><dd>{{item.admissionTimes??'—'}}</dd></div><div><dt>字段</dt><dd>{{item.fieldName||'—'}}</dd></div><div><dt>原值</dt><dd>{{item.originalValue||'—'}}</dd></div><div class="wide error"><dt>错误说明</dt><dd>{{item.errorMessage}}</dd></div></dl></article></div>
</el-dialog>
</section>
</template>
