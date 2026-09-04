import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import FeeCoefficientView from './FeeCoefficientView.vue'

const mocks=vi.hoisted(()=>({get:vi.fn(),post:vi.fn(),warning:vi.fn(),success:vi.fn()}))
vi.mock('../api/http',()=>({http:{get:mocks.get,post:mocks.post}}))
vi.mock('element-plus',async(importOriginal)=>{const actual=await importOriginal<typeof import('element-plus')>();return{...actual,ElMessage:{...actual.ElMessage,warning:mocks.warning,success:mocks.success,error:vi.fn()},ElMessageBox:{...actual.ElMessageBox,confirm:vi.fn().mockResolvedValue('confirm')}}})

const row={id:1,feeTypeId:10,feeCode:'YB01',feeType:'城镇职工基本医疗保险',coefficient:0.5,enabled:true,effectiveAt:'2026-09-03T08:00:00+08:00',disabledAt:null,createdAt:'2026-09-02T08:00:00+08:00',createdByName:'admin',enabledByName:'admin',disabledByName:null}
describe('FeeCoefficientView',()=>{
  beforeEach(()=>{mocks.get.mockReset().mockResolvedValue({data:{data:[row]}});mocks.post.mockReset().mockResolvedValue({data:{data:row}});mocks.warning.mockReset();mocks.success.mockReset()})
  it('展示编码、正确计算口径和原始押金示例',async()=>{const wrapper=mount(FeeCoefficientView,{global:{plugins:[ElementPlus]}});await flushPromises();expect(wrapper.text()).toContain('费别编码');expect(wrapper.text()).toContain('YB01');expect(wrapper.text()).toContain('最终应交押金 = 原始应交押金');expect(wrapper.text()).toContain('100,000 × 0.5000 = 50,000.00 元')})
  it('新增费别时规范化编码并提交名称和系数',async()=>{const wrapper=mount(FeeCoefficientView,{attachTo:document.body,global:{plugins:[ElementPlus]}});await flushPromises();await wrapper.findAll('button').find(b=>b.text()==='新增费别')!.trigger('click');await flushPromises();const inputs=document.body.querySelectorAll<HTMLInputElement>('.el-dialog input');inputs[0].value='yb01';inputs[0].dispatchEvent(new Event('input'));inputs[1].value='医保';inputs[1].dispatchEvent(new Event('input'));await wrapper.findAll('button').find(b=>b.text()==='保存')!.trigger('click');await flushPromises();expect(mocks.post).toHaveBeenCalledWith('/system/fee-coefficients',{feeCode:'YB01',feeType:'医保',coefficient:1});wrapper.unmount()})
  it('拒绝包含特殊字符的编码',async()=>{const wrapper=mount(FeeCoefficientView,{attachTo:document.body,global:{plugins:[ElementPlus]}});await flushPromises();await wrapper.findAll('button').find(b=>b.text()==='新增费别')!.trigger('click');await flushPromises();const inputs=document.body.querySelectorAll<HTMLInputElement>('.el-dialog input');inputs[0].value='YB-01';inputs[0].dispatchEvent(new Event('input'));inputs[1].value='医保';inputs[1].dispatchEvent(new Event('input'));await wrapper.findAll('button').find(b=>b.text()==='保存')!.trigger('click');expect(mocks.warning).toHaveBeenCalledWith('费别编码仅支持1～32位字母和数字');expect(mocks.post).not.toHaveBeenCalled();wrapper.unmount()})
})
