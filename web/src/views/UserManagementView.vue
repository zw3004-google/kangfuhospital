<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, type ApiResponse } from '../api/http'

interface Department { id: number; departmentCode: string; departmentName: string; enabled: boolean }
interface Role { id: number; roleCode: string; roleName: string; builtIn: boolean; enabled: boolean }
interface User { id: number; loginName: string; displayName: string; employeeNo: string; wecomUserId: string; departmentId: number; departmentName: string; enabled: boolean; mustChangePassword: boolean; roles: Role[] }
interface PageResult<T> { items: T[]; total: number; page: number; pageSize: number }

const activeTab = ref('users')
const loading = ref(false)
const users = ref<User[]>([])
const departments = ref<Department[]>([])
const roles = ref<Role[]>([])
const total = ref(0)
const query = reactive({ keyword: '', departmentId: undefined as number | undefined, page: 1, pageSize: 50 })
const userDialog = ref(false)
const departmentDialog = ref(false)
const roleDialog = ref(false)
const saving = ref(false)
const userForm = reactive({ displayName: '', employeeNo: '', wecomUserId: '', departmentId: undefined as number | undefined })
const departmentForm = reactive({ departmentCode: '', departmentName: '' })
const roleForm = reactive({ userId: 0, displayName: '', roleIds: [] as number[] })

async function loadReferences() {
  const [departmentResponse, roleResponse] = await Promise.all([
    http.get<ApiResponse<Department[]>>('/system/departments'),
    http.get<ApiResponse<Role[]>>('/system/roles'),
  ])
  departments.value = departmentResponse.data.data
  roles.value = roleResponse.data.data
}

async function loadUsers() {
  loading.value = true
  try {
    const response = await http.get<ApiResponse<PageResult<User>>>('/system/users', { params: query })
    users.value = response.data.data.items
    total.value = response.data.data.total
  } catch (error) { ElMessage.error(messageOf(error)) }
  finally { loading.value = false }
}

async function initialize() {
  try { await loadReferences(); await loadUsers() }
  catch (error) { ElMessage.error(messageOf(error)) }
}

function openUser() {
  Object.assign(userForm, { displayName: '', employeeNo: '', wecomUserId: '', departmentId: undefined })
  userDialog.value = true
}

async function createUser() {
  if (!userForm.displayName.trim() || !userForm.employeeNo.trim() || !userForm.wecomUserId.trim() || !userForm.departmentId) {
    ElMessage.warning('请完整填写用户信息'); return
  }
  saving.value = true
  try {
    const response = await http.post<ApiResponse<User>>('/system/users', userForm)
    ElMessage.success(`用户已创建，登录名：${response.data.data.loginName}`)
    userDialog.value = false; await loadUsers()
  } catch (error) { ElMessage.error(messageOf(error)) }
  finally { saving.value = false }
}

function openDepartment() {
  Object.assign(departmentForm, { departmentCode: '', departmentName: '' })
  departmentDialog.value = true
}

async function createDepartment() {
  if (!departmentForm.departmentCode.trim() || !departmentForm.departmentName.trim()) {
    ElMessage.warning('请完整填写科室信息'); return
  }
  saving.value = true
  try {
    await http.post('/system/departments', departmentForm)
    ElMessage.success('科室已创建'); departmentDialog.value = false; await loadReferences()
  } catch (error) { ElMessage.error(messageOf(error)) }
  finally { saving.value = false }
}

async function toggleUser(user: User) {
  const action = user.enabled ? '停用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户“${user.displayName}”？`, `${action}确认`, { type: 'warning' })
  try { await http.post(`/system/users/${user.id}/${user.enabled ? 'disable' : 'enable'}`); ElMessage.success(`${action}成功`); await loadUsers() }
  catch (error) { ElMessage.error(messageOf(error)) }
}

async function toggleDepartment(department: Department) {
  const action = department.enabled ? '停用' : '启用'
  await ElMessageBox.confirm(`确认${action}科室“${department.departmentName}”？`, `${action}确认`, { type: 'warning' })
  try { await http.post(`/system/departments/${department.id}/${department.enabled ? 'disable' : 'enable'}`); ElMessage.success(`${action}成功`); await loadReferences() }
  catch (error) { ElMessage.error(messageOf(error)) }
}

async function resetPassword(user: User) {
  await ElMessageBox.confirm(`将“${user.displayName}”的密码重置为系统初始密码，并要求下次登录修改。`, '重置密码', { type: 'warning' })
  try { await http.post(`/system/users/${user.id}/reset-password`); ElMessage.success('密码已重置') }
  catch (error) { ElMessage.error(messageOf(error)) }
}

function openRoles(user: User) {
  roleForm.userId = user.id; roleForm.displayName = user.displayName; roleForm.roleIds = user.roles.map(role => role.id)
  roleDialog.value = true
}

async function saveRoles() {
  saving.value = true
  try {
    await http.put(`/system/users/${roleForm.userId}/roles`, { roleIds: roleForm.roleIds })
    ElMessage.success('角色分配已保存'); roleDialog.value = false; await loadUsers()
  } catch (error) { ElMessage.error(messageOf(error)) }
  finally { saving.value = false }
}

