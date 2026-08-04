<template>
  <div>
    <el-card v-if="warningList.length > 0" style="margin-bottom: 20px; border: 2px solid #F56C6C">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;color:#F56C6C">⚠️ 库存不足预警（剩余不足7天）</span>
          <el-tag type="danger">{{ warningList.length }}种药品需补货</el-tag>
        </div>
      </template>
      <el-table :data="warningList" size="small" border>
        <el-table-column prop="medicineName" label="药品名称" width="180" />
        <el-table-column prop="brandName" label="品牌" width="80" />
        <el-table-column prop="approvalNumber" label="国药准字号" width="160" />
        <el-table-column prop="specification" label="规格" width="120" />
        <el-table-column label="剩余天数" width="100">
          <template #default="{ row }">
            <el-tag :type="row.remainingDays <= 3 ? 'danger' : 'warning'">{{ row.remainingDays }}天</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="quickAdd(row)">一键添加购药</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card style="margin-bottom: 20px">
      <el-row :gutter="10" align="middle">
        <el-col :span="5">
          <el-select v-model="search.userId" placeholder="选择用户" clearable @change="loadData">
            <el-option v-for="u in userList" :key="u.userId" :label="formatUserLabel(u)" :value="u.userId" />
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-input v-model="search.approvalNumber" placeholder="国药准字号" clearable />
        </el-col>
        <el-col :span="3">
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-col>
        <el-col :span="11" style="text-align: right">
          <el-button type="success" @click="handleAdd">新增购药记录</el-button>
          <el-button @click="handleExport">Excel导出</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <el-table :data="tableData" border stripe>
        <el-table-column prop="purchaseDate" label="购药日期" width="110" />
        <el-table-column label="用户" width="100">
          <template #default="{ row }">{{ row.userName }}</template>
        </el-table-column>
        <el-table-column label="药品名称" width="180">
          <template #default="{ row }">
            <span v-if="row.brandName" style="color:#409EFF">（{{ row.brandName }}）</span>{{ row.medicineName }}
          </template>
        </el-table-column>
        <el-table-column prop="quantityBoxes" label="盒数" width="70" />
        <el-table-column prop="unitPrice" label="单价" width="80">
          <template #default="{ row }">¥{{ row.unitPrice }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="总价" width="80">
          <template #default="{ row }">¥{{ row.totalPrice }}</template>
        </el-table-column>
        <el-table-column prop="purchasePlatform" label="购药平台" width="90" />
        <el-table-column label="收货状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.receiptStatus === 1 ? 'success' : 'warning'" size="small">
              {{ row.receiptStatus === 1 ? '已确认收货' : '已下单未收货' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.receiptStatus !== 1" size="small" type="success" @click="handleConfirmReceipt(row)">确认收货</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑购药记录' : '新增购药记录'" width="650px">
      <el-steps :active="stepActive" align-center style="margin-bottom: 20px">
        <el-step title="选择用户" />
        <el-step title="选择方案" />
        <el-step title="填写详情" />
      </el-steps>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="选择用户" prop="userId">
          <el-select v-model="form.userId" placeholder="请先选择购药用户" style="width: 100%" @change="onUserChange">
            <el-option v-for="u in userList" :key="u.userId" :label="formatUserLabel(u)" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="用药方案" prop="prescriptionId">
          <el-select v-model="form.prescriptionId" placeholder="请先选择用户，再选择方案" style="width: 100%" :disabled="!form.userId" @change="onPrescriptionChange">
            <el-option v-for="p in prescriptionList" :key="p.prescriptionId" :label="formatPrescriptionLabel(p)" :value="p.prescriptionId" />
          </el-select>
        </el-form-item>
        <el-form-item label="购药日期" prop="purchaseDate">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="购买天数">
          <el-input-number v-model="purchaseDays" :min="1" @change="calcBoxes" />
          <el-button style="margin-left: 10px" type="primary" @click="calcBoxes" :disabled="!form.prescriptionId">计算盒数</el-button>
        </el-form-item>
        <el-form-item label="购买盒数" prop="quantityBoxes">
          <el-input-number v-model="form.quantityBoxes" :min="1" />
        </el-form-item>
        <div v-if="calcDetail" style="margin: -10px 0 15px 100px; padding: 10px 15px; background: #f0f9eb; border-radius: 6px; font-size: 13px; color: #67C23A; line-height: 1.8">
          <div>计算公式：所需盒数 = 向上取整(购买天数 × 每日消耗量 ÷ 每盒单位数)</div>
          <div>每日消耗量：{{ calcDetail.dailyConsumption }}（每日{{ calcDetail.dailyTimes }}次 × 每次{{ calcDetail.dosagePerTime }}{{ calcDetail.dosageUnit }}）</div>
          <div>每盒单位数：{{ calcDetail.unitPerBox }}</div>
          <div>计算结果：向上取整({{ purchaseDays }} × {{ calcDetail.dailyConsumption }} ÷ {{ calcDetail.unitPerBox }}) = <b style="color:#409EFF">{{ calcDetail.result }}盒</b></div>
        </div>
        <el-form-item label="单价" prop="unitPrice">
          <el-input v-model="form.unitPrice" type="number" step="0.01" min="0" placeholder="如：15.48" style="width:200px" />
          <span style="margin-left:8px;color:#999">元/盒</span>
        </el-form-item>
        <el-form-item label="总价">
          <span style="font-weight: bold; color: #409EFF; font-size: 18px">¥{{ totalPrice }}</span>
        </el-form-item>
        <el-form-item label="有效期" prop="expiryDate">
          <el-date-picker v-model="form.expiryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="购药平台">
          <el-select v-model="form.purchasePlatform" placeholder="选择购药平台" style="width: 100%" clearable>
            <el-option-group label="线上平台">
              <el-option label="京东" value="京东" />
              <el-option label="淘宝" value="淘宝" />
              <el-option label="美团" value="美团" />
              <el-option label="饿了么" value="饿了么" />
            </el-option-group>
            <el-option-group label="线下渠道">
              <el-option label="医院" value="医院" />
              <el-option label="线下药店" value="线下药店" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="收货状态">
          <el-radio-group v-model="form.receiptStatus">
            <el-radio :value="0">已下单未收货</el-radio>
            <el-radio :value="1">已确认收货</el-radio>
          </el-radio-group>
          <div style="margin-top:4px;color:#999;font-size:12px">线下购药可直接选"已确认收货"，线上购药等药品到货后再确认</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { getPurchasePage, addPurchase, updatePurchase, deletePurchase, confirmReceipt } from '../../api/purchase'
import { getPrescriptionByUserId } from '../../api/prescription'
import { getUserList } from '../../api/user'
import { getAdminDashboard, calcBoxes as calcBoxesApi } from '../../api/dashboard'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadFile } from '../../utils/download'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const tableData = ref([])
const userList = ref([])
const prescriptionList = ref([])
const warningList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const purchaseDays = ref(30)
const calcDetail = ref(null)
const page = reactive({ current: 1, size: 10, total: 0 })
const search = reactive({ userId: null, approvalNumber: '' })
const roleMap = { ADMIN: '管理员', ELDER: '父母', CHILD: '子女' }

const formatUserLabel = (u) => {
  return `${u.realName}（${roleMap[u.role] || '未知'}）`
}

const formatPrescriptionLabel = (p) => {
  return `${p.medicineName} - ${p.takeFrequencyLabel || ''} - 每次${p.dosagePerTime}${p.dosageUnit || '片'}`
}

const form = reactive({
  purchaseId: null, userId: null, prescriptionId: null,
  purchaseDate: '', quantityBoxes: 1, unitPrice: '', expiryDate: '', purchasePlatform: '',
  receiptStatus: 0
})

const totalPrice = computed(() => {
  const price = parseFloat(form.unitPrice) || 0
  return (price * form.quantityBoxes).toFixed(2)
})

const stepActive = computed(() => {
  if (!form.userId) return 0
  if (!form.prescriptionId) return 1
  return 2
})

const rules = {
  userId: [{ required: true, message: '请选择用户', trigger: 'change' }],
  prescriptionId: [{ required: true, message: '请选择用药方案', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  quantityBoxes: [{ required: true, message: '请输入盒数', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  expiryDate: [{ required: true, message: '请选择有效期', trigger: 'change' }]
}

const loadWarningList = async () => {
  try {
    const res = await getAdminDashboard()
    warningList.value = res.data.warningList || []
  } catch (e) {}
}

const loadData = async () => {
  const res = await getPurchasePage({ current: page.current, size: page.size, ...search })
  tableData.value = res.data.records
  page.total = res.data.total
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    purchaseId: null, userId: null, prescriptionId: null,
    purchaseDate: '', quantityBoxes: 1, unitPrice: '', expiryDate: '', purchasePlatform: '',
    receiptStatus: 0
  })
  prescriptionList.value = []
  calcDetail.value = null
  dialogVisible.value = true
}

const quickAdd = async (item) => {
  isEdit.value = false
  Object.assign(form, {
    purchaseId: null, userId: item.userId, prescriptionId: item.prescriptionId,
    purchaseDate: '', quantityBoxes: 1, unitPrice: '', expiryDate: '', purchasePlatform: '',
    receiptStatus: 0
  })
  if (item.userId) {
    const res = await getPrescriptionByUserId(item.userId)
    prescriptionList.value = res.data
  }
  calcDetail.value = null
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row, { unitPrice: String(row.unitPrice || ''), receiptStatus: row.receiptStatus || 0 })
  if (row.userId) {
    onUserChange(row.userId)
  }
  calcDetail.value = null
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  form.operatorId = userStore.userInfo.userId
  form.unitPrice = parseFloat(form.unitPrice) || 0
  if (isEdit.value) {
    await updatePurchase(form)
  } else {
    await addPurchase(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
  loadWarningList()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该购药记录？', '提示')
  await deletePurchase(row.purchaseId)
  ElMessage.success('已删除')
  loadData()
  loadWarningList()
}

const handleConfirmReceipt = async (row) => {
  await ElMessageBox.confirm('确认已收到药品？确认后系统将自动更新库存。', '确认收货', { type: 'warning' })
  await confirmReceipt(row.purchaseId)
  ElMessage.success('已确认收货，库存已更新')
  loadData()
  loadWarningList()
}

const onUserChange = async (userId) => {
  form.prescriptionId = null
  calcDetail.value = null
  if (!userId) {
    prescriptionList.value = []
    return
  }
  const res = await getPrescriptionByUserId(userId)
  prescriptionList.value = res.data
}

const onPrescriptionChange = () => {
  calcDetail.value = null
  if (form.prescriptionId && purchaseDays.value) {
    calcBoxes()
  }
}

const calcBoxes = async () => {
  if (!form.prescriptionId || !purchaseDays.value) return
  try {
    const res = await calcBoxesApi(form.prescriptionId, purchaseDays.value)
    form.quantityBoxes = res.data
    const p = prescriptionList.value.find(item => item.prescriptionId === form.prescriptionId)
    if (p) {
      calcDetail.value = {
        dailyConsumption: p.dailyConsumption,
        dailyTimes: p.dailyTimes,
        dosagePerTime: p.dosagePerTime,
        dosageUnit: p.dosageUnit || '片',
        unitPerBox: p.unitPerBox,
        result: res.data
      }
    }
  } catch (e) {}
}

const handleExport = async () => {
  try {
    await downloadFile('/export/purchase', { userId: search.userId }, '购药记录.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {}
}

onMounted(async () => {
  const uRes = await getUserList()
  userList.value = uRes.data
  loadData()
  loadWarningList()
})
</script>
