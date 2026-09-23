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
    mocks.get.mockImplementation((url:string)=>Promise.resolve({data:{data:url==='/system/departments'?{items:[{id:3,departmentCode:'KF',departmentName:'康复科',enabled:true}],total:1,page:1,pageSize:50}:url==='/system/roles'?[{id:2,roleCode:'DOCTOR',roleName:'主管医生',builtIn:true,enabled:true}]:{items:[{id:1,loginName:'zhangsan',displayName:'张三',employeeNo:'D001',wecomUserId:'wx001',departmentId:3,departmentName:'康复科',enabled:true,mustChangePassword:false,roles:[],departmentAccessNames:['康复科','骨科','神经科','内科']}],total:1,page:1,pageSize:50}}}))
    mocks.post.mockResolvedValue({data:{data:null}})
    const wrapper=mount(UserManagementView,{global:{plugins:[ElementPlus],directives:{permission:()=>{}}}});await flushPromises()
    expect(wrapper.findAll('.admin-user-card')).toHaveLength(1)
    expect(wrapper.text()).toContain('张三');expect(wrapper.text()).toContain('科室权限');expect(wrapper.text()).toContain('康复科、骨科、神经科');expect(wrapper.text()).toContain('...');expect(wrapper.text()).toContain('新增科室')
    expect(wrapper.findAll('.desktop-only').length).toBe(2)
  })

  it('导出时仅携带勾选的三名用户',async()=>{
    mocks.confirm.mockResolvedValue('confirm')
    mocks.get.mockImplementation((url:string)=>Promise.resolve({data:{data:url==='/system/departments'?{items:[{id:3,departmentCode:'KF',departmentName:'康复科',enabled:true}],total:1,page:1,pageSize:50}:url==='/system/roles'?[{id:2,roleCode:'DOCTOR',roleName:'主管医生',builtIn:true,enabled:true}]:url==='/system/users/export'?new Blob(['xlsx']):{items:[{id:1,loginName:'zhangsan',displayName:'张三',employeeNo:'D001',wecomUserId:'wx001',departmentId:3,departmentName:'康复科',enabled:true,mustChangePassword:false,roles:[],departmentAccessNames:[]}],total:1,page:1,pageSize:50}}}))
    vi.stubGlobal('URL',{createObjectURL:vi.fn(()=> 'blob:test'),revokeObjectURL:vi.fn()})
    const click=vi.spyOn(HTMLAnchorElement.prototype,'click').mockImplementation(()=>{})
    const wrapper=mount(UserManagementView,{global:{plugins:[ElementPlus],directives:{permission:()=>{}}}});await flushPromises()
    wrapper.findComponent({name:'ElTable'}).vm.$emit('selection-change',[{id:1},{id:2},{id:3}]);await flushPromises()
    await wrapper.findAll('button').find(item=>item.text()==='用户导出')!.trigger('click');await flushPromises()
    expect(mocks.get).toHaveBeenLastCalledWith('/system/users/export?ids=1&ids=2&ids=3',{params:undefined,responseType:'blob'})
    click.mockRestore();wrapper.unmount()
  })})
