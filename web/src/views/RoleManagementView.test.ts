import { flushPromises, mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { nextTick } from 'vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import RoleManagementView from './RoleManagementView.vue'

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
  put: vi.fn(),
  confirm: vi.fn(),
  success: vi.fn(),
  error: vi.fn(),
}))

vi.mock('../api/http', () => ({ http: { get: mocks.get, put: mocks.put } }))
vi.mock('element-plus', async (importOriginal) => { const actual = await importOriginal<typeof import('element-plus')>(); return { ...actual, ElMessage: { ...actual.ElMessage, success: mocks.success, error: mocks.error }, ElMessageBox: { ...actual.ElMessageBox, confirm: mocks.confirm } } })

const response = (data: unknown) => Promise.resolve({ data: { data } })

describe('RoleManagementView', () => {
  beforeEach(() => {
    mocks.get.mockReset().mockImplementation((url: string) => {
      if (url === '/system/roles') return response([
        { id: 1, roleName: '系统管理员', roleCode: 'SYSTEM_ADMIN' },
        { id: 2, roleName: '普通角色', roleCode: 'USER' },
      ])
      if (url === '/system/permissions') return response([
        { id: 11, permissionName: '系统菜单', permissionCode: 'MENU_SYSTEM', permissionType: 'MENU' },
        { id: 12, permissionName: '用户管理', permissionCode: 'API_USER_MANAGE', permissionType: 'API' },
      ])
      if (url === '/system/departments') return response({ items: [], total: 0, page: 1, pageSize: 200 })
      if (url.endsWith('/departments')) return response([])
      return response([])
    })
    mocks.put.mockReset().mockResolvedValue({ data: { data: null } })
    mocks.confirm.mockReset().mockResolvedValue('confirm')
    mocks.success.mockReset(); mocks.error.mockReset()
  })

  it('全选按钮选择全部菜单、接口和字段权限', async () => {
    const wrapper = mount(RoleManagementView, {
      global: { plugins: [ElementPlus], directives: { permission: () => {} } },
    })
    await flushPromises()
    ;(wrapper.vm as unknown as { roleId: number }).roleId = 2
    await nextTick()

    const selectAll = wrapper.findAll('button').find(button => button.text() === '全选')
    expect(selectAll).toBeDefined()
    await selectAll!.trigger('click')
    expect(wrapper.text()).toContain('取消全选')

    const save = wrapper.findAll('button').find(button => button.text() === '保存权限')
    await save!.trigger('click')
    await flushPromises()
    expect(mocks.confirm).toHaveBeenCalledWith(expect.stringContaining('普通角色'), '保存权限确认', expect.objectContaining({ type: 'warning' }))
    expect(mocks.put).toHaveBeenCalledWith('/system/permissions/roles/2/scope', { permissionIds: [11, 12], departmentIds: [], expectedPermissionIds: [], expectedDepartmentIds: [] })
    expect(wrapper.find('.permission-check-grid').exists()).toBe(true)
  })

  it('选择系统管理员时自动拥有全部权限', async () => {
    const wrapper = mount(RoleManagementView, {
      global: { plugins: [ElementPlus], directives: { permission: () => {} } },
    })
    await flushPromises()
    await (wrapper.vm as unknown as { choose: (id: number) => Promise<void> }).choose(1)
    await nextTick()

    expect(wrapper.text()).toContain('已全选')
    const allButton = wrapper.findAll('button').find(button => button.text() === '已全选')
    expect(allButton?.attributes('disabled')).toBeDefined()
  })
})
