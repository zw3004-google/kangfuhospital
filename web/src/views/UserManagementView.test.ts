import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import UserManagementView from './UserManagementView.vue'

const mocks=vi.hoisted(()=>({get:vi.fn(),post:vi.fn(),put:vi.fn(),confirm:vi.fn()}))
vi.mock('../api/http',()=>({http:{get:mocks.get,post:mocks.post,put:mocks.put}}))
vi.mock('element-plus',async(importOriginal)=>{const actual=await importOriginal<typeof import('element-plus')>();return{...actual,ElMessageBox:{...actual.ElMessageBox,confirm:mocks.confirm}}})

describe('UserManagementView H4',()=>{
  it('提供用户和科室移动卡片及高风险操作入口',async()=>{
    mocks.confirm.mockResolvedValue('confirm')
    mocks.get.mockImplementation((url:string)=>Promise.resolve({data:{data:url==='/system/departments'?{items:[{id:3,departmentCode:'KF',departmentName:'康复科',enabled:true}],total:1,page:1,pageSize:50}:url==='/system/roles'?[{id:2,roleCode:'DOCTOR',roleName:'主管医生',builtIn:true,enabled:true}]:{items:[{id:1,loginName:'zhangsan',displayName:'张三',employeeNo:'D001',wecomUserId:'wx001',departmentId:3,departmentName:'康复科',enabled:true,mustChangePassword:false,roles:[]}],total:1,page:1,pageSize:50}}}))
    mocks.post.mockResolvedValue({data:{data:null}})
    const wrapper=mount(UserManagementView,{global:{plugins:[ElementPlus],directives:{permission:()=>{}}}});await flushPromises()
    expect(wrapper.findAll('.admin-user-card')).toHaveLength(1)
    expect(wrapper.text()).toContain('张三');expect(wrapper.text()).toContain('新增科室')
    expect(wrapper.findAll('.desktop-only').length).toBe(2)
  })
})
