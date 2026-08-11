<template>
  <div class="elder-dashboard">
    <section class="welcome-card">
      <div>
        <span class="welcome-date">{{ dateText }}</span>
        <h2>{{ greeting }}，{{ userStore.userInfo.realName || '您好' }}</h2>
        <p>今天也要按计划用药，有任何库存变化可以提交申请让家人协助处理。</p>
      </div>
      <div class="welcome-actions">
        <el-button type="success" size="large" round @click="$router.push('/elder/today')"><el-icon><Calendar /></el-icon>查看今日用药</el-button>
        <el-button size="large" round @click="checkIn"><el-icon><CircleCheck /></el-icon>我今天很好，给家人报平安</el-button>
      </div>
    </section>

    <el-row :gutter="14" class="metric-row">
      <el-col :xs="8" :sm="8">
        <div class="metric-card metric-green"><span>在用药品</span><strong>{{ dashboard.activePrescriptions || 0 }}</strong><small>种</small></div>
      </el-col>
      <el-col :xs="8" :sm="8">
        <div class="metric-card metric-amber"><span>库存预警</span><strong>{{ dashboard.warningCount || 0 }}</strong><small>项</small></div>
      </el-col>
      <el-col :xs="8" :sm="8">
        <div class="metric-card metric-red"><span>临期药品</span><strong>{{ dashboard.expiringCount || 0 }}</strong><small>项</small></div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="15">
        <el-card class="content-card" shadow="never">
          <template #header><div class="card-header"><span>需要关注</span><el-button text type="primary" @click="$router.push('/elder/my-medicine')">查看全部</el-button></div></template>
          <div v-if="dashboard.warningList?.length" class="warning-list">
            <article v-for="item in dashboard.warningList" :key="item.stockId" class="warning-item">
              <div class="medicine-symbol"><el-icon><FirstAidKit /></el-icon></div>
              <div class="warning-copy"><strong>{{ item.medicineName }}</strong><span>{{ item.specification || '请按医嘱服用' }}</span></div>
              <el-tag :type="item.remainingDays <= 3 ? 'danger' : 'warning'">剩余 {{ item.remainingDays }} 天</el-tag>
            </article>
          </div>
          <el-empty v-else description="目前没有库存预警，保持得很好" :image-size="72" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="9">
        <el-card class="content-card action-card" shadow="never">
          <template #header><div class="card-header"><span>常用功能</span></div></template>
          <button @click="$router.push('/elder/today')"><el-icon><Calendar /></el-icon><span><strong>今日用药</strong><small>查看早、中、晚计划</small></span><el-icon><ArrowRight /></el-icon></button>
          <button @click="$router.push('/elder/submit-apply')"><el-icon><EditPen /></el-icon><span><strong>提交申请</strong><small>新增用药或修正库存</small></span><el-icon><ArrowRight /></el-icon></button>
          <button @click="$router.push('/elder/security')"><el-icon><Lock /></el-icon><span><strong>账号安全</strong><small>修改当前登录密码</small></span><el-icon><ArrowRight /></el-icon></button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getElderDashboard } from '../../api/dashboard'
import { useUserStore } from '../../stores/user'
import { sendFamilyCheckIn } from '../../api/family'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const dashboard = ref({})
const dateText = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date()))
const greeting = computed(() => { const hour = new Date().getHours(); return hour < 11 ? '早上好' : hour < 14 ? '中午好' : hour < 18 ? '下午好' : '晚上好' })
const checkIn = async () => { await sendFamilyCheckIn(); ElMessage.success('已经告诉家人：您今天很好') }

onMounted(async () => {
  try { const res = await getElderDashboard(userStore.userInfo.userId); dashboard.value = res.data } catch (e) {}
})
</script>

<style scoped>
.welcome-card { display: flex; justify-content: space-between; align-items: center; gap: 24px; padding: 28px 30px; margin-bottom: 16px; border-radius: 20px; color: #fff; background: linear-gradient(135deg,#31583b,#6d9f71); box-shadow: 0 14px 36px rgba(49,88,59,.2); }
.welcome-date { color: rgba(255,255,255,.72); font-size: 14px; }.welcome-card h2 { margin: 7px 0 8px; font-size: 27px; }.welcome-card p { max-width: 620px; margin: 0; color: rgba(255,255,255,.78); line-height: 1.6; }.welcome-card .el-button { border: 1px solid rgba(255,255,255,.36); background: rgba(255,255,255,.15); }
.welcome-actions { display:flex; flex-direction:column; gap:10px; min-width:230px; }.welcome-actions .el-button { width:100%; margin:0; }
.metric-row { margin-bottom: 16px; }.metric-card { position: relative; min-height: 104px; padding: 20px; overflow: hidden; border-radius: 16px; background: #fff; box-shadow: 0 8px 24px rgba(49,88,59,.07); }.metric-card span { display: block; color: #859088; font-size: 14px; }.metric-card strong { display: inline-block; margin-top: 8px; font-size: 32px; }.metric-card small { margin-left: 4px; color: #9ba39d; }.metric-green strong { color:#4f8b5b }.metric-amber strong { color:#d6902f }.metric-red strong { color:#dc6666 }
.content-card { margin-bottom: 16px; border: 0; border-radius: 16px; }.card-header { display:flex; justify-content:space-between; align-items:center; font-size:18px; font-weight:700; }.warning-list { display:grid; gap:10px; }.warning-item { display:flex; align-items:center; gap:12px; padding:13px; border-radius:13px; background:#f8faf8; }.medicine-symbol { width:40px; height:40px; display:grid; place-items:center; flex:0 0 40px; border-radius:12px; color:#4f8b5b; background:#e8f3ea; }.warning-copy { min-width:0; margin-right:auto; }.warning-copy strong,.warning-copy span { display:block; }.warning-copy strong { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.warning-copy span { margin-top:3px; color:#929a94; font-size:12px; }
.action-card button { width:100%; display:flex; align-items:center; gap:12px; padding:15px 4px; border:0; border-bottom:1px solid #edf1ee; color:#4d6554; background:transparent; text-align:left; cursor:pointer; }.action-card button:last-child { border-bottom:0; }.action-card button>.el-icon:first-child { width:36px; height:36px; border-radius:10px; color:#fff; background:#6d9f71; }.action-card button span { flex:1; }.action-card strong,.action-card small { display:block; }.action-card small { margin-top:4px; color:#96a099; }
@media (max-width:700px) { .welcome-card { align-items:flex-start; flex-direction:column; padding:21px; border-radius:16px; }.welcome-card h2 { font-size:23px; }.welcome-card p { font-size:13px; }.welcome-card .el-button { width:100%; }.metric-row { margin-left:-5px!important; margin-right:-5px!important; }.metric-row :deep(.el-col) { padding-left:5px!important; padding-right:5px!important; }.metric-card { min-height:88px; padding:13px 10px; text-align:center; }.metric-card span { font-size:12px; }.metric-card strong { font-size:26px; }.metric-card small { display:none; }.warning-item { align-items:flex-start; flex-wrap:wrap; }.warning-copy { width:calc(100% - 54px); }.warning-item .el-tag { margin-left:52px; } }
</style>
