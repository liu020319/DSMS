<template>
  <el-container class="app-shell">
    <el-aside v-if="!isMobile" :width="sidebarWidth" class="desktop-aside">
      <AdminNavigation :active-path="$route.path" :pending-count="pendingCount" :notification-count="notificationCount" :collapsed="sidebarCollapsed" />
    </el-aside>

    <el-drawer v-model="mobileNavVisible" direction="ltr" size="82%" :with-header="false" class="mobile-drawer">
      <AdminNavigation :active-path="$route.path" :pending-count="pendingCount" :notification-count="notificationCount" @navigate="mobileNavVisible = false" />
    </el-drawer>

    <el-container class="content-shell">
      <el-header class="app-header">
        <div class="header-left">
          <el-tooltip :content="isMobile ? '打开导航' : (sidebarCollapsed ? '展开导航' : '收起导航')" placement="bottom">
            <button class="icon-action" @click="toggleNavigation"><el-icon><Fold v-if="!sidebarCollapsed && !isMobile" /><Expand v-else /></el-icon></button>
          </el-tooltip>
          <div class="page-context">
            <el-breadcrumb separator="/" class="page-breadcrumb"><el-breadcrumb-item>康联云</el-breadcrumb-item><el-breadcrumb-item>{{ routeGroup }}</el-breadcrumb-item></el-breadcrumb>
            <strong>{{ $route.meta.title || '智慧工作台' }}</strong>
          </div>
        </div>

        <div class="header-actions">
          <button v-if="!isMobile" class="global-search" @click="commandVisible = true"><el-icon><Search /></el-icon><span>搜索功能、药品或业务</span><kbd>Ctrl K</kbd></button>
          <el-tooltip content="消息中心" placement="bottom">
            <el-badge :value="notificationCount" :hidden="!notificationCount" :max="99"><button class="icon-action" @click="$router.push('/notifications')"><el-icon><Bell /></el-icon></button></el-badge>
          </el-tooltip>
          <el-dropdown trigger="click" @command="handleCreateCommand">
            <el-button type="primary" class="create-button"><el-icon><Plus /></el-icon><span v-if="!isMobile">快速新建</span></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="medicine"><el-icon><FirstAidKit /></el-icon>新建药品档案</el-dropdown-item>
                <el-dropdown-item command="prescription"><el-icon><Document /></el-icon>新建用药方案</el-dropdown-item>
                <el-dropdown-item command="purchase"><el-icon><ShoppingCart /></el-icon>登记购药记录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-dropdown trigger="click">
            <div class="user-entry">
              <el-avatar :size="36">{{ userStore.userInfo.realName?.slice(0, 1) || '家' }}</el-avatar>
              <div v-if="!isMobile"><strong>{{ userStore.userInfo.realName || '家庭管理员' }}</strong><small>家庭协同管理员</small></div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/security')"><el-icon><Lock /></el-icon>账号与安全</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="app-main"><router-view /></el-main>
    </el-container>

    <el-dialog v-model="commandVisible" title="全局导航" width="560px" class="command-dialog" :show-close="false">
      <el-input v-model="commandKeyword" size="large" autofocus placeholder="输入功能名称，例如：药品、物流、报表" :prefix-icon="Search" />
      <div class="command-list">
        <button v-for="item in filteredCommands" :key="item.path" @click="goCommand(item.path)">
          <span class="command-icon"><el-icon><component :is="item.icon" /></el-icon></span>
          <span><strong>{{ item.label }}</strong><small>{{ item.group }}</small></span>
          <el-icon class="command-arrow"><Right /></el-icon>
        </button>
        <el-empty v-if="!filteredCommands.length" description="没有匹配的功能" :image-size="56" />
      </div>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserStore } from '../stores/user'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getPendingList } from '../api/approval'
import AdminNavigation from '../components/AdminNavigation.vue'
import { getUnreadNotificationCount } from '../api/family'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const pendingCount = ref(0)
const notificationCount = ref(0)
const isMobile = ref(window.innerWidth <= 900)
const mobileNavVisible = ref(false)
const sidebarCollapsed = ref(localStorage.getItem('adminSidebarCollapsed') === '1')
const commandVisible = ref(false)
const commandKeyword = ref('')
const sidebarWidth = computed(() => sidebarCollapsed.value ? '76px' : '248px')

const commands = [
  { label: '智慧工作台', group: '总览', path: '/dashboard', icon: 'DataBoard' },
  { label: '药品档案', group: '药事协同', path: '/medicine', icon: 'FirstAidKit' },
  { label: '用药方案', group: '药事协同', path: '/prescription', icon: 'Document' },
  { label: '申请审批', group: '药事协同', path: '/approval', icon: 'Stamp' },
  { label: '用药风险中心', group: '药事协同', path: '/risk-center', icon: 'WarningFilled' },
  { label: '购药记录', group: '购药履约', path: '/purchase', icon: 'ShoppingCart' },
  { label: '代购订单与物流', group: '购药履约', path: '/family-orders', icon: 'Van' },
  { label: '购药资金台账', group: '购药履约', path: '/family-fund', icon: 'Wallet' },
  { label: '费用凭证档案', group: '购药履约', path: '/evidence', icon: 'Tickets' },
  { label: '统计分析', group: '运营洞察', path: '/statistics', icon: 'TrendCharts' },
  { label: '消息中心', group: '运营洞察', path: '/notifications', icon: 'Bell' },
  { label: '数据质量', group: '运营洞察', path: '/data-quality', icon: 'DataAnalysis' },
  { label: '家庭与用户', group: '系统治理', path: '/system', icon: 'UserFilled' },
  { label: '操作审计', group: '系统治理', path: '/log', icon: 'Notebook' },
  { label: '数据导出', group: '系统治理', path: '/export', icon: 'Download' }
]
const filteredCommands = computed(() => {
  const keyword = commandKeyword.value.trim().toLowerCase()
  return keyword ? commands.filter(item => `${item.label}${item.group}`.toLowerCase().includes(keyword)) : commands.slice(0, 8)
})
const routeGroup = computed(() => commands.find(item => item.path === route.path)?.group || '家庭协同')

