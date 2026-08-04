<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background-color: #304156; overflow-y: auto">
      <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: bold; border-bottom: 1px solid #3a4a5d">
        用药安全管理系统
      </div>
      <el-menu
        :default-active="$route.path"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页工作台</span>
        </el-menu-item>
        <el-menu-item index="/medicine">
          <el-icon><FirstAidKit /></el-icon>
          <span>药品档案管理</span>
        </el-menu-item>
        <el-menu-item index="/prescription">
          <el-icon><Document /></el-icon>
          <span>用药方案管理</span>
        </el-menu-item>
        <el-menu-item index="/purchase">
          <el-icon><ShoppingCart /></el-icon>
          <span>购药记录管理</span>
        </el-menu-item>
        <el-menu-item index="/approval">
          <el-icon><Stamp /></el-icon>
          <span>审批中心</span>
          <el-badge v-if="pendingCount > 0" :value="pendingCount" :max="99" style="margin-left: 8px" />
        </el-menu-item>
        <el-menu-item index="/statistics">
          <el-icon><TrendCharts /></el-icon>
          <span>统计报表中心</span>
        </el-menu-item>
        <el-menu-item index="/system">
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </el-menu-item>
        <el-menu-item index="/log">
          <el-icon><Notebook /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item index="/export">
          <el-icon><Download /></el-icon>
          <span>数据导出中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #e6e6e6; padding: 0 20px; height: auto; min-height: 50px">
        <div style="display: flex; align-items: center; justify-content: space-between; height: 50px">
          <span style="font-size: 18px; font-weight: bold; color: #333">{{ $route.meta.title || '首页工作台' }}</span>
          <div style="display: flex; align-items: center; gap: 15px">
            <el-tag type="success">子女端</el-tag>
            <span>{{ userStore.userInfo.realName }}</span>
            <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
          </div>
        </div>
        <div class="quick-nav-bar">
          <div class="quick-nav-item nav-medicine" @click="$router.push('/medicine')">
            <el-icon><FirstAidKit /></el-icon>
            <span>药品档案</span>
          </div>
          <div class="quick-nav-item nav-prescription" @click="$router.push('/prescription')">
            <el-icon><Document /></el-icon>
            <span>用药方案</span>
          </div>
          <div class="quick-nav-item nav-purchase" @click="$router.push('/purchase')">
            <el-icon><ShoppingCart /></el-icon>
            <span>购药记录</span>
          </div>
          <div class="quick-nav-item nav-approval" @click="$router.push('/approval')">
            <el-icon><Stamp /></el-icon>
            <span>审批中心</span>
            <el-badge v-if="pendingCount > 0" :value="pendingCount" :max="99" class="quick-badge" />
          </div>
          <div class="quick-nav-item nav-statistics" @click="$router.push('/statistics')">
            <el-icon><TrendCharts /></el-icon>
            <span>统计报表</span>
          </div>
        </div>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px; overflow-y: auto">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'
import { getPendingList } from '../api/approval'

const userStore = useUserStore()
const router = useRouter()
const pendingCount = ref(0)

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(async () => {
  try {
    const res = await getPendingList({ current: 1, size: 1 })
    pendingCount.value = res.data.total || 0
  } catch (e) {}
})
</script>

<style scoped>
.el-aside::-webkit-scrollbar {
  width: 0;
}
.el-menu {
  border-right: none;
}
.el-menu-item {
  height: 50px;
  line-height: 50px;
}
.quick-nav-bar {
  display: flex;
  gap: 8px;
  border-top: 1px solid #f0f0f0;
  padding: 8px 0;
  margin: 0 -20px;
  padding-left: 20px;
  padding-right: 20px;
}
.quick-nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
  border-radius: 6px;
  white-space: nowrap;
}
.quick-nav-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  filter: brightness(1.1);
}
.quick-nav-item .el-icon {
  font-size: 16px;
}
.nav-medicine {
  background: linear-gradient(135deg, #409EFF, #337ecc);
  box-shadow: 0 2px 6px rgba(64, 158, 255, 0.3);
}
.nav-prescription {
  background: linear-gradient(135deg, #67C23A, #529b2e);
  box-shadow: 0 2px 6px rgba(103, 194, 58, 0.3);
}
.nav-purchase {
  background: linear-gradient(135deg, #E6A23C, #cf8e24);
  box-shadow: 0 2px 6px rgba(230, 162, 60, 0.3);
}
.nav-approval {
  background: linear-gradient(135deg, #F56C6C, #dd5a5a);
  box-shadow: 0 2px 6px rgba(245, 108, 108, 0.3);
}
.nav-statistics {
  background: linear-gradient(135deg, #909399, #73767a);
  box-shadow: 0 2px 6px rgba(144, 147, 153, 0.3);
}
.quick-badge {
  position: absolute;
  top: -4px;
  right: -4px;
}
</style>
