<template>
  <div>
    <el-card style="margin-bottom: 20px">
      <el-row :gutter="10" align="middle">
        <el-col :span="6">
          <el-input v-model="search.realName" placeholder="输入用户名搜索(如:王大爷)" clearable @clear="loadData" @keyup.enter="loadData" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-col>
        <el-col :span="14" style="text-align: right">
          <el-button type="success" @click="handleAdd">新增方案</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="realName" label="用户" width="100">
          <template #default="{ row }">{{ row.realName }}（{{ roleMap[row.role] || '未知' }}）</template>
        </el-table-column>
        <el-table-column label="药品名称" width="200">
          <template #default="{ row }">
            <span v-if="row.brandName" style="color:#409EFF">（{{ row.brandName }}）</span>{{ row.medicineName }}
          </template>
        </el-table-column>
        <el-table-column prop="approvalNumber" label="国药准字号" width="160" />
        <el-table-column prop="dailyTimes" label="每日次数" width="90">
          <template #default="{ row }">{{ row.dailyTimes }}次/天</template>
        </el-table-column>
        <el-table-column prop="dosagePerTime" label="每次用量" width="100">
          <template #default="{ row }">{{ row.dosagePerTime }}{{ row.dosageUnit || '片' }}</template>
        </el-table-column>
        <el-table-column prop="dailyConsumption" label="每日消耗" width="100">
          <template #default="{ row }">{{ row.dailyConsumption }}{{ row.dosageUnit || '片' }}/天</template>
        </el-table-column>
        <el-table-column label="服用频次" width="140">
          <template #default="{ row }">
            <span v-if="row.takeFrequencyLabel" style="font-weight:bold;color:#409EFF">{{ row.takeFrequencyLabel }}</span>
            <span v-else style="color:#999">-</span>
          </template>
        </el-table-column>
        <el-table-column label="服用时段" width="180">
          <template #default="{ row }">
            <PeriodTags :periods="parsePeriods(row.takePeriods)" :deducted="parseDeducted(row)" :frequency-code="row.takeFrequencyCode" />
          </template>
        </el-table-column>
        <el-table-column prop="totalRemainingUnits" label="剩余量" width="90">
          <template #default="{ row }">
            <span :style="{ color: row.remainingDays < 7 ? '#F56C6C' : '' }">{{ row.totalRemainingUnits || 0 }}{{ row.dosageUnit || '片' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remainingDays" label="剩余天数" width="90">
          <template #default="{ row }">
            <el-tag :type="row.remainingDays < 7 ? 'danger' : 'success'">{{ row.remainingDays || 0 }}天</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '在用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" @click="handleHistory(row)">历史</el-button>
            <el-button size="small" type="danger" v-if="row.status === 1" @click="handleStop(row)">停用</el-button>
            <el-button size="small" type="success" v-if="row.status === 0" @click="handleEnable(row)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        style="margin-top: 15px; justify-content: flex-end"
        v-model:current-page="page.current"
        v-model:page-size="page.size"
        :total="page.total"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用药方案' : '新增用药方案'" width="680px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="用户" prop="userId">
          <el-select v-model="form.userId" placeholder="选择用户" style="width: 100%" @change="onUserChange">
            <el-option v-for="u in userList" :key="u.userId" :label="u.realName + '（' + roleMap[u.role] + '）'" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="药品" prop="medicineId">
          <el-select v-model="form.medicineId" placeholder="选择药品" style="width: 100%" filterable @change="onMedicineChange">
            <el-option v-for="m in medicineList" :key="m.medicineId" :label="formatMedicineLabel(m)" :value="m.medicineId" />
          </el-select>
        </el-form-item>
        <el-form-item label="服用频次" required>
          <div style="display:flex;gap:10px;width:100%">
            <el-select v-model="form.dailyTimes" placeholder="每日次数" style="flex:1" @change="onDailyTimesChange">
              <el-option :label="'一日1次'" :value="1" />
              <el-option :label="'一日2次'" :value="2" />
              <el-option :label="'一日3次'" :value="3" />
            </el-select>
            <el-select v-model="form.takeFrequencyCode" placeholder="服用时段" style="flex:1" @change="onFrequencyChange">
              <el-option v-for="f in filteredFrequencies" :key="f.code" :label="f.periodLabel" :value="f.code" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="服用方式">
          <el-select v-model="form.takeMethod" placeholder="选择服用方式" style="width:100%" clearable>
            <el-option-group label="口服类">
              <el-option label="口服" value="口服" />
              <el-option label="口服空腹" value="口服空腹" />
              <el-option label="口服餐前" value="口服餐前" />
              <el-option label="口服餐后" value="口服餐后" />
              <el-option label="每晨口服" value="每晨口服" />
              <el-option label="每天早晨口服" value="每天早晨口服" />
              <el-option label="睡前口服" value="睡前口服" />
            </el-option-group>
            <el-option-group label="其他">
              <el-option label="舌下含服" value="舌下含服" />
              <el-option label="外敷" value="外敷" />
              <el-option label="注射" value="注射" />
              <el-option label="饭前" value="饭前" />
              <el-option label="空腹" value="空腹" />
              <el-option label="餐前空腹" value="餐前空腹" />
              <el-option label="每晨" value="每晨" />
              <el-option label="每天早晨" value="每天早晨" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="每次用量" prop="dosagePerTime">
          <el-input-number v-model="form.dosagePerTime" :min="1" />
          <span style="margin-left:10px;font-weight:bold;color:#409EFF">{{ form.dosageUnit || '片' }}</span>
          <span style="margin-left:6px;color:#999">由药品规格自动代入</span>
        </el-form-item>
        <el-form-item label="服用时段">
          <PeriodTags :periods="currentPeriods" :deducted="[]" :frequency-code="form.takeFrequencyCode" />
          <span style="margin-left:10px;color:#999">由服用频次自动设定</span>
        </el-form-item>
        <el-form-item label="每日消耗量">
          <span style="font-weight: bold; color: #409EFF; font-size: 16px">{{ form.dailyTimes * form.dosagePerTime }} {{ form.dosageUnit || '片' }}/天</span>
          <span style="margin-left: 10px; color: #999">（每日次数 × 每次用量，自动计算）</span>
        </el-form-item>
        <el-form-item label="服药备注">
          <el-input v-model="form.takeNotes" type="textarea" :rows="2" :placeholder="defaultNotesPlaceholder" />
          <div style="margin-top:4px;color:#999;font-size:12px">默认根据药品档案自动生成，可手动修改</div>
        </el-form-item>
        <el-form-item label="变更原因" v-if="isEdit">
          <el-input v-model="changeReason" placeholder="请输入变更原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" title="方案历史版本" width="700px">
      <el-timeline>
        <el-timeline-item v-for="h in historyList" :key="h.historyId" :timestamp="h.createTime" placement="top">
          <el-card>
            <p>每日{{ h.dailyTimes }}次，每次{{ h.dosagePerTime }}{{ h.dosageUnit || '片' }}，日消耗{{ h.dailyConsumption }}，单盒{{ h.daysPerBox }}天</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!historyList.length" description="暂无历史记录" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getPrescriptionPage, addPrescription, updatePrescription, stopPrescription, enablePrescription, getPrescriptionHistory, getPrescriptionByUserId } from '../../api/prescription'
import { getMedicineList } from '../../api/medicine'
import { getUserList } from '../../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import PeriodTags from '../../components/PeriodTags.vue'

const roleMap = { ADMIN: '管理员', ELDER: '父母', CHILD: '子女' }

const tableData = ref([])
const userList = ref([])
const medicineList = ref([])
const dialogVisible = ref(false)
const historyVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const changeReason = ref('')
const historyList = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })
const search = reactive({ realName: '' })

