import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import ArrearsReportView from './ArrearsReportView.vue'

const { getMock } = vi.hoisted(() => ({ getMock: vi.fn() }))
vi.mock('../api/http', () => ({ http: { get: getMock } }))
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, error: vi.fn() } }
})

describe('ArrearsReportView 阶段5边界展示', () => {
  it('Top3不足时不补空卡，并正确展示长科室名、大金额和中文欠费类型', async () => {
    const longDepartment = '神经系统重症及高压氧综合康复医学中心超长病区名称'
    getMock.mockResolvedValue({ data: { data: {
      totalAmount: 123456789012.34, people: 1, scopeType: 'DEPARTMENT', scopeLabel: '本科室',
      latestSuccessfulBatch: { batchNo: 'ARR-LONG', dataAsOf: '2026-09-01T08:00:00+08:00', summaryStatus: 'READY' },
      top3: [{ departmentName: longDepartment, amount: 123456789012.34, people: 1 }],
      ranking: [{ departmentName: longDepartment, amount: 123456789012.34, people: 1 }],
      patientTop10: [{ rank: 1, inpatientNo: 'ZY-LONG', admissionTimes: 1, patientName: '患者', departmentName: longDepartment,
        doctorName: '医生', arrearsType: 'DISCHARGED_SETTLED', arrearsAmount: 123456789012.34, recoveryProgress: 'LEGAL_ACTION' }],
    } } })
    const wrapper = mount(ArrearsReportView, { global: { plugins: [ElementPlus], directives: { permission: () => {} } } })
    await flushPromises()

    expect(wrapper.findAll('.report-top-card')).toHaveLength(1)
    expect(wrapper.find('.report-top3').classes()).toContain('count-1')
    expect(wrapper.text()).toContain(longDepartment)
    expect(wrapper.text()).toContain('123,456,789,012.34 元')
    expect(wrapper.text()).toContain('出院已结算')
    expect(wrapper.text()).toContain('移交法务')
    expect(wrapper.find('[role="progressbar"]').attributes('aria-valuenow')).toBe('100')
  })
})
