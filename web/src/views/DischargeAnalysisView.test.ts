import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DischargeAnalysisView from './DischargeAnalysisView.vue'

vi.setConfig({ testTimeout: 20000 })

const { getMock, postMock, setOptionMock, disposeMock, resizeMock, messageErrorMock, messageSuccessMock } = vi.hoisted(() => ({
  getMock: vi.fn(),
  postMock: vi.fn(),
  setOptionMock: vi.fn(),
  disposeMock: vi.fn(),
  resizeMock: vi.fn(),
  messageErrorMock: vi.fn(),
  messageSuccessMock: vi.fn(),
}))

vi.mock('../api/http', () => ({ http: { get: getMock, post: postMock } }))
vi.mock('echarts/core', () => ({
  use: vi.fn(),
  init: vi.fn(() => ({ setOption: setOptionMock, dispose: disposeMock, resize: resizeMock })),
}))
vi.mock('echarts/charts', () => ({ LineChart: {} }))
vi.mock('echarts/components', () => ({ GridComponent: {}, LegendComponent: {}, TooltipComponent: {} }))
vi.mock('echarts/renderers', () => ({ CanvasRenderer: {} }))
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, error: messageErrorMock, success: messageSuccessMock } }
})

const metrics = {
  month: '2026-09',
  nearDischarge3DayCount: 2,
  currentWeekDischargeCount: 5,
  dischargeCount: 4,
  unplannedCount: 2,
  nutritionCount: 1,
  homeRehabCount: 2,
  outpatientCount: 3,
  unplannedRate: 50,
  nutritionRate: 25,
  homeRehabRate: 50,
  outpatientRate: 75,
  trend: [
    { day: '2026-09-01', dischargeCount: 0, unplannedCount: 0, nutritionCount: 0, homeRehabCount: 0, outpatientCount: 0, unplannedRate: null, nutritionRate: null, homeRehabRate: null, outpatientRate: null },
    { day: '2026-09-02', dischargeCount: 4, unplannedCount: 2, nutritionCount: 1, homeRehabCount: 2, outpatientCount: 3, unplannedRate: 50, nutritionRate: 25, homeRehabRate: 50, outpatientRate: 75 },
  ],
}

