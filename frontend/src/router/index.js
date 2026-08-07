import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue'), meta: { title: '智慧工作台' } },
      { path: 'medicine', name: 'Medicine', component: () => import('../views/admin/Medicine.vue'), meta: { title: '药品档案管理' } },
      { path: 'prescription', name: 'Prescription', component: () => import('../views/admin/Prescription.vue'), meta: { title: '用药方案管理' } },
      { path: 'purchase', name: 'Purchase', component: () => import('../views/admin/Purchase.vue'), meta: { title: '购药记录管理' } },
      { path: 'approval', name: 'Approval', component: () => import('../views/admin/Approval.vue'), meta: { title: '审批中心' } },
      { path: 'risk-center', name: 'RiskCenter', component: () => import('../views/admin/RiskCenter.vue'), meta: { title: '用药风险中心' } },
      { path: 'family-orders', name: 'AdminFamilyOrders', component: () => import('../views/FamilyOrders.vue'), meta: { title: '代购订单与物流' } },
      { path: 'family-fund', name: 'AdminFamilyFund', component: () => import('../views/FamilyFund.vue'), meta: { title: '购药资金台账' } },
      { path: 'evidence', name: 'EvidenceArchive', component: () => import('../views/admin/EvidenceArchive.vue'), meta: { title: '费用凭证档案' } },
      { path: 'notifications', name: 'AdminNotifications', component: () => import('../views/Notifications.vue'), meta: { title: '消息中心' } },
      { path: 'statistics', name: 'Statistics', component: () => import('../views/admin/Statistics.vue'), meta: { title: '统计报表中心' } },
      { path: 'data-quality', name: 'DataQuality', component: () => import('../views/admin/DataQuality.vue'), meta: { title: '数据质量中心' } },
      { path: 'system', name: 'System', component: () => import('../views/admin/System.vue'), meta: { title: '系统管理' } },
      { path: 'security', name: 'AdminSecurity', component: () => import('../views/Security.vue'), meta: { title: '账号与安全' } },
      { path: 'log', name: 'Log', component: () => import('../views/admin/Log.vue'), meta: { title: '操作日志' } },
      { path: 'export', name: 'Export', component: () => import('../views/admin/Export.vue'), meta: { title: '数据导出中心' } }
    ]
  },
  {
    path: '/elder',
    component: () => import('../layout/ElderLayout.vue'),
    redirect: '/elder/dashboard',
    meta: { requiresAuth: true, role: 'ELDER' },
    children: [
      { path: 'dashboard', name: 'ElderDashboard', component: () => import('../views/elder/Dashboard.vue'), meta: { title: '首页工作台' } },
      { path: 'my-medicine', name: 'MyMedicine', component: () => import('../views/elder/MyMedicine.vue'), meta: { title: '我的用药' } },
      { path: 'today', name: 'TodayMedication', component: () => import('../views/elder/TodayMedication.vue'), meta: { title: '今日用药计划' } },
      { path: 'submit-apply', name: 'SubmitApply', component: () => import('../views/elder/SubmitApply.vue'), meta: { title: '提交用药申请' } },
      { path: 'my-apply', name: 'MyApply', component: () => import('../views/elder/MyApply.vue'), meta: { title: '我的申请记录' } },
      { path: 'orders', name: 'ElderFamilyOrders', component: () => import('../views/FamilyOrders.vue'), meta: { title: '订单与物流' } },
      { path: 'fund', name: 'ElderFamilyFund', component: () => import('../views/FamilyFund.vue'), meta: { title: '购药余额' } },
      { path: 'notifications', name: 'ElderNotifications', component: () => import('../views/Notifications.vue'), meta: { title: '消息中心' } },
      { path: 'security', name: 'ElderSecurity', component: () => import('../views/Security.vue'), meta: { title: '账号与安全' } }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.role && userInfo.role !== to.meta.role) {
    if (userInfo.role === 'ADMIN') next('/')
    else if (userInfo.role === 'ELDER') next('/elder')
    else next('/login')
  } else {
    next()
  }
})

export default router
