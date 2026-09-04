<script setup lang="ts">
import {reactive,ref,watch} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {http,type ApiResponse} from '../../api/http'

type ConsultationType='NUTRITION'|'HOME'
interface Consultation{id:number;encounterId:number;appointmentAt:string;executorName:string|null;executionResult:string|null}
const props=defineProps<{encounterId:number;type:ConsultationType;disabled?:boolean}>()
const emit=defineEmits<{changed:[]}>()
const rows=ref<Consultation[]>([]),loading=ref(false),saving=ref(false),dialog=ref(false),editing=ref<Consultation>()
const form=reactive({appointmentAt:'',executorName:'',executionResult:''})
const label=()=>props.type==='NUTRITION'?'营养会诊':'居家康复'
const formatTime=(value:string)=>new Date(value).toLocaleString('zh-CN',{hour12:false})
const load=async()=>{loading.value=true;try{rows.value=(await http.get<ApiResponse<Consultation[]>>('/discharge/consultations',{params:{encounterId:props.encounterId,type:props.type}})).data.data}catch(e){ElMessage.error(e instanceof Error?e.message:`${label()}记录加载失败`)}finally{loading.value=false}}
const open=(row?:Consultation)=>{editing.value=row;form.appointmentAt=row?.appointmentAt||'';form.executorName=row?.executorName||'';form.executionResult=row?.executionResult||'';dialog.value=true}
const save=async()=>{if(!form.appointmentAt){ElMessage.warning('请填写预约时间');return}saving.value=true;try{const body={...form,executorName:form.executorName.trim()||null,executionResult:form.executionResult.trim()||null};if(editing.value)await http.put(`/discharge/consultations/${editing.value.id}`,body,{params:{type:props.type}});else await http.post('/discharge/consultations',body,{params:{encounterId:props.encounterId,type:props.type}});await load();emit('changed');ElMessage.success('预约记录已保存');dialog.value=false}catch(e){ElMessage.error(e instanceof Error?e.message:'预约记录保存失败')}finally{saving.value=false}}
const remove=async(row:Consultation)=>{try{await ElMessageBox.confirm('确认删除该预约记录？','删除确认',{type:'warning'});await http.delete(`/discharge/consultations/${row.id}`,{params:{type:props.type}});await load();emit('changed');ElMessage.success('预约记录已删除')}catch(e){if(e instanceof Error)ElMessage.error(e.message)}}
watch(()=>[props.encounterId,props.type],load,{immediate:true})
</script>

<template><section class="discharge-responsibility-section consultation-section"><header><strong>{{type==='NUTRITION'?'营养科填报':'居家康复科填报'}}</strong><span>{{type==='NUTRITION'?'营养会诊':'居家康复'}}信息，可维护多条记录</span><el-button type="primary" plain size="small" :disabled="disabled||rows.length>=10" @click="open()">新增{{type==='NUTRITION'?'营养会诊':'居家康复'}}</el-button></header><el-table v-loading="loading" :data="rows" size="small" :empty-text="`暂无${type==='NUTRITION'?'营养会诊':'居家康复'}记录`"><el-table-column label="预约时间" min-width="180"><template #default="s">{{formatTime(s.row.appointmentAt)}}</template></el-table-column><el-table-column label="执行人" prop="executorName"/><el-table-column label="执行结果" prop="executionResult" min-width="180"/><el-table-column label="操作" width="120"><template #default="s"><el-button link type="primary" :disabled="disabled" @click="open(s.row)">修改</el-button><el-button link type="danger" :disabled="disabled" @click="remove(s.row)">删除</el-button></template></el-table-column></el-table><el-dialog v-model="dialog" :title="`${editing?'修改':'新增'}${type==='NUTRITION'?'营养会诊':'居家康复'}`" width="560px" append-to-body><el-form label-position="top"><el-form-item label="预约时间" required><el-date-picker v-model="form.appointmentAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" style="width:100%"/></el-form-item><el-form-item label="执行人"><el-input v-model="form.executorName" maxlength="128"/></el-form-item><el-form-item label="执行结果"><el-input v-model="form.executionResult" type="textarea" maxlength="1000" show-word-limit/></el-form-item></el-form><template #footer><el-button @click="dialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template></el-dialog></section></template>
