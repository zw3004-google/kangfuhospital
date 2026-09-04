<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { hasPermission } from '../auth'
import { http, type ApiResponse } from '../api/http'

interface PushRecord {
  id: number
  businessType: string
  reminderType: string
  recipientName: string | null
  contentSummary: string
  status: string
  displayStatus: string
  triggerType: string
  scheduledAt: string
  sentAt: string | null
  pushTime: string
  retryCount: number
  lastError: string | null
}
interface PushRecordResponse extends Partial<PushRecord> {
  id: number
  businessType: string
  reminderType: string
  recipientName: string | null
  content?: string
  status: string
}
interface Attempt {
  attemptNo: number
  triggerType: string
  scheduledAt: string
  attemptedAt: string | null
  recipientName: string | null
  recipientWecomId: string | null
  status: string
  errorCode: string | null
  errorMessage: string | null
}
interface Page<T> { items: T[]; total: number; page: number; pageSize: number }
interface RetryBatchResult { requestedCount: number; successCount: number; skippedCount: number; failedCount: number }

const route = useRoute()
const rows = ref<PushRecord[]>([])
const selectedRows = ref<PushRecord[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(50)
const status = ref('')
const dateRange = ref<[string, string] | []>([])
const loading = ref(false)
const retryingId = ref<number | null>(null)
const batchRetrying = ref(false)
const dialog = ref(false)
const attemptsLoading = ref(false)
const attempts = ref<Attempt[]>([])
const current = ref<PushRecord | null>(null)
const businessType = computed(() => String(route.meta.businessType || 'ARREARS'))
const businessLabel = computed(() => businessType.value === 'DISCHARGE' ? '预出院' : '欠费')
const canRetry = computed(() => hasPermission('PERM_API_PUSH_RETRY'))

const statusOptions = [
  { label: '发送中', value: 'SENDING' },
  { label: '重试中', value: 'RETRYING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAILED' },
]

const fmt = (value: string | null) => value
  ? new Date(value).toLocaleString('zh-CN', { hour12: false })
  : '—'
const triggerLabel = (value: string | null) => value === 'MANUAL' ? '人工重发' : '系统自动'
const attemptStatusLabel = (value: string) => ({
  PENDING: '发送中', SENDING: '发送中', RETRYING: '重试中', SENT: '成功',
  FAILED: '失败', CANCELLED: '已取消',
}[value] || value)
const statusTagType = (value: string) => ({
  PENDING: 'primary', SENDING: 'primary', RETRYING: 'warning', SENT: 'success',
  FAILED: 'danger', CANCELLED: 'info',
}[value] as 'primary' | 'warning' | 'success' | 'danger' | 'info' | undefined)

const normalizeRecord = (item: PushRecordResponse): PushRecord => ({
  id: item.id,
  businessType: item.businessType,
  reminderType: item.reminderType,
  recipientName: item.recipientName,
  contentSummary: item.contentSummary || item.content || '—',
  status: item.status,
  displayStatus: item.displayStatus || attemptStatusLabel(item.status),
  triggerType: item.triggerType || 'AUTOMATIC',
  scheduledAt: item.scheduledAt || '',
  sentAt: item.sentAt || null,
  pushTime: item.pushTime || item.sentAt || item.scheduledAt || '',
  retryCount: item.retryCount ?? 0,
  lastError: item.lastError || null,
})

const load = async () => {
  loading.value = true
  selectedRows.value = []
  try {
    const response = await http.get<ApiResponse<Page<PushRecordResponse>>>('/arrears/push-records', {
      params: {
        page: page.value,
        pageSize: pageSize.value,
        status: status.value || undefined,
        startDate: dateRange.value[0] || undefined,
        endDate: dateRange.value[1] || undefined,
        businessType: businessType.value,
      },
    })
    rows.value = response.data.data.items.map(normalizeRecord)
    total.value = response.data.data.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '推送记录加载失败')
  } finally {
    loading.value = false
  }
}

const query = () => { page.value = 1; void load() }
const reset = () => {
  status.value = ''
  dateRange.value = []
  page.value = 1
  void load()
}
const selectable = (row: PushRecord) => canRetry.value && row.status === 'FAILED'
const onSelectionChange = (selection: PushRecord[]) => { selectedRows.value = selection }

const showAttempts = async (row: PushRecord) => {
  current.value = row
  dialog.value = true
  attempts.value = []
  attemptsLoading.value = true
  try {
    attempts.value = (await http.get<ApiResponse<Attempt[]>>(`/arrears/push-records/${row.id}/attempts`)).data.data
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送尝试加载失败')
  } finally {
    attemptsLoading.value = false
  }
}

const retry = async (row: PushRecord) => {
  try {
    await ElMessageBox.confirm(
      `确认重新发送给“${row.recipientName || '未命名接收人'}”吗？失败原因：${row.lastError || '无'}`,
      '确认单条重发',
      { type: 'warning', confirmButtonText: '确认重发', cancelButtonText: '取消' },
    )
  } catch { return }
  retryingId.value = row.id
  try {
    await http.post(`/arrears/push-records/${row.id}/retry`)
    ElMessage.success('已加入重发队列')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重发失败')
  } finally {
    retryingId.value = null
  }
}