const allFrequencies = [
  { code: 'DAILY_1_MORNING', label: '一日1次晨服', periodLabel: '晨服', dailyTimes: 1, periods: ['MORNING'] },
  { code: 'DAILY_1_NOON', label: '一日1次午服', periodLabel: '午服', dailyTimes: 1, periods: ['NOON'] },
  { code: 'DAILY_1_EVENING', label: '一日1次晚服', periodLabel: '晚服', dailyTimes: 1, periods: ['EVENING'] },
  { code: 'DAILY_2_MORNING_EVENING', label: '一日2次早晚服', periodLabel: '早晚服', dailyTimes: 2, periods: ['MORNING', 'EVENING'] },
  { code: 'DAILY_3_FULL_DAY', label: '一日3次早中晚服', periodLabel: '早中晚服', dailyTimes: 3, periods: ['MORNING', 'NOON', 'EVENING'] }
]

const filteredFrequencies = computed(() => {
  return allFrequencies.filter(f => f.dailyTimes === form.dailyTimes)
})

const form = reactive({
  prescriptionId: null, userId: null, medicineId: null,
  dailyTimes: 1, dosagePerTime: 1, dosageUnit: '片', takeNotes: '',
  takeTiming: '', takeFrequencyCode: 'DAILY_1_MORNING', takePeriods: '["MORNING"]',
  takeMethod: '口服'
})

const currentPeriods = computed(() => {
  const opt = allFrequencies.find(f => f.code === form.takeFrequencyCode)
  return opt ? opt.periods : ['MORNING']
})

const selectedMedicine = computed(() => {
  return medicineList.value.find(m => m.medicineId === form.medicineId)
})

