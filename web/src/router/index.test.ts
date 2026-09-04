import { describe, expect, it } from 'vitest'
import { legacyDischargeRedirect } from './index'

describe('预出院旧入口兼容重定向', () => {
  it('会诊预约旧地址重定向到预计出院管理并保留患者参数', () => {
    expect(legacyDischargeRedirect('consultation', { encounterId: '21' })).toEqual({
      path: '/discharge/management',
      query: { encounterId: '21', section: 'consultation' },
    })
  })

  it('出院随访旧地址重定向到预计出院管理并保留患者参数', () => {
    expect(legacyDischargeRedirect('follow-up', { recordId: '35' })).toEqual({
      path: '/discharge/management',
      query: { recordId: '35', section: 'follow-up' },
    })
  })
})