const syncViewport = () => { isMobile.value = window.innerWidth <= 900; if (!isMobile.value) mobileNavVisible.value = false }
const toggleNavigation = () => {
  if (isMobile.value) return (mobileNavVisible.value = true)
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('adminSidebarCollapsed', sidebarCollapsed.value ? '1' : '0')
}
const handleLogout = () => { userStore.logout(); router.push('/login') }
const goCommand = path => { commandVisible.value = false; commandKeyword.value = ''; router.push(path) }
const handleCreateCommand = command => router.push({ path: `/${command}`, query: { action: 'create', at: Date.now() } })
const onShortcut = event => { if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); commandVisible.value = !commandVisible.value } }
let notificationTimer = null
const refreshHeaderCounts = async () => {
  try { const res = await getPendingList({ current: 1, size: 1 }); pendingCount.value = res.data.total || 0 } catch (e) {}
  try { const res = await getUnreadNotificationCount(); notificationCount.value = res.data || 0 } catch (e) {}
}

onMounted(async () => {
  window.addEventListener('resize', syncViewport)
  window.addEventListener('keydown', onShortcut)
  window.addEventListener('dsms-notification-read', refreshHeaderCounts)
  await refreshHeaderCounts()
  notificationTimer = window.setInterval(refreshHeaderCounts, 30000)
})
onBeforeUnmount(() => { window.removeEventListener('resize', syncViewport); window.removeEventListener('keydown', onShortcut); window.removeEventListener('dsms-notification-read', refreshHeaderCounts); if(notificationTimer)window.clearInterval(notificationTimer) })
</script>

<style scoped>
.app-shell { height: 100vh; height: 100dvh; background: #f3f6f7; }.desktop-aside { overflow: hidden; background: #102d35; transition: width .22s ease; }.content-shell { min-width: 0; }
.app-header { height: 72px; display: flex; align-items: center; justify-content: space-between; padding: 0 22px; background: rgba(255,255,255,.95); border-bottom: 1px solid #e6ebed; box-shadow: 0 4px 18px rgba(20,49,58,.04); backdrop-filter: blur(16px); z-index: 3; }
.header-left,.header-actions,.user-entry { display: flex; align-items: center; }.header-left { gap: 13px; min-width: 0; }.header-actions { gap: 10px; }.icon-action { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid #e4eaec; border-radius: 11px; color: #456169; background: #fff; cursor: pointer; font-size: 18px; }.icon-action:hover { color: #087f68; border-color: #b9ddd4; background: #f1faf7; }
.page-context { min-width: 0; }.page-context strong { display: block; margin-top: 4px; color: #14343d; font-size: 18px; }.page-breadcrumb { font-size: 11px; }
.global-search { width: 278px; height: 38px; display: flex; align-items: center; gap: 8px; padding: 0 9px 0 13px; border: 1px solid #e1e8ea; border-radius: 11px; color: #87969a; background: #f8fafb; cursor: pointer; }.global-search span { flex: 1; text-align: left; }.global-search kbd { padding: 3px 6px; border: 1px solid #dce4e6; border-radius: 5px; color: #708286; background: #fff; font: 10px/1 sans-serif; }
.create-button { height: 38px; border-radius: 10px; background: linear-gradient(135deg,#10a879,#087f7a); border: 0; box-shadow: 0 7px 18px rgba(16,168,121,.18); }
.user-entry { gap: 9px; margin-left: 2px; color: #36535b; cursor: pointer; }.user-entry .el-avatar { color: #fff; background: linear-gradient(135deg,#21b982,#2e8798); }.user-entry strong,.user-entry small { display: block; white-space: nowrap; }.user-entry strong { font-size: 13px; }.user-entry small { margin-top: 2px; color: #94a1a4; font-size: 10px; }
.app-main { overflow-y: auto; padding: 24px; background: radial-gradient(circle at 90% 0, rgba(28,174,137,.055), transparent 24%), #f3f6f7; }
:deep(.mobile-drawer) { height: 100%; height: 100dvh; overflow: hidden; }
:deep(.mobile-drawer .el-drawer__body) { height: 100%; min-height: 0; overflow: hidden; padding: 0; background: #102d35; }
.command-list { max-height: 430px; overflow-y: auto; margin-top: 14px; }.command-list button { width: 100%; display: flex; align-items: center; gap: 12px; padding: 11px; margin-bottom: 6px; border: 0; border-radius: 11px; color: #28464e; background: transparent; cursor: pointer; text-align: left; }.command-list button:hover { background: #f0f8f5; }.command-icon { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 9px; color: #0b8d70; background: #e9f7f2; }.command-list strong,.command-list small { display: block; }.command-list strong { font-size: 14px; }.command-list small { margin-top: 2px; color: #96a3a6; font-size: 11px; }.command-arrow { margin-left: auto; color: #aab4b6; }
@media (max-width: 900px) { .app-header { height: 60px; padding: 0 11px; }.page-breadcrumb { display: none; }.page-context strong { max-width: 140px; margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 16px; }.header-left { gap: 6px; }.header-actions { gap: 6px; }.app-main { padding: 12px; }.user-entry { gap: 5px; }.create-button { width: 38px; padding: 0; }.icon-action { width: 36px; height: 36px; } }
</style>
