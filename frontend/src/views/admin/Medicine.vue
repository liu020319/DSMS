<template>
  <div>
    <el-card style="margin-bottom: 20px">
      <el-row :gutter="10" align="middle">
        <el-col :span="6">
          <el-input v-model="search.keyword" placeholder="药品名/品牌/厂家" clearable @clear="loadData" />
        </el-col>
        <el-col :span="6">
          <el-input v-model="search.approvalNumber" placeholder="国药准字号" clearable @clear="loadData" />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="loadData">查询</el-button>
        </el-col>
        <el-col :span="8" style="text-align: right">
          <el-button type="success" @click="handleAdd">新增药品</el-button>
          <el-button @click="handleExport">Excel导出</el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <div style="margin-bottom:12px;display:flex;gap:8px" v-if="selectedRows.length > 0">
        <el-tag type="info">已选 {{ selectedRows.length }} 项</el-tag>
        <el-button type="success" size="small" @click="handleBatchEnable">一键启用</el-button>
        <el-button type="danger" size="small" @click="handleBatchDisable">一键禁用</el-button>
      </div>
      <el-table :data="tableData" border stripe @selection-change="handleSelectionChange" ref="tableRef">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="approvalNumber" label="国药准字号" width="160" />
        <el-table-column prop="medicineName" label="药品通用名" />
        <el-table-column prop="brandName" label="品牌名" />
        <el-table-column prop="specification" label="规格" width="140" />
        <el-table-column prop="unitPerBox" label="每盒总数量" width="100" />
        <el-table-column prop="manufacturer" label="生产厂家" />
        <el-table-column prop="referencePrice" label="参考价" width="90">
          <template #default="{ row }">¥{{ row.referencePrice }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="handleDisable(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑药品' : '新增药品'" width="650px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="国药准字号" prop="approvalNumberRaw">
          <div style="display:flex;align-items:center;gap:8px;width:100%">
            <el-tag type="info" style="flex-shrink:0">国药准字</el-tag>
            <el-input v-model="form.approvalNumberRaw" :disabled="isEdit" placeholder="如：H20050001" style="flex:1" />
          </div>
        </el-form-item>
        <el-form-item label="药品通用名" prop="medicineName">
          <el-input v-model="form.medicineName" />
        </el-form-item>
        <el-form-item label="品牌名" prop="brandName">
          <el-input v-model="form.brandName" />
        </el-form-item>
        <el-form-item label="规格" required>
          <div style="display:flex;align-items:center;gap:8px;width:100%">
            <el-input-number v-model="form.doseValue" :min="0.01" :step="0.5" :precision="2" controls-position="right" style="width:120px" />
            <el-select v-model="form.doseUnit" style="width:90px">
              <el-option label="毫克" value="mg" />
              <el-option label="克" value="g" />
              <el-option label="毫升" value="ml" />
            </el-select>
            <span style="font-weight:bold;color:#999">×</span>
            <el-input-number v-model="form.boxQuantity" :min="1" controls-position="right" style="width:100px" />
            <el-select v-model="form.boxUnit" style="width:90px">
              <el-option label="片" value="片" />
              <el-option label="粒" value="粒" />
              <el-option label="支" value="支" />
              <el-option label="瓶" value="瓶" />
              <el-option label="块" value="块" />
              <el-option label="袋" value="袋" />
            </el-select>
          </div>
          <div style="margin-top:4px;color:#409EFF;font-size:13px">自动生成规格：{{ generatedSpec }}</div>
        </el-form-item>
        <el-form-item label="每盒总数量">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-weight:bold;color:#409EFF;font-size:16px">{{ form.unitPerBox || '-' }}</span>
            <span style="color:#999">{{ form.boxUnit || '片' }}/盒</span>
            <el-tooltip content="由规格中的包装数量自动生成，用于计算库存和可吃天数" placement="top">
              <el-icon style="color:#909399;cursor:help;font-size:18px"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
        </el-form-item>
        <el-form-item label="生产厂家" prop="manufacturer">
          <el-input v-model="form.manufacturer" />
        </el-form-item>
        <el-form-item label="参考价格" prop="referencePrice">
          <el-input v-model="form.referencePrice" type="number" step="0.01" min="0" placeholder="如：15.48" style="width:200px" />
          <span style="margin-left:8px;color:#999">元/盒</span>
        </el-form-item>
        <el-form-item label="药品图片">
          <el-upload
            :action="'/api/upload/image?approvalNumber=' + (form.approvalNumber||'') + '&medicineName=' + (form.medicineName||'')"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            accept="image/*"
          >
            <ProtectedImage v-if="form.imageUrl" :src="form.imageUrl" style="width: 100px; height: 100px" fit="cover" />
            <el-button v-else size="small" type="primary">上传图片</el-button>
          </el-upload>
          <el-button v-if="form.imageUrl" size="small" style="margin-left:10px" @click="form.imageUrl=''">删除</el-button>
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
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { getMedicinePage, addMedicine, updateMedicine, disableMedicine, deleteMedicine } from '../../api/medicine'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadFile } from '../../utils/download'
import { QuestionFilled } from '@element-plus/icons-vue'
import ProtectedImage from '../../components/ProtectedImage.vue'

const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const tableRef = ref(null)
const selectedRows = ref([])
const page = reactive({ current: 1, size: 10, total: 0 })
const search = reactive({ keyword: '', approvalNumber: '' })