describe('DischargeAnalysisView 阶段1', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
    setOptionMock.mockReset()
    disposeMock.mockReset()
    resizeMock.mockReset()
    messageErrorMock.mockReset()
    messageSuccessMock.mockReset()
  })

  it('展示四项核心比率及其分子分母', async () => {
    getMock.mockImplementation((url: string) => Promise.resolve({ data: { data: url === '/discharge/analysis' ? metrics : url.endsWith('filter-options') ? [] : { items: [], total: 0 } } }))
    const wrapper = mount(DischargeAnalysisView, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/discharge/analysis', expect.objectContaining({ params: expect.any(Object) }))
    expect(wrapper.text()).toContain('3天内预计出院2 人')
    expect(wrapper.text()).toContain('本周内预计出院5 人')
    expect(wrapper.text()).toContain('非计划出院率50%')
    expect(wrapper.text()).toContain('非计划 2 人 / 出院 4 人')
    expect(wrapper.text()).toContain('营养会诊预约率25%')
    expect(wrapper.text()).toContain('居家康复预约率50%')
    expect(wrapper.text()).toContain('复诊预约率75%')
    expect(wrapper.text()).toContain('预约 3 人 / 出院 4 人')
  })

  it('配置四条累计比例曲线并保留空比率点', async () => {
    getMock.mockImplementation((url: string) => Promise.resolve({ data: { data: url === '/discharge/analysis' ? metrics : url.endsWith('filter-options') ? [] : { items: [], total: 0 } } }))
    mount(DischargeAnalysisView, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    const option = setOptionMock.mock.calls[0][0]
    expect(option.yAxis).toMatchObject({ min: 0, max: 100 })
    expect(option.series.map((item: { name: string }) => item.name)).toEqual([
      '非计划出院率', '营养会诊预约率', '居家康复预约率', '复诊预约率',
    ])
    expect(option.series.every((item: { connectNulls: boolean }) => item.connectNulls === false)).toBe(true)
    expect(option.series[3].data).toEqual([null, 75])
  })

  it('快速切换月份时忽略较晚返回的旧请求', async () => {
    let resolveOld!: (value: unknown) => void
    const oldRequest = new Promise(resolve => { resolveOld = resolve })
    let analysisCalls = 0
    const octoberMetrics = { ...metrics, month: '2026-10', nearDischarge3DayCount: 9 }
    getMock.mockImplementation((url: string) => {
      if (url === '/discharge/analysis') return ++analysisCalls === 1 ? oldRequest : Promise.resolve({ data: { data: octoberMetrics } })
      if (url.endsWith('filter-options')) return Promise.resolve({ data: { data: [] } })
      return Promise.resolve({ data: { data: { items: [], total: 0 } } })
    })
    const wrapper = mount(DischargeAnalysisView, { global: { plugins: [ElementPlus] } })
    const picker = wrapper.findAllComponents({ name: 'ElDatePicker' })[0]
    picker.vm.$emit('update:modelValue', '2026-10')
    picker.vm.$emit('change', '2026-10')
    await flushPromises()
    expect(wrapper.text()).toContain('3天内预计出院9 人')

    resolveOld({ data: { data: metrics } })
    await flushPromises()
    expect(wrapper.text()).toContain('3天内预计出院9 人')
    expect(wrapper.text()).not.toContain('3天内预计出院2 人')
  })

  it('趋势为空时展示明确空状态且不初始化空图表', async () => {
    getMock.mockImplementation((url: string) => Promise.resolve({ data: { data: url === '/discharge/analysis' ? { ...metrics, trend: [] } : url.endsWith('filter-options') ? [] : { items: [], total: 0 } } }))
    const wrapper = mount(DischargeAnalysisView, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    expect(wrapper.text()).toContain('当前月份暂无趋势数据')
    expect(setOptionMock).not.toHaveBeenCalled()
  })

  it('零分母时显示横线', async () => {
    getMock.mockImplementation((url: string) => Promise.resolve({ data: { data: url === '/discharge/analysis' ? { ...metrics, dischargeCount: 0, unplannedCount: 0, nutritionCount: 0, homeRehabCount: 0, outpatientCount: 0, unplannedRate: null, nutritionRate: null, homeRehabRate: null, outpatientRate: null } : url.endsWith('filter-options') ? [] : { items: [], total: 0 } } }))
    const wrapper = mount(DischargeAnalysisView, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    expect(wrapper.findAll('.analysis-stat-grid strong').map(node => node.text())).toEqual(['2 人', '5 人', '—', '—', '—', '—'])
  })

  it('展示六类业务页签并按页签请求服务端分页数据', async () => {
    getMock.mockImplementation((url: string, config?: { params?: { category?: string } }) => {
      if (url === '/discharge/analysis') return Promise.resolve({ data: { data: metrics } })
      if (url.endsWith('filter-options')) return Promise.resolve({ data: { data: [] } })
      const item = { id: 1, patientName: '测试患者', gender: '男', inpatientNo: 'ZY001', admissionTimes: 1, departmentName: '康复科', doctorName: '李医生', latestNutritionAppointmentAt: '2026-09-05T08:00:00+08:00', abnormalCodes: [] }
      return Promise.resolve({ data: { data: { items: config?.params?.category === 'NUTRITION' ? [item] : [], total: config?.params?.category === 'NUTRITION' ? 1 : 0 } } })
    })
    const wrapper = mount(DischargeAnalysisView, { global: { plugins: [ElementPlus] } })
    await flushPromises()

    for (const label of ['预出院看板', '院后管理列表', '营养会诊', '居家康复', '复诊预约', '异常列表']) expect(wrapper.text()).toContain(label)
    expect(getMock).toHaveBeenCalledWith('/discharge/records', { params: expect.objectContaining({ category: 'BOARD', page: 1, pageSize: 50 }) })
    wrapper.findComponent({ name: 'ElTabs' }).vm.$emit('tab-change', 'NUTRITION')
    await flushPromises()
    expect(getMock).toHaveBeenCalledWith('/discharge/records', { params: expect.objectContaining({ category: 'NUTRITION', page: 1, pageSize: 50 }) })
    expect(wrapper.text()).toContain('最近营养会诊预约')
    expect(wrapper.text()).toContain('测试患者')
    expect(wrapper.findComponent({ name: 'ElPagination' }).props('pageSizes')).toEqual([20, 50, 100, 200])
  })

  it('将科室、时间、日期和关键词同时用于查询与导出', async () => {
    const createUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:test')
    const revokeUrl = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {})
    getMock.mockImplementation((url: string) => {
      if (url === '/discharge/analysis') return Promise.resolve({ data: { data: metrics } })
      if (url.endsWith('filter-options')) return Promise.resolve({ data: { data: [{ id: 7, departmentName: '康复一科' }] } })
      if (url.endsWith('/export')) return Promise.resolve({ data: new Blob(['test']) })
      return Promise.resolve({ data: { data: { items: [], total: 0 } } })
    })
    const wrapper = mount(DischargeAnalysisView, { global: { plugins: [ElementPlus], directives: { permission: () => {} } } })
    await flushPromises()
    const selects = wrapper.findAllComponents({ name: 'ElSelect' })
    await selects[0].setValue(7)
    await selects[1].setValue('PLANNED_DISCHARGE')
    const datePicker = wrapper.findAllComponents({ name: 'ElDatePicker' })[1]
    await datePicker.setValue(['2026-09-01', '2026-09-03'])
    await wrapper.find('input[placeholder="患者姓名 / 住院号 / 主管医生"]').setValue(' 张三 ')
    await wrapper.findAll('button').find(button => button.text() === '查询')!.trigger('click')
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/discharge/records', { params: expect.objectContaining({
      category: 'BOARD', departmentId: 7, timeType: 'PLANNED_DISCHARGE',
      startAt: '2026-09-01T00:00:00+08:00', keyword: '张三', page: 1, pageSize: 50,
    }) })
    await wrapper.findAll('button').find(button => button.text() === '导出 XLSX')!.trigger('click')
    await flushPromises()
    expect(getMock).toHaveBeenCalledWith('/discharge/records/export', expect.objectContaining({
      params: expect.objectContaining({ category: 'BOARD', departmentId: 7, timeType: 'PLANNED_DISCHARGE', keyword: '张三', format: 'xlsx' }),
      responseType: 'blob',
    }))
    expect(createUrl).toHaveBeenCalled()
    expect(revokeUrl).toHaveBeenCalledWith('blob:test')
  }, 20000)

  it('预览并生成正式提醒任务', async () => {
    getMock.mockImplementation((url: string) => {
      if (url === '/discharge/analysis') return Promise.resolve({ data: { data: metrics } })
      if (url.endsWith('filter-options')) return Promise.resolve({ data: { data: [] } })
      if (url.endsWith('/preview')) return Promise.resolve({ data: { data: { reminderDate: '2026-09-03', nutritionCount: 2, homeRehabCount: 1, followUpCount: 3, unplannedCount: 1, totalPatients: 7, items: [
        { type: 'NUTRITION', label: '营养会诊', patientCount: 2, recipientScope: '营养科岗位人员', triggerBasis: '预约日期为提醒当日', messagePreview: '患者（姓名脱敏），住院号：****，今日需要营养会诊。' },
      ] } } })
      return Promise.resolve({ data: { data: { items: [], total: 0 } } })
    })
    postMock.mockResolvedValue({ data: { data: { createdTasks: 4, message: '提醒任务已生成；重复任务已自动忽略' } } })
    const wrapper = mount(DischargeAnalysisView, { global: { plugins: [ElementPlus], directives: { permission: () => {} } } })
    await flushPromises()
    await wrapper.findAll('button').find(button => button.text() === '推送提醒')!.trigger('click')
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/discharge/reminders/preview')
    expect(wrapper.text()).toContain('营养科岗位人员')
    expect(wrapper.text()).toContain('预约日期为提醒当日')
    expect(wrapper.text()).toContain('涉及业务记录7 条')
    expect(wrapper.text()).toContain('营养会诊2 人')
    await wrapper.findAll('button').find(button => button.text() === '确认生成提醒')!.trigger('click')
    await flushPromises()
    expect(postMock).toHaveBeenCalledWith('/discharge/reminders/trigger')
    expect(messageSuccessMock).toHaveBeenCalledWith('提醒任务已生成；重复任务已自动忽略，新增 4 条任务')
  }, 20000)
})
