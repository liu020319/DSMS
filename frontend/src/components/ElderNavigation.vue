<template>
  <div class="nav-shell">
    <div class="nav-brand">
      <div class="brand-mark"><el-icon><FirstAidKit /></el-icon></div>
      <div><strong>康联云</strong><span>安心用药端</span></div>
    </div>
    <el-menu :default-active="activePath" router @select="$emit('navigate')">
      <el-menu-item index="/elder/dashboard"><el-icon><HomeFilled /></el-icon><span>首页</span></el-menu-item>
      <el-menu-item index="/elder/today"><el-icon><Calendar /></el-icon><span>今日用药</span></el-menu-item>
      <el-menu-item index="/elder/my-medicine"><el-icon><FirstAidKit /></el-icon><span>我的药品</span></el-menu-item>
      <el-menu-item index="/elder/submit-apply"><el-icon><EditPen /></el-icon><span>提交申请</span></el-menu-item>
      <el-menu-item index="/elder/my-apply"><el-icon><Document /></el-icon><span>申请记录</span></el-menu-item>
      <el-menu-item index="/elder/orders"><el-icon><Van /></el-icon><span>订单与物流</span></el-menu-item>
      <el-menu-item index="/elder/fund"><el-icon><Wallet /></el-icon><span>购药余额</span></el-menu-item>
      <el-menu-item index="/elder/notifications"><el-icon><Bell /></el-icon><span>消息中心</span><el-badge v-if="notificationCount > 0" :value="notificationCount" class="nav-badge" /></el-menu-item>
      <el-menu-item index="/elder/security"><el-icon><Lock /></el-icon><span>账号与安全</span></el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
defineProps({ activePath: { type: String, default: '' }, notificationCount: { type: Number, default: 0 } })
defineEmits(['navigate'])
</script>

<style scoped>
.nav-shell { height: 100%; color: #eef7ee; background: linear-gradient(180deg, #416f4c, #31583b); }
.nav-brand { height: 76px; display: flex; align-items: center; gap: 12px; padding: 0 18px; border-bottom: 1px solid rgba(255,255,255,.1); }
.brand-mark { width: 42px; height: 42px; display: grid; place-items: center; border-radius: 13px; background: rgba(255,255,255,.15); color: #fff; font-size: 23px; }
.nav-brand strong,.nav-brand span { display: block; }.nav-brand strong { font-size: 19px; }.nav-brand span { margin-top: 3px; color: #c8ddca; font-size: 12px; letter-spacing: 2px; }
.el-menu { border: 0; background: transparent; padding: 12px 10px; }
.el-menu-item { height: 56px; margin: 5px 0; border-radius: 12px; color: #e2efe3; font-size: 17px; }
.el-menu-item .el-icon { font-size: 21px; }.el-menu-item:hover { color: #fff; background: rgba(255,255,255,.09); }.el-menu-item.is-active { color: #31583b; background: #fff; font-weight: 700; }
.nav-badge { margin-left: auto; }
</style>
