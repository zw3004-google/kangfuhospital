<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface DepartmentStat { departmentName: string; amount: number; people: number }
interface PatientStat { rank: number; inpatientNo: string; admissionTimes: number; patientName: string; departmentName: string; doctorName: string; arrearsType: string | null; arrearsAmount: number; recoveryProgress: string | null }
interface BatchSnapshot { batchNo: string; dataAsOf: string; summaryStatus: string }
interface ReportData { totalAmount: number; people: number; scopeType: string; scopeLabel: string; top3: DepartmentStat[]; ranking: DepartmentStat[]; patientTop10: PatientStat[]; latestSuccessfulBatch: BatchSnapshot | null }
interface NoticeDepartment { department: string; total: number; inpatient: number; dischargedSettled: number; dischargedUnsettled: number }
interface NoticePreview { batchNo: string; dataAsOf: string; scopeLabel: string; totalAmount: number; departments: NoticeDepartment[]; systemLink: string; content: string }

const data = ref<ReportData | null>(null)
const loading = ref(false)
const error = ref('')
const noticeDialog = ref(false)
const noticeLoading = ref(false)
const notice = ref<NoticePreview | null>(null)
const exporting = ref(false)

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    data.value = (await http.get<ApiResponse<ReportData>>('/arrears/report')).data.data
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '加载失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const showNotice = async () => {
  noticeDialog.value = true
  noticeLoading.value = true
  notice.value = null
  try {
    notice.value = (await http.get<ApiResponse<NoticePreview | null>>('/arrears/report/notice-preview')).data.data
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? reason.message : '通报内容加载失败')
  } finally {
    noticeLoading.value = false
  }
}

