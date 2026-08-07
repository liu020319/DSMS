<template>
  <div class="nav-shell">
    <div class="nav-brand">
      <div class="brand-mark"><el-icon><FirstAidKit /></el-icon></div>
      <div><strong>安康药管家</strong><span>家庭守护端</span></div>
    </div>
    <el-menu :default-active="activePath" router @select="$emit('navigate')">
      <el-menu-item index="/dashboard"><el-icon><HomeFilled /></el-icon><span>首页工作台</span></el-menu-item>
      <el-menu-item index="/medicine"><el-icon><FirstAidKit /></el-icon><span>药品档案</span></el-menu-item>
      <el-menu-item index="/prescription"><el-icon><Document /></el-icon><span>用药方案</span></el-menu-item>
      <el-menu-item index="/purchase"><el-icon><ShoppingCart /></el-icon><span>购药记录</span></el-menu-item>
      <el-menu-item index="/approval">
        <el-icon><Stamp /></el-icon><span>审批中心</span>
        <el-badge v-if="pendingCount > 0" :value="pendingCount" :max="99" class="nav-badge" />
      </el-menu-item>
      <el-menu-item index="/family-orders"><el-icon><Van /></el-icon><span>代购订单与物流</span></el-menu-item>
      <el-menu-item index="/family-fund"><el-icon><Wallet /></el-icon><span>购药资金台账</span></el-menu-item>
      <el-menu-item index="/notifications"><el-icon><Bell /></el-icon><span>消息中心</span><el-badge v-if="notificationCount > 0" :value="notificationCount" :max="99" class="nav-badge" /></el-menu-item>
      <el-menu-item index="/statistics"><el-icon><TrendCharts /></el-icon><span>统计报表</span></el-menu-item>
      <el-menu-item index="/system"><el-icon><Setting /></el-icon><span>家庭与用户</span></el-menu-item>
      <el-menu-item index="/security"><el-icon><Lock /></el-icon><span>账号与安全</span></el-menu-item>
      <el-menu-item index="/log"><el-icon><Notebook /></el-icon><span>操作日志</span></el-menu-item>
      <el-menu-item index="/export"><el-icon><Download /></el-icon><span>数据导出</span></el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
defineProps({ activePath: { type: String, default: '' }, pendingCount: { type: Number, default: 0 }, notificationCount: { type: Number, default: 0 } })
defineEmits(['navigate'])
</script>

<style scoped>
.nav-shell { height: 100%; color: #d7e3e6; background: linear-gradient(180deg, #16343d, #102a32); }
.nav-brand { height: 72px; display: flex; align-items: center; gap: 12px; padding: 0 18px; border-bottom: 1px solid rgba(255,255,255,.08); }
.brand-mark { width: 38px; height: 38px; display: grid; place-items: center; border-radius: 11px; color: #fff; background: linear-gradient(135deg, #32b67a, #3a8d8d); font-size: 20px; }
.nav-brand strong,.nav-brand span { display: block; }.nav-brand strong { color: #fff; font-size: 16px; }.nav-brand span { margin-top: 3px; color: #8facb3; font-size: 11px; letter-spacing: 2px; }
.el-menu { border: 0; background: transparent; padding: 10px; }
.el-menu-item { height: 48px; margin: 4px 0; border-radius: 10px; color: #b8cbd0; }
.el-menu-item:hover { color: #fff; background: rgba(255,255,255,.07); }
.el-menu-item.is-active { color: #fff; background: linear-gradient(135deg, rgba(50,182,122,.95), rgba(58,141,141,.9)); }
.nav-badge { margin-left: auto; }
</style>
