import {flushPromises,mount} from '@vue/test-utils'
import ElementPlus from 'element-plus'
import {describe,expect,it,vi} from 'vitest'
import ConsultationSection from './ConsultationSection.vue'
import FollowUpSection from './FollowUpSection.vue'

const {getMock}=vi.hoisted(()=>({getMock:vi.fn()}))
vi.mock('../../api/http',()=>({http:{get:getMock,post:vi.fn(),put:vi.fn(),delete:vi.fn()}}))

describe('预计出院患者业务分区',()=>{
  it('会诊分区直接使用当前患者上下文加载对应类型记录',async()=>{
    getMock.mockResolvedValue({data:{data:[{id:1,encounterId:101,appointmentAt:'2026-09-08T09:00:00+08:00',executorName:'营养师甲',executionResult:'待执行'}]}})
    const wrapper=mount(ConsultationSection,{props:{encounterId:101,type:'NUTRITION'},global:{plugins:[ElementPlus]}})
    await flushPromises()

    expect(getMock).toHaveBeenCalledWith('/discharge/consultations',{params:{encounterId:101,type:'NUTRITION'}})
    expect(wrapper.text()).toContain('营养科填报')
    expect(wrapper.text()).toContain('营养师甲')
    expect(wrapper.text()).toContain('待执行')
    expect(wrapper.text()).toContain('新增营养会诊')
  })

  it('随访分区通过双向绑定承载第7、30、60天随访',()=>{
    const detail=(day:number)=>({day,followUpAt:'',executorName:'随访员甲',recoveryCondition:'恢复良好',remindOutpatient:true,homeRehabEvaluation:'满意',homeRehabUnsatisfiedReason:'',homeRehabAssessmentAt:'',nutritionEvaluation:'合格',nutritionUnsatisfiedReason:'',nutritionAssessmentAt:''})
    const wrapper=mount(FollowUpSection,{props:{followUpRequired:true,details:[detail(7),detail(30),detail(60)]},global:{plugins:[ElementPlus]}})

    expect(wrapper.text()).toContain('随访员填报')
    expect(wrapper.text()).toContain('第7天回访')
    expect(wrapper.text()).toContain('第30天回访')
    expect(wrapper.text()).toContain('第60天回访')
    expect(wrapper.text()).toContain('患者恢复情况')
    expect(wrapper.text()).toContain('提醒预约复诊')
    expect(wrapper.text()).toContain('患者对居家康复的评价')
    expect(wrapper.text()).toContain('预约营养评估')
  },15000)

  it('会诊记录达到10条后禁止继续新增',async()=>{
    getMock.mockResolvedValue({data:{data:Array.from({length:10},(_,index)=>({id:index+1,encounterId:101,appointmentAt:'2026-09-08T09:00:00+08:00',executorName:`执行人${index+1}`,executionResult:'待执行'}))}})
    const wrapper=mount(ConsultationSection,{props:{encounterId:101,type:'HOME'},global:{plugins:[ElementPlus]}})
    await flushPromises()

    const addButton=wrapper.findAll('button').find(button=>button.text()==='新增居家康复')
    expect(addButton?.attributes('disabled')).toBeDefined()
  },15000)
})
