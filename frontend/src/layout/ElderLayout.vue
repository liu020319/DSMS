<template>
  <el-container class="app-shell elder-shell">
    <el-aside v-if="!isMobile" width="232px" class="desktop-aside"><ElderNavigation :active-path="$route.path" :notification-count="notificationCount" /></el-aside>
    <el-drawer v-model="mobileNavVisible" direction="ltr" size="82%" :with-header="false" class="elder-drawer">
      <ElderNavigation :active-path="$route.path" :notification-count="notificationCount" @navigate="mobileNavVisible = false" />
    </el-drawer>
    <el-container class="content-shell">
      <el-header class="app-header">
        <el-button v-if="isMobile" text class="menu-button" @click="mobileNavVisible = true"><el-icon><Menu /></el-icon></el-button>
        <div class="page-heading">
          <span>{{ $route.meta.title || '首页' }}</span>
          <small v-if="!isMobile">康联云 · 安心用药端</small>
        </div>
        <el-dropdown trigger="click">
          <div class="user-entry">
            <el-avatar :size="38">{{ userStore.userInfo.realName?.slice(0, 1) || '安' }}</el-avatar>
            <span v-if="!isMobile">{{ userStore.userInfo.realName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/elder/security')"><el-icon><Lock /></el-icon>修改密码</el-dropdown-item>
              <el-dropdown-item divided @click="handleLogout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="app-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import ElderNavigation from '../components/ElderNavigation.vue'
import { getUnreadNotificationCount } from '../api/family'

const userStore = useUserStore()
const router = useRouter()
const isMobile = ref(window.innerWidth <= 900)
const mobileNavVisible = ref(false)
const notificationCount = ref(0)
const syncViewport = () => { isMobile.value = window.innerWidth <= 900; if (!isMobile.value) mobileNavVisible.value = false }
const handleLogout = () => { userStore.logout(); router.push('/login') }
onMounted(async () => { window.addEventListener('resize', syncViewport); try { const res = await getUnreadNotificationCount(); notificationCount.value = res.data || 0 } catch (e) {} })
onBeforeUnmount(() => window.removeEventListener('resize', syncViewport))
</script>

<style scoped>
.app-shell { height: 100vh; background: #f4f8f4; }.desktop-aside { overflow-y: auto; background: #416f4c; }.desktop-aside::-webkit-scrollbar { width: 0; }.content-shell { min-width: 0; }
.app-header { height: 68px; display: flex; align-items: center; gap: 10px; padding: 0 24px; background: rgba(255,255,255,.97); border-bottom: 1px solid #e2ebe3; box-shadow: 0 2px 14px rgba(49,88,59,.05); z-index: 3; }
.page-heading { margin-right: auto; }.page-heading span,.page-heading small { display: block; }.page-heading span { color: #31583b; font-size: 21px; font-weight: 750; }.page-heading small { margin-top: 3px; color: #91a596; font-size: 12px; }
.user-entry { display: flex; align-items: center; gap: 9px; color: #3f5c46; font-size: 17px; cursor: pointer; }.user-entry .el-avatar { color: #fff; background: linear-gradient(135deg,#6d9f71,#416f4c); }.menu-button { font-size: 24px; }
.app-main { overflow-y: auto; padding: 22px; color: #33473a; background: #f4f8f4; font-size: 17px; }
:deep(.elder-drawer .el-drawer__body) { padding: 0; background: #416f4c; }
@media (max-width: 900px) { .app-header { height: 58px; padding: 0 12px; }.page-heading span { max-width: 190px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 18px; }.user-entry { gap: 5px; }.app-main { padding: 12px; font-size: 16px; } }
</style>
