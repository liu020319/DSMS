<template>
  <div class="my-medicine-page">
    <section class="page-intro">
      <div><h2>我的用药方案</h2><p>查看服用时段、库存天数和未来30天的补药建议。</p></div>
      <el-button type="success" @click="$router.push('/elder/today')"><el-icon><Calendar /></el-icon>今日计划</el-button>
    </section>

    <el-card class="desktop-table" shadow="never">
      <el-table :data="prescriptionList" border size="large">
        <el-table-column prop="medicineName" label="药品名称" min-width="160" />
        <el-table-column prop="specification" label="规格" min-width="140" />
        <el-table-column label="服用方式" min-width="190"><template #default="{ row }"><strong>{{ row.dosagePerTime }}{{ row.dosageUnit || '片' }}</strong> · {{ row.takeFrequencyLabel }}</template></el-table-column>
        <el-table-column label="服用时段" min-width="190"><template #default="{ row }"><PeriodTags :periods="parsePeriods(row.takePeriods)" :deducted="[]" :frequency-code="row.takeFrequencyCode" /></template></el-table-column>
        <el-table-column prop="remainingDays" label="库存" width="110"><template #default="{ row }"><el-tag :type="row.remainingDays < 7 ? 'danger' : 'success'">{{ row.remainingDays || 0 }}天</el-tag></template></el-table-column>
        <el-table-column label="补药建议" min-width="130"><template #default="{ row }"><span v-if="recommendedBoxes(row) > 0" class="refill-warning">建议补{{ recommendedBoxes(row) }}盒</span><span v-else class="refill-ok">30天内充足</span></template></el-table-column>
        <el-table-column prop="expiryDate" label="有效期" width="120" />
      </el-table>
    </el-card>

    <div class="mobile-list">
      <article v-for="row in prescriptionList" :key="row.prescriptionId" class="mobile-card">
        <div class="mobile-card-head"><div><h3>{{ row.medicineName }}</h3><span>{{ row.specification }}</span></div><el-tag :type="row.remainingDays < 7 ? 'danger' : 'success'">剩余{{ row.remainingDays || 0 }}天</el-tag></div>
        <div class="dose-summary"><strong>{{ row.dosagePerTime }}{{ row.dosageUnit || '片' }}</strong><span>{{ row.takeFrequencyLabel || row.takeTiming }}</span></div>
        <PeriodTags :periods="parsePeriods(row.takePeriods)" :deducted="[]" :frequency-code="row.takeFrequencyCode" />
        <div class="mobile-meta"><span>有效期：{{ row.expiryDate || '未填写' }}</span><b :class="recommendedBoxes(row) > 0 ? 'refill-warning' : 'refill-ok'">{{ recommendedBoxes(row) > 0 ? `未来30天建议补${recommendedBoxes(row)}盒` : '未来30天库存充足' }}</b></div>
        <p v-if="row.takeNotes" class="notes">医嘱：{{ row.takeNotes }}</p>
      </article>
    </div>
    <el-empty v-if="!prescriptionList.length" description="暂无用药方案" :image-size="80" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPrescriptionByUserId } from '../../api/prescription'
import { useUserStore } from '../../stores/user'
import PeriodTags from '../../components/PeriodTags.vue'

const userStore = useUserStore()
const prescriptionList = ref([])
const parsePeriods = value => { if (!value) return []; try { return JSON.parse(value) } catch { return [] } }
const recommendedBoxes = row => {
  const needed = Math.max(0, 30 * Number(row.dailyConsumption || 0) - Number(row.totalRemainingUnits || 0))
  const unitsPerBox = Number(row.unitPerBox || 0)
  return unitsPerBox > 0 ? Math.ceil(needed / unitsPerBox) : 0
}
onMounted(async () => { try { const res = await getPrescriptionByUserId(userStore.userInfo.userId); prescriptionList.value = res.data || [] } catch (e) {} })
</script>

<style scoped>
.page-intro { display:flex; justify-content:space-between; align-items:center; gap:18px; padding:22px 24px; margin-bottom:16px; border-radius:16px; background:#fff; }.page-intro h2 { margin:0 0 6px; color:#31583b; }.page-intro p { margin:0; color:#8c9990; font-size:14px; }.desktop-table { border:0; border-radius:16px; }.mobile-list { display:none; }.refill-warning { color:#d88922; font-weight:700; }.refill-ok { color:#4f8b5b; }.mobile-card { padding:17px; border:1px solid #e4ece5; border-radius:15px; background:#fff; box-shadow:0 8px 24px rgba(49,88,59,.06); }.mobile-card-head { display:flex; justify-content:space-between; gap:10px; }.mobile-card-head h3 { margin:0 0 5px; font-size:17px; }.mobile-card-head span { color:#909a92; font-size:12px; }.dose-summary { display:flex; align-items:baseline; gap:10px; margin:17px 0 12px; }.dose-summary strong { color:#31583b; font-size:25px; }.dose-summary span { color:#5f6f63; }.mobile-meta { display:flex; justify-content:space-between; gap:10px; margin-top:15px; padding-top:13px; border-top:1px solid #eef2ef; font-size:12px; }.notes { margin:12px 0 0; padding:10px; border-radius:9px; color:#667469; background:#f5f8f5; font-size:13px; }
@media (max-width:760px) { .page-intro { align-items:flex-start; padding:18px; }.page-intro h2 { font-size:21px; }.page-intro p { font-size:12px; line-height:1.5; }.page-intro .el-button { flex:0 0 auto; }.desktop-table { display:none; }.mobile-list { display:grid; gap:12px; }.mobile-meta { flex-direction:column; } }
</style>