const defaultNotesPlaceholder = computed(() => {
  const m = selectedMedicine.value
  if (!m) return '如：每次90mg口服'
  const doseValue = m.specification ? m.specification.match(/^([\d.]+)/) : null
  const doseStr = doseValue ? doseValue[1] : ''
  const unitMap = { mg: '毫克', g: '克', ml: '毫升' }
  const doseUnitText = m.specification ? (m.specification.match(/(毫克|克|毫升|mg|g|ml)/) || [,'毫克'])[1] : '毫克'
  return `每次${doseStr}${unitMap[doseUnitText] || doseUnitText}口服`
})

const rules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  medicineId: [{ required: true, message: '请选择药品', trigger: 'change' }],
  dosagePerTime: [{ required: true, message: '请输入每次用量', trigger: 'blur' }]
}

const parsePeriods = (periodsStr) => {
  if (!periodsStr) return []
  try { return JSON.parse(periodsStr) } catch { return [] }
}

const parseDeducted = (row) => {
  if (!row.todayDeductedPeriods) return []
  try { return JSON.parse(row.todayDeductedPeriods) } catch { return [] }
}

const onDailyTimesChange = (val) => {
  const match = allFrequencies.find(f => f.dailyTimes === val)
  if (match) {
    form.takeFrequencyCode = match.code
    form.takePeriods = JSON.stringify(match.periods)
    const timingMap = { 'DAILY_1_MORNING': '每晨', 'DAILY_1_NOON': '午服', 'DAILY_1_EVENING': '晚间', 'DAILY_2_MORNING_EVENING': '早晚', 'DAILY_3_FULL_DAY': '一日三次' }
    form.takeTiming = timingMap[match.code] || ''
  }
}

const onFrequencyChange = (code) => {
  const opt = allFrequencies.find(f => f.code === code)
  if (opt) {
    form.dailyTimes = opt.dailyTimes
    form.takePeriods = JSON.stringify(opt.periods)
    const timingMap = { 'DAILY_1_MORNING': '每晨', 'DAILY_1_NOON': '午服', 'DAILY_1_EVENING': '晚间', 'DAILY_2_MORNING_EVENING': '早晚', 'DAILY_3_FULL_DAY': '一日三次' }
    form.takeTiming = timingMap[code] || ''
  }
}

const onUserChange = () => {}

const formatMedicineLabel = (m) => {
  return `( ${m.brandName || '无品牌'} ) ${m.medicineName}（${m.approvalNumber}）`
}

const onMedicineChange = (medicineId) => {
  const m = medicineList.value.find(item => item.medicineId === medicineId)
  if (m) {
    form.dosageUnit = m.boxUnit || '片'
    const doseValue = m.specification ? m.specification.match(/^([\d.]+)/) : null
    const doseStr = doseValue ? doseValue[1] : ''
    const unitMap = { mg: '毫克', g: '克', ml: '毫升' }
    const doseUnitMatch = m.specification ? (m.specification.match(/(毫克|克|毫升|mg|g|ml)/) || [,'毫克']) : [,'毫克']
    const doseUnitText = unitMap[doseUnitMatch[1]] || doseUnitMatch[1]
    form.takeNotes = `每次${doseStr}${doseUnitText}口服`
    form.takeMethod = '口服'
  }
}

const loadData = async () => {
  const params = { current: page.current, size: page.size }
  if (search.realName && search.realName.trim()) {
    params.realName = search.realName.trim()
  }
  const res = await getPrescriptionPage(params)
  tableData.value = res.data.records
  page.total = res.data.total
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    prescriptionId: null, userId: null, medicineId: null,
    dailyTimes: 1, dosagePerTime: 1, dosageUnit: '片', takeNotes: '',
    takeTiming: '每晨', takeFrequencyCode: 'DAILY_1_MORNING', takePeriods: '["MORNING"]',
    takeMethod: '口服'
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  changeReason.value = ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  if (!isEdit.value && form.userId && form.medicineId) {
    const existRes = await getPrescriptionByUserId(form.userId)
    const exists = existRes.data || []
    const dup = exists.find(p => p.medicineId === form.medicineId && p.status === 1)
    if (dup) {
      ElMessage.error('该用户已有此药品的用药方案，无法重复添加')
      return
    }
  }
  if (isEdit.value) {
    await updatePrescription(form, changeReason.value)
  } else {
    await addPrescription(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

const handleStop = async (row) => {
  await ElMessageBox.confirm('确定停用该用药方案？', '提示')
  await stopPrescription(row.prescriptionId)
  ElMessage.success('已停用')
  loadData()
}

const handleEnable = async (row) => {
  await enablePrescription(row.prescriptionId)
  ElMessage.success('已启用')
  loadData()
}

const handleHistory = async (row) => {
  const res = await getPrescriptionHistory(row.prescriptionId)
  historyList.value = res.data
  historyVisible.value = true
}

onMounted(async () => {
  const [uRes, mRes] = await Promise.all([getUserList(), getMedicineList()])
  userList.value = uRes.data
  medicineList.value = mRes.data
  loadData()
})
</script>
