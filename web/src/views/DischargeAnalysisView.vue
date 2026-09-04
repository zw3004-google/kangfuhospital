<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { use, init, type ECharts } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { http, type ApiResponse } from '../api/http'

use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

interface TrendPoint {
  day: string
  dischargeCount: number
  unplannedCount: number
  nutritionCount: number
  homeRehabCount: number
  outpatientCount: number
  unplannedRate: number | null
  nutritionRate: number | null
  homeRehabRate: number | null
  outpatientRate: number | null
}

interface Metrics {
  month: string
  nearDischarge3DayCount: number
  currentWeekDischargeCount: number
  dischargeCount: number
  unplannedCount: number
  nutritionCount: number
  homeRehabCount: number
  outpatientCount: number
  unplannedRate: number | null
  nutritionRate: number | null
  homeRehabRate: number | null
  outpatientRate: number | null
  trend: TrendPoint[]
}

type DetailCategory = 'BOARD' | 'FOLLOW_UP' | 'NUTRITION' | 'HOME_REHAB' | 'OUTPATIENT' | 'ABNORMAL'
interface DetailRow {
  id: number
  inpatientNo: string
  admissionTimes: number
  patientName: string
  gender: string | null
  departmentName: string | null
  primaryDiagnosis: string | null
  doctorName: string | null
  admittedAt: string | null
  plannedDischargeAt: string | null
  actualDischargeAt: string | null
  outpatientAppointmentAt: string | null
  outpatientArrived: boolean | null
  outpatientArrivalAt: string | null
  outpatientNoShowReason: string | null
  latestNutritionAppointmentAt: string | null
  latestHomeRehabAppointmentAt: string | null
  latestFollowUpAt: string | null
  followUpRequired: boolean | null
  followUpDay7: string | null
  followUpDay30: string | null
  followUpDay60: string | null
  abnormalCodes: string[]
  abnormalReason: string | null
}

const metrics = ref<Metrics>()
const chartEl = ref<HTMLElement>()
const loading = ref(false)
const month = ref(new Date().toISOString().slice(0, 7))
const activeCategory = ref<DetailCategory>('BOARD')
const detailRows = ref<DetailRow[]>([])
const detailTotal = ref(0)
const detailPage = ref(1)
const detailPageSize = ref(50)
const detailLoading = ref(false)
const departments = ref<Array<{ id: number; departmentName: string }>>([])
const departmentId = ref<number>()
const timeType = ref('')
const dateRange = ref<string[]>([])
const keyword = ref('')
const exporting = ref(false)
const loadError = ref('')
const reminderDialog = ref(false)
const reminderLoading = ref(false)
const reminderSending = ref(false)
type ReminderPreviewItem = { type: string; label: string; patientCount: number; recipientScope: string; triggerBasis: string; messagePreview: string }
const reminderPreview = ref<{ reminderDate: string; nutritionCount: number; homeRehabCount: number; followUpCount: number; unplannedCount: number; totalPatients: number; items: ReminderPreviewItem[] }>()
let chart: ECharts | undefined
let metricsRequestSequence = 0
let detailRequestSequence = 0

const timeTypeOptions: Record<DetailCategory, Array<[string, string]>> = {
  BOARD: [['ADMITTED', '入院时间'], ['PLANNED_DISCHARGE', '预计出院时间']],
  FOLLOW_UP: [['ACTUAL_DISCHARGE', '实际出院时间'], ['FOLLOW_UP', '随访时间']],
  NUTRITION: [['NUTRITION', '营养会诊预约时间']],
  HOME_REHAB: [['HOME_REHAB', '居家康复预约时间']],
  OUTPATIENT: [['OUTPATIENT', '复诊预约时间']],
  ABNORMAL: [['ACTUAL_DISCHARGE', '实际出院时间'], ['PLANNED_DISCHARGE', '预计出院时间']],
}

