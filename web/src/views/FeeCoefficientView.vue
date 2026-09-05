<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface FeeCoefficient { id:number;feeTypeId:number;feeCode:string;feeType:string;coefficient:number;enabled:boolean;effectiveAt:string|null;disabledAt:string|null;createdAt:string;createdByName:string|null;enabledByName:string|null;disabledByName:string|null }
const loading=ref(false),saving=ref(false),records=ref<FeeCoefficient[]>([]),dialogVisible=ref(false),versionMode=ref(false)
const filters=reactive<{feeCode:string;feeType:string;enabled:''|'true'|'false'}>({feeCode:'',feeType:'',enabled:''})
const form=reactive({feeCode:'',feeType:'',coefficient:1})
const dialogTitle=computed(()=>versionMode.value?'新增系数版本':'新增费别')
const operator=(row:FeeCoefficient)=>row.disabledByName||row.enabledByName||row.createdByName||'-'
const example=(value:number)=>`100,000 × ${Number(value).toFixed(4)} = ${(100000*Number(value)).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2})} 元`
const formatTime=(value:string|null)=>value?new Date(value).toLocaleString('zh-CN',{hour12:false}):'-'

async function load(){loading.value=true;try{const response=await http.get<ApiResponse<FeeCoefficient[]>>('/system/fee-coefficients',{params:{feeCode:filters.feeCode||undefined,feeType:filters.feeType||undefined,enabled:filters.enabled===''?undefined:filters.enabled==='true'}});records.value=response.data.data}catch(error){ElMessage.error(error instanceof Error?error.message:'加载失败')}finally{loading.value=false}}
function reset(){filters.feeCode='';filters.feeType='';filters.enabled='';void load()}
function openCreate(){versionMode.value=false;form.feeCode='';form.feeType='';form.coefficient=1;dialogVisible.value=true}
function openVersion(row:FeeCoefficient){versionMode.value=true;form.feeCode=row.feeCode;form.feeType=row.feeType;form.coefficient=Number(row.coefficient);dialogVisible.value=true}
async function createRecord(){
  const code=form.feeCode.trim().toUpperCase(),name=form.feeType.trim()
  if(!/^[A-Z0-9]{1,32}$/.test(code)){ElMessage.warning('费别编码仅支持1～32位字母和数字');return}
  if(!name){ElMessage.warning('请输入费别名称');return}
  saving.value=true;try{await http.post('/system/fee-coefficients',{feeCode:code,feeType:name,coefficient:form.coefficient});ElMessage.success(versionMode.value?'系数版本已新增，请确认后启用':'费别已新增，请确认后启用初始系数');dialogVisible.value=false;await load()}catch(error){ElMessage.error(error instanceof Error?error.message:'保存失败')}finally{saving.value=false}
}
async function changeStatus(row:FeeCoefficient){const action=row.enabled?'停用':'启用';await ElMessageBox.confirm(row.enabled?`确认停用“${row.feeType}”当前系数？停用后该费别将暂时无法导入。`:`确认启用系数 ${Number(row.coefficient).toFixed(4)}？同费别的原启用版本将自动停用。`,`${action}确认`,{type:'warning'});try{await http.post(`/system/fee-coefficients/${row.id}/${row.enabled?'disable':'enable'}`);ElMessage.success(`${action}成功`);await load()}catch(error){ElMessage.error(error instanceof Error?error.message:`${action}失败`)}}
onMounted(load)
</script>

