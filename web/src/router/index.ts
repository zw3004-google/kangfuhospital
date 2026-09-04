import { createRouter, createWebHistory, type LocationQuery, type RouteRecordRaw } from 'vue-router'
import { loadPermissions, hasPermission } from '../auth'

const view = (name: string) => () => import(`../views/${name}.vue`)
export const legacyDischargeRedirect = (section: 'consultation'|'follow-up', query: LocationQuery) => ({
  path: '/discharge/management',
  query: { ...query, section },
})

export const routes: RouteRecordRaw[] = [
  { path: '/login', component: view('LoginView'), meta: { public: true, title: '登录' } },
  { path: '/forbidden', component: view('ForbiddenView'), meta: { public: true, title: '无权限' } },
  { path: '/system/change-password', component: view('ChangePasswordView'), meta: { title: '修改密码' } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', component: view('DashboardView'), meta: { title: '工作台' } },
  { path: '/arrears/details', component: view('ArrearsDetailsView'), meta: { title: '欠费明细', permission: 'PERM_API_ARREARS_REPORT' } },
  { path: '/arrears/report', component: view('ArrearsReportView'), meta: { title: '通报报表', permission: 'PERM_API_ARREARS_REPORT' } },
  { path: '/arrears/import-batches', component: view('ImportBatchView'), meta: { title: '欠费导入记录', businessType: 'ARREARS', permission: 'PERM_API_ARREARS_IMPORT' } },
  { path: '/arrears/push-records', component: view('ArrearsPushRecordsView'), meta: { title: '欠费推送记录', businessType: 'ARREARS', permission: 'PERM_API_PUSH_RECORD_VIEW' } },
  { path: '/discharge/management', component: view('DischargeManagementView'), meta: { title: '预计出院管理', permission: 'PERM_API_DISCHARGE_ANALYSIS' } },
  { path: '/discharge/analysis', component: view('DischargeAnalysisView'), meta: { title: '统计分析', permission: 'PERM_API_DISCHARGE_ANALYSIS' } },
  { path: '/discharge/consultations', redirect: to => legacyDischargeRedirect('consultation', to.query) },
  { path: '/discharge/follow-up', redirect: to => legacyDischargeRedirect('follow-up', to.query) },
  { path: '/discharge/import-batches', component: view('ImportBatchView'), meta: { title: '预出院导入记录', businessType: 'DISCHARGE', permission: 'PERM_API_DISCHARGE_IMPORT' } },
  { path: '/discharge/push-records', component: view('ArrearsPushRecordsView'), meta: { title: '预出院推送记录', businessType: 'DISCHARGE', permission: 'PERM_API_PUSH_RECORD_VIEW' } },
  { path: '/system/fee-coefficients', component: view('FeeCoefficientView'), meta: { title: '费别系数配置', permission: 'PERM_API_FEE_CONFIG' } },
  { path: '/system/users', component: view('UserManagementView'), meta: { title: '用户管理', permission: 'PERM_API_USER_MANAGE' } },
  { path: '/system/roles', component: view('RoleManagementView'), meta: { title: '权限管理', permission: 'PERM_API_ROLE_MANAGE' } },
  { path: '/system/audit-logs', component: view('AuditLogView'), meta: { title: '审计日志', permission: 'PERM_API_AUDIT_VIEW' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})
router.beforeEach(async to => { if(to.meta.public) return true; const permissions=await loadPermissions(); if(permissions.size===0) return '/login'; const p=to.meta.permission as string|undefined; if(p&&!hasPermission(p)) return '/forbidden'; return true })
export default router
