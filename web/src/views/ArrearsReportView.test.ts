import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ArrearsReportView from './ArrearsReportView.vue'

const { getMock, messageErrorMock } = vi.hoisted(() => ({ getMock: vi.fn(), messageErrorMock: vi.fn() }))

vi.mock('../api/http', () => ({ http: { get: getMock } }))
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, error: messageErrorMock } }
})

const report = {
  totalAmount: 325000.5,
  people: 5,
  scopeType: 'ALL',
  scopeLabel: '全院',
  latestSuccessfulBatch: { batchNo: 'ARR-20260901080000', dataAsOf: '2026-09-01T08:00:00+08:00', summaryStatus: 'READY' },
  top3: [
    { departmentName: '神经康复一科', amount: 200000, people: 2 },
    { departmentName: '重症康复科', amount: 100000, people: 2 },
    { departmentName: '骨关节康复科', amount: 25000.5, people: 1 },
  ],
  ranking: [
    { departmentName: '神经康复一科', amount: 200000, people: 2 },
    { departmentName: '重症康复科', amount: 100000, people: 2 },
    { departmentName: '骨关节康复科', amount: 25000.5, people: 1 },
  ],
  patientTop10: [{
    rank: 1, inpatientNo: 'ZY-1001', admissionTimes: 2, patientName: '测试患者', departmentName: '神经康复一科',
    doctorName: '测试医生', arrearsType: '在院患者', arrearsAmount: 150000, recoveryProgress: 'NEGOTIATING',
  }],
}

const render = () => mount(ArrearsReportView, { global: { plugins: [ElementPlus] } })

describe('ArrearsReportView 阶段2', () => {
  beforeEach(() => {
    getMock.mockReset()
    messageErrorMock.mockReset()
  })

  it('展示批次、范围、Top3、科室排行及服务端Top10', async () => {
    getMock.mockResolvedValue({ data: { data: report } })
    const wrapper = render()
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/arrears/report')
    expect(wrapper.text()).toContain('ARR-20260901080000')
    expect(wrapper.text()).toContain('更新完成')
    expect(wrapper.text()).toContain('325,000.50 元')
    expect(wrapper.text()).toContain('全院科室欠费 Top3')
    expect(wrapper.findAll('.report-top-card')).toHaveLength(3)
    expect(wrapper.findAll('.department-rank-row')).toHaveLength(3)
    expect(wrapper.text()).toContain('患者欠费金额 Top10')
    expect(wrapper.text()).toContain('ZY-1001')
    expect(wrapper.text()).toContain('协商中')
    expect(wrapper.find('.department-rank-track i').attributes('style')).toContain('width: 100%')
  })

  it('没有汇总完成批次时展示批次空状态', async () => {
    getMock.mockResolvedValue({ data: { data: { ...report, latestSuccessfulBatch: null, top3: [], ranking: [], patientTop10: [] } } })
    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('暂无汇总完成的成功批次')
    expect(wrapper.find('.report-batch-bar').exists()).toBe(false)
  })

  it('请求失败时展示错误状态并支持重新加载', async () => {
    getMock.mockRejectedValueOnce(new Error('报表服务暂不可用')).mockResolvedValueOnce({ data: { data: report } })
    const wrapper = render()
    await flushPromises()

    expect(wrapper.text()).toContain('报表加载失败')
    expect(wrapper.text()).toContain('报表服务暂不可用')
    expect(messageErrorMock).toHaveBeenCalledWith('报表服务暂不可用')

    const retry = wrapper.findAll('button').find(button => button.text().includes('重新加载'))
    expect(retry).toBeTruthy()
    await retry!.trigger('click')
    await flushPromises()
    expect(getMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('ARR-20260901080000')
  })
})
