<template>
  <div>
    <el-card>
      <template #header><span style="font-weight: bold; font-size: 20px">📝 提交用药申请</span></template>
      <el-form :model="form" label-width="120px" size="large">
        <el-form-item label="申请类型">
          <el-radio-group v-model="form.taskType" size="large">
            <el-radio-button value="NEW_MEDICINE">新增用药</el-radio-button>
            <el-radio-button value="LOSS_ADJUST">丢失调整</el-radio-button>
            <el-radio-button value="STOCK_CORRECT">库存修正</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.taskType === 'NEW_MEDICINE'">
          <el-form-item label="选择药品">
            <el-select v-model="newMedicine.medicineId" placeholder="请选择药品" filterable style="width: 100%">
              <el-option v-for="m in medicineList" :key="m.medicineId" :label="m.medicineName + '(' + m.approvalNumber + ')'" :value="m.medicineId" />
            </el-select>
          </el-form-item>
          <el-form-item label="每日次数">
            <el-input-number v-model="newMedicine.dailyTimes" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="每次用量">
            <el-input-number v-model="newMedicine.dosagePerTime" :min="1" />
            <span style="margin-left: 10px; color: #999">单位数</span>
          </el-form-item>
          <el-form-item label="购买盒数">
            <el-input-number v-model="newMedicine.quantityBoxes" :min="0" />
          </el-form-item>
          <el-form-item label="单价">
            <el-input-number v-model="newMedicine.unitPrice" :min="0" :precision="2" />
          </el-form-item>
          <el-form-item label="有效期">
            <el-date-picker v-model="newMedicine.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </template>

        <template v-if="form.taskType === 'LOSS_ADJUST'">
          <el-form-item label="选择方案">
            <el-select v-model="lossAdjust.prescriptionId" placeholder="请选择用药方案" style="width: 100%">
              <el-option v-for="p in prescriptionList" :key="p.prescriptionId" :label="p.medicineName + '(' + p.approvalNumber + ')'" :value="p.prescriptionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="丢失盒数">
            <el-input-number v-model="lossAdjust.lossBoxes" :min="1" />
          </el-form-item>
        </template>

        <template v-if="form.taskType === 'STOCK_CORRECT'">
          <el-form-item label="选择方案">
            <el-select v-model="stockCorrect.prescriptionId" placeholder="请选择用药方案" style="width: 100%">
              <el-option v-for="p in prescriptionList" :key="p.prescriptionId" :label="p.medicineName + '(' + p.approvalNumber + ')'" :value="p.prescriptionId" />
            </el-select>
          </el-form-item>
          <el-form-item label="修正盒数">
            <el-input-number v-model="stockCorrect.correctBoxes" :min="1" />
          </el-form-item>
          <el-form-item label="有效期">
            <el-date-picker v-model="stockCorrect.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </template>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { submitApproval } from '../../api/approval'
import { getMedicineList } from '../../api/medicine'
import { getPrescriptionByUserId } from '../../api/prescription'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const medicineList = ref([])
const prescriptionList = ref([])

const form = reactive({ taskType: 'NEW_MEDICINE' })
const newMedicine = reactive({ medicineId: null, dailyTimes: 1, dosagePerTime: 1, quantityBoxes: 0, unitPrice: 0, expiryDate: '' })
const lossAdjust = reactive({ prescriptionId: null, lossBoxes: 1 })
const stockCorrect = reactive({ prescriptionId: null, correctBoxes: 1, expiryDate: '' })

const handleSubmit = async () => {
  const userId = userStore.userInfo.userId
  const parentId = userStore.userInfo.bindParentId
  if (!parentId) {
    ElMessage.error('您尚未绑定子女账号，无法提交申请')
    return
  }

  let contentJson = {}
  if (form.taskType === 'NEW_MEDICINE') {
    if (!newMedicine.medicineId) { ElMessage.error('请选择药品'); return }
    contentJson = { userId, ...newMedicine }
  } else if (form.taskType === 'LOSS_ADJUST') {
    if (!lossAdjust.prescriptionId) { ElMessage.error('请选择方案'); return }
    contentJson = { ...lossAdjust }
  } else {
    if (!stockCorrect.prescriptionId) { ElMessage.error('请选择方案'); return }
    contentJson = { ...stockCorrect }
  }

  await submitApproval({
    applicantId: userId,
    handlerId: parentId,
    taskType: form.taskType,
    contentJson: JSON.stringify(contentJson)
  })
  ElMessage.success('申请已提交，等待子女审批')
}

onMounted(async () => {
  const [mRes, pRes] = await Promise.all([
    getMedicineList(),
    getPrescriptionByUserId(userStore.userInfo.userId)
  ])
  medicineList.value = mRes.data
  prescriptionList.value = pRes.data
})
</script>
