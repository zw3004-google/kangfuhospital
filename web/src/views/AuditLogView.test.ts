import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import AuditLogView from './AuditLogView.vue'

const {get}=vi.hoisted(()=>({get:vi.fn()}))
vi.mock('../api/http',()=>({http:{get}}))

describe('AuditLogView H4',()=>{
  it('以移动卡片展示审计主体并保留变更详情',async()=>{
    get.mockResolvedValue({data:{data:[{id:1,moduleCode:'SYSTEM',businessType:'ROLE',businessId:'2',actionType:'ASSIGN_PERMISSIONS',operatorName:'admin',beforeData:'[]',afterData:'[1]',clientIp:'127.0.0.1',operatedAt:'2026-09-05T08:00:00+08:00'}]}})
    const wrapper=mount(AuditLogView,{global:{plugins:[ElementPlus]}});await flushPromises()
    expect(wrapper.findAll('.audit-mobile-card')).toHaveLength(1)
    expect(wrapper.find('.audit-mobile-card').text()).toContain('ASSIGN_PERMISSIONS')
    expect(wrapper.find('.desktop-only').exists()).toBe(true)
  })
})
