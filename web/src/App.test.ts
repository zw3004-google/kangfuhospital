import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { defineComponent } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App.vue'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  loadPermissions: vi.fn(),
  hasPermission: vi.fn(),
  clearPermissions: vi.fn(),
}))

vi.mock('./api/http', () => ({ http: { get: mocks.get } }))
vi.mock('./auth', () => ({
  loadPermissions: mocks.loadPermissions,
  hasPermission: mocks.hasPermission,
  clearPermissions: mocks.clearPermissions,
}))
describe('App 登录后菜单刷新', () => {
  beforeEach(() => {
    mocks.get.mockReset().mockResolvedValue({ data: { data: { loginName: 'admin', mustChangePassword: true } } })
    mocks.loadPermissions.mockReset().mockResolvedValue(new Set(['ROLE_SYSTEM_ADMIN']))
    mocks.hasPermission.mockReset().mockReturnValue(true)
  })

  it('首次登录进入工作台后立即展示完整菜单树', async () => {
    const emptyView = defineComponent({ template: '<div class="route-view" />' })
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: emptyView, meta: { public: true, title: '登录' } },
        { path: '/dashboard', component: emptyView, meta: { title: '工作台' } },
        { path: '/arrears/details', component: emptyView, meta: { title: '欠费明细' } },
      ],
    })
    await router.push('/login')
    await router.isReady()
    const wrapper = mount(App, {
      global: {
        plugins: [ElementPlus, router],
      },
    })

    expect(wrapper.find('.sidebar').exists()).toBe(false)

    await router.push('/dashboard')
    await flushPromises()

    expect(mocks.loadPermissions).toHaveBeenCalledOnce()
    expect(wrapper.find('.sidebar').exists()).toBe(true)
    expect(wrapper.text()).toContain('欠费管理')
    expect(wrapper.text()).toContain('预出院管理')
    expect(wrapper.text()).toContain('系统管理')
    expect(wrapper.text()).toContain('预计出院管理')
    expect(wrapper.text()).toContain('统计分析')
    expect(wrapper.text()).toContain('导入记录')
    expect(wrapper.text()).toContain('推送记录')
    expect(wrapper.text()).not.toContain('会诊预约管理')
    expect(wrapper.text()).not.toContain('出院随访填报')
    expect(wrapper.text()).toContain('admin')

    await router.push('/arrears/details')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/arrears/details')
    expect(wrapper.find('.topbar h1').text()).toBe('欠费明细')
    expect(wrapper.find('.sidebar').exists()).toBe(true)
    wrapper.unmount()
  })
})
