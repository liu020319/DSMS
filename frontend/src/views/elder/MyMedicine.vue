<template>
  <div>
    <el-card>
      <template #header><span style="font-weight: bold; font-size: 20px">💊 我的用药方案</span></template>
      <el-table :data="prescriptionList" border size="large">
        <el-table-column prop="medicineName" label="药品名称" width="160" />
        <el-table-column prop="specification" label="规格" width="140" />
        <el-table-column prop="dailyTimes" label="每日次数" width="100">
          <template #default="{ row }">{{ row.dailyTimes }}次/天</template>
        </el-table-column>
        <el-table-column prop="dosagePerTime" label="每次用量" width="100">
          <template #default="{ row }">{{ row.dosagePerTime }}{{ row.dosageUnit || '片' }}</template>
        </el-table-column>
        <el-table-column label="服用频次" width="160">
          <template #default="{ row }">
            <span v-if="row.takeFrequencyLabel" style="font-weight:bold;color:#409EFF;font-size:16px">{{ row.takeFrequencyLabel }}</span>
            <span v-else style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="服用时段" width="200">
          <template #default="{ row }">
            <PeriodTags :periods="parsePeriods(row.takePeriods)" :deducted="[]" :frequency-code="row.takeFrequencyCode" />
          </template>
        </el-table-column>
        <el-table-column prop="remainingDays" label="剩余天数" width="120">
          <template #default="{ row }">
            <el-tag :type="row.remainingDays < 7 ? 'danger' : 'success'" size="large">
              {{ row.remainingDays || 0 }}天
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="有效期" width="120">
          <template #default="{ row }">
            <span :style="{ color: isExpiring(row.expiryDate) ? '#F56C6C' : '' }">{{ row.expiryDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="takeNotes" label="备注" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="!prescriptionList.length" description="暂无用药方案" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPrescriptionByUserId } from '../../api/prescription'
import { useUserStore } from '../../stores/user'
import PeriodTags from '../../components/PeriodTags.vue'

const userStore = useUserStore()
const prescriptionList = ref([])

const parsePeriods = (periodsStr) => {
  if (!periodsStr) return []
  try { return JSON.parse(periodsStr) } catch { return [] }
}

const isExpiring = (date) => {
  if (!date) return false
  const thirtyDaysLater = new Date()
  thirtyDaysLater.setDate(thirtyDaysLater.getDate() + 30)
  return new Date(date) <= thirtyDaysLater
}

onMounted(async () => {
  try {
    const res = await getPrescriptionByUserId(userStore.userInfo.userId)
    prescriptionList.value = res.data
  } catch (e) {}
})
</script>
