import {flushPromises,mount} from '@vue/test-utils'
import ElementPlus from 'element-plus'
import {describe,expect,it,vi} from 'vitest'
import ImportBatchView from './ImportBatchView.vue'

const {getMock}=vi.hoisted(()=>({getMock:vi.fn()}))
vi.mock('../api/http',()=>({http:{get:getMock}}))
vi.mock('vue-router',()=>({useRoute:()=>({meta:{businessType:'ARREARS'}})}))

describe('ImportBatchView H5',()=>{
  it('以移动卡片区分接口同步和 Excel 导入并可查看错误',async()=>{
    getMock.mockImplementation((url:string)=>{
      if(url==='/import-batches')return Promise.resolve({data:{data:[
        {batchNo:'HIS-001',businessType:'ARREARS',sourceType:'API',transactionCode:'BJKF_ZYCX',triggerType:'AUTO',filename:null,status:'FAILED',total:2,success:1,failure:1,added:1,overwritten:0,skipped:0,summaryStatus:null,startedAt:'2026-09-09T07:30:00+08:00',finishedAt:null,errorMessage:'部分数据校验失败'},
        {batchNo:'EXCEL-001',businessType:'ARREARS',sourceType:'EXCEL',transactionCode:null,triggerType:null,filename:'欠费.xlsx',status:'SUCCESS',total:1,success:1,failure:0,added:1,overwritten:0,skipped:0,summaryStatus:null,startedAt:'2026-09-09T08:00:00+08:00',finishedAt:null,errorMessage:null},
      ]}})
      return Promise.resolve({data:{data:[{rowNumber:2,inpatientNo:'ZY001',admissionTimes:1,fieldName:'科室',originalValue:'未知病区',errorCode:'DEPARTMENT_NOT_FOUND',errorMessage:'未匹配到科室'}]}})
    })
    const wrapper=mount(ImportBatchView,{global:{plugins:[ElementPlus]}})
    await flushPromises()
    expect(wrapper.findAll('.import-batch-mobile-card')).toHaveLength(2)
    const text=wrapper.find('.import-batch-mobile-list').text()
    expect(text).toContain('接口同步')
    expect(text).toContain('BJKF_ZYCX · 自动')
    expect(text).toContain('Excel 导入')
    expect(text).toContain('欠费.xlsx')
    await wrapper.find('.import-batch-mobile-card button').trigger('click')
    await flushPromises()
    expect(getMock).toHaveBeenCalledWith('/import-batches/HIS-001/errors')
  })
})