const retryBatch = async () => {
  if (!selectedRows.value.length) return ElMessage.warning('请先勾选需要重发的失败记录')
  try {
    await ElMessageBox.confirm(
      `确认批量重发已勾选的 ${selectedRows.value.length} 条失败记录吗？`,
      '确认批量重发',
      { type: 'warning', confirmButtonText: '确认重发', cancelButtonText: '取消' },
    )
  } catch { return }
  batchRetrying.value = true
  try {
    const response = await http.post<ApiResponse<RetryBatchResult>>('/arrears/push-records/retry-batch', {
      businessType: businessType.value,
      ids: selectedRows.value.map(item => item.id),
    })
    const result = response.data.data
    ElMessage.success(`批量重发完成：成功 ${result.successCount} 条，跳过 ${result.skippedCount} 条，失败 ${result.failedCount} 条`)
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '批量重发失败')
  } finally {
    batchRetrying.value = false
  }
}

watch(businessType, () => { page.value = 1; void load() }, { immediate: true })
</script>

<template>
  <section class="push-record-page">
    <div class="page-card push-filter-card">
      <div class="page-heading">
        <div>
          <h2>推送记录</h2>
          <p>查询{{ businessLabel }}消息的推送留痕，查看接收人、实际内容、发送结果及完整尝试历史。</p>
        </div>
      </div>
      <div class="filter-bar push-filter-bar">
        <el-select v-model="status" clearable placeholder="全部状态" class="push-status-filter">
          <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="push-date-filter"
        />
        <el-button type="primary" :loading="loading" @click="query">查询</el-button>
        <el-button :disabled="loading" @click="reset">重置</el-button>
      </div>
    </div>

    <div class="page-card push-list-card">
      <div class="push-list-heading">
        <div>
          <h3>推送留痕</h3>
          <span>共 {{ total }} 条记录</span>
        </div>
        <el-button
          v-if="canRetry"
          type="danger"
          plain
          :disabled="selectedRows.length === 0"
          :loading="batchRetrying"
          @click="retryBatch"
        >批量重发<span v-if="selectedRows.length">（{{ selectedRows.length }}）</span></el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        class="push-record-table"
        empty-text="暂无符合条件的推送记录"
        @selection-change="onSelectionChange"
      >
        <el-table-column v-if="canRetry" type="selection" width="48" fixed="left" :selectable="selectable" />
        <el-table-column label="推送时间" width="180">
          <template #default="scope">{{ fmt(scope.row.pushTime) }}</template>
        </el-table-column>
        <el-table-column prop="recipientName" label="接收人" width="150">
          <template #default="scope">{{ scope.row.recipientName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="contentSummary" label="推送内容" min-width="280" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" effect="light">{{ scope.row.displayStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="触发方式" width="110">
          <template #default="scope">{{ triggerLabel(scope.row.triggerType) }}</template>
        </el-table-column>
        <el-table-column prop="retryCount" label="重试次数" width="90" align="center" />
        <el-table-column prop="lastError" label="最近错误" min-width="180" show-overflow-tooltip>
          <template #default="scope"><span :class="{ 'danger-text': scope.row.lastError }">{{ scope.row.lastError || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="showAttempts(scope.row)">尝试详情</el-button>
            <el-button
              v-if="canRetry && scope.row.status === 'FAILED'"
              link
              type="danger"
              :loading="retryingId === scope.row.id"
              @click="retry(scope.row)"
            >重发</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
      <div class="push-trace-note">所有推送均保留接收人、推送时间、实际内容、发送结果及每次自动重试或人工重发记录。批量重发仅处理当前页明确勾选的失败任务。</div>
    </div>

    <el-dialog v-model="dialog" :title="`发送尝试 · 任务 #${current?.id || ''}`" width="78%">
      <div v-loading="attemptsLoading" class="push-attempt-wrap">
        <el-empty v-if="!attemptsLoading && !attempts.length" description="尚无发送尝试" />
        <el-table v-else-if="attempts.length" :data="attempts">
          <el-table-column prop="attemptNo" label="#" width="55" />
          <el-table-column label="触发方式" width="110">
            <template #default="scope">{{ triggerLabel(scope.row.triggerType) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope"><el-tag :type="statusTagType(scope.row.status)">{{ attemptStatusLabel(scope.row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="recipientName" label="接收人" width="120" />
          <el-table-column prop="recipientWecomId" label="企微 ID" width="140" />
          <el-table-column label="计划时间" width="180"><template #default="scope">{{ fmt(scope.row.scheduledAt) }}</template></el-table-column>
          <el-table-column label="尝试时间" width="180"><template #default="scope">{{ fmt(scope.row.attemptedAt) }}</template></el-table-column>
          <el-table-column prop="errorCode" label="错误码" width="120" />
          <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>
    </el-dialog>
  </section>
</template>
