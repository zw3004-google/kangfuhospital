import {flushPromises,mount} from '@vue/test-utils'
import ElementPlus from 'element-plus'
import {describe,expect,it,vi} from 'vitest'
import HisSyncConfigView from './HisSyncConfigView.vue'

const {getMock,putMock}=vi.hoisted(()=>({getMock:vi.fn(),putMock:vi.fn()}))
vi.mock('../api/http',()=>({http:{get:getMock,put:putMock}}))

const configs=[
  {syncType:'INPATIENT_ARREARS',enabled:true,dailyTime:'07:30:00',lastAttemptAt:'2026-09-09T07:30:00+08:00',lastSuccessAt:'2026-09-09T07:30:10+08:00',lastBatchNo:'HIS-001',lastError:null,running:false,updatedAt:'2026-09-09T07:30:10+08:00'},
  {syncType:'DISCHARGED_ARREARS',enabled:false,dailyTime:'08:00:00',lastAttemptAt:null,lastSuccessAt:null,lastBatchNo:null,lastError:'网关暂不可用',running:false,updatedAt:'2026-09-09T07:30:10+08:00'},
  {syncType:'PATIENT_INFO',enabled:true,dailyTime:'06:30:00',lastAttemptAt:null,lastSuccessAt:null,lastBatchNo:null,lastError:null,running:true,updatedAt:'2026-09-09T07:30:10+08:00'},
]

describe('HisSyncConfigView H5',()=>{
  it('使用移动卡片展示三类配置并保存当前配置',async()=>{
    getMock.mockResolvedValue({data:{data:configs}})
    putMock.mockImplementation((_url:string,body:unknown)=>Promise.resolve({data:{data:{...configs[1],...(body as object)}}}))
    const wrapper=mount(HisSyncConfigView,{global:{plugins:[ElementPlus]}})
    await flushPromises()
    expect(wrapper.findAll('.his-sync-mobile-card')).toHaveLength(3)
    expect(wrapper.find('.his-sync-mobile-list').text()).toContain('在院患者欠费')
    expect(wrapper.find('.his-sync-mobile-list').text()).toContain('网关暂不可用')
    expect(wrapper.find('.his-sync-mobile-list').text()).toContain('运行中')
    await wrapper.findAll('.his-sync-mobile-card footer button')[1].trigger('click')
    await flushPromises()
    expect(putMock).toHaveBeenCalledWith('/integration/his-sync/configs/DISCHARGED_ARREARS',{enabled:false,dailyTime:'08:00:00'})
  })
})
