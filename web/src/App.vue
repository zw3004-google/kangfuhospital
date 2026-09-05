<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearPermissions, hasPermission, loadPermissions } from './auth'
import { http } from './api/http'
import { clearAllSessionDrafts } from './sessionDraft'

const IDLE_LIMIT = 30 * 60 * 1000
const route = useRoute()
const router = useRouter()
const permissionReady = ref(0)
const currentUser = ref('')
const mobileMenuOpen = ref(false)
const privacyCovered = ref(false)
const online = ref(typeof navigator === 'undefined' ? true : navigator.onLine)
let idleTimer: number | undefined

const title = computed(() => String(route.meta.title ?? '工作台'))
const watermarkText = computed(() => currentUser.value ? `${currentUser.value} · ${new Date().toLocaleDateString('zh-CN')}` : '')
const menuPermissions: Record<string, string> = {
  '/arrears/details': 'PERM_API_ARREARS_REPORT', '/arrears/report': 'PERM_API_ARREARS_REPORT',
  '/arrears/import-batches': 'PERM_API_ARREARS_IMPORT', '/arrears/push-records': 'PERM_API_PUSH_RECORD_VIEW',
  '/discharge/management': 'PERM_API_DISCHARGE_ANALYSIS', '/discharge/analysis': 'PERM_API_DISCHARGE_ANALYSIS',
  '/discharge/import-batches': 'PERM_API_DISCHARGE_IMPORT', '/discharge/push-records': 'PERM_API_PUSH_RECORD_VIEW',
  '/system/fee-coefficients': 'PERM_API_FEE_CONFIG', '/system/users': 'PERM_API_USER_MANAGE',
  '/system/roles': 'PERM_API_ROLE_MANAGE', '/system/audit-logs': 'PERM_API_AUDIT_VIEW',
}
const menuGroups = [
  { title: '欠费管理', items: [['/arrears/details', '欠费明细'], ['/arrears/report', '通报报表'], ['/arrears/import-batches', '导入记录'], ['/arrears/push-records', '推送记录']] },
  { title: '预出院管理', items: [['/discharge/management', '预计出院管理'], ['/discharge/analysis', '统计分析'], ['/discharge/import-batches', '导入记录'], ['/discharge/push-records', '推送记录']] },
  { title: '系统管理', items: [['/system/fee-coefficients', '费别系数配置'], ['/system/users', '用户管理'], ['/system/roles', '权限管理'], ['/system/audit-logs', '审计日志']] },
] as const
const canSee = (path: string) => { void permissionReady.value; const permission = menuPermissions[path]; return !permission || hasPermission(permission) }
const visibleGroups = computed(() => menuGroups.map(group => ({ ...group, items: group.items.filter(item => canSee(item[0])) })).filter(group => group.items.length))

function goToLogin(reason: 'timeout' | 'logout') {
  if (idleTimer) window.clearTimeout(idleTimer)
  clearPermissions()
  clearAllSessionDrafts()
  currentUser.value = ''
  const redirect = route.fullPath
  const query = reason === 'timeout' ? { timeout: '1', redirect } : undefined
  void router.replace({ path: '/login', query })
}

function resetIdle() {
  if (idleTimer) window.clearTimeout(idleTimer)
  if (currentUser.value) idleTimer = window.setTimeout(() => { void expireSession() }, IDLE_LIMIT)
}

function updateNetworkStatus() { online.value = navigator.onLine }

function handleVisibility() {
  privacyCovered.value = document.hidden && Boolean(currentUser.value) && !route.meta.public
  if (!document.hidden) resetIdle()
}

async function expireSession() {
  try { await http.post('/auth/logout') } catch { /* local state still needs to be cleared */ }
  goToLogin('timeout')
}

async function logout() {
  try { await http.post('/auth/logout'); goToLogin('logout') }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '退出未完成，请恢复网络后重试') }
}

async function refreshShell() {
  mobileMenuOpen.value = false
  if (route.meta.public) { if (idleTimer) window.clearTimeout(idleTimer); currentUser.value = ''; return }
  await loadPermissions()
  permissionReady.value++
  try {
    const response = await http.get<{ data: { loginName: string } }>('/system/me')
    currentUser.value = response.data.data.loginName
    resetIdle()
  } catch { currentUser.value = '' }
}

const activityEvents = ['click', 'keydown', 'pointerdown', 'touchstart'] as const
onMounted(() => {
  activityEvents.forEach(event => window.addEventListener(event, resetIdle, { passive: true }))
  document.addEventListener('visibilitychange', handleVisibility)
  window.addEventListener('online', updateNetworkStatus)
  window.addEventListener('offline', updateNetworkStatus)
})
onBeforeUnmount(() => {
  if (idleTimer) window.clearTimeout(idleTimer)
  activityEvents.forEach(event => window.removeEventListener(event, resetIdle))
  document.removeEventListener('visibilitychange', handleVisibility)
  window.removeEventListener('online', updateNetworkStatus)
  window.removeEventListener('offline', updateNetworkStatus)
})
watch(() => route.fullPath, refreshShell, { immediate: true })
</script>

<template>
  <router-view v-if="route.meta.public" />
  <el-container v-else class="app-shell">
    <el-aside class="sidebar">
      <router-link to="/dashboard" class="brand"><span class="brand-mark">康</span><span><strong>康复医院</strong><small>运营管理系统</small></span></router-link>
      <el-menu router :default-active="route.path" class="nav-menu">
        <el-sub-menu v-for="(group, index) in visibleGroups" :key="group.title" :index="String(index)"><template #title>{{ group.title }}</template><el-menu-item v-for="item in group.items" :key="item[0]" :index="item[0]">{{ item[1] }}</el-menu-item></el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container class="workspace">
      <el-header class="topbar">
        <button class="mobile-menu-button" type="button" aria-label="打开功能导航" @click="mobileMenuOpen = true">☰</button>
        <div class="topbar-title"><span class="crumb">康复医院运营管理系统</span><h1>{{ title }}</h1></div>
        <div class="user-chip"><span>{{ currentUser || '未登录' }}</span><el-button link @click="logout">退出</el-button></div>
      </el-header>
      <el-main class="main-content"><div v-if="!online" class="network-status-banner" role="status">院内网络已断开，未保存内容将保留在当前会话，请恢复网络后重试。</div><router-view /></el-main>
    </el-container>
    <el-drawer v-model="mobileMenuOpen" direction="ltr" size="min(84vw, 320px)" class="mobile-nav-drawer" :with-header="false">
      <router-link to="/dashboard" class="brand mobile-brand"><span class="brand-mark">康</span><span><strong>康复医院</strong><small>运营管理系统</small></span></router-link>
      <el-menu router :default-active="route.path" class="nav-menu" @select="mobileMenuOpen = false">
        <el-sub-menu v-for="(group, index) in visibleGroups" :key="group.title" :index="`mobile-${index}`"><template #title>{{ group.title }}</template><el-menu-item v-for="item in group.items" :key="item[0]" :index="item[0]">{{ item[1] }}</el-menu-item></el-sub-menu>
      </el-menu>
    </el-drawer>
    <div v-if="watermarkText" class="privacy-watermark" aria-hidden="true"><span v-for="index in 18" :key="index">{{ watermarkText }}</span></div>
    <div v-if="privacyCovered" class="privacy-cover" role="status"><strong>内容已保护</strong><span>返回系统后可继续操作</span></div>
  </el-container>
</template>