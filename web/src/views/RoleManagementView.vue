<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface Role { id: number; roleName: string; roleCode: string }
interface Permission { id: number; permissionName: string; permissionCode: string; permissionType: string }

const roles = ref<Role[]>([])
const permissions = ref<Permission[]>([])
const roleId = ref<number>()
const selected = ref<number[]>([])
const originalSelected = ref<number[]>([])
const saving = ref(false)
const menuKeyword = ref('')
const apiKeyword = ref('')
const groupPermissions = (type: 'MENU' | 'OTHER') => permissions.value.filter(item => (type === 'MENU' ? item.permissionType === 'MENU' : item.permissionType !== 'MENU'))
const filteredPermissions = (type: 'MENU' | 'OTHER', keyword: string) => groupPermissions(type).filter(item => `${item.permissionName} ${item.permissionCode}`.toLowerCase().includes(keyword.trim().toLowerCase()))
const toggleGroup = (ids: number[], selectedIds: number[]) => { const set = new Set(selectedIds); const all = ids.length > 0 && ids.every(id => set.has(id)); ids.forEach(id => all ? set.delete(id) : set.add(id)); return [...set] }
const selectedRole = computed(() => roles.value.find(role => role.id === roleId.value))
const isSystemAdmin = computed(() => selectedRole.value?.roleCode === 'SYSTEM_ADMIN')
const allPermissionIds = computed(() => permissions.value.map(permission => permission.id))
const allSelected = computed(() => allPermissionIds.value.length > 0 && allPermissionIds.value.every(id => selected.value.includes(id)))
const toggleMenu = () => { if (!isSystemAdmin.value) selected.value = toggleGroup(groupPermissions('MENU').map(item => item.id), selected.value) }
const toggleApi = () => { if (!isSystemAdmin.value) selected.value = toggleGroup(groupPermissions('OTHER').map(item => item.id), selected.value) }
const load = async () => { roles.value = (await http.get<ApiResponse<Role[]>>('/system/roles')).data.data; permissions.value = (await http.get<ApiResponse<Permission[]>>('/system/permissions')).data.data }
const choose = async (id: number) => { roleId.value = id; selected.value = (await http.get<ApiResponse<number[]>>(`/system/permissions/roles/${id}`)).data.data; if (isSystemAdmin.value) selected.value = [...allPermissionIds.value]; originalSelected.value = [...selected.value] }
const toggleSelectAll = () => { if (!isSystemAdmin.value) selected.value = allSelected.value ? [] : [...allPermissionIds.value] }
const save = async () => { if (!roleId.value || saving.value) return; const roleName = selectedRole.value?.roleName || '当前角色'; try { await ElMessageBox.confirm(`确认保存“${roleName}”的权限？错误配置可能造成越权或无法访问。`, '保存权限确认', { type: 'warning', confirmButtonText: '确认保存' }) } catch { return }; saving.value = true; try { await http.put(`/system/permissions/roles/${roleId.value}`, { permissionIds: selected.value, expectedPermissionIds: originalSelected.value }); originalSelected.value = [...selected.value]; ElMessage.success('权限已保存') } catch (error) { ElMessage.error(error instanceof Error ? error.message : '保存失败') } finally { saving.value = false } }
onMounted(load)
</script>

<template><section class="page-card role-management-page"><h2>权限管理</h2><div class="filter-bar role-mobile-toolbar"><el-select v-model="roleId" placeholder="选择角色" @change="choose"><el-option v-for="role in roles" :key="role.id" :label="role.roleName" :value="role.id" /></el-select><el-button :disabled="!roleId || permissions.length === 0 || isSystemAdmin" @click="toggleSelectAll">{{ isSystemAdmin ? '已全选' : allSelected ? '取消全选' : '全选' }}</el-button><el-button v-permission="'PERM_API_ROLE_MANAGE'" type="primary" :loading="saving" @click="save">保存权限</el-button></div><el-divider /><el-form label-position="top" class="role-permission-form"><el-form-item label="菜单权限"><div class="permission-group-toolbar"><el-input v-model="menuKeyword" clearable placeholder="筛选菜单权限"/><el-button :disabled="isSystemAdmin" @click="toggleMenu">全选/取消全选</el-button></div><el-checkbox-group v-model="selected" class="permission-check-grid"><el-checkbox v-for="permission in filteredPermissions('MENU', menuKeyword)" :key="permission.id" :value="permission.id" :disabled="isSystemAdmin">{{ permission.permissionName }}</el-checkbox></el-checkbox-group></el-form-item><el-form-item label="接口及字段权限"><div class="permission-group-toolbar"><el-input v-model="apiKeyword" clearable placeholder="筛选接口及字段权限"/><el-button :disabled="isSystemAdmin" @click="toggleApi">全选/取消全选</el-button></div><el-checkbox-group v-model="selected" class="permission-check-grid"><el-checkbox v-for="permission in filteredPermissions('OTHER', apiKeyword)" :key="permission.id" :value="permission.id" :disabled="isSystemAdmin">{{ permission.permissionName }}</el-checkbox></el-checkbox-group></el-form-item></el-form></section></template>