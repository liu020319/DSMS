<template>
  <div class="nav-shell" :class="{ collapsed }">
    <div class="nav-brand">
      <div class="brand-mark"><span class="brand-pulse"></span><el-icon><Connection /></el-icon></div>
      <div v-show="!collapsed" class="brand-copy"><strong>康联云</strong><span>家庭药事协同平台</span></div>
    </div>

    <div v-show="!collapsed" class="workspace-label">家庭协同端</div>
    <el-menu
      :default-active="activePath"
      :default-openeds="defaultOpened"
      :collapse="collapsed"
      :collapse-transition="false"
      router
      @select="$emit('navigate')"
    >
      <el-menu-item index="/dashboard">
        <el-icon><DataBoard /></el-icon><template #title>智慧工作台</template>
      </el-menu-item>

      <el-sub-menu index="medication">
        <template #title><el-icon><FirstAidKit /></el-icon><span>药事协同</span></template>
        <el-menu-item index="/medicine">药品档案</el-menu-item>
        <el-menu-item index="/prescription">用药方案</el-menu-item>
        <el-menu-item index="/approval">
          <span>申请审批</span><el-badge v-if="pendingCount > 0" :value="pendingCount" :max="99" class="nav-badge" />
        </el-menu-item>
        <el-menu-item index="/risk-center">用药风险中心</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="fulfillment">
        <template #title><el-icon><ShoppingCart /></el-icon><span>购药履约</span></template>
        <el-menu-item index="/purchase">购药记录</el-menu-item>
        <el-menu-item index="/family-orders">代购订单与物流</el-menu-item>
        <el-menu-item index="/family-fund">购药资金台账</el-menu-item>
        <el-menu-item index="/evidence">费用凭证档案</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="insight">
        <template #title><el-icon><TrendCharts /></el-icon><span>运营洞察</span></template>
        <el-menu-item index="/statistics">统计分析</el-menu-item>
        <el-menu-item index="/notifications">
          <span>消息中心</span><el-badge v-if="notificationCount > 0" :value="notificationCount" :max="99" class="nav-badge" />
        </el-menu-item>
        <el-menu-item index="/data-quality">数据质量</el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="governance">
        <template #title><el-icon><Management /></el-icon><span>系统治理</span></template>
        <el-menu-item index="/system">家庭与用户</el-menu-item>
        <el-menu-item index="/security">账号与安全</el-menu-item>
        <el-menu-item index="/log">操作审计</el-menu-item>
        <el-menu-item index="/export">数据导出</el-menu-item>
      </el-sub-menu>
    </el-menu>

    <div v-if="!collapsed" class="nav-footer">
      <span class="status-dot"></span>
      <div><strong>系统运行正常</strong><small>数据安全通道已连接</small></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  activePath: { type: String, default: '' },
  pendingCount: { type: Number, default: 0 },
  notificationCount: { type: Number, default: 0 },
  collapsed: { type: Boolean, default: false }
})
defineEmits(['navigate'])
const defaultOpened = ['medication', 'fulfillment']
</script>

<style scoped>
.nav-shell { position: relative; height: 100%; min-height: 0; display: flex; flex-direction: column; overflow: hidden; color: #dce9e9; background: linear-gradient(180deg, #102d35 0%, #0b242c 62%, #091e25 100%); }
.nav-brand { flex: 0 0 78px; height: 78px; display: flex; align-items: center; gap: 12px; padding: 0 18px; border-bottom: 1px solid rgba(255,255,255,.07); overflow: hidden; }
.brand-mark { position: relative; flex: 0 0 40px; width: 40px; height: 40px; display: grid; place-items: center; border-radius: 13px; color: #fff; background: linear-gradient(135deg, #2bc58a, #2b8da2); box-shadow: 0 10px 26px rgba(43,197,138,.24); font-size: 21px; }
.brand-pulse { position: absolute; right: -2px; top: -2px; width: 9px; height: 9px; border-radius: 50%; background: #93f5c9; border: 2px solid #102d35; }
.brand-copy { min-width: 0; white-space: nowrap; }.brand-copy strong,.brand-copy span { display: block; }.brand-copy strong { color: #fff; font-size: 18px; letter-spacing: 1px; }.brand-copy span { margin-top: 4px; color: #83a3aa; font-size: 10px; letter-spacing: 1px; }
.workspace-label { flex: none; padding: 18px 20px 7px; color: #658990; font-size: 11px; letter-spacing: 2px; }
.el-menu { flex: 1 1 auto; min-height: 0; overflow-x: hidden; overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; scrollbar-color: rgba(152,190,194,.22) transparent; --el-menu-bg-color: transparent; --el-menu-hover-bg-color: rgba(255,255,255,.065); border: 0; background: transparent !important; padding: 0 10px 10px; }
.el-menu::-webkit-scrollbar { width: 4px; }.el-menu::-webkit-scrollbar-track { background: transparent; }.el-menu::-webkit-scrollbar-thumb { border-radius: 999px; background: rgba(152,190,194,.22); }
:deep(.el-menu-item), :deep(.el-sub-menu__title) { height: 46px; margin: 3px 0; border-radius: 11px; color: #aac0c4; }
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) { color: #fff; background: rgba(255,255,255,.065); }
:deep(.el-menu-item.is-active) { color: #fff; background: linear-gradient(135deg, rgba(43,197,138,.96), rgba(43,141,162,.92)); box-shadow: 0 8px 22px rgba(11,34,42,.25); }
:deep(.el-sub-menu .el-menu) { margin: 3px 0 8px; padding: 5px 4px 6px 10px; border: 1px solid rgba(255,255,255,.045); border-radius: 12px; background: rgba(255,255,255,.035) !important; box-shadow: inset 0 1px rgba(255,255,255,.025); }
:deep(.el-sub-menu .el-menu-item) { min-width: 0; height: 39px; padding-left: 42px !important; font-size: 13px; }
.nav-badge { margin-left: auto; }
.nav-footer { position: relative; z-index: 1; flex: none; display: flex; align-items: center; gap: 10px; margin: 10px 14px max(16px, env(safe-area-inset-bottom)); padding: 11px 12px; border: 1px solid rgba(255,255,255,.07); border-radius: 12px; color: #bfd0d3; background: rgba(15,48,57,.96); box-shadow: 0 -12px 28px rgba(4,22,28,.18); }.nav-footer strong,.nav-footer small { display: block; }.nav-footer strong { font-size: 12px; }.nav-footer small { margin-top: 3px; color: #6f9299; font-size: 10px; }.status-dot { flex: none; width: 9px; height: 9px; border-radius: 50%; background: #47d99a; box-shadow: 0 0 0 5px rgba(71,217,154,.1); }
.collapsed .nav-brand { justify-content: center; padding: 0; }.collapsed .el-menu { padding-left: 8px; padding-right: 8px; }
.collapsed :deep(.el-menu-item), .collapsed :deep(.el-sub-menu__title) { padding: 0 18px !important; }
</style>
