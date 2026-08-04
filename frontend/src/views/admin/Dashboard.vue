<template>
  <div>
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6">
        <el-card shadow="hover" style="border-left: 4px solid #409EFF; cursor: pointer" @click="$router.push('/prescription')">
          <div style="font-size: 14px; color: #999">在用药品数</div>
          <div style="font-size: 32px; font-weight: bold; color: #409EFF; margin-top: 8px">{{ dashboard.activePrescriptions || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="border-left: 4px solid #E6A23C; cursor: pointer" @click="scrollToWarning">
          <div style="font-size: 14px; color: #999">库存预警</div>
          <div style="font-size: 32px; font-weight: bold; color: #E6A23C; margin-top: 8px">{{ dashboard.warningCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="border-left: 4px solid #F56C6C; cursor: pointer" @click="scrollToExpiring">
          <div style="font-size: 14px; color: #999">临期药品</div>
          <div style="font-size: 32px; font-weight: bold; color: #F56C6C; margin-top: 8px">{{ dashboard.expiringCount || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" style="border-left: 4px solid #909399; cursor: pointer" @click="$router.push('/approval')">
          <div style="font-size: 14px; color: #999">待审批</div>
          <div style="font-size: 32px; font-weight: bold; color: #909399; margin-top: 8px">{{ dashboard.pendingApprovalCount || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: bold">⚠️ 库存不足预警（剩余不足7天）</span>
              <el-button type="primary" size="small" @click="$router.push('/purchase')">去购药</el-button>
            </div>
          </template>
          <div style="height: 320px; overflow-y: auto">
            <div v-for="item in warningPageData" :key="item.stockId"
              class="warning-card"
              :class="{ 'warning-card-danger': item.remainingDays <= 3 }">
              <div class="warning-card-header">
                <span class="warning-card-name">{{ item.medicineName }}</span>
                <PeriodTags :periods="parsePeriods(item.takePeriods)" :deducted="parseDeducted(item.todayDeductedPeriods)" :frequency-code="item.takeFrequencyCode" />
              </div>
              <div class="warning-card-body">
                <span style="color:#999">{{ item.specification }}</span>
                <span style="margin-left:10px">{{ item.realName }}</span>
                <el-tag :type="item.remainingDays <= 3 ? 'danger' : 'warning'" style="margin-left:auto">
                  剩余{{ item.remainingDays }}天
                </el-tag>
                <el-button size="small" type="warning" style="margin-left:8px" @click="openAdjust(item)">修正</el-button>
              </div>
            </div>
            <el-empty v-if="!dashboard.warningList?.length" description="暂无预警" :image-size="60" />
          </div>
          <div v-if="dashboard.warningList?.length > warningPageSize" style="margin-top: 10px; display: flex; justify-content: flex-end; align-items: center; gap: 10px">
            <span style="font-size: 12px; color: #999">共{{ dashboard.warningList.length }}条</span>
            <el-pagination
              small
              v-model:current-page="warningCurrentPage"
              :page-size="warningPageSize"
              :total="dashboard.warningList?.length || 0"
              layout="prev, pager, next"
            />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span style="font-weight: bold">📅 临期药品预警（30天内到期）</span>
            </div>
          </template>
          <div style="height: 320px; overflow-y: auto">
            <el-table :data="expiringPageData" size="small">
              <el-table-column prop="realName" label="用户" width="80" />
              <el-table-column prop="medicineName" label="药品名称" />
              <el-table-column prop="specification" label="规格" width="120" />
              <el-table-column prop="expiryDate" label="有效期" width="110">
                <template #default="{ row }">
                  <el-tag type="warning">{{ row.expiryDate }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!dashboard.expiringList?.length" description="暂无临期药品" :image-size="60" />
          </div>
          <div v-if="dashboard.expiringList?.length > expiringPageSize" style="margin-top: 10px; display: flex; justify-content: flex-end; align-items: center; gap: 10px">
            <span style="font-size: 12px; color: #999">共{{ dashboard.expiringList.length }}条</span>
            <el-pagination
              small
              v-model:current-page="expiringCurrentPage"
              :page-size="expiringPageSize"
              :total="dashboard.expiringList?.length || 0"
              layout="prev, pager, next"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="adjustVisible" title="库存手动修正" width="450px">
      <el-form label-width="100px">
        <el-form-item label="药品">
          <span style="font-weight:bold">{{ adjustItem.medicineName }}</span>
        </el-form-item>
        <el-form-item label="当前剩余">
          <span>{{ adjustItem.totalRemainingUnits }}{{ adjustItem.dosageUnit || '片' }}</span>
        </el-form-item>
        <el-form-item label="调整量">
          <el-input-number v-model="adjustForm.adjustUnits" :step="1" />
          <div style="font-size:12px;color:#999;margin-top:4px">正数=补扣(减少库存)，负数=回滚(增加库存)</div>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="2" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAdjust">确定修正</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { getAdminDashboard, manualAdjustStock } from '../../api/dashboard'
import { ElMessage } from 'element-plus'
import PeriodTags from '../../components/PeriodTags.vue'

const dashboard = ref({})
const warningCurrentPage = ref(1)
const warningPageSize = ref(5)
const expiringCurrentPage = ref(1)
const expiringPageSize = ref(5)
const adjustVisible = ref(false)
const adjustItem = reactive({})
const adjustForm = reactive({ adjustUnits: 0, reason: '' })

const warningPageData = computed(() => {
  const list = dashboard.value.warningList || []
  const start = (warningCurrentPage.value - 1) * warningPageSize.value
  return list.slice(start, start + warningPageSize.value)
})

const expiringPageData = computed(() => {
  const list = dashboard.value.expiringList || []
  const start = (expiringCurrentPage.value - 1) * expiringPageSize.value
  return list.slice(start, start + expiringPageSize.value)
})

const parsePeriods = (periodsStr) => {
  if (!periodsStr) return []
  try { return JSON.parse(periodsStr) } catch { return [] }
}

const parseDeducted = (str) => {
  if (!str) return []
  try { return JSON.parse(str) } catch { return [] }
}

const scrollToWarning = () => {
  window.scrollTo({ top: 200, behavior: 'smooth' })
}

const scrollToExpiring = () => {
  window.scrollTo({ top: 200, behavior: 'smooth' })
}

const openAdjust = (item) => {
  Object.assign(adjustItem, item)
  adjustForm.adjustUnits = 0
  adjustForm.reason = ''
  adjustVisible.value = true
}

const handleAdjust = async () => {
  if (adjustForm.adjustUnits === 0) {
    ElMessage.warning('调整量不能为0')
    return
  }
  await manualAdjustStock(adjustItem.stockId, adjustForm.adjustUnits, adjustForm.reason)
  ElMessage.success('库存修正成功')
  adjustVisible.value = false
  const res = await getAdminDashboard()
  dashboard.value = res.data
}

onMounted(async () => {
  try {
    const res = await getAdminDashboard()
    dashboard.value = res.data
    localStorage.setItem('dashboardCache', JSON.stringify(res.data))
  } catch (e) {
    const cached = localStorage.getItem('dashboardCache')
    if (cached) {
      dashboard.value = JSON.parse(cached)
      ElMessage.warning('网络异常，展示上次缓存数据')
    }
  }
})
</script>

<style scoped>
.warning-card {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  border: 1px solid #E6A23C;
  background: #FDF6EC;
}
.warning-card-danger {
  border-color: #F56C6C;
  background: #FEF0F0;
}
.warning-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.warning-card-name {
  font-weight: bold;
  font-size: 14px;
}
.warning-card-body {
  display: flex;
  align-items: center;
  font-size: 12px;
}
</style>
