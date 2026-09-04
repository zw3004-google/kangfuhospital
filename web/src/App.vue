<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { clearPermissions, hasPermission, loadPermissions } from './auth'
import { http } from './api/http'
let idleTimer:number|undefined
const IDLE_LIMIT=30*60*1000
const resetIdle=()=>{if(idleTimer)window.clearTimeout(idleTimer);if(sessionStorage.getItem('basicAuth'))idleTimer=window.setTimeout(()=>{sessionStorage.removeItem('basicAuth');clearPermissions();location.href='/login?timeout=1'},IDLE_LIMIT)}
;['click','keydown','mousemove','touchstart'].forEach(e=>window.addEventListener(e,resetIdle,{passive:true}));resetIdle()

const route = useRoute()
const permissionReady = ref(0)
const currentUser = ref('')
const logout=()=>{sessionStorage.removeItem('basicAuth');clearPermissions();location.href='/login'}
const title = computed(() => String(route.meta.title ?? '工作台'))
const menuPermissions: Record<string,string> = {'/arrears/details':'PERM_API_ARREARS_REPORT','/arrears/report':'PERM_API_ARREARS_REPORT','/arrears/import-batches':'PERM_API_ARREARS_IMPORT','/arrears/push-records':'PERM_API_PUSH_RECORD_VIEW','/discharge/management':'PERM_API_DISCHARGE_ANALYSIS','/discharge/analysis':'PERM_API_DISCHARGE_ANALYSIS','/discharge/import-batches':'PERM_API_DISCHARGE_IMPORT','/discharge/push-records':'PERM_API_PUSH_RECORD_VIEW','/system/fee-coefficients':'PERM_API_FEE_CONFIG','/system/users':'PERM_API_USER_MANAGE','/system/roles':'PERM_API_ROLE_MANAGE','/system/audit-logs':'PERM_API_AUDIT_VIEW'}
const canSee = (path:string) => { void permissionReady.value; const p=menuPermissions[path]; return !p || hasPermission(p) }
const visibleGroups = computed(() => menuGroups.map(g=>({...g,items:g.items.filter(i=>canSee(i[0]))})).filter(g=>g.items.length))

const refreshShell = async () => {
  if (route.meta.public) return
  await loadPermissions()
  permissionReady.value++
  try {
    const response = await http.get<{data:{loginName:string}}>('/system/me')
    currentUser.value = response.data.data.loginName
  } catch {
    currentUser.value = ''
  }
}

watch(() => route.path, refreshShell, { immediate: true })

const menuGroups = [
  {
    title: '欠费管理',
    items: [
      ['/arrears/details', '欠费明细'],
      ['/arrears/report', '通报报表'],
      ['/arrears/import-batches', '导入记录'],
      ['/arrears/push-records', '推送记录'],
    ],
  },
  {
    title: '预出院管理',
    items: [
      ['/discharge/management', '预计出院管理'],
      ['/discharge/analysis', '统计分析'],
      ['/discharge/import-batches', '导入记录'],
      ['/discharge/push-records', '推送记录'],
    ],
  },
  {
    title: '系统管理',
    items: [
      ['/system/fee-coefficients', '费别系数配置'],
      ['/system/users', '用户管理'],
      ['/system/roles', '权限管理'],
      ['/system/audit-logs', '审计日志'],
    ],
  },
] as const
</script>

<template>
  <router-view v-if="route.meta.public" />
  <el-container v-else class="app-shell">
    <el-aside width="236px" class="sidebar">
      <router-link to="/dashboard" class="brand">
        <span class="brand-mark">康</span>
        <span><strong>康复医院</strong><small>运营管理系统</small></span>
      </router-link>
      <el-menu router :default-active="route.path" class="nav-menu">
        <el-sub-menu v-for="(group, index) in visibleGroups" :key="group.title" :index="String(index)">
          <template #title>{{ group.title }}</template>
          <template v-for="item in group.items" :key="item[0]"><el-menu-item :index="item[0]">
            {{ item[1] }}
          </el-menu-item></template>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div>
          <span class="crumb">康复医院运营管理系统</span>
          <h1>{{ title }}</h1>
        </div>
        <div class="user-chip">{{ currentUser || '未登录' }} <el-button link @click="logout">退出</el-button></div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>
