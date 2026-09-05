import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import ArrearsDetailsView from './ArrearsDetailsView.vue'

const { getMock } = vi.hoisted(() => ({ getMock: vi.fn() }))
vi.mock('../api/http', () => ({ http: { get: getMock, post: vi.fn(), put: vi.fn() }, ApiRequestError: class extends Error {} }))
vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))

describe('ArrearsDetailsView 主管医生工号', () => {
  it('在搜索提示和欠费明细列表中展示主管医生工号', async () => {
    getMock.mockImplementation((url: string) => {
      if (url === '/arrears/records/filter-options') return Promise.resolve({ data: { data: { departments: [], feeTypes: [], arrearsTypes: [] } } })
      if (url === '/arrears/records/summary') return Promise.resolve({ data: { data: { totalPeople: 1, inpatientPeople: 1, dischargedUnsettledPeople: 0, dischargedSettledPeople: 0, totalAmount: 600, uncollectedPeople: 1, legalPeople: 0, sourceUpdatedAt: null } } })
      return Promise.resolve({ data: { data: { items: [{ id: 1, inpatientNo: 'ZY001', admissionTimes: 1, patientName: '张三', departmentName: '康复科', wardName: '康复一病区', feeType: '自费', arrearsType: 'INPATIENT', doctorName: '李医生', doctorEmployeeNo: 'D001', admittedAt: null, dischargedAt: null, totalCost: 1000, prepaidAmount: 200, medicalInsurancePaid: 0, personalAccountPaid: 0, finalRequiredDeposit: 800, arrearsAmount: 600, inArrears: true, paymentStatus: 'UNPAID', arrearsReason: null, recoveryProgress: 'NOT_STARTED', previousRecoveryProgress: 'NOT_STARTED', lastOperatedBy: null, sourceUpdatedAt: null }], total: 1, page: 1, pageSize: 50 } } })
    })

    const wrapper = mount(ArrearsDetailsView, { global: { plugins: [ElementPlus], directives: { permission: () => {} } } })
    await flushPromises()

    expect(wrapper.find('input[placeholder="住院号 / 姓名 / 主管医生 / 医生工号"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('主管医生工号')
    expect(wrapper.text()).toContain('D001')
    expect(wrapper.find('.mobile-filter-toolbar').exists()).toBe(true)
    expect(wrapper.findAll('.mobile-record-card')).toHaveLength(1)
    expect(wrapper.find('.arrears-mobile-card').text()).toContain('600.00 元')
    expect(wrapper.find('.arrears-mobile-card').text()).toContain('康复一病区')

  })
})
