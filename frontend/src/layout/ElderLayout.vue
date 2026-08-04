<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background-color: #5B8C5A; overflow-y: auto">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; border-bottom: 1px solid #6da36c">
        💊 用药管理
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#5B8C5A"
        text-color="#e8f5e9"
        active-text-color="#fff"
        router
        style="font-size: 18px"
      >
        <el-menu-item index="/elder/dashboard" style="height: 60px; line-height: 60px; font-size: 18px">
          <el-icon style="font-size: 22px"><HomeFilled /></el-icon>
          <span>首页工作台</span>
        </el-menu-item>
        <el-menu-item index="/elder/my-medicine" style="height: 60px; line-height: 60px; font-size: 18px">
          <el-icon style="font-size: 22px"><FirstAidKit /></el-icon>
          <span>我的用药</span>
        </el-menu-item>
        <el-menu-item index="/elder/submit-apply" style="height: 60px; line-height: 60px; font-size: 18px">
          <el-icon style="font-size: 22px"><EditPen /></el-icon>
          <span>提交用药申请</span>
        </el-menu-item>
        <el-menu-item index="/elder/my-apply" style="height: 60px; line-height: 60px; font-size: 18px">
          <el-icon style="font-size: 22px"><Document /></el-icon>
          <span>我的申请记录</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; display: flex; align-items: center; justify-content: space-between; padding: 0 20px">
        <span style="font-size: 22px; font-weight: bold; color: #333">{{ $route.meta.title || '首页' }}</span>
        <div style="display: flex; align-items: center; gap: 15px">
          <el-tag type="warning" size="large">老人端</el-tag>
          <span style="font-size: 18px">{{ userStore.userInfo.realName }}</span>
          <el-button type="danger" size="large" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main style="background: #f5f9f5; padding: 20px; overflow-y: auto; font-size: 18px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.el-aside::-webkit-scrollbar {
  width: 0;
}
.el-menu {
  border-right: none;
}
</style>
