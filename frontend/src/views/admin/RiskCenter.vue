<template>
  <div class="enterprise-page">
    <header class="page-title"><div><span>RISK OPERATIONS</span><h1>用药风险中心</h1><p>把库存不足、临期药品和待处理申请集中到一张风险清单，按紧急程度闭环处理。</p></div><el-button type="primary" @click="load"><el-icon><Refresh /></el-icon>刷新风险</el-button></header>
    <section class="metrics">
      <article class="critical"><small>高风险（≤3天）</small><strong>{{ highRisk.length }}</strong><span>建议今天处理</span></article>
      <article class="warning"><small>库存预警（≤7天）</small><strong>{{ warningList.length }}</strong><span>需要安排购药</span></article>
      <article class="expiry"><small>30天内到期</small><strong>{{ expiringList.length }}</strong><span>检查批次与有效期</span></article>
      <article><small>待审批任务</small><strong>{{ dashboard.pendingApprovalCount || 0 }}</strong><span>等待家庭管理员处理</span></article>
    </section>
    <el-card shadow="never" class="board-card">
      <div class="board-head"><div><b>风险处置看板</b><span>按紧急程度自动排序</span></div><el-radio-group v-model="tab" size="small"><el-radio-button label="all">全部</el-radio-button><el-radio-button label="stock">库存</el-radio-button><el-radio-button label="expiry">临期</el-radio-button></el-radio-group></div>
      <div class="risk-list" v-loading="loading">
        <article v-for="item in filteredRisks" :key="item.key" class="risk-row">
          <span class="risk-level" :class="item.level">{{ item.level === 'critical' ? '高' : item.level === 'warning' ? '中' : '低' }}</span>
          <div class="risk-main"><b>{{ item.medicineName }}</b><p>{{ item.realName || '家庭成员' }} · {{ item.specification || '规格待完善' }}</p></div>
          <div class="risk-description"><small>{{ item.type === 'stock' ? '预计可用' : '有效期' }}</small><strong>{{ item.type === 'stock' ? `${item.remainingDays} 天` : item.expiryDate }}</strong></div>
          <div class="risk-advice"><small>处置建议</small><span>{{ item.advice }}</span></div>
          <el-button type="primary" plain size="small" @click="goHandle(item)">{{ item.type === 'stock' ? '安排购药' : '查看方案' }}</el-button>
        </article>
        <el-empty v-if="!filteredRisks.length" description="当前筛选下没有风险" />
      </div>
    </el-card>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAdminDashboard } from '../../api/dashboard'
const router = useRouter(); const dashboard = ref({}); const tab = ref('all'); const loading = ref(false)
const warningList = computed(() => dashboard.value.warningList || []); const expiringList = computed(() => dashboard.value.expiringList || []); const highRisk = computed(() => warningList.value.filter(i => Number(i.remainingDays) <= 3))
const risks = computed(() => [
  ...warningList.value.map(i => ({ ...i, key: `s${i.stockId}`, type: 'stock', level: Number(i.remainingDays) <= 3 ? 'critical' : 'warning', advice: Number(i.remainingDays) <= 3 ? '立即核对余量并发起购药' : '本周内安排补充库存' })),
  ...expiringList.value.map(i => ({ ...i, key: `e${i.stockId}`, type: 'expiry', level: 'info', advice: '优先使用近效期批次，必要时联系家属' }))
].sort((a,b) => ({ critical: 0, warning: 1, info: 2 }[a.level] - ({ critical: 0, warning: 1, info: 2 }[b.level]))))
const filteredRisks = computed(() => tab.value === 'all' ? risks.value : risks.value.filter(i => i.type === tab.value))
const load = async () => { loading.value = true; try { dashboard.value = (await getAdminDashboard()).data || {} } finally { loading.value = false } }
const goHandle = item => router.push(item.type === 'stock' ? '/purchase' : '/prescription')
onMounted(load)
</script>
<style scoped>
.enterprise-page{max-width:1500px;margin:auto}.page-title{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:20px}.page-title span{color:#0b8f73;font-size:10px;font-weight:800;letter-spacing:2px}.page-title h1{margin:7px 0 5px;color:#13343d;font-size:27px}.page-title p{margin:0;color:#768a90;font-size:13px}.metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:16px}.metrics article{padding:18px;border:1px solid #e4eaec;border-radius:15px;background:#fff}.metrics small,.metrics strong,.metrics span{display:block}.metrics small{color:#7d8e92;font-size:11px}.metrics strong{margin:5px 0;color:#183a43;font-size:25px}.metrics span{color:#9ba8ab;font-size:10px}.metrics .critical{border-top:3px solid #d95f4b}.metrics .warning{border-top:3px solid #e3a33b}.metrics .expiry{border-top:3px solid #578eb5}.board-card{border:1px solid #e4eaec;border-radius:16px}.board-head{display:flex;align-items:center;justify-content:space-between;padding-bottom:15px;border-bottom:1px solid #edf1f2}.board-head b,.board-head span{display:block}.board-head span{margin-top:3px;color:#97a5a8;font-size:10px}.risk-list{min-height:240px}.risk-row{display:grid;grid-template-columns:36px minmax(180px,1.2fr) 120px minmax(180px,1fr) auto;align-items:center;gap:14px;padding:14px 4px;border-bottom:1px solid #edf1f2}.risk-level{width:30px;height:30px;display:grid;place-items:center;border-radius:9px;font-weight:800;font-size:12px}.risk-level.critical{color:#c94d3a;background:#fbeae7}.risk-level.warning{color:#b27617;background:#fff3dc}.risk-level.info{color:#397ba7;background:#eaf3f9}.risk-main b{color:#21434c}.risk-main p{margin:4px 0 0;color:#8b9a9e;font-size:11px}.risk-description small,.risk-description strong,.risk-advice small,.risk-advice span{display:block}.risk-description small,.risk-advice small{color:#9ba7aa;font-size:9px}.risk-description strong,.risk-advice span{margin-top:3px;color:#486269;font-size:12px}@media(max-width:760px){.page-title{align-items:flex-start;flex-direction:column;gap:12px}.metrics{grid-template-columns:1fr 1fr;gap:8px}.risk-row{grid-template-columns:34px 1fr auto}.risk-description,.risk-advice{display:none}}
</style>
