import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ArrearsPushRecordsView from './ArrearsPushRecordsView.vue'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  hasPermission: vi.fn(),
  confirm: vi.fn(),
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  routeMeta: { businessType: 'ARREARS' },
}))

vi.mock('../api/http', () => ({ http: { get: mocks.get, post: mocks.post } }))
vi.mock('../auth', () => ({ hasPermission: mocks.hasPermission }))
vi.mock('vue-router', () => ({ useRoute: () => ({ meta: mocks.routeMeta }) }))
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessage: { ...actual.ElMessage, success: mocks.success, error: mocks.error, warning: mocks.warning },
    ElMessageBox: { ...actual.ElMessageBox, confirm: mocks.confirm },
  }
})

const failed = {
  id: 101,
  businessType: 'ARREARS',
  reminderType: 'ARREARS_NOTICE',
  recipientName: '张主任',
  contentSummary: '神经康复科欠费通报内容',
  status: 'FAILED',
  displayStatus: '失败',
  triggerType: 'AUTOMATIC',
  scheduledAt: '2026-09-02T08:00:00+08:00',
  sentAt: null,
  pushTime: '2026-09-02T08:00:00+08:00',
  retryCount: 4,
  lastError: '企微接口超时',
}
const success = {
  ...failed,
  id: 102,
  recipientName: '李医生',
  status: 'SENT',
  displayStatus: '成功',
  sentAt: '2026-09-02T08:01:00+08:00',
  pushTime: '2026-09-02T08:01:00+08:00',
  retryCount: 1,
  lastError: null,
}
const page = { items: [failed, success], total: 2, page: 1, pageSize: 50 }
const render = () => mount(ArrearsPushRecordsView, { attachTo: document.body, global: { plugins: [ElementPlus] } })