const exportReport = async () => {
  exporting.value = true
  try {
    const response = await http.get('/arrears/report/export', { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '通报报表.xlsx'
    link.click()
    setTimeout(() => URL.revokeObjectURL(url), 0)
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? reason.message : '通报报表导出失败')
  } finally {
    exporting.value = false
  }
}

const maxDepartmentAmount = computed(() => Math.max(0, ...(data.value?.ranking.map(item => Number(item.amount)) ?? [])))
const scopeHeading = computed(() => {
  if (data.value?.scopeType === 'ALL') return '全院科室'
  if (data.value?.scopeType === 'DEPARTMENT') return '本科室'
  if (data.value?.scopeType === 'DOCTOR') return '本人负责患者所属科室'
  return data.value?.scopeLabel || '授权范围'
})
const formatMoney = (value: number) => `${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} 元`
const formatTime = (value?: string) => value ? new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(new Date(value)) : '—'
const rankPercentage = (amount: number) => maxDepartmentAmount.value > 0 ? Math.round(Number(amount) / maxDepartmentAmount.value * 100) : 0
const rankWidth = (amount: number) => maxDepartmentAmount.value > 0 ? `${Math.max(2, Number(amount) / maxDepartmentAmount.value * 100)}%` : '0%'
const summaryStatusLabels: Record<string, string> = { READY: '更新完成', PENDING: '更新中', FAILED: '更新失败' }
const progressLabels: Record<string, string> = { NOT_STARTED: '未催缴', NEGOTIATING: '协商中', REFUSED: '拒绝缴费', LEGAL_ACTION: '移交法务', PAID: '已缴费' }
const arrearsTypeLabels: Record<string, string> = { INPATIENT: '在院患者', DISCHARGED_UNSETTLED: '出院未结算', DISCHARGED_SETTLED: '出院已结算' }
const summaryStatus = (status?: string) => summaryStatusLabels[status || ''] || status || '—'
const progressLabel = (value?: string | null) => progressLabels[value || ''] || value || '—'
const arrearsTypeLabel = (value?: string | null) => arrearsTypeLabels[value || ''] || value || '—'

onMounted(load)
</script>

<template>
  <section class="page-card arrears-report-page">
    <div class="page-heading">
      <div><h2>通报报表</h2><p>统计范围：是否欠费且未标记已缴费的在院及出院患者。</p></div>
      <div class="heading-actions">
        <el-button v-permission="'PERM_API_ARREARS_EXPORT'" :loading="exporting" :disabled="!data?.latestSuccessfulBatch" @click="exportReport">导出通报</el-button>
        <el-button :disabled="!data?.latestSuccessfulBatch" @click="showNotice">通报内容展示</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <el-skeleton v-if="loading && !data" class="report-loading" :rows="8" animated />
    <el-result v-else-if="error && !data" icon="error" title="报表加载失败" :sub-title="error">
      <template #extra><el-button type="primary" @click="load">重新加载</el-button></template>
    </el-result>
    <el-empty v-else-if="!data?.latestSuccessfulBatch" description="暂无汇总完成的成功批次" />

    <template v-else>
      <div class="report-batch-bar">
        <div><span>数据范围</span><strong>{{ data.scopeLabel }}</strong></div>
        <div><span>数据截至时间</span><strong>{{ formatTime(data.latestSuccessfulBatch.dataAsOf) }}</strong></div>
        <div><span>最新成功批次</span><strong>{{ data.latestSuccessfulBatch.batchNo }}</strong></div>
        <el-tag type="success" effect="light">{{ summaryStatus(data.latestSuccessfulBatch.summaryStatus) }}</el-tag>
      </div>

      <div class="stat-grid report-stat-grid">
        <div><span>欠费总额</span><strong>{{ formatMoney(data.totalAmount) }}</strong></div>
        <div><span>欠费人数</span><strong>{{ data.people.toLocaleString('zh-CN') }} 人</strong></div>
      </div>

      <el-empty v-if="!data.ranking.length" description="当前数据范围内暂无未缴费欠费患者" />
      <template v-else>
        <h3 class="section-title">{{ scopeHeading }}欠费 Top3</h3>
        <div :class="['report-top3', `count-${Math.min(data.top3.length, 3)}`]">
          <article v-for="(item, index) in data.top3" :key="item.departmentName" :class="`report-top-card top-${index + 1}`">
            <span class="top-rank">TOP{{ index + 1 }}</span><h4>{{ item.departmentName }}</h4>
            <strong>{{ formatMoney(item.amount) }}</strong><p>欠费患者 {{ item.people }} 人</p>
          </article>
        </div>

        <h3 class="section-title">{{ scopeHeading }}欠费排行</h3>
        <div class="department-ranking">
          <div v-for="(item, index) in data.ranking" :key="item.departmentName" class="department-rank-row">
            <span class="department-rank-index">{{ index + 1 }}</span><span class="department-rank-name" :title="item.departmentName">{{ item.departmentName }}</span>
            <div class="department-rank-track"><i role="progressbar" aria-valuemin="0" aria-valuemax="100" :aria-valuenow="rankPercentage(item.amount)" :aria-label="`${item.departmentName}欠费金额占最高科室比例`" :style="{ width: rankWidth(item.amount) }" /></div>
            <strong>{{ formatMoney(item.amount) }}</strong><span>{{ item.people }} 人</span>
          </div>
        </div>

        <h3 class="section-title">患者欠费金额 Top10</h3>
        <el-table :data="data.patientTop10" stripe class="report-patient-table" empty-text="暂无患者数据">
          <el-table-column prop="rank" label="排名" width="70"><template #default="scope"><strong :class="{ 'top-patient-rank': scope.row.rank <= 3 }">{{ scope.row.rank }}</strong></template></el-table-column>
          <el-table-column prop="inpatientNo" label="住院号" min-width="120" />
          <el-table-column prop="admissionTimes" label="住院次数" width="90" align="right" />
          <el-table-column prop="patientName" label="姓名" width="100" />
          <el-table-column prop="departmentName" label="住院病区" min-width="170" show-overflow-tooltip />
          <el-table-column prop="doctorName" label="主管医生" width="110" />
          <el-table-column prop="arrearsType" label="欠费类型" min-width="120"><template #default="scope">{{ arrearsTypeLabel(scope.row.arrearsType) }}</template></el-table-column>
          <el-table-column label="欠费金额" min-width="140" align="right"><template #default="scope"><strong class="report-money">{{ formatMoney(scope.row.arrearsAmount) }}</strong></template></el-table-column>
          <el-table-column label="追缴进度" min-width="110"><template #default="scope">{{ progressLabel(scope.row.recoveryProgress) }}</template></el-table-column>
        </el-table>
      </template>
    </template>

    <el-dialog v-model="noticeDialog" title="通报内容展示" width="720px" destroy-on-close>
      <div v-loading="noticeLoading" class="notice-preview-wrap">
        <el-empty v-if="!noticeLoading && !notice" description="暂无可展示的通报内容" />
        <template v-else-if="notice">
          <div class="notice-preview-meta">
            <div><span>数据范围</span><strong>{{ notice.scopeLabel }}</strong></div>
            <div><span>数据截至时间</span><strong>{{ formatTime(notice.dataAsOf) }}</strong></div>
            <div><span>批次号</span><strong>{{ notice.batchNo }}</strong></div>
          </div>
          <pre class="notice-preview-content">{{ notice.content }}</pre>
        </template>
      </div>
      <template #footer><el-button @click="noticeDialog=false">关闭</el-button></template>
    </el-dialog>
  </section>
</template>
