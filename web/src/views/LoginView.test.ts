import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginView from './LoginView.vue'

const mocks = vi.hoisted(() => ({
  get: vi.fn(), replace: vi.fn(), clearPermissions: vi.fn(), loadPermissions: vi.fn(), warning: vi.fn(), error: vi.fn(),
}))

vi.mock('../api/http', () => ({ http: { get: mocks.get } }))
vi.mock('../auth', () => ({ clearPermissions: mocks.clearPermissions, loadPermissions: mocks.loadPermissions }))
vi.mock('vue-router', () => ({ useRouter: () => ({ replace: mocks.replace }) }))
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return { ...actual, ElMessage: { ...actual.ElMessage, warning: mocks.warning, error: mocks.error } }
})

const render = () => mount(LoginView, { global: { plugins: [ElementPlus] } })

describe('LoginView', () => {
  beforeEach(() => {
    sessionStorage.clear()
    mocks.get.mockReset()
    mocks.replace.mockReset()
    mocks.clearPermissions.mockReset()
    mocks.loadPermissions.mockReset().mockResolvedValue(undefined)
    mocks.warning.mockReset()
    mocks.error.mockReset()
  })

  it('展示正式登录表单且不提供客户端角色选择', () => {
    const wrapper = render()
    expect(wrapper.text()).toContain('康复医院运营管理系统')
    expect(wrapper.text()).toContain('欠费通报 · 预出院管理')
    expect(wrapper.find('input[name="username"]').attributes('autocomplete')).toBe('username')
    expect(wrapper.find('input[name="password"]').attributes('autocomplete')).toBe('current-password')
    expect(wrapper.find('select').exists()).toBe(false)
  })

  it('空表单提交时提示补全账号和密码', async () => {
    const wrapper = render()
    await wrapper.find('form').trigger('submit')
    expect(mocks.warning).toHaveBeenCalledWith('请输入账号和密码')
    expect(mocks.get).not.toHaveBeenCalled()
  })

  it('登录成功后加载权限并跳转工作台', async () => {
    mocks.get.mockResolvedValue({ data: { data: {} } })
    const wrapper = render()
    await wrapper.find('input[name="username"]').setValue('zhangkj')
    await wrapper.find('input[name="password"]').setValue('password123')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(sessionStorage.getItem('basicAuth')).toBe(btoa('zhangkj:password123'))
    expect(mocks.get).toHaveBeenCalledWith('/system/me')
    expect(mocks.loadPermissions).toHaveBeenCalledOnce()
    expect(mocks.replace).toHaveBeenCalledWith('/dashboard')
  })

  it('登录失败后清理临时凭证并显示错误', async () => {
    mocks.get.mockRejectedValue(new Error('账号或密码错误'))
    const wrapper = render()
    await wrapper.find('input[name="username"]').setValue('wrong')
    await wrapper.find('input[name="password"]').setValue('wrong')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(sessionStorage.getItem('basicAuth')).toBeNull()
    expect(mocks.clearPermissions).toHaveBeenCalledTimes(2)
    expect(mocks.error).toHaveBeenCalledWith('账号或密码错误')
  })
})
