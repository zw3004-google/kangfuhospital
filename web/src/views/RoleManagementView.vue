<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface Role { id: number; roleName: string; roleCode: string }
interface Permission { id: number; permissionName: string; permissionCode: string; permissionType: string }
interface Department { id: number; departmentName: string; enabled: boolean }

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const departments = ref<Department[]>([])
const roleId = ref<number>()
const selected = ref<number[]>([])
const deptSelected = ref<number[]>([])
const originalSelected = ref<number[]>([])
const originalDeptSelected = ref<number[]>([])
const saving = ref(false)

const allPermissionIds = computed(() => permissions.value.map(permission => permission.id))
const selectedRole = computed(() => roles.value.find(role => role.id === roleId.value))
const isSystemAdmin = computed(() => selectedRole.value?.roleCode === 'SYSTEM_ADMIN')
const allSelected = computed(() =>
  allPermissionIds.value.length > 0 && allPermissionIds.value.every(id => selected.value.includes(id)),
)

const load = async () => {
  roles.value = (await http.get<ApiResponse<Role[]>>('/system/roles')).data.data
  permissions.value = (await http.get<ApiResponse<Permission[]>>('/system/permissions')).data.data
  departments.value = (await http.get<ApiResponse<Department[]>>('/system/departments')).data.data
}

const choose = async (id: number) => {
  roleId.value = id
  selected.value = (await http.get<ApiResponse<number[]>>(`/system/permissions/roles/${id}`)).data.data
  if (isSystemAdmin.value) selected.value = [...allPermissionIds.value]
  deptSelected.value = (await http.get<ApiResponse<number[]>>(`/system/permissions/roles/${id}/departments`)).data.data
  originalSelected.value = [...selected.value]
  originalDeptSelected.value = [...deptSelected.value]
}

const toggleSelectAll = () => {
  if (isSystemAdmin.value) return
  selected.value = allSelected.value ? [] : [...allPermissionIds.value]
}

const save = async () => {
  if (!roleId.value || saving.value) return
  const roleName = selectedRole.value?.roleName || '当前角色'
  try { await ElMessageBox.confirm(`确认保存“${roleName}”的权限与可访问科室？错误配置可能造成越权或无法访问。`, '保存权限确认', { type: 'warning', confirmButtonText: '确认保存' }) } catch { return }
  saving.value = true
  try {
    await http.put(`/system/permissions/roles/${roleId.value}/scope`, { permissionIds: selected.value, departmentIds: deptSelected.value, expectedPermissionIds: originalSelected.value, expectedDepartmentIds: originalDeptSelected.value })
    originalSelected.value = [...selected.value]; originalDeptSelected.value = [...deptSelected.value]
    ElMessage.success('权限范围已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '保存失败') }
  finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <section class="page-card role-management-page">
    <h2>权限管理</h2>
    <div class="filter-bar role-mobile-toolbar">
      <el-select v-model="roleId" placeholder="选择角色" @change="choose">
        <el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" />
      </el-select>
      <el-button :disabled="!roleId || permissions.length === 0 || isSystemAdmin" @click="toggleSelectAll">
        {{ isSystemAdmin ? '已全选' : allSelected ? '取消全选' : '全选' }}
      </el-button>
      <el-button v-permission="'PERM_API_ROLE_MANAGE'" type="primary" :loading="saving" @click="save">保存权限</el-button>
    </div>
    <el-divider />
    <el-form label-position="top" class="role-permission-form">
      <el-form-item label="菜单权限">
        <el-checkbox-group v-model="selected" class="permission-check-grid">
          <el-checkbox v-for="permission in permissions.filter(item => item.permissionType === 'MENU')" :key="permission.id" :value="permission.id" :disabled="isSystemAdmin">
            {{ permission.permissionName }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="接口及字段权限">
        <el-checkbox-group v-model="selected" class="permission-check-grid">
          <el-checkbox v-for="permission in permissions.filter(item => item.permissionType !== 'MENU')" :key="permission.id" :value="permission.id" :disabled="isSystemAdmin">
            {{ permission.permissionName }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="可访问科室">
        <el-checkbox-group v-model="deptSelected" class="permission-check-grid department-check-grid">
          <el-checkbox v-for="department in departments.filter(item => item.enabled)" :key="department.id" :value="department.id">
            {{ department.departmentName }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </section>
</template>
