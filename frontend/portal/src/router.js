import { createRouter, createWebHashHistory } from 'vue-router'
const router = createRouter({
  history: createWebHashHistory(),
  scrollBehavior() {
    return { left: 0, top: 0 }
  },
  routes: [
    { path: '/login', component: () => import('./views/Login.vue'), meta: { loginPage: true, title: '安全登录' } },
    { path: '/register', component: () => import('./views/Register.vue'), meta: { publicPage: true, title: '朋友账号注册' } },
    { path: '/', component: () => import('./views/PortalHome.vue'), meta: { publicPage: true, title: '数字门户' } },
    { path: '/services', component: () => import('./views/PublicServices.vue'), meta: { publicPage: true, title: '毕业设计指导与软件服务' } },
    { path: '/finance', component: () => import('./views/Finance.vue'), meta: { title: '个人记账' } },
    { path: '/service-workspace', component: () => import('./views/Services.vue'), meta: { title: '服务交付工作台' } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(to => {
  document.title = `${to.meta.title || '数字工作台'} · 小刘云`
  if (!to.meta.publicPage && !to.meta.loginPage && !localStorage.getItem('token')) {
    return `/login?redirect=${encodeURIComponent(to.fullPath)}`
  }
  if (to.path === '/login' && localStorage.getItem('token')) return '/'
})

export default router
