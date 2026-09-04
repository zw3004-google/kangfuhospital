import {flushPromises,mount} from '@vue/test-utils'
import ElementPlus from 'element-plus'
import {afterEach,describe,expect,it,vi} from 'vitest'
import DischargeManagementView from './DischargeManagementView.vue'

const {getMock,putMock}=vi.hoisted(()=>({getMock:vi.fn(),putMock:vi.fn()}))
vi.mock('../api/http',()=>({http:{get:getMock,post:vi.fn(),put:putMock,delete:vi.fn()}}))
vi.mock('../auth',()=>({hasPermission:vi.fn(()=>true)}))

const row={id:1,encounterId:101,inpatientNo:'ZY001',admissionTimes:2,patientName:'张三',gender:'男',departmentName:'康复一科',primaryDiagnosis:'脑卒中恢复期',doctorName:'李医生',admittedAt:'2026-09-01T08:00:00+08:00',plannedDischargeAt:'2026-09-05T08:00:00+08:00',actualDischargeAt:'2026-09-04T08:00:00+08:00',latestOutpatientAppointmentAt:'2026-09-10T08:00:00+08:00',latestNutritionAppointmentAt:'2026-09-06T08:00:00+08:00',latestHomeRehabAppointmentAt:'2026-09-07T08:00:00+08:00',latestFollowUpAt:'2026-09-11T08:00:00+08:00',status:'已出院',abnormalCodes:['DATE_MISMATCH','LATE_PLAN'],abnormalReason:'患者临时要求出院',specialPatient:false,followUpRequired:true}

describe('DischargeManagementView',()=>{
  afterEach(()=>vi.useRealTimers())
  it('加载同源统计、科室筛选和完整预计出院列表',async()=>{
    vi.useFakeTimers();vi.setSystemTime(new Date('2026-09-03T08:00:00+08:00'))
    getMock.mockImplementation((url:string)=>{
      if(url==='/discharge/records/filter-options')return Promise.resolve({data:{data:[{id:1,departmentName:'康复一科'}]}})
      if(url==='/discharge/records/summary')return Promise.resolve({data:{data:{inpatientCount:10,plannedCount:8,nutritionPatientCount:3,nutritionRecordCount:4,homeRehabPatientCount:2,homeRehabRecordCount:3,outpatientPatientCount:5}}})
      return Promise.resolve({data:{data:{items:[row],total:1,page:1,pageSize:50}}})
    })
    const wrapper=mount(DischargeManagementView,{global:{plugins:[ElementPlus],directives:{permission:()=>{}}}})
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/discharge/records',expect.objectContaining({params:expect.objectContaining({page:1,pageSize:50})}))
    expect(getMock).toHaveBeenCalledWith('/discharge/records/summary',expect.anything())
    expect(wrapper.text()).toContain('在院患者10 人')
    expect(wrapper.text()).toContain('共 4 条预约记录')
    for(const header of ['患者姓名','患者性别','住院号','住院次数','所属科室','入院时间','主诊断','主管医生','预约复诊时间','预计出院时间','实际出院时间','预约营养会诊时间','预约居家康复时间','随访时间','状态','异常原因','操作'])expect(wrapper.text()).toContain(header)
    expect(wrapper.text()).toContain('预计与实际出院日期不一致；出院前12小时内填报；患者临时要求出院')
    expect(wrapper.text()).toContain('异常')
  })

  it('提供完整筛选项和规定分页规格',async()=>{
    getMock.mockImplementation((url:string)=>url==='/discharge/records/filter-options'?Promise.resolve({data:{data:[]}}):url==='/discharge/records/summary'?Promise.resolve({data:{data:{}}}):Promise.resolve({data:{data:{items:[],total:0,page:1,pageSize:50}}}))
    const wrapper=mount(DischargeManagementView,{global:{plugins:[ElementPlus],directives:{permission:()=>{}}}});await flushPromises()
    expect(wrapper.find('input[placeholder="住院号 / 姓名 / 主管医生"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('时间类型（全部）')
    expect(wrapper.findComponent({name:'ElPagination'}).props('pageSizes')).toEqual([20,50,100,200])
    expect(wrapper.find('input[type="file"]').attributes('accept')).toBe('.xlsx')
  })

  it('使用结构化弹窗加载详情、异常规则和操作历史',async()=>{
    const detail={...row,followUpDay7:'恢复良好',followUpDay30:'',followUpDay60:'',specialReason:null}
    getMock.mockImplementation((url:string)=>{
      if(url==='/discharge/records/filter-options')return Promise.resolve({data:{data:[]}})
      if(url==='/discharge/records/summary')return Promise.resolve({data:{data:{}}})
      if(url==='/discharge/records/1')return Promise.resolve({data:{data:detail}})
      if(url==='/discharge/records/1/history')return Promise.resolve({data:{data:[{id:9,operatorName:'doctor01',operatedAt:'2026-09-03T09:00:00+08:00',actionType:'UPDATE',beforeData:'{"plannedDischargeAt":"2026-09-04"}',afterData:'{"plannedDischargeAt":"2026-09-05"}'}]}})
      if(url==='/discharge/consultations')return Promise.resolve({data:{data:[]}})
      return Promise.resolve({data:{data:{items:[detail],total:1,page:1,pageSize:50}}})
    })
    const wrapper=mount(DischargeManagementView,{global:{plugins:[ElementPlus],directives:{permission:()=>{}}}});await flushPromises()
    const editButton=wrapper.findAll('button').find(button=>button.text()==='编辑')
    await editButton!.trigger('click');await flushPromises()
    expect(wrapper.text()).toContain('编辑院后管理信息')
    expect(wrapper.text()).toContain('系统判定异常')
    expect(wrapper.text()).toContain('预计与实际出院日期不一致')
    expect(wrapper.text()).toContain('患者已出院，预计出院时间不可修改')
    expect(wrapper.text()).toContain('操作历史')
    expect(wrapper.text()).toContain('doctor01')
    expect(wrapper.text()).toContain('预计出院时间：2026-09-04 → 2026-09-05')
    for(const section of ['主管医生填报','门诊部填报','营养科填报','居家康复科填报','随访员填报'])expect(wrapper.text()).toContain(section)
    expect(wrapper.text()).toContain('新增营养会诊')
    expect(wrapper.text()).toContain('新增居家康复')
    expect(wrapper.text()).toContain('是否到诊')
    expect(wrapper.text()).toContain('填报人')
    expect(wrapper.text()).toContain('患者恢复情况')
    expect(wrapper.text()).toContain('预约居家康复评估')
    expect(wrapper.text()).toContain('预约营养评估')
    expect(wrapper.find('a[href^="/discharge/"]').exists()).toBe(false)
    expect(getMock).toHaveBeenCalledWith('/discharge/consultations',{params:{encounterId:101,type:'NUTRITION'}})
    expect(getMock).toHaveBeenCalledWith('/discharge/consultations',{params:{encounterId:101,type:'HOME'}})
  },30000)
})
