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
  totalAmount: 200000, people: 2, scopeType: 'ALL', scopeLabel: '全院',
  latestSuccessfulBatch: { batchNo: 'ARR-20260901080000', dataAsOf: '2026-09-01T08:00:00+08:00', summaryStatus: 'READY' },
  top3: [{ departmentName: '神经康复一科', amount: 200000, people: 2 }],
  ranking: [{ departmentName: '神经康复一科', amount: 200000, people: 2 }],
  patientTop10: [],
}
const preview = {
  batchNo: 'ARR-20260901080000', dataAsOf: '2026-09-01T08:00:00+08:00', scopeLabel: '全院', totalAmount: 200000,
  departments: [{ department: '神经康复一科', total: 200000, inpatient: 100000, dischargedSettled: 30000, dischargedUnsettled: 70000 }],
  systemLink: 'http://oa.kfyy.local/arrears',
  content: '截至 2026-09-01 08:00，全院患者欠费合计 20.00万 元\n1. 神经康复一科：20.00万 元\nhttp://oa.kfyy.local/arrears',
}
const render = () => mount(ArrearsReportView, { attachTo: document.body, global: { plugins: [ElementPlus] } })

describe('ArrearsReportView 阶段3通报内容展示', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    getMock.mockReset()
    messageErrorMock.mockReset()
  })

  it('按需加载并只读展示与当前范围一致的通报内容', async () => {
    getMock.mockImplementation((url: string) => Promise.resolve({ data: { data: url.endsWith('notice-preview') ? preview : report } }))
    const wrapper = render()
    await flushPromises()

    const button = wrapper.findAll('button').find(item => item.text().includes('通报内容展示'))
    expect(button).toBeTruthy()
    expect(button!.attributes('disabled')).toBeUndefined()
    await button!.trigger('click')
    await flushPromises()

    expect(getMock).toHaveBeenNthCalledWith(2, '/arrears/report/notice-preview')
    expect(document.body.textContent).toContain('截至 2026-09-01 08:00，全院患者欠费合计 20.00万 元')
    expect(document.body.textContent).toContain('http://oa.kfyy.local/arrears')
    expect(document.body.textContent).not.toContain('确认推送')
    expect(document.body.querySelector('.notice-preview-content')).toBeTruthy()
    expect(document.body.querySelector('select')).toBeNull()
    wrapper.unmount()
  })

  it('预览接口失败时保留只读弹窗并展示空状态', async () => {
    getMock.mockResolvedValueOnce({ data: { data: report } }).mockRejectedValueOnce(new Error('通报预览加载失败'))
    const wrapper = render()
    await flushPromises()
    await wrapper.findAll('button').find(item => item.text().includes('通报内容展示'))!.trigger('click')
    await flushPromises()

    expect(messageErrorMock).toHaveBeenCalledWith('通报预览加载失败')
    expect(document.body.textContent).toContain('暂无可展示的通报内容')
    expect(document.body.textContent).not.toContain('确认推送')
    wrapper.unmount()
  })
})