<template>
  <div class="fee-page">
    <section class="page-card fee-rule-card">
      <div class="page-heading"><div><h2>计算口径</h2><p>欠费导入按费别名称匹配当前启用版本，费别编码用于系统内唯一识别。</p></div></div>
      <el-alert type="info" :closable="false" show-icon>
        <template #title>最终应交押金 = 原始应交押金 × 当前启用的费别系数</template>
        <p class="formula-lines">押金差额 = 预交金 − 最终应交押金<br>是否欠费 = 押金差额 &lt; 0；欠费金额 = 欠费时取押金差额绝对值，否则为 0</p>
      </el-alert>
    </section>
    <section class="page-card">
      <div class="page-heading"><div><h2>患者费别系数</h2><p>同一费别可保留多个历史版本，但同一时间只能启用一个版本；历史数据不追溯重算。</p></div><el-button type="primary" @click="openCreate">新增费别</el-button></div>
      <div class="filter-bar fee-filter-bar">
        <el-input v-model="filters.feeCode" clearable placeholder="费别编码" />
        <el-input v-model="filters.feeType" clearable placeholder="费别名称" @keyup.enter="load" />
        <el-select v-model="filters.enabled" placeholder="状态（全部）" clearable><el-option label="已启用" value="true"/><el-option label="已停用" value="false"/></el-select>
        <el-button type="primary" plain @click="load">查询</el-button><el-button @click="reset">重置</el-button>
      </div>
      <div v-loading="loading" class="mobile-only mobile-record-list admin-mobile-list"><el-empty v-if="!loading&&!records.length" description="暂无费别系数"/><article v-for="row in records" :key="row.id" class="mobile-record-card fee-mobile-card"><header><div><strong>{{row.feeType}}</strong><span>{{row.feeCode}} · 版本 #{{row.id}}</span></div><el-tag :type="row.enabled?'success':'info'">{{row.enabled?'已启用':'已停用'}}</el-tag></header><div class="mobile-record-primary"><span>当前系数</span><strong>{{Number(row.coefficient).toFixed(4)}}</strong></div><dl><div><dt>生效时间</dt><dd>{{formatTime(row.effectiveAt)}}</dd></div><div><dt>最近操作人</dt><dd>{{operator(row)}}</dd></div><div class="admin-card-wide"><dt>计算示例</dt><dd>{{example(row.coefficient)}}</dd></div></dl><footer><el-button link type="primary" @click="openVersion(row)">新增版本</el-button><el-button link :type="row.enabled?'danger':'primary'" @click="changeStatus(row)">{{row.enabled?'停用':'启用'}}</el-button></footer></article></div>
      <el-table v-loading="loading" :data="records" stripe class="desktop-only">
        <el-table-column prop="feeCode" label="费别编码" min-width="120"/>
        <el-table-column prop="feeType" label="费别名称" min-width="200"/>
        <el-table-column label="系数" width="110"><template #default="scope">{{ Number(scope.row.coefficient).toFixed(4) }}</template></el-table-column>
        <el-table-column label="计算示例（原始应交押金10万元）" min-width="270"><template #default="scope">{{ example(scope.row.coefficient) }}</template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="scope.row.enabled?'success':'info'">{{ scope.row.enabled?'已启用':'已停用' }}</el-tag></template></el-table-column>
        <el-table-column label="生效时间" min-width="180"><template #default="scope">{{ formatTime(scope.row.effectiveAt) }}</template></el-table-column>
        <el-table-column label="停用时间" min-width="180"><template #default="scope">{{ formatTime(scope.row.disabledAt) }}</template></el-table-column>
        <el-table-column label="最近操作人" min-width="130"><template #default="scope">{{ operator(scope.row) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right"><template #default="scope"><el-button link type="primary" @click="openVersion(scope.row)">新增版本</el-button><el-button link :type="scope.row.enabled?'danger':'primary'" @click="changeStatus(scope.row)">{{ scope.row.enabled?'停用':'启用' }}</el-button></template></el-table-column>
        <template #empty><el-empty description="暂无费别系数，请先新增"/></template>
      </el-table>
      <div class="fee-note">费别编码保存后不可修改；欠费导入模板无需填写编码，仍按费别名称匹配。系数调整只影响后续导入。</div>
    </section>
  </div>
  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" class="mobile-full-dialog">
    <el-alert :title="versionMode?'编码和名称继承原费别；新增版本默认为停用状态。':'费别编码仅支持字母和数字，保存后不可修改。'" type="info" :closable="false"/>
    <el-form label-position="top" class="dialog-form">
      <el-form-item label="费别编码" required><el-input v-model="form.feeCode" maxlength="32" :disabled="versionMode" placeholder="例如：YB01" @input="form.feeCode=String($event).toUpperCase()"/></el-form-item>
      <el-form-item label="费别名称" required><el-input v-model="form.feeType" maxlength="64" :disabled="versionMode" placeholder="例如：城镇职工基本医疗保险"/></el-form-item>
      <el-form-item label="系数" required><el-input-number v-model="form.coefficient" :min="0" :precision="4" :step="0.1" style="width:100%"/></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="createRecord">保存</el-button></template>
  </el-dialog>
</template>