const endExclusive = (date: string) => {
  const value = new Date(`${date}T00:00:00+08:00`)
  value.setUTCDate(value.getUTCDate() + 1)
  return value.toISOString()
}
const detailParams = () => ({
  category: activeCategory.value,
  departmentId: departmentId.value,
  timeType: timeType.value || undefined,
  startAt: dateRange.value[0] ? `${dateRange.value[0]}T00:00:00+08:00` : undefined,
  endAt: dateRange.value[1] ? endExclusive(dateRange.value[1]) : undefined,
  keyword: keyword.value.trim() || undefined,
})

const renderChart = async () => {
  await nextTick()
  if (!chartEl.value || !metrics.value) return
  chart?.dispose()
  chart = init(chartEl.value)
  const trend = metrics.value.trend
  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (items: Array<{ dataIndex: number; marker: string; seriesName: string; value: number | null }>) => {
        const point = trend[items[0]?.dataIndex ?? 0]
        if (!point) return ''
        return [
          `${point.day}（累计出院 ${point.dischargeCount} 人）`,
          ...items.map(item => `${item.marker}${item.seriesName}：${item.value == null ? '—' : `${item.value}%`}`),
        ].join('<br/>')
      },
    },
    legend: { data: ['非计划出院率', '营养会诊预约率', '居家康复预约率', '复诊预约率'] },
    grid: { left: 55, right: 28, bottom: 35, top: 58 },
    xAxis: { type: 'category', data: trend.map(item => item.day.slice(5)) },
    yAxis: { type: 'value', name: '比例', min: 0, max: 100, axisLabel: { formatter: '{value}%' } },
    series: [
      { name: '非计划出院率', type: 'line', smooth: true, connectNulls: false, data: trend.map(item => item.unplannedRate) },
      { name: '营养会诊预约率', type: 'line', smooth: true, connectNulls: false, data: trend.map(item => item.nutritionRate) },
      { name: '居家康复预约率', type: 'line', smooth: true, connectNulls: false, data: trend.map(item => item.homeRehabRate) },
      { name: '复诊预约率', type: 'line', smooth: true, connectNulls: false, data: trend.map(item => item.outpatientRate) },
    ],
  })
}

const load = async () => {
  const requestSequence = ++metricsRequestSequence
  loading.value = true
  loadError.value = ''
  try {
    const response = await http.get<ApiResponse<Metrics>>('/discharge/analysis', { params: { month: month.value } })
    if (requestSequence !== metricsRequestSequence) return
    metrics.value = response.data.data
    await renderChart()
  } catch (error) {
    if (requestSequence !== metricsRequestSequence) return
    loadError.value = error instanceof Error ? error.message : '加载失败'
    ElMessage.error(loadError.value)
  } finally {
    if (requestSequence === metricsRequestSequence) loading.value = false
  }
}

const loadDetails = async () => {
  const requestSequence = ++detailRequestSequence
  detailLoading.value = true
  try {
    const response = await http.get<ApiResponse<{ items: DetailRow[]; total: number }>>('/discharge/records', {
      params: { ...detailParams(), page: detailPage.value, pageSize: detailPageSize.value },
    })
    if (requestSequence !== detailRequestSequence) return
    detailRows.value = response.data.data.items
    detailTotal.value = response.data.data.total
  } catch (error) {
    if (requestSequence !== detailRequestSequence) return
    ElMessage.error(error instanceof Error ? error.message : '明细加载失败')
  } finally {
    if (requestSequence === detailRequestSequence) detailLoading.value = false
  }
}

