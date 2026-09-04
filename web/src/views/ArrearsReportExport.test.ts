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
  totalAmount: 100, people: 1, scopeType: 'ALL', scopeLabel: '全院',
  latestSuccessfulBatch: { batchNo: 'ARR-1', dataAsOf: '2026-09-01T08:00:00+08:00', summaryStatus: 'READY' },
  top3: [{ departmentName: '康复科', amount: 100, people: 1 }], ranking: [{ departmentName: '康复科', amount: 100, people: 1 }], patientTop10: [],
}

describe('ArrearsReportView 阶段4导出', () => {
  beforeEach(() => {
    getMock.mockReset()
    messageErrorMock.mockReset()
  })

  it('按当前报表权限调用Excel导出并释放临时地址', async () => {
    const blob = new Blob(['xlsx'])
    getMock.mockResolvedValueOnce({ data: { data: report } }).mockResolvedValueOnce({ data: blob })
    const createUrl = vi.fn(() => 'blob:report')
    const revokeUrl = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createUrl })
    Object.defineProperty(URL, 'revokeObjectURL', { configurable: true, value: revokeUrl })
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const wrapper = mount(ArrearsReportView, { global: { plugins: [ElementPlus], directives: { permission: () => {} } } })
    await flushPromises()

    await wrapper.findAll('button').find(item => item.text().includes('导出通报'))!.trigger('click')
    await flushPromises()
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(getMock).toHaveBeenNthCalledWith(2, '/arrears/report/export', { responseType: 'blob' })
    expect(createUrl).toHaveBeenCalledWith(blob)
    expect(click).toHaveBeenCalledOnce()
    expect(revokeUrl).toHaveBeenCalledWith('blob:report')
    click.mockRestore()
  })

  it('导出失败时显示错误且不创建下载地址', async () => {
    getMock.mockResolvedValueOnce({ data: { data: report } }).mockRejectedValueOnce(new Error('导出服务不可用'))
    const createUrl = vi.fn()
    Object.defineProperty(URL, 'createObjectURL', { configurable: true, value: createUrl })
    const wrapper = mount(ArrearsReportView, { global: { plugins: [ElementPlus], directives: { permission: () => {} } } })
    await flushPromises()
    await wrapper.findAll('button').find(item => item.text().includes('导出通报'))!.trigger('click')
    await flushPromises()

    expect(messageErrorMock).toHaveBeenCalledWith('导出服务不可用')
    expect(createUrl).not.toHaveBeenCalled()
  })
})
