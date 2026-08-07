<template>
  <div class="today-page">
    <section class="today-hero">
      <div>
        <span class="eyebrow">{{ todayText }}</span>
        <h2>今日用药计划</h2>
        <p>系统会按服务器时间自动计算各时段的库存变化，无需通过登录触发。</p>
      </div>
      <div class="hero-progress">
        <strong>{{ completedCount }}/{{ totalCount }}</strong>
        <span>今日时段已计算</span>
      </div>
    </section>

    <el-alert
      title="“已计算”表示系统已计入该时段的计划用量，不代表人工确认已经服药。"
      type="info"
      :closable="false"
      show-icon
      class="info-alert"
    />

    <div v-loading="loading" class="period-list">
      <section v-for="period in periods" :key="period.code" class="period-section">
        <div class="period-heading">
          <div :class="['period-badge', `period-${period.code.toLowerCase()}`]">
            <el-icon><component :is="period.icon" /></el-icon>
          </div>
          <div>
            <h3>{{ period.label }}</h3>
            <p>建议时间：{{ period.time }}</p>
          </div>
          <el-tag v-if="period.code === currentPeriod" type="success" effect="dark">当前时段</el-tag>
        </div>

        <div v-if="medicinesFor(period.code).length" class="medicine-grid">
          <article v-for="item in medicinesFor(period.code)" :key="`${period.code}-${item.stockId}`" class="medicine-card">
            <div class="medicine-card-top">
              <div>
                <h4>{{ item.medicineName }}</h4>
                <span>{{ item.specification || '规格未填写' }}</span>
              </div>
              <el-tag :type="isDeducted(item, period.code) ? 'success' : 'warning'">
                {{ isDeducted(item, period.code) ? '已计算' : '待计算' }}
              </el-tag>
            </div>
            <div class="dose-line">
              <strong>{{ item.dosagePerTime || 1 }}{{ item.dosageUnit || '片' }}</strong>
              <span>{{ item.takeTiming || item.takeFrequencyLabel || '按医嘱服用' }}</span>
            </div>
            <div class="medicine-meta">
              <span>剩余约 {{ item.remainingDays || 0 }} 天</span>
              <span v-if="item.expiryDate">有效期 {{ item.expiryDate }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else :description="`${period.label}暂无用药安排`" :image-size="56" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getAllStock } from '../../api/dashboard'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const loading = ref(false)
const stockList = ref([])
const periods = [
  { code: 'MORNING', label: '晨间用药', time: '09:00 前后', icon: 'Sunrise' },
  { code: 'NOON', label: '午间用药', time: '13:00 前后', icon: 'Sunny' },
  { code: 'EVENING', label: '晚间用药', time: '21:00 前后', icon: 'MoonNight' }
]

const todayText = computed(() => new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date()))
const currentPeriod = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return 'MORNING'
  if (hour < 18) return 'NOON'
  return 'EVENING'
})

const parseArray = value => {
  if (Array.isArray(value)) return value
  if (!value) return []
  try { return JSON.parse(value) } catch { return [] }
}
const medicinesFor = code => stockList.value.filter(item => parseArray(item.takePeriods).includes(code))
const isDeducted = (item, code) => parseArray(item.todayDeductedPeriods).includes(code)
const totalCount = computed(() => periods.reduce((sum, period) => sum + medicinesFor(period.code).length, 0))
const completedCount = computed(() => periods.reduce((sum, period) => sum + medicinesFor(period.code).filter(item => isDeducted(item, period.code)).length, 0))

onMounted(async () => {
  loading.value = true
  try {
    const res = await getAllStock(userStore.userInfo.userId)
    stockList.value = res.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.today-page { max-width: 1080px; margin: 0 auto; }
.today-hero { display: flex; justify-content: space-between; align-items: center; gap: 24px; padding: 28px 32px; border-radius: 20px; color: white; background: linear-gradient(135deg, #315c47, #6d9f71); box-shadow: 0 16px 40px rgba(49, 92, 71, .2); }
.eyebrow { font-size: 14px; color: rgba(255,255,255,.72); }
.today-hero h2 { margin: 7px 0 8px; font-size: 28px; }
.today-hero p { margin: 0; color: rgba(255,255,255,.78); }
.hero-progress { min-width: 142px; padding: 18px; text-align: center; border-radius: 16px; background: rgba(255,255,255,.14); }
.hero-progress strong { display: block; font-size: 30px; }.hero-progress span { font-size: 13px; color: rgba(255,255,255,.76); }
.info-alert { margin: 16px 0; }
.period-list { display: grid; gap: 16px; }
.period-section { padding: 22px; border: 1px solid #e7ece9; border-radius: 18px; background: #fff; }
.period-heading { display: flex; align-items: center; gap: 13px; margin-bottom: 16px; }
.period-heading h3 { margin: 0 0 4px; font-size: 19px; }.period-heading p { margin: 0; color: #8a948e; font-size: 13px; }.period-heading .el-tag { margin-left: auto; }
.period-badge { width: 44px; height: 44px; display: grid; place-items: center; border-radius: 13px; color: #fff; font-size: 22px; }
.period-morning { background: #67c23a; }.period-noon { background: #e6a23c; }.period-evening { background: #33415c; }
.medicine-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.medicine-card { padding: 17px; border: 1px solid #edf0ee; border-radius: 14px; background: #fbfcfb; }
.medicine-card-top { display: flex; justify-content: space-between; gap: 12px; }.medicine-card h4 { margin: 0 0 4px; font-size: 16px; }.medicine-card-top span { color: #8a948e; font-size: 12px; }
.dose-line { display: flex; align-items: baseline; gap: 10px; margin: 18px 0 12px; }.dose-line strong { color: #315c47; font-size: 25px; }.dose-line span { color: #626d66; }
.medicine-meta { display: flex; justify-content: space-between; gap: 10px; color: #8a948e; font-size: 12px; }
@media (max-width: 700px) {
  .today-hero { align-items: flex-start; padding: 22px; border-radius: 16px; }.today-hero h2 { font-size: 23px; }.today-hero p { font-size: 13px; line-height: 1.6; }
  .hero-progress { min-width: 88px; padding: 12px; }.hero-progress strong { font-size: 23px; }.hero-progress span { font-size: 11px; }
  .period-section { padding: 16px; border-radius: 14px; }.medicine-grid { grid-template-columns: 1fr; }.medicine-meta { flex-direction: column; gap: 4px; }
}
</style>