const form = reactive({
  medicineId: null, approvalNumberRaw: '', approvalNumber: '', medicineName: '', brandName: '',
  doseValue: null, doseUnit: 'mg', boxQuantity: null, boxUnit: '片',
  specification: '', unitPerBox: 1, manufacturer: '', referencePrice: '', imageUrl: ''
})

const doseUnitMap = { mg: '毫克', g: '克', ml: '毫升' }

const generatedSpec = computed(() => {
  if (!form.doseValue || !form.boxQuantity) return ''
  return `${form.doseValue}${doseUnitMap[form.doseUnit] || form.doseUnit}×${form.boxQuantity}${form.boxUnit}`
})

watch(() => form.boxQuantity, (val) => {
  if (val) form.unitPerBox = val
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: 'Bearer ' + token } : {}
})

const handleUploadSuccess = (response) => {
  if (response.code === 200) {
    form.imageUrl = response.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const rules = {
  approvalNumberRaw: [{ required: true, message: '请输入国药准字号编号', trigger: 'blur' }],
  medicineName: [{ required: true, message: '请输入药品通用名', trigger: 'blur' }],
  brandName: [{ required: true, message: '请输入品牌名', trigger: 'blur' }],
  manufacturer: [{ required: true, message: '请输入生产厂家', trigger: 'blur' }],
  referencePrice: [{ required: true, message: '请输入参考价格', trigger: 'blur' }]
}

const loadData = async () => {
  const res = await getMedicinePage({ current: page.current, size: page.size, ...search })
  tableData.value = res.data.records
  page.total = res.data.total
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    medicineId: null, approvalNumberRaw: '', approvalNumber: '', medicineName: '', brandName: '',
    doseValue: null, doseUnit: 'mg', boxQuantity: null, boxUnit: '片',
    specification: '', unitPerBox: 1, manufacturer: '', referencePrice: '', imageUrl: ''
  })
  dialogVisible.value = true
}

const parseSpec = (spec, boxUnit) => {
  const result = { doseValue: null, doseUnit: 'mg', boxQuantity: null, boxUnit: boxUnit || '片' }
  if (!spec) return result
  let match = spec.match(/^([\d.]+)(毫克|克|毫升|mg|g|ml)×(\d+)(片|粒|支|瓶|块|袋)$/)
  if (!match) {
    match = spec.match(/^([\d.]+)(毫克|克|毫升|mg|g|ml)[×x](\d+)$/)
  }
  if (match) {
    result.doseValue = parseFloat(match[1])
    const unitMap = { '毫克': 'mg', '克': 'g', '毫升': 'ml', 'mg': 'mg', 'g': 'g', 'ml': 'ml' }
    result.doseUnit = unitMap[match[2]] || 'mg'
    result.boxQuantity = parseInt(match[3])
    if (match[4]) result.boxUnit = match[4]
  }
  return result
}

const handleEdit = (row) => {
  isEdit.value = true
  const rawApproval = row.approvalNumber ? row.approvalNumber.replace(/^国药准字/, '') : ''
  const parsed = parseSpec(row.specification, row.boxUnit)
  Object.assign(form, row, {
    approvalNumberRaw: rawApproval,
    referencePrice: String(row.referencePrice || ''),
    doseValue: parsed.doseValue,
    doseUnit: parsed.doseUnit,
    boxQuantity: parsed.boxQuantity,
    boxUnit: parsed.boxUnit
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value.validate()
  form.approvalNumber = '国药准字' + form.approvalNumberRaw
  form.specification = generatedSpec.value
  form.unitPerBox = form.boxQuantity || 1
  form.referencePrice = parseFloat(form.referencePrice) || 0
  if (isEdit.value) {
    await updateMedicine(form)
  } else {
    await addMedicine(form)
  }
  ElMessage.success('操作成功')
  dialogVisible.value = false
  loadData()
}

const handleDisable = async (row) => {
  await ElMessageBox.confirm(`确定${row.status === 1 ? '禁用' : '启用'}该药品？`, '提示', { type: 'warning' })
  await disableMedicine(row.medicineId)
  ElMessage.success('操作成功')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定删除药品「${row.medicineName}」？此操作不可恢复！`, '二次确认', { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' })
  await ElMessageBox.confirm(`再次确认：删除「${row.medicineName}」将同时影响关联的用药方案和库存数据，确定要删除吗？`, '最终确认', { type: 'error', confirmButtonText: '确认删除', cancelButtonText: '取消' })
  await deleteMedicine(row.medicineId)
  ElMessage.success('删除成功')
  loadData()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleBatchEnable = async () => {
  const names = selectedRows.value.map(r => r.medicineName).join('、')
  await ElMessageBox.confirm(`确定启用以下药品？\n${names}`, '批量启用', { type: 'warning' })
  for (const row of selectedRows.value) {
    if (row.status !== 1) await disableMedicine(row.medicineId)
  }
  ElMessage.success('批量启用成功')
  loadData()
}

const handleBatchDisable = async () => {
  const names = selectedRows.value.map(r => r.medicineName).join('、')
  await ElMessageBox.confirm(`确定禁用以下药品？\n${names}`, '批量禁用', { type: 'warning' })
  for (const row of selectedRows.value) {
    if (row.status !== 0) await disableMedicine(row.medicineId)
  }
  ElMessage.success('批量禁用成功')
  loadData()
}

const handleExport = async () => {
  try {
    await downloadFile('/export/medicine', {}, '药品档案.xlsx')
    ElMessage.success('导出成功')
  } catch (e) {}
}

onMounted(loadData)
</script>