describe('ArrearsPushRecordsView 阶段5', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    mocks.routeMeta.businessType = 'ARREARS'
    mocks.get.mockReset().mockResolvedValue({ data: { data: page } })
    mocks.post.mockReset()
    mocks.hasPermission.mockReset().mockReturnValue(true)
    mocks.confirm.mockReset().mockResolvedValue('confirm')
    mocks.success.mockReset()
    mocks.error.mockReset()
    mocks.warning.mockReset()
  })

  it('展示推送留痕字段且按四种业务状态和日期查询', async () => {
    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('推送留痕')
    expect(wrapper.text()).toContain('推送时间')
    expect(wrapper.text()).toContain('推送内容')
    expect(wrapper.text()).toContain('人工重发')
    expect(wrapper.text()).not.toContain('渠道')
    expect(wrapper.text()).toContain('神经康复科欠费通报内容')
    expect(wrapper.text()).toContain('失败')
    expect(wrapper.text()).toContain('成功')

    const select = wrapper.findComponent({ name: 'ElSelect' })
    select.vm.$emit('update:modelValue', 'FAILED')
    const datePicker = wrapper.findComponent({ name: 'ElDatePicker' })
    datePicker.vm.$emit('update:modelValue', ['2026-09-01', '2026-09-02'])
    await nextTick()
    await wrapper.findAll('button').find(item => item.text() === '查询')!.trigger('click')
    await flushPromises()

    expect(mocks.get).toHaveBeenLastCalledWith('/arrears/push-records', {
      params: {
        page: 1,
        pageSize: 50,
        status: 'FAILED',
        startDate: '2026-09-01',
        endDate: '2026-09-02',
        businessType: 'ARREARS',
      },
    })
    wrapper.unmount()
  })

  it('兼容旧版接口并展示实际推送内容和中文状态', async () => {
    mocks.get.mockResolvedValue({
      data: {
        data: {
          items: [{
            id: 20,
            businessType: 'ARREARS',
            reminderType: 'ARREARS_NOTICE',
            recipientName: '王冰芯',
            content: '【开发环境展示测试】请关注主管患者欠费情况。',
            status: 'SENT',
            triggerType: 'MANUAL',
            scheduledAt: '2026-09-02T11:25:00+08:00',
            sentAt: '2026-09-02T11:26:11+08:00',
            retryCount: 2,
            lastError: null,
          }],
          total: 1,
          page: 1,
          pageSize: 50,
        },
      },
    })

    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('【开发环境展示测试】请关注主管患者欠费情况。')
    expect(wrapper.text()).toContain('成功')
    expect(wrapper.text()).toContain('人工重发')
    wrapper.unmount()
  })

  it('只批量提交当前页明确勾选的失败任务并展示结构化结果', async () => {
    mocks.post.mockResolvedValue({
      data: { data: { requestedCount: 1, successCount: 1, skippedCount: 0, failedCount: 0 } },
    })
    const wrapper = render()
    await flushPromises()

    wrapper.findComponent({ name: 'ElTable' }).vm.$emit('selection-change', [failed])
    await nextTick()
    const batchButton = wrapper.findAll('button').find(item => item.text().includes('批量重发'))!
    expect(batchButton.text()).toContain('1')
    await batchButton.trigger('click')
    await flushPromises()

    expect(mocks.confirm).toHaveBeenCalledWith(
      expect.stringContaining('1 条失败记录'),
      '确认批量重发',
      expect.objectContaining({ type: 'warning' }),
    )
    expect(mocks.post).toHaveBeenCalledWith('/arrears/push-records/retry-batch', {
      businessType: 'ARREARS',
      ids: [101],
    })
    expect(mocks.success).toHaveBeenCalledWith('批量重发完成：成功 1 条，跳过 0 条，失败 0 条')
    wrapper.unmount()
  })

  it('没有重发权限时保持只读且不展示选择和重发操作', async () => {
    mocks.hasPermission.mockReturnValue(false)
    const wrapper = render()
    await flushPromises()

    expect(wrapper.find('.el-table-column--selection').exists()).toBe(false)
    expect(wrapper.findAll('button').some(item => item.text().includes('批量重发'))).toBe(false)
    expect(wrapper.findAll('button').some(item => item.text() === '重发')).toBe(false)
    expect(wrapper.findAll('button').some(item => item.text() === '尝试详情')).toBe(true)
    wrapper.unmount()
  })

  it('只允许选择失败任务并在重置后清空状态和日期条件', async () => {
    const wrapper = render()
    await flushPromises()

    const selectionColumn = wrapper.findAllComponents({ name: 'ElTableColumn' })
      .find(item => item.props('type') === 'selection')!
    const selectable = selectionColumn.props('selectable') as (row: { status: string }) => boolean
    expect(selectable(failed)).toBe(true)
    expect(selectable(success)).toBe(false)

    wrapper.findComponent({ name: 'ElSelect' }).vm.$emit('update:modelValue', 'RETRYING')
    wrapper.findComponent({ name: 'ElDatePicker' }).vm.$emit('update:modelValue', ['2026-08-01', '2026-08-31'])
    await nextTick()
    await wrapper.findAll('button').find(item => item.text() === '重置')!.trigger('click')
    await flushPromises()

    expect(mocks.get).toHaveBeenLastCalledWith('/arrears/push-records', {
      params: {
        page: 1, pageSize: 50, status: undefined, startDate: undefined, endDate: undefined,
        businessType: 'ARREARS',
      },
    })
    wrapper.unmount()
  })

  it('取消单条重发确认时不发送请求', async () => {
    mocks.confirm.mockRejectedValue('cancel')
    const wrapper = render()
    await flushPromises()

    await wrapper.findAll('button').find(item => item.text() === '重发')!.trigger('click')
    await flushPromises()

    expect(mocks.confirm).toHaveBeenCalledWith(
      expect.stringContaining('张主任'),
      '确认单条重发',
      expect.objectContaining({ type: 'warning' }),
    )
    expect(mocks.post).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('尝试详情中文展示自动与人工触发方式及失败信息', async () => {
    mocks.get.mockImplementation((url: string) => Promise.resolve({
      data: { data: url.endsWith('/attempts') ? [{
        attemptNo: 1, triggerType: 'MANUAL', scheduledAt: '2026-09-02T08:00:00+08:00',
        attemptedAt: '2026-09-02T08:01:00+08:00', recipientName: '张主任', recipientWecomId: 'zhang',
        status: 'FAILED', errorCode: 'TIMEOUT', errorMessage: '企微接口超时',
      }] : page },
    }))
    const wrapper = render()
    await flushPromises()
    await wrapper.findAll('button').find(item => item.text() === '尝试详情')!.trigger('click')
    await flushPromises()

    expect(mocks.get).toHaveBeenLastCalledWith('/arrears/push-records/101/attempts')
    expect(document.body.textContent).toContain('人工重发')
    expect(document.body.textContent).toContain('失败')
    expect(document.body.textContent).toContain('TIMEOUT')
    expect(document.body.textContent).toContain('企微接口超时')
    wrapper.unmount()
  })

  it('预出院入口复用页面并携带预出院业务类型查询', async () => {
    mocks.routeMeta.businessType = 'DISCHARGE'
    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('查询预出院消息的推送留痕')
    expect(mocks.get).toHaveBeenLastCalledWith('/arrears/push-records', {
      params: expect.objectContaining({ businessType: 'DISCHARGE' }),
    })
    wrapper.unmount()
  })
})
