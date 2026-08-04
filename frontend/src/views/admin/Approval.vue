<template>
  <div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="待审批" name="pending">
        <el-table :data="pendingList" border stripe>
          <el-table-column prop="createTime" label="申请时间" width="170" />
          <el-table-column prop="applicantId" label="申请人ID" width="90" />
          <el-table-column prop="taskType" label="类型" width="130">
            <template #default="{ row }">
              <el-tag :type="taskTypeColor(row.taskType)">{{ taskTypeLabel(row.taskType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="contentJson" label="申请内容" show-overflow-tooltip />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="handleApprove(row)">通过</el-button>
              <el-button size="small" type="danger" @click="handleReject(row)">驳回</el-button>
              <el-button size="small" type="warning" @click="handleModifyApprove(row)">修改并审批</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="全部记录" name="all">
        <el-table :data="allList" border stripe>
          <el-table-column prop="createTime" label="申请时间" width="170" />
          <el-table-column prop="applicantId" label="申请人ID" width="90" />
          <el-table-column prop="taskType" label="类型" width="130">
            <template #default="{ row }">
              <el-tag :type="taskTypeColor(row.taskType)">{{ taskTypeLabel(row.taskType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'">
                {{ row.status === 'PENDING' ? '待审批' : row.status === 'APPROVED' ? '已通过' : '已驳回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="handlerComment" label="审批意见" show-overflow-tooltip />
          <el-table-column prop="contentJson" label="申请内容" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rejectVisible" title="驳回申请" width="400px">
      <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="请输入驳回意见" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确定驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="modifyVisible" title="修改并审批" width="600px">
      <el-form :model="modifyForm" label-width="100px">
        <el-form-item label="申请内容JSON">
          <el-input v-model="modifyForm.contentJson" type="textarea" :rows="6" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="modifyForm.comment" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modifyVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmModifyApprove">确定通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { getPendingList, getAllTasks, approveTask, rejectTask, modifyAndApproveTask } from '../../api/approval'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('pending')
const pendingList = ref([])
const allList = ref([])
const rejectVisible = ref(false)
const modifyVisible = ref(false)
const rejectComment = ref('')
const currentTask = ref(null)
const modifyForm = reactive({ contentJson: '', comment: '' })

const taskTypeLabel = (type) => {
  const map = { NEW_MEDICINE: '新增用药', LOSS_ADJUST: '丢失调整', STOCK_CORRECT: '库存修正' }
  return map[type] || type
}

const taskTypeColor = (type) => {
  const map = { NEW_MEDICINE: 'primary', LOSS_ADJUST: 'warning', STOCK_CORRECT: 'info' }
  return map[type] || ''
}

const loadPending = async () => {
  const res = await getPendingList({ current: 1, size: 100 })
  pendingList.value = res.data.records
}

const loadAll = async () => {
  const res = await getAllTasks({ current: 1, size: 100 })
  allList.value = res.data.records
}

const handleApprove = async (row) => {
  await ElMessageBox.confirm('确定通过该申请？', '提示')
  await approveTask(row.taskId, '')
  ElMessage.success('已通过')
  loadPending()
  loadAll()
}

const handleReject = (row) => {
  currentTask.value = row
  rejectComment.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  await rejectTask(currentTask.value.taskId, rejectComment.value)
  ElMessage.success('已驳回')
  rejectVisible.value = false
  loadPending()
  loadAll()
}

const handleModifyApprove = (row) => {
  currentTask.value = row
  modifyForm.contentJson = row.contentJson
  modifyForm.comment = ''
  modifyVisible.value = true
}

const confirmModifyApprove = async () => {
  await modifyAndApproveTask(currentTask.value.taskId, { contentJson: modifyForm.contentJson }, modifyForm.comment)
  ElMessage.success('已修改并审批通过')
  modifyVisible.value = false
  loadPending()
  loadAll()
}

watch(activeTab, (val) => {
  if (val === 'pending') loadPending()
  else loadAll()
})

onMounted(() => {
  loadPending()
})
</script>