const changeCategory = (category: string | number) => {
  activeCategory.value = String(category) as DetailCategory
  if (!timeTypeOptions[activeCategory.value].some(([value]) => value === timeType.value)) timeType.value = ''
  detailPage.value = 1
  loadDetails()
}
const queryDetails = () => { detailPage.value = 1; loadDetails() }
const resetDetails = () => { departmentId.value = undefined; timeType.value = ''; dateRange.value = []; keyword.value = ''; queryDetails() }
const loadDepartments = async () => {
  try { departments.value = (await http.get<ApiResponse<Array<{ id: number; departmentName: string }>>>('/discharge/records/filter-options')).data.data }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '科室筛选项加载失败') }
}
const exportDetails = async () => {
  exporting.value = true
  try {
    const response = await http.get('/discharge/records/export', { params: { ...detailParams(), format: 'xlsx' }, responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `统计分析_${activeCategory.value}_${new Date().toISOString().slice(0, 10)}.xlsx`
    anchor.click()
    URL.revokeObjectURL(url)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '导出失败') }
  finally { exporting.value = false }
}
const openReminder = async () => {
  reminderDialog.value = true
  reminderLoading.value = true
  reminderPreview.value = undefined
  try { reminderPreview.value = (await http.get<ApiResponse<typeof reminderPreview.value>>('/discharge/reminders/preview')).data.data }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '提醒预览加载失败') }
  finally { reminderLoading.value = false }
}
const triggerReminder = async () => {
  reminderSending.value = true
  try {
    const response = await http.post<ApiResponse<{ createdTasks: number; message: string }>>('/discharge/reminders/trigger')
    ElMessage.success(`${response.data.data.message}，新增 ${response.data.data.createdTasks} 条任务`)
    reminderDialog.value = false
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '提醒任务生成失败') }
  finally { reminderSending.value = false }
}
const formatTime = (value: string | null) => value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
const abnormalLabels: Record<string, string> = { LATE_PLAN: '出院前12小时内填报', MISSING_PLAN: '未填报预计出院时间', DATE_MISMATCH: '预计与实际出院日期不一致' }
const abnormalText = (row: DetailRow) => [...(row.abnormalCodes || []).map(code => abnormalLabels[code] || code), ...(row.abnormalReason ? [row.abnormalReason] : [])].join('；') || '—'

const resize = () => chart?.resize()
onMounted(() => { window.addEventListener('resize', resize); load(); loadDepartments(); loadDetails() })
onBeforeUnmount(() => { metricsRequestSequence++; detailRequestSequence++; window.removeEventListener('resize', resize); chart?.dispose() })
</script>

