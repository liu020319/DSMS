<template>
  <el-container class="app-shell">
    <el-aside v-if="!isMobile" width="232px" class="desktop-aside">
      <AdminNavigation :active-path="$route.path" :pending-count="pendingCount" :notification-count="notificationCount" />
    </el-aside>

    <el-drawer v-model="mobileNavVisible" direction="ltr" size="82%" :with-header="false" class="mobile-drawer">
      <AdminNavigation :active-path="$route.path" :pending-count="pendingCount" :notification-count="notificationCount" @navigate="mobileNavVisible = false" />
    </el-drawer>

    <el-container class="content-shell">
      <el-header class="app-header">
        <div class="header-main">
          <el-button v-if="isMobile" text class="menu-button" @click="mobileNavVisible = true"><el-icon><Menu /></el-icon></el-button>
          <div class="page-heading">
            <span>{{ $route.meta.title || '首页工作台' }}</span>
            <small v-if="!isMobile">安康药管家 · 家庭守护端</small>
          </div>
          <el-dropdown trigger="click">
            <div class="user-entry">
              <el-avatar :size="34">{{ userStore.userInfo.realName?.slice(0, 1) || '家' }}</el-avatar>
              <span v-if="!isMobile">{{ userStore.userInfo.realName }}</span>
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
        <div v-if="!isMobile" class="quick-nav-bar">
          <button @click="$router.push('/medicine')"><el-icon><FirstAidKit /></el-icon>药品档案</button>
          <button @click="$router.push('/prescription')"><el-icon><Document /></el-icon>用药方案</button>
          <button @click="$router.push('/purchase')"><el-icon><ShoppingCart /></el-icon>购药记录</button>
          <button @click="$router.push('/approval')"><el-icon><Stamp /></el-icon>审批中心<el-badge v-if="pendingCount" :value="pendingCount" /></button>
        </div>
      </el-header>
      <el-main class="app-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { getPendingList } from '../api/approval'
import AdminNavigation from '../components/AdminNavigation.vue'
import { getUnreadNotificationCount } from '../api/family'

const userStore = useUserStore()
const router = useRouter()
const pendingCount = ref(0)
const notificationCount = ref(0)
const isMobile = ref(window.innerWidth <= 900)
const mobileNavVisible = ref(false)
const syncViewport = () => { isMobile.value = window.innerWidth <= 900; if (!isMobile.value) mobileNavVisible.value = false }
const handleLogout = () => { userStore.logout(); router.push('/login') }

onMounted(async () => {
  window.addEventListener('resize', syncViewport)
  try { const res = await getPendingList({ current: 1, size: 1 }); pendingCount.value = res.data.total || 0 } catch (e) {}
  try { const res = await getUnreadNotificationCount(); notificationCount.value = res.data || 0 } catch (e) {}
})
onBeforeUnmount(() => window.removeEventListener('resize', syncViewport))
</script>

<style scoped>
.app-shell { height: 100vh; background: #f2f5f4; }.desktop-aside { overflow-y: auto; background: #16343d; }.desktop-aside::-webkit-scrollbar { width: 0; }.content-shell { min-width: 0; }
.app-header { height: auto; padding: 0; background: rgba(255,255,255,.96); border-bottom: 1px solid #e5ebe8; box-shadow: 0 2px 12px rgba(24,53,61,.04); z-index: 3; }
.header-main { height: 64px; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; }.page-heading span,.page-heading small { display: block; }.page-heading span { color: #173640; font-size: 19px; font-weight: 750; }.page-heading small { margin-top: 3px; color: #98a5a1; font-size: 11px; letter-spacing: 1px; }
.user-entry { display: flex; align-items: center; gap: 9px; color: #314a51; cursor: pointer; }.user-entry .el-avatar { color: #fff; background: linear-gradient(135deg,#32b67a,#3a8d8d); }.menu-button { margin-right: 8px; font-size: 23px; }.page-heading { margin-right: auto; }
.quick-nav-bar { height: 43px; display: flex; gap: 8px; align-items: center; padding: 0 24px; border-top: 1px solid #f0f3f2; }.quick-nav-bar button { display: flex; align-items: center; gap: 6px; padding: 7px 13px; border: 0; border-radius: 9px; color: #476169; background: #f1f6f4; cursor: pointer; }.quick-nav-bar button:hover { color: #167458; background: #e4f3ed; }
.app-main { overflow-y: auto; padding: 22px; background: #f2f5f4; }
:deep(.mobile-drawer .el-drawer__body) { padding: 0; background: #16343d; }
@media (max-width: 900px) { .header-main { height: 56px; padding: 0 12px; }.page-heading span { max-width: 190px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 17px; }.app-main { padding: 12px; }.user-entry { gap: 5px; } }
</style>