function messageOf(error: unknown) { return error instanceof Error ? error.message : '操作失败' }
onMounted(initialize)
</script>

<template>
  <section class="page-card">
    <div class="page-heading">
      <div><h2>用户与科室管理</h2><p>用户只能归属一个科室；角色在用户导入或创建后由管理员分配。</p></div>
      <div><el-button v-permission="'PERM_API_DEPT_MANAGE'" @click="openDepartment">新增科室</el-button><el-button v-permission="'PERM_API_USER_MANAGE'" type="primary" @click="openUser">新增用户</el-button></div>
    </div>
    <el-tabs v-model="activeTab" class="management-tabs">
      <el-tab-pane label="用户管理" name="users">
        <div class="filter-bar">
          <el-input v-model="query.keyword" clearable placeholder="姓名、登录名、工号或企微ID" style="width:300px" @keyup.enter="query.page=1; loadUsers()" />
          <el-select v-model="query.departmentId" clearable placeholder="全部科室" style="width:200px">
            <el-option v-for="item in departments" :key="item.id" :label="item.departmentName" :value="item.id" />
          </el-select>
          <el-button type="primary" plain @click="query.page=1; loadUsers()">查询</el-button>
        </div>
        <el-table v-loading="loading" :data="users" stripe>
          <el-table-column prop="displayName" label="姓名" width="120" />
          <el-table-column prop="employeeNo" label="工号" min-width="130" />
          <el-table-column prop="loginName" label="登录名" min-width="140" />
          <el-table-column prop="wecomUserId" label="企微ID" min-width="160" />
          <el-table-column prop="departmentName" label="所属科室" min-width="140" />
          <el-table-column label="角色" min-width="250">
            <template #default="scope"><el-tag v-for="role in scope.row.roles" :key="role.id" size="small" class="role-tag">{{ role.roleName }}</el-tag><span v-if="!scope.row.roles.length" class="muted">未分配</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100"><template #default="scope"><el-tag :type="scope.row.enabled?'success':'info'">{{ scope.row.enabled?'启用':'停用' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="250" fixed="right"><template #default="scope">
            <el-button v-permission="'PERM_API_USER_MANAGE'" link type="primary" @click="openRoles(scope.row)">分配角色</el-button>
            <el-button v-permission="'PERM_API_USER_MANAGE'" link @click="resetPassword(scope.row)">重置密码</el-button>
            <el-button v-permission="'PERM_API_USER_MANAGE'" link :type="scope.row.enabled?'danger':'primary'" @click="toggleUser(scope.row)">{{ scope.row.enabled?'停用':'启用' }}</el-button>
          </template></el-table-column>
        </el-table>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :page-sizes="[20,50,100,200]" :total="total" layout="total, sizes, prev, pager, next" class="pagination" @change="loadUsers" />
      </el-tab-pane>
      <el-tab-pane label="科室管理" name="departments">
        <el-table :data="departments" stripe>
          <el-table-column prop="departmentCode" label="科室编码" min-width="180" />
          <el-table-column prop="departmentName" label="科室名称" min-width="220" />
          <el-table-column label="状态" width="120"><template #default="scope"><el-tag :type="scope.row.enabled?'success':'info'">{{ scope.row.enabled?'启用':'停用' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="scope"><el-button link :type="scope.row.enabled?'danger':'primary'" @click="toggleDepartment(scope.row)">{{ scope.row.enabled?'停用':'启用' }}</el-button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </section>

  <el-dialog v-model="userDialog" title="新增用户" width="500px"><el-alert title="登录名根据姓名拼音自动生成；工号用于主管医生唯一匹配；角色创建后单独分配。" type="info" :closable="false" /><el-form label-position="top" class="dialog-form"><el-form-item label="姓名" required><el-input v-model="userForm.displayName" maxlength="128" /></el-form-item><el-form-item label="工号" required><el-input v-model="userForm.employeeNo" maxlength="64" /></el-form-item><el-form-item label="企微ID" required><el-input v-model="userForm.wecomUserId" maxlength="128" /></el-form-item><el-form-item label="所属科室" required><el-select v-model="userForm.departmentId" style="width:100%"><el-option v-for="item in departments.filter(i=>i.enabled)" :key="item.id" :label="item.departmentName" :value="item.id" /></el-select></el-form-item></el-form><template #footer><el-button @click="userDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="createUser">保存</el-button></template></el-dialog>
  <el-dialog v-model="departmentDialog" title="新增科室" width="500px"><el-form label-position="top"><el-form-item label="科室编码" required><el-input v-model="departmentForm.departmentCode" placeholder="例如：KF01" /></el-form-item><el-form-item label="科室名称" required><el-input v-model="departmentForm.departmentName" /></el-form-item></el-form><template #footer><el-button @click="departmentDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="createDepartment">保存</el-button></template></el-dialog>
  <el-dialog v-model="roleDialog" :title="`分配角色：${roleForm.displayName}`" width="560px"><el-checkbox-group v-model="roleForm.roleIds" class="role-grid"><el-checkbox v-for="role in roles" :key="role.id" :value="role.id" border>{{ role.roleName }}</el-checkbox></el-checkbox-group><template #footer><el-button @click="roleDialog=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRoles">保存</el-button></template></el-dialog>
</template>