<template>
  <section class="page-card">
    <div class="page-heading">
      <div>
        <h2>统计分析</h2>
        <p>指标按实际出院月份归属；趋势为月初至各日期的累计值，并受当前用户数据范围控制。</p>
      </div>
      <div class="heading-actions">
        <el-button v-permission="'PERM_API_DISCHARGE_REMINDER'" type="primary" plain @click="openReminder">推送提醒</el-button>
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" placeholder="选择月份" :clearable="false" @change="load" />
      </div>
    </div>
    <el-skeleton v-if="loading && !metrics" :rows="4" animated />
    <el-result v-else-if="loadError && !metrics" icon="error" title="统计数据加载失败" :sub-title="loadError"><template #extra><el-button type="primary" @click="load">重新加载</el-button></template></el-result>
    <template v-else-if="metrics">
      <el-alert type="info" :closable="false" show-icon title="以下指标、趋势、明细和导出均受当前账号数据范围控制" class="analysis-scope-alert" />
      <div class="stat-grid analysis-stat-grid">
        <div><span>3天内预计出院</span><strong>{{ metrics.nearDischarge3DayCount }} 人</strong><small>尚未出院且预计时间在未来72小时内</small></div>
        <div><span>本周内预计出院</span><strong>{{ metrics.currentWeekDischargeCount }} 人</strong><small>按当前自然周统计尚未出院患者</small></div>
        <div><span>非计划出院率</span><strong>{{ metrics.unplannedRate === null ? '—' : metrics.unplannedRate + '%' }}</strong><small>非计划 {{ metrics.unplannedCount }} 人 / 出院 {{ metrics.dischargeCount }} 人</small></div>
        <div><span>营养会诊预约率</span><strong>{{ metrics.nutritionRate === null ? '—' : metrics.nutritionRate + '%' }}</strong><small>预约 {{ metrics.nutritionCount }} 人 / 出院 {{ metrics.dischargeCount }} 人</small></div>
        <div><span>居家康复预约率</span><strong>{{ metrics.homeRehabRate === null ? '—' : metrics.homeRehabRate + '%' }}</strong><small>预约 {{ metrics.homeRehabCount }} 人 / 出院 {{ metrics.dischargeCount }} 人</small></div>
        <div><span>复诊预约率</span><strong>{{ metrics.outpatientRate === null ? '—' : metrics.outpatientRate + '%' }}</strong><small>预约 {{ metrics.outpatientCount }} 人 / 出院 {{ metrics.dischargeCount }} 人</small></div>
      </div>
      <h3 class="section-title">核心指标趋势（当月累计）</h3>
      <div v-if="metrics.trend.length" ref="chartEl" v-loading="loading" class="trend-chart"></div>
      <el-empty v-else description="当前月份暂无趋势数据" class="trend-empty" />
      <h3 class="section-title">业务明细</h3>
      <el-tabs v-model="activeCategory" class="analysis-tabs" @tab-change="changeCategory">
        <el-tab-pane label="预出院看板" name="BOARD" />
        <el-tab-pane label="院后管理列表" name="FOLLOW_UP" />
        <el-tab-pane label="营养会诊" name="NUTRITION" />
        <el-tab-pane label="居家康复" name="HOME_REHAB" />
        <el-tab-pane label="复诊预约" name="OUTPATIENT" />
        <el-tab-pane label="异常列表" name="ABNORMAL" />
      </el-tabs>
      <div class="filter-bar analysis-filter-bar">
        <el-select v-model="departmentId" clearable placeholder="全部科室">
          <el-option v-for="item in departments" :key="item.id" :label="item.departmentName" :value="item.id" />
        </el-select>
        <el-select v-model="timeType" clearable placeholder="时间类型（全部）">
          <el-option v-for="item in timeTypeOptions[activeCategory]" :key="item[0]" :label="item[1]" :value="item[0]" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" />
        <el-input v-model="keyword" clearable placeholder="患者姓名 / 住院号 / 主管医生" @keyup.enter="queryDetails" />
        <el-button type="primary" @click="queryDetails">查询</el-button>
        <el-button @click="resetDetails">重置</el-button>
        <el-button v-permission="'PERM_API_DISCHARGE_EXPORT'" :loading="exporting" @click="exportDetails">导出 XLSX</el-button>
      </div>
      <el-empty v-if="!detailLoading && !detailRows.length" description="当前条件下暂无业务明细" />
      <el-table v-else v-loading="detailLoading" :data="detailRows" stripe class="analysis-detail-table">
        <el-table-column prop="patientName" label="患者姓名" width="110" fixed="left" />
        <el-table-column prop="gender" label="患者性别" width="90" />
        <el-table-column prop="inpatientNo" label="住院号" width="130" fixed="left" />
        <el-table-column prop="admissionTimes" label="住院次数" width="90" align="center" />
        <el-table-column prop="departmentName" label="所属科室" min-width="140" show-overflow-tooltip />
        <el-table-column prop="doctorName" label="主管医生" width="110" />
        <template v-if="activeCategory === 'BOARD'">
          <el-table-column label="入院时间" width="175"><template #default="scope">{{ formatTime(scope.row.admittedAt) }}</template></el-table-column>
          <el-table-column label="主诊断" min-width="180" show-overflow-tooltip><template #default="scope">{{ scope.row.primaryDiagnosis || '—' }}</template></el-table-column>
          <el-table-column label="预计出院时间" width="175"><template #default="scope">{{ formatTime(scope.row.plannedDischargeAt) }}</template></el-table-column>
        </template>
        <template v-else-if="activeCategory === 'FOLLOW_UP'">
          <el-table-column label="实际出院时间" width="175"><template #default="scope">{{ formatTime(scope.row.actualDischargeAt) }}</template></el-table-column>
          <el-table-column label="第7天随访" min-width="160"><template #default="scope">{{ scope.row.followUpDay7 || '未回访' }}</template></el-table-column>
          <el-table-column label="第30天随访" min-width="160"><template #default="scope">{{ scope.row.followUpRequired === false ? '无需随访' : scope.row.followUpDay30 || '未回访' }}</template></el-table-column>
          <el-table-column label="第60天随访" min-width="160"><template #default="scope">{{ scope.row.followUpRequired === false ? '无需随访' : scope.row.followUpDay60 || '未回访' }}</template></el-table-column>
        </template>
        <template v-else-if="activeCategory === 'NUTRITION'">
          <el-table-column label="最近营养会诊预约" min-width="190"><template #default="scope">{{ formatTime(scope.row.latestNutritionAppointmentAt) }}</template></el-table-column>
        </template>
        <template v-else-if="activeCategory === 'HOME_REHAB'">
          <el-table-column label="最近居家康复预约" min-width="190"><template #default="scope">{{ formatTime(scope.row.latestHomeRehabAppointmentAt) }}</template></el-table-column>
        </template>
        <template v-else-if="activeCategory === 'OUTPATIENT'">
          <el-table-column label="复诊预约时间" width="175"><template #default="scope">{{ formatTime(scope.row.outpatientAppointmentAt) }}</template></el-table-column>
          <el-table-column label="到诊状态" width="100"><template #default="scope">{{ scope.row.outpatientArrived === true ? '已到诊' : scope.row.outpatientArrived === false ? '未到诊' : '未跟踪' }}</template></el-table-column>
          <el-table-column label="实际到诊时间" width="175"><template #default="scope">{{ formatTime(scope.row.outpatientArrivalAt) }}</template></el-table-column>
          <el-table-column label="未到诊原因" min-width="180"><template #default="scope">{{ scope.row.outpatientNoShowReason || '—' }}</template></el-table-column>
        </template>
        <template v-else>
          <el-table-column label="预计出院时间" width="175"><template #default="scope">{{ formatTime(scope.row.plannedDischargeAt) }}</template></el-table-column>
          <el-table-column label="实际出院时间" width="175"><template #default="scope">{{ formatTime(scope.row.actualDischargeAt) }}</template></el-table-column>
          <el-table-column label="异常原因" min-width="260"><template #default="scope"><span class="danger-text">{{ abnormalText(scope.row) }}</span></template></el-table-column>
        </template>
      </el-table>
      <el-pagination v-model:current-page="detailPage" v-model:page-size="detailPageSize" :page-sizes="[20, 50, 100, 200]" :total="detailTotal" layout="total,sizes,prev,pager,next" class="pagination" @change="loadDetails" />
    </template>
    <el-dialog v-model="reminderDialog" title="推送提醒" width="min(960px, 92vw)">
      <div v-loading="reminderLoading" class="reminder-preview">
        <template v-if="reminderPreview">
          <el-alert type="warning" :closable="false" show-icon title="系统将按正式提醒规则生成今日任务；已存在的重复任务不会再次创建。" />
          <div class="reminder-summary"><span>提醒日期</span><strong>{{ reminderPreview.reminderDate }}</strong><span>涉及业务记录</span><strong>{{ reminderPreview.totalPatients }} 条</strong></div>
          <el-table :data="reminderPreview.items" size="small" border>
            <el-table-column prop="label" label="提醒类型" width="100" />
            <el-table-column prop="patientCount" label="人数" width="70"><template #default="scope">{{ scope.row.patientCount }} 人</template></el-table-column>
            <el-table-column prop="recipientScope" label="接收范围" min-width="130" />
            <el-table-column prop="triggerBasis" label="触发依据" min-width="190" />
            <el-table-column prop="messagePreview" label="消息预览" min-width="260" show-overflow-tooltip />
          </el-table>
          <p class="muted">接收人由患者业务类型、主管医生和对应岗位角色自动确定；发送结果可在“预出院推送记录”中查看。</p>
        </template>
      </div>
      <template #footer><el-button @click="reminderDialog=false">取消</el-button><el-button type="primary" :loading="reminderSending" :disabled="reminderLoading || !reminderPreview" @click="triggerReminder">确认生成提醒</el-button></template>
    </el-dialog>
  </section>
</template>
