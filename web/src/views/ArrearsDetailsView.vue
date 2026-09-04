<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';import { useRouter } from 'vue-router';import { ElMessage,ElMessageBox } from 'element-plus';import { ArrowDown } from '@element-plus/icons-vue';import { http,ApiRequestError,type ApiResponse } from '../api/http'
interface Row{id:number;inpatientNo:string;admissionTimes:number;patientName:string;departmentName:string;wardName:string|null;feeType:string;arrearsType:string;doctorName:string;doctorEmployeeNo:string|null;admittedAt:string|null;dischargedAt:string|null;totalCost:number;prepaidAmount:number;medicalInsurancePaid:number;personalAccountPaid:number;finalRequiredDeposit:number;arrearsAmount:number;inArrears:boolean;paymentStatus:string;arrearsReason:string|null;recoveryProgress:string;previousRecoveryProgress:string;lastOperatedBy:string|null;sourceUpdatedAt:string|null} interface Page<T>{items:T[];total:number;page:number;pageSize:number}
interface FilterOptions{departments:{id:number;name:string}[];feeTypes:string[];arrearsTypes:string[]}
interface Summary{totalPeople:number;inpatientPeople:number;dischargedUnsettledPeople:number;dischargedSettledPeople:number;totalAmount:number;uncollectedPeople:number;legalPeople:number;sourceUpdatedAt:string|null}
interface HistoryItem{id:number;operatorName:string;operatedAt:string;actionType:string;beforeData:string;afterData:string;changeDescription:string}
interface ImportResult{batchNo:string;total:number;success:number;failure:number;added:number;overwritten:number;skipped:number}
interface ImportFailure{batchNo:string|null;errors:unknown[]}
interface ImportFeedback extends ImportResult{status:'success'|'error';message:string}
const progressOptions=[['NOT_STARTED','未催缴'],['NEGOTIATING','协商中'],['REFUSED','拒绝缴费'],['LEGAL_ACTION','移交法务发起诉讼'],['PAID','已缴费']] as const
const progressLabel=(value:string)=>progressOptions.find(([code])=>code===value)?.[1]||value
type TagType='primary'|'success'|'warning'|'info'|'danger'
const progressTagType=(value:string):TagType=>({NOT_STARTED:'danger',NEGOTIATING:'primary',REFUSED:'warning',LEGAL_ACTION:'info',PAID:'success'}[value] as TagType||'info')
const arrearsTypeLabels:Record<string,string>={INPATIENT:'在院患者',DISCHARGED_UNSETTLED:'出院未结算',DISCHARGED_SETTLED:'出院已结算'}
const arrearsTypeLabel=(value:string)=>arrearsTypeLabels[value]||value||'—'
const router=useRouter()
const rows=ref<Row[]>([]),total=ref(0),summary=ref<Summary>(),page=ref(1),pageSize=ref(50),keyword=ref(''),departmentId=ref<number>(),arrearsType=ref(''),feeType=ref(''),recoveryProgress=ref(''),inArrears=ref<boolean>(true),filterOptions=ref<FilterOptions>({departments:[],feeTypes:[],arrearsTypes:[]}),loading=ref(false),summaryLoading=ref(false),uploading=ref(false),saving=ref(false),historyLoading=ref(false),dialog=ref(false),current=ref<Row|null>(null),historyItems=ref<HistoryItem[]>([]),importFeedback=ref<ImportFeedback|null>(null),form=reactive({paymentStatus:'UNPAID',arrearsReason:'',recoveryProgress:''})
const queryParams=()=>({page:page.value,pageSize:pageSize.value,keyword:keyword.value||undefined,departmentId:departmentId.value,arrearsType:arrearsType.value||undefined,feeType:feeType.value||undefined,recoveryProgress:recoveryProgress.value||undefined,inArrears:inArrears.value})
const summaryParams=()=>{const {page:_,pageSize:__,...params}=queryParams();return params}
const load=async()=>{loading.value=true;try{const r=(await http.get<ApiResponse<Page<Row>>>('/arrears/records',{params:queryParams()})).data.data;rows.value=r.items;total.value=r.total}catch(e){ElMessage.error(e instanceof Error?e.message:'加载失败')}finally{loading.value=false}}
const loadSummary=async()=>{summaryLoading.value=true;try{summary.value=(await http.get<ApiResponse<Summary>>('/arrears/records/summary',{params:summaryParams()})).data.data}catch(e){ElMessage.error(e instanceof Error?e.message:'统计加载失败')}finally{summaryLoading.value=false}}
const loadAll=()=>Promise.all([load(),loadSummary()])
const loadFilterOptions=async()=>{try{filterOptions.value=(await http.get<ApiResponse<FilterOptions>>('/arrears/records/filter-options')).data.data}catch(e){ElMessage.error(e instanceof Error?e.message:'筛选项加载失败')}}
const reset=()=>{keyword.value='';departmentId.value=undefined;arrearsType.value='';feeType.value='';recoveryProgress.value='';inArrears.value=true;page.value=1;loadAll()}
const upload=async(file:File)=>{if(!/\.xlsx$/i.test(file.name)){ElMessage.error('仅支持 .xlsx 文件');return false}const data=new FormData();data.append('file',file);uploading.value=true;importFeedback.value=null;try{const result=(await http.post<ApiResponse<ImportResult>>('/arrears/import',data)).data.data;importFeedback.value={...result,status:'success',message:'欠费报表导入完成'};ElMessage.success('导入成功');page.value=1;await loadAll()}catch(e){const failure=e instanceof ApiRequestError?e.data as ImportFailure|undefined:undefined;importFeedback.value={status:'error',message:e instanceof Error?e.message:'导入失败',batchNo:failure?.batchNo||'',total:failure?.errors?.length||0,success:0,failure:failure?.errors?.length||0,added:0,overwritten:0,skipped:0};ElMessage.error(importFeedback.value.message)}finally{uploading.value=false}return false}
const openImportRecords=()=>router.push('/arrears/import-batches')
const loadHistory=async(id:number)=>{historyLoading.value=true;try{historyItems.value=(await http.get<ApiResponse<HistoryItem[]>>(`/arrears/records/${id}/history`)).data.data}catch(e){historyItems.value=[];ElMessage.error(e instanceof Error?e.message:'操作历史加载失败')}finally{historyLoading.value=false}}
const edit=(row:Row)=>{current.value=row;form.paymentStatus=row.paymentStatus;form.arrearsReason=row.arrearsReason||'';form.recoveryProgress=row.recoveryProgress||'NOT_STARTED';historyItems.value=[];dialog.value=true;loadHistory(row.id)}
const save=async()=>{if(!current.value)return;saving.value=true;try{form.arrearsReason=form.arrearsReason.trim();form.paymentStatus=form.recoveryProgress==='PAID'?'PAID':'UNPAID';await http.put(`/arrears/records/${current.value.id}`,form);ElMessage.success('已保存');await loadAll();await loadHistory(current.value.id);dialog.value=false}catch(e){ElMessage.error(e instanceof Error?e.message:'保存失败')}finally{saving.value=false}}
const togglePaid=async(row:Row)=>{const paid=row.paymentStatus==='PAID',action=paid?'恢复未缴费':'标记缴费';try{await ElMessageBox.confirm(`确认将“${row.patientName}”（欠费 ${money(row.arrearsAmount)} 元）${action}？`,`${action}确认`,{type:'warning',confirmButtonText:'确认',cancelButtonText:'取消'})}catch{return}try{await http.put(`/arrears/records/${row.id}`,{paymentStatus:paid?'UNPAID':'PAID',arrearsReason:row.arrearsReason,recoveryProgress:paid?null:'PAID'});ElMessage.success(`已${action}`);await loadAll()}catch(e){ElMessage.error(e instanceof Error?e.message:'操作失败')}}
const money=(value:number)=>Number(value||0).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})
const time=(value:string|null|undefined)=>value?new Date(value).toLocaleString('zh-CN',{hour12:false}):'—'
const date=(value:string|null|undefined)=>value?new Intl.DateTimeFormat('zh-CN',{year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date(value)).replaceAll('/','-'):'—'
const ward=(row:Row)=>row.wardName||row.departmentName||'—'
const isInpatient=(row:Row)=>row.arrearsType==='INPATIENT'||row.arrearsType==='在院患者'||!row.dischargedAt
const settlementMoney=(row:Row,value:number)=>isInpatient(row)?'—':money(value)
const exportStamp=()=>{const d=new Date(),part=(v:number)=>String(v).padStart(2,'0');return `${d.getFullYear()}${part(d.getMonth()+1)}${part(d.getDate())}_${part(d.getHours())}${part(d.getMinutes())}${part(d.getSeconds())}`}
const exportData=async(format:'xlsx'|'csv')=>{try{const r=await http.get('/arrears/records/export',{params:{...queryParams(),format},responseType:'blob'});const url=URL.createObjectURL(r.data),a=document.createElement('a');a.href=url;a.download=`欠费明细_${exportStamp()}.${format}`;a.click();setTimeout(()=>URL.revokeObjectURL(url),0)}catch(e){ElMessage.error(e instanceof Error?e.message:'导出失败')}};onMounted(()=>{loadFilterOptions();loadAll()})
</script>
<template>
<section class="page-card">
<div class="page-heading">
<div>
<h2>欠费明细</h2>
<p>列表、筛选、统计与导出使用相同数据范围。</p>
</div>
<div class="heading-actions">
<el-dropdown v-permission="'PERM_API_ARREARS_EXPORT'" @command="exportData">
<el-button plain>导出<el-icon class="el-icon--right"><ArrowDown/></el-icon></el-button>
<template #dropdown><el-dropdown-menu><el-dropdown-item command="xlsx">导出 Excel</el-dropdown-item><el-dropdown-item command="csv">导出 CSV</el-dropdown-item></el-dropdown-menu></template>
</el-dropdown>
<el-upload :show-file-list="false" :before-upload="upload" accept=".xlsx">
<el-button v-permission="'PERM_API_ARREARS_IMPORT'" type="primary" :loading="uploading">导入欠费报表</el-button>
</el-upload>
</div>
</div>
<el-alert v-if="importFeedback" :type="importFeedback.status" :closable="true" show-icon class="import-feedback" @close="importFeedback=null">
<template #title>{{importFeedback.message}}<span v-if="importFeedback.batchNo"> · 批次 {{importFeedback.batchNo}}</span></template>
<div>总数 {{importFeedback.total}} · 成功 {{importFeedback.success}} · 失败 {{importFeedback.failure}} · 新增 {{importFeedback.added}} · 覆盖 {{importFeedback.overwritten}} · 跳过 {{importFeedback.skipped}}</div>
<el-button v-if="importFeedback.status==='error'&&importFeedback.batchNo" link type="primary" @click="openImportRecords">查看导入记录</el-button>
</el-alert>
<div v-loading="summaryLoading" class="stat-grid">
<div>
<span>欠费患者数</span>
<strong>{{summary?.totalPeople||0}} 人</strong>
<small>在院 {{summary?.inpatientPeople||0}} · 出院未结算 {{summary?.dischargedUnsettledPeople||0}} · 出院已结算 {{summary?.dischargedSettledPeople||0}}</small>
</div>
<div>
<span>欠费金额合计</span>
<strong>{{money(summary?.totalAmount||0)}} 元</strong>
<small>数据更新于 {{time(summary?.sourceUpdatedAt)}}</small>
</div>
<div>
<span>未催缴</span>
<strong>{{summary?.uncollectedPeople||0}} 人</strong>
<small>点击“编辑标注”维护追缴进度</small>
</div>
<div>
<span>移交法务发起诉讼</span>
<strong>{{summary?.legalPeople||0}} 人</strong>
<small>法务角色可在授权范围内查看并跟进</small>
</div>
</div>
<div class="filter-bar">
<el-select v-model="arrearsType" clearable placeholder="全部欠费类型">
<el-option v-for="item in filterOptions.arrearsTypes" :key="item" :label="arrearsTypeLabel(item)" :value="item"/>
</el-select>
<el-select v-model="departmentId" clearable placeholder="全部科室">
<el-option v-for="item in filterOptions.departments" :key="item.id" :label="item.name" :value="item.id"/>
</el-select>
<el-select v-model="feeType" clearable placeholder="全部费别">
<el-option v-for="item in filterOptions.feeTypes" :key="item" :label="item" :value="item"/>
</el-select>
<el-select v-model="recoveryProgress" clearable placeholder="追缴进度（全部）">
<el-option v-for="item in progressOptions" :key="item[0]" :label="item[1]" :value="item[0]"/>
</el-select>
<el-select v-model="inArrears" placeholder="欠费状态">
<el-option label="当前欠费" :value="true"/>
<el-option label="未欠费" :value="false"/>
</el-select>
<el-input v-model="keyword" clearable placeholder="住院号 / 姓名 / 主管医生 / 医生工号" @keyup.enter="page=1;loadAll()"/>
<el-button type="primary" @click="page=1;loadAll()">查询</el-button>
<el-button @click="reset">重置</el-button>
</div>
<h3 class="table-section-title">欠费患者列表</h3>
<el-table v-loading="loading" :data="rows" stripe class="arrears-table">
<el-table-column prop="inpatientNo" label="住院号" width="130" fixed="left" show-overflow-tooltip/>
<el-table-column prop="admissionTimes" label="住院次数" width="90" align="center"/>
<el-table-column prop="patientName" label="姓名" width="100" fixed="left" show-overflow-tooltip/>
<el-table-column label="住院病区" width="130" show-overflow-tooltip>
<template #default="s">{{ward(s.row)}}</template>
</el-table-column>
<el-table-column prop="feeType" label="费别" width="110" show-overflow-tooltip/>
<el-table-column label="欠费类型" width="120">
<template #default="s">{{arrearsTypeLabel(s.row.arrearsType)}}</template>
</el-table-column>
<el-table-column prop="doctorName" label="主管医生" width="110" show-overflow-tooltip/>
<el-table-column prop="doctorEmployeeNo" label="主管医生工号" width="130" show-overflow-tooltip>
<template #default="s">{{s.row.doctorEmployeeNo||'—'}}</template>
</el-table-column>
<el-table-column label="入区日期" width="115">
<template #default="s">{{date(s.row.admittedAt)}}</template>
</el-table-column>
<el-table-column label="出区日期" width="115">
<template #default="s">{{s.row.dischargedAt?date(s.row.dischargedAt):'未出区'}}</template>
</el-table-column>
<el-table-column label="总费用" width="125" align="right" header-align="right">
<template #default="s">
<span class="money-cell">{{money(s.row.totalCost)}}</span>
</template>
</el-table-column>
<el-table-column label="预交金" width="125" align="right" header-align="right">
<template #default="s">
<span class="money-cell">{{money(s.row.prepaidAmount)}}</span>
</template>
</el-table-column>
<el-table-column label="医保支付" width="125" align="right" header-align="right">
<template #default="s">
<span class="money-cell">{{settlementMoney(s.row,s.row.medicalInsurancePaid)}}</span>
</template>
</el-table-column>
<el-table-column label="个人账户支付" width="135" align="right" header-align="right">
<template #default="s">
<span class="money-cell">{{settlementMoney(s.row,s.row.personalAccountPaid)}}</span>
</template>
</el-table-column>
<el-table-column label="应交押金" width="125" align="right" header-align="right">
<template #default="s">
<span class="money-cell">{{money(s.row.finalRequiredDeposit)}}</span>
</template>
</el-table-column>
<el-table-column label="欠费金额" width="130" align="right" header-align="right">
<template #default="s">
<span class="money-cell arrears-money">{{money(s.row.arrearsAmount)}}</span>
</template>
</el-table-column>
<el-table-column prop="arrearsReason" label="欠费原因" min-width="180" show-overflow-tooltip>
<template #default="s">{{s.row.arrearsReason||'—'}}</template>
</el-table-column>
<el-table-column label="追缴进度" width="160">
<template #default="s">
<el-tag :type="progressTagType(s.row.recoveryProgress)" effect="light">{{progressLabel(s.row.recoveryProgress)}}</el-tag>
</template>
</el-table-column>
<el-table-column prop="lastOperatedBy" label="最近操作人" width="120" show-overflow-tooltip>
<template #default="s">{{s.row.lastOperatedBy||'—'}}</template>
</el-table-column>
<el-table-column label="数据更新时间" width="180">
<template #default="s">{{time(s.row.sourceUpdatedAt)}}</template>
</el-table-column>
<el-table-column label="操作" width="180" fixed="right">
<template #default="s">
<el-button v-permission="'PERM_API_ARREARS_EDIT'" link @click="edit(s.row)">编辑标注</el-button>
<el-button v-permission="'PERM_API_ARREARS_EDIT'" link @click="togglePaid(s.row)">{{s.row.paymentStatus==='PAID'?'恢复未缴费':'标记缴费'}}</el-button>
</template>
</el-table-column>
</el-table>
<el-pagination class="pagination" v-model:current-page="page" v-model:page-size="pageSize" :page-sizes="[20,50,100,200]" :total="total" layout="total,sizes,prev,pager,next" @change="load"/>
<el-dialog v-model="dialog" title="编辑催缴信息" width="720px">
<el-descriptions v-if="current" :column="3" border class="arrears-edit-summary">
<el-descriptions-item label="姓名">{{current.patientName}}</el-descriptions-item>
<el-descriptions-item label="住院号">{{current.inpatientNo}}</el-descriptions-item>
<el-descriptions-item label="住院次数">{{current.admissionTimes}}</el-descriptions-item>
<el-descriptions-item label="住院病区">{{ward(current)}}</el-descriptions-item>
<el-descriptions-item label="主管医生">{{current.doctorName||'—'}}</el-descriptions-item>
<el-descriptions-item label="医生工号">{{current.doctorEmployeeNo||'—'}}</el-descriptions-item>
<el-descriptions-item label="欠费金额"><span class="arrears-money">{{money(current.arrearsAmount)}} 元</span></el-descriptions-item>
</el-descriptions>
<el-form label-width="90px">
<el-form-item label="欠费原因">
<el-input v-model="form.arrearsReason" type="textarea" :rows="3" maxlength="500" show-word-limit/>
</el-form-item>
<el-form-item label="追缴进度">
<el-select v-model="form.recoveryProgress" style="width:100%">
<el-option v-for="item in progressOptions" :key="item[0]" :label="item[1]" :value="item[0]"/>
</el-select>
</el-form-item>
</el-form>
<h4 class="history-title">操作历史</h4>
<div v-loading="historyLoading" class="history-list">
<el-empty v-if="!historyLoading&&!historyItems.length" description="暂无操作历史" :image-size="60"/>
<el-timeline v-else>
<el-timeline-item v-for="item in historyItems" :key="item.id" :timestamp="time(item.operatedAt)" placement="top">
<strong>{{item.operatorName||'system'}}</strong><p>{{item.changeDescription}}</p>
</el-timeline-item>
</el-timeline>
</div>
<template #footer>
<el-button :disabled="saving" @click="dialog=false">取消</el-button>
<el-button type="primary" :loading="saving" @click="save">保存</el-button>
</template>
</el-dialog>
</section>
</template>
