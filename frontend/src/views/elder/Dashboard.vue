<template>
  <div>
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="8">
        <el-card shadow="hover" style="border-left: 4px solid #67C23A; text-align: center">
          <div style="font-size: 18px; color: #999">在用药品</div>
          <div style="font-size: 48px; font-weight: bold; color: #67C23A; margin-top: 10px">{{ dashboard.activePrescriptions || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" style="border-left: 4px solid #E6A23C; text-align: center">
          <div style="font-size: 18px; color: #999">库存预警</div>
          <div style="font-size: 48px; font-weight: bold; color: #E6A23C; margin-top: 10px">{{ dashboard.warningCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" style="border-left: 4px solid #F56C6C; text-align: center">
          <div style="font-size: 18px; color: #999">临期药品</div>
          <div style="font-size: 48px; font-weight: bold; color: #F56C6C; margin-top: 10px">{{ dashboard.expiringCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 20px">
      <template #header><span style="font-weight: bold; font-size: 20px">⚠️ 库存不足预警</span></template>
      <el-table :data="dashboard.warningList || []" size="large">
        <el-table-column prop="medicineName" label="药品名称" />
        <el-table-column prop="remainingDays" label="剩余天数" width="120">
          <template #default="{ row }">
            <span style="color: #F56C6C; font-weight: bold; font-size: 20px">{{ row.remainingDays }}天</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!dashboard.warningList?.length" description="暂无预警" :image-size="80" />
    </el-card>

    <el-card>
      <template #header><span style="font-weight: bold; font-size: 20px">🚀 快速申请</span></template>
      <el-space wrap size="large">
        <el-button type="primary" size="large" @click="$router.push('/elder/submit-apply')">提交用药申请</el-button>
        <el-button type="warning" size="large" @click="$router.push('/elder/my-apply')">查看申请记录</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getElderDashboard } from '../../api/dashboard'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const dashboard = ref({})

onMounted(async () => {
  try {
    const res = await getElderDashboard(userStore.userInfo.userId)
    dashboard.value = res.data
  } catch (e) {}
})
</script>
