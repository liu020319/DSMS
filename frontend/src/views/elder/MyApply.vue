<template>
  <div>
    <el-card>
      <template #header><span style="font-weight: bold; font-size: 20px">📋 我的申请记录</span></template>
      <el-table :data="taskList" border size="large">
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column prop="taskType" label="类型" width="130">
          <template #default="{ row }">
            <el-tag :type="taskTypeColor(row.taskType)" size="large">{{ taskTypeLabel(row.taskType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'" size="large">
              {{ row.status === 'PENDING' ? '待审批' : row.status === 'APPROVED' ? '已通过' : '已驳回' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handlerComment" label="审批意见" show-overflow-tooltip />
        <el-table-column prop="contentJson" label="申请内容" show-overflow-tooltip />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 'REJECTED'" type="primary" size="small" @click="handleResubmit(row)">修改重提</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!taskList.length" description="暂无申请记录" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyTasks } from '../../api/approval'
import { useUserStore } from '../../stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const taskList = ref([])

const taskTypeLabel = (type) => {
  const map = { NEW_MEDICINE: '新增用药', LOSS_ADJUST: '丢失调整', STOCK_CORRECT: '库存修正' }
  return map[type] || type
}

const taskTypeColor = (type) => {
  const map = { NEW_MEDICINE: 'primary', LOSS_ADJUST: 'warning', STOCK_CORRECT: 'info' }
  return map[type] || ''
}

const handleResubmit = (row) => {
  router.push('/elder/submit-apply')
}

onMounted(async () => {
  try {
    const res = await getMyTasks({ current: 1, size: 100, applicantId: userStore.userInfo.userId })
    taskList.value = res.data.records
  } catch (e) {}
})
</script>
