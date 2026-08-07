<template>
  <div class="medicine-page">
    <section class="page-hero">
      <div>
        <span class="eyebrow">MEDICATION MASTER DATA</span>
        <h1>药品主数据中心</h1>
        <p>统一维护批准文号、规格、生产企业和价格基准，并追踪关联方案、库存风险与购药历史。</p>
      </div>
      <div v-if="isSystemAdmin" class="hero-actions">
        <el-button @click="handleExport"><el-icon><Download /></el-icon>导出档案</el-button>
        <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon>新建药品档案</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article><span class="metric-icon teal"><el-icon><Collection /></el-icon></span><div><small>药品档案总数</small><strong>{{ number(summary.total_count) }}</strong><em>统一主数据</em></div></article>
      <article><span class="metric-icon green"><el-icon><CircleCheckFilled /></el-icon></span><div><small>启用中</small><strong>{{ number(summary.active_count) }}</strong><em>可用于新建方案</em></div></article>
      <article><span class="metric-icon amber"><el-icon><DataAnalysis /></el-icon></span><div><small>平均参考价</small><strong>¥{{ money(summary.average_price) }}</strong><em>每盒价格基准</em></div></article>
      <article :class="{ attention: number(summary.incomplete_count) > 0 }"><span class="metric-icon red"><el-icon><WarningFilled /></el-icon></span><div><small>待完善档案</small><strong>{{ number(summary.incomplete_count) }}</strong><em>缺图片或关键字段</em></div></article>
    </section>

    <el-card shadow="never" class="workspace-card">
      <div class="filter-toolbar">
        <div class="filter-main">
          <el-input v-model="search.keyword" :prefix-icon="Search" placeholder="搜索药品、品牌或生产企业" clearable @keyup.enter="searchData" @clear="searchData" />
          <el-input v-model="search.approvalNumber" placeholder="国药准字号" clearable @keyup.enter="searchData" @clear="searchData" />
          <el-select v-model="search.status" clearable placeholder="全部状态" @change="searchData"><el-option label="启用中" :value="1" /><el-option label="已停用" :value="0" /></el-select>
          <el-button type="primary" plain @click="searchData"><el-icon><Search /></el-icon>筛选</el-button>
          <el-button text @click="resetSearch">重置</el-button>
        </div>
        <div class="view-hint"><span></span>点击药品名或批准文号查看完整业务画像</div>
      </div>

      <div v-if="isSystemAdmin && selectedRows.length" class="batch-bar">
        <b>已选择 {{ selectedRows.length }} 项</b><span>可对选中档案执行批量状态操作</span>
        <el-button type="success" size="small" @click="handleBatchEnable">批量启用</el-button>
        <el-button type="danger" plain size="small" @click="handleBatchDisable">批量停用</el-button>
      </div>

      <el-table v-loading="loading" :data="tableData" class="medicine-table" @selection-change="handleSelectionChange">
        <el-table-column v-if="isSystemAdmin" type="selection" width="48" />
        <el-table-column label="药品档案" min-width="230">
          <template #default="{ row }">
            <button class="medicine-cell" @click="openProfile(row)">
              <span class="medicine-avatar">{{ row.medicineName?.slice(0, 1) }}</span>
              <span><strong>{{ row.medicineName }}</strong><small>{{ row.brandName || '未填写品牌' }}</small></span>
            </button>
          </template>
        </el-table-column>
        <el-table-column label="国药准字号" min-width="170">
          <template #default="{ row }"><button class="approval-link" @click="openProfile(row)">{{ row.approvalNumber }}</button></template>
        </el-table-column>
        <el-table-column prop="specification" label="规格 / 包装" min-width="150"><template #default="{ row }"><span class="spec-text">{{ row.specification }}</span><small class="subline">{{ row.unitPerBox }}{{ row.boxUnit || '单位' }}/盒</small></template></el-table-column>
        <el-table-column prop="manufacturer" label="生产企业" min-width="190" show-overflow-tooltip />
        <el-table-column label="参考价" width="110" align="right"><template #default="{ row }"><strong class="price">¥{{ money(row.referencePrice) }}</strong></template></el-table-column>
        <el-table-column label="档案状态" width="108" align="center"><template #default="{ row }"><span class="status-pill" :class="row.status === 1 ? 'enabled' : 'disabled'"><i></i>{{ row.status === 1 ? '启用中' : '已停用' }}</span></template></el-table-column>
        <el-table-column label="操作" :width="viewportWidth<=760?92:138" :fixed="viewportWidth<=760?false:'right'" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openProfile(row)">查看</el-button>
            <el-dropdown v-if="isSystemAdmin" trigger="click" @command="command => handleRowCommand(command, row)">
              <el-button link>更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown><el-dropdown-menu><el-dropdown-item command="edit">编辑档案</el-dropdown-item><el-dropdown-item command="toggle">{{ row.status === 1 ? '停用药品' : '启用药品' }}</el-dropdown-item><el-dropdown-item command="delete" divided>删除档案</el-dropdown-item></el-dropdown-menu></template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span>共 {{ page.total }} 条药品主数据</span>
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total" :page-sizes="[10, 20, 50, 100]" layout="sizes, prev, pager, next" @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <el-drawer v-model="drawerVisible" :size="drawerSize" :with-header="false" class="profile-drawer">
      <div v-loading="profileLoading" class="profile-shell">
        <template v-if="profile.medicine">
          <header class="profile-header">
            <button class="drawer-close" @click="drawerVisible = false"><el-icon><Close /></el-icon></button>
            <div class="profile-brand-mark"><el-icon><FirstAidKit /></el-icon></div>
            <div class="profile-title"><span>药品业务画像</span><h2>{{ profile.medicine.medicineName }}</h2><p>{{ profile.medicine.brandName }} · {{ profile.medicine.specification }}</p></div>
            <span class="status-pill" :class="profile.medicine.status === 1 ? 'enabled' : 'disabled'"><i></i>{{ profile.medicine.status === 1 ? '启用中' : '已停用' }}</span>
          </header>

          <section class="profile-kpis">
            <article><small>关联方案</small><strong>{{ number(profile.stats?.prescription_count) }}</strong><span>其中 {{ number(profile.stats?.active_prescription_count) }} 个在用</span></article>
            <article><small>累计购药</small><strong>{{ number(profile.stats?.purchase_count) }}</strong><span>{{ number(profile.stats?.total_purchase_boxes) }} 盒</span></article>
            <article><small>累计支出</small><strong>¥{{ compactMoney(profile.stats?.total_purchase_amount) }}</strong><span>相关购药明细</span></article>
            <article :class="{ risk: number(profile.stats?.low_stock_count) > 0 }"><small>低库存风险</small><strong>{{ number(profile.stats?.low_stock_count) }}</strong><span>剩余不足 7 天</span></article>
          </section>

          <el-tabs v-model="profileTab" class="profile-tabs">
            <el-tab-pane label="基础档案" name="base">
              <section class="info-section"><div class="section-title"><b>监管与包装信息</b><span>MASTER DATA</span></div><dl class="info-grid"><div><dt>国药准字号</dt><dd>{{ profile.medicine.approvalNumber }}</dd></div><div><dt>通用名</dt><dd>{{ profile.medicine.medicineName }}</dd></div><div><dt>品牌名</dt><dd>{{ profile.medicine.brandName }}</dd></div><div><dt>规格</dt><dd>{{ profile.medicine.specification }}</dd></div><div><dt>每盒总数量</dt><dd>{{ profile.medicine.unitPerBox }}{{ profile.medicine.boxUnit || '单位' }}</dd></div><div><dt>参考价格</dt><dd>¥{{ money(profile.medicine.referencePrice) }}/盒</dd></div><div class="wide"><dt>生产企业</dt><dd>{{ profile.medicine.manufacturer }}</dd></div><div><dt>最近更新</dt><dd>{{ dateTime(profile.medicine.updateTime) }}</dd></div></dl></section>
              <section class="profile-note"><el-icon><InfoFilled /></el-icon><div><b>档案用途</b><p>批准文号用于收货核验；规格和每盒数量用于库存消耗计算；参考价用于购药申请估价。修改前请确认信息来源。</p></div></section>
            </el-tab-pane>

            <el-tab-pane :label="`关联用药 ${profile.relatedUsers?.length || 0}`" name="users">
              <div v-for="item in profile.relatedUsers" :key="item.prescription_id" class="relation-card">
                <div class="person-avatar">{{ item.real_name?.slice(0, 1) }}</div><div><b>{{ item.real_name }}</b><p>每日 {{ item.daily_times }} 次，每次 {{ item.dosage_per_time }}{{ item.dosage_unit || '单位' }} · {{ item.take_timing || '按方案服用' }}</p></div>
                <div class="stock-state" :class="Number(item.remaining_days) < 7 ? 'risk' : ''"><strong>{{ item.remaining_days ?? '-' }}</strong><small>剩余天数</small></div>
              </div>
              <el-empty v-if="!profile.relatedUsers?.length" description="暂无关联用药方案" />
            </el-tab-pane>

            <el-tab-pane :label="`购药轨迹 ${profile.recentPurchases?.length || 0}`" name="purchases">
              <el-timeline class="purchase-timeline">
                <el-timeline-item v-for="item in profile.recentPurchases" :key="item.purchase_id" :timestamp="dateTime(item.purchase_time || item.purchase_date)" placement="top" color="#10a879">
                  <div class="timeline-card"><div><b>{{ item.user_name || '家庭成员' }}购入 {{ item.quantity_boxes }} 盒</b><span>{{ item.purchase_platform || '未填写平台' }} · {{ item.purchase_channel === 'OFFLINE' ? '线下' : '线上' }}</span></div><strong>¥{{ money(item.total_price) }}</strong></div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="!profile.recentPurchases?.length" description="暂无购药记录" />
            </el-tab-pane>
          </el-tabs>

          <footer class="profile-footer"><el-button @click="drawerVisible = false">关闭</el-button><el-button v-if="isSystemAdmin" type="primary" @click="editFromDrawer">编辑药品档案</el-button></footer>
        </template>
      </div>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑药品档案' : '新建药品档案'" width="680px" class="form-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid">
          <el-form-item label="国药准字号" prop="approvalNumberRaw"><el-input v-model="form.approvalNumberRaw" :disabled="isEdit"><template #prepend>国药准字</template></el-input></el-form-item>
          <el-form-item label="药品通用名" prop="medicineName"><el-input v-model="form.medicineName" /></el-form-item>
          <el-form-item label="品牌名" prop="brandName"><el-input v-model="form.brandName" /></el-form-item>
          <el-form-item label="生产企业" prop="manufacturer"><el-input v-model="form.manufacturer" /></el-form-item>
          <el-form-item label="单剂量"><div class="inline-field"><el-input-number v-model="form.doseValue" :min="0.01" :step="0.5" :precision="2" /><el-select v-model="form.doseUnit"><el-option label="毫克" value="mg" /><el-option label="克" value="g" /><el-option label="毫升" value="ml" /></el-select></div></el-form-item>
          <el-form-item label="包装数量"><div class="inline-field"><el-input-number v-model="form.boxQuantity" :min="1" /><el-select v-model="form.boxUnit"><el-option v-for="unit in ['片','粒','支','瓶','块','袋']" :key="unit" :label="unit" :value="unit" /></el-select></div></el-form-item>
          <el-form-item label="参考价格（元/盒）" prop="referencePrice"><el-input-number v-model="form.referencePrice" :min="0" :precision="2" :step="1" /></el-form-item>
          <el-form-item label="自动生成规格"><div class="generated-spec">{{ generatedSpec || '请填写单剂量和包装数量' }}</div></el-form-item>
        </div>
        <el-form-item label="药品图片">
          <el-upload :action="uploadAction" :headers="uploadHeaders" :show-file-list="false" :on-success="handleUploadSuccess" accept="image/*"><ProtectedImage v-if="form.imageUrl" :src="form.imageUrl" class="upload-preview" fit="cover" /><div v-else class="upload-placeholder"><el-icon><Plus /></el-icon><span>上传药品图片</span></div></el-upload>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="handleSubmit">保存档案</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { getMedicinePage, getMedicineOverview, getMedicineProfile, addMedicine, updateMedicine, disableMedicine, deleteMedicine } from '../../api/medicine'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { downloadFile } from '../../utils/download'
import ProtectedImage from '../../components/ProtectedImage.vue'

const route = useRoute()
const userStore = useUserStore()
const isSystemAdmin = computed(() => userStore.userInfo.role === 'ADMIN')
const tableData = ref([])
const summary = ref({})
const profile = ref({})
const loading = ref(false)
const profileLoading = ref(false)
const saving = ref(false)
const drawerVisible = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const profileTab = ref('base')
const formRef = ref(null)
const selectedRows = ref([])
const viewportWidth = ref(window.innerWidth)
const page = reactive({ current: 1, size: 10, total: 0 })
const search = reactive({ keyword: '', approvalNumber: '', status: null })
const form = reactive({ medicineId: null, approvalNumberRaw: '', approvalNumber: '', medicineName: '', brandName: '', doseValue: null, doseUnit: 'mg', boxQuantity: null, boxUnit: '片', specification: '', unitPerBox: 1, manufacturer: '', referencePrice: 0, imageUrl: '' })
const doseUnitMap = { mg: '毫克', g: '克', ml: '毫升' }
const generatedSpec = computed(() => form.doseValue && form.boxQuantity ? `${form.doseValue}${doseUnitMap[form.doseUnit] || form.doseUnit}×${form.boxQuantity}${form.boxUnit}` : '')
const drawerSize = computed(() => viewportWidth.value <= 760 ? '100%' : '720px')
const uploadHeaders = computed(() => localStorage.getItem('token') ? { Authorization: 'Bearer ' + localStorage.getItem('token') } : {})
const uploadAction = computed(() => `/api/upload/image?approvalNumber=${encodeURIComponent(form.approvalNumber || '')}&medicineName=${encodeURIComponent(form.medicineName || '')}`)
const rules = { approvalNumberRaw: [{ required: true, message: '请输入批准文号', trigger: 'blur' }], medicineName: [{ required: true, message: '请输入药品通用名', trigger: 'blur' }], brandName: [{ required: true, message: '请输入品牌名', trigger: 'blur' }], manufacturer: [{ required: true, message: '请输入生产企业', trigger: 'blur' }], referencePrice: [{ required: true, message: '请输入参考价格', trigger: 'change' }] }

const number = value => Number(value || 0)
const money = value => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const compactMoney = value => Number(value || 0).toLocaleString('zh-CN', { notation: 'compact', maximumFractionDigits: 1 })
const dateTime = value => value ? String(value).replace('T', ' ').slice(0, 16) : '暂无记录'

const loadOverview = async () => { const res = await getMedicineOverview(); summary.value = res.data || {} }
const loadData = async () => { loading.value = true; try { const res = await getMedicinePage({ current: page.current, size: page.size, ...search }); tableData.value = res.data.records; page.total = res.data.total } finally { loading.value = false } }
const searchData = () => { page.current = 1; loadData() }
const resetSearch = () => { Object.assign(search, { keyword: '', approvalNumber: '', status: null }); searchData() }

const openProfile = async row => { drawerVisible.value = true; profileTab.value = 'base'; profileLoading.value = true; try { const res = await getMedicineProfile(row.medicineId); profile.value = res.data || {} } finally { profileLoading.value = false } }
const handleAdd = () => { isEdit.value = false; Object.assign(form, { medicineId: null, approvalNumberRaw: '', approvalNumber: '', medicineName: '', brandName: '', doseValue: null, doseUnit: 'mg', boxQuantity: null, boxUnit: '片', specification: '', unitPerBox: 1, manufacturer: '', referencePrice: 0, imageUrl: '' }); dialogVisible.value = true }
const parseSpec = (spec, boxUnit) => { const result = { doseValue: null, doseUnit: 'mg', boxQuantity: null, boxUnit: boxUnit || '片' }; const match = String(spec || '').match(/^([\d.]+)(毫克|克|毫升|mg|g|ml)[×x](\d+)(片|粒|支|瓶|块|袋)?/); if (match) { result.doseValue = Number(match[1]); result.doseUnit = { 毫克: 'mg', 克: 'g', 毫升: 'ml' }[match[2]] || match[2]; result.boxQuantity = Number(match[3]); result.boxUnit = match[4] || result.boxUnit } return result }
const handleEdit = row => { isEdit.value = true; const parsed = parseSpec(row.specification, row.boxUnit); Object.assign(form, row, parsed, { approvalNumberRaw: String(row.approvalNumber || '').replace(/^国药准字/, ''), referencePrice: Number(row.referencePrice || 0) }); dialogVisible.value = true }
const editFromDrawer = () => { const medicine = profile.value.medicine; drawerVisible.value = false; if (medicine) handleEdit(medicine) }
const handleSubmit = async () => { await formRef.value.validate(); if (!generatedSpec.value) return ElMessage.warning('请完整填写单剂量和包装数量'); saving.value = true; try { const payload = { ...form, approvalNumber: '国药准字' + form.approvalNumberRaw, specification: generatedSpec.value, unitPerBox: form.boxQuantity || 1, referencePrice: Number(form.referencePrice || 0) }; isEdit.value ? await updateMedicine(payload) : await addMedicine(payload); ElMessage.success('药品档案已保存'); dialogVisible.value = false; await Promise.all([loadData(), loadOverview()]) } finally { saving.value = false } }
const handleUploadSuccess = response => { if (response.code === 200) { form.imageUrl = response.data; ElMessage.success('图片上传成功') } else ElMessage.error(response.message || '上传失败') }
const handleDisable = async row => { await ElMessageBox.confirm(`确定${row.status === 1 ? '停用' : '启用'}“${row.medicineName}”吗？`, '状态确认', { type: 'warning' }); await disableMedicine(row.medicineId); ElMessage.success('状态已更新'); await Promise.all([loadData(), loadOverview()]) }
const handleDelete = async row => { await ElMessageBox.confirm(`删除“${row.medicineName}”会影响关联方案，请确认没有在用业务后再继续。`, '删除药品档案', { type: 'error', confirmButtonText: '确认删除' }); await deleteMedicine(row.medicineId); ElMessage.success('档案已删除'); await Promise.all([loadData(), loadOverview()]) }
const handleRowCommand = (command, row) => ({ edit: () => handleEdit(row), toggle: () => handleDisable(row), delete: () => handleDelete(row) }[command]?.())
const handleSelectionChange = rows => { selectedRows.value = rows }
const batchToggle = async target => { const rows = selectedRows.value.filter(row => row.status !== target); if (!rows.length) return ElMessage.info('所选药品已经是目标状态'); await ElMessageBox.confirm(`将 ${rows.length} 个药品设为${target === 1 ? '启用' : '停用'}状态？`, '批量操作', { type: 'warning' }); for (const row of rows) await disableMedicine(row.medicineId); ElMessage.success('批量状态已更新'); await Promise.all([loadData(), loadOverview()]) }
const handleBatchEnable = () => batchToggle(1)
const handleBatchDisable = () => batchToggle(0)
const handleExport = async () => { await downloadFile('/export/medicine', {}, '药品主数据.xlsx'); ElMessage.success('药品档案已导出') }
const syncViewport = () => { viewportWidth.value = window.innerWidth }

watch(() => route.query.at, () => { if (route.query.action === 'create') handleAdd() })
onMounted(async () => { window.addEventListener('resize', syncViewport); await Promise.all([loadData(), loadOverview()]); if (route.query.action === 'create') handleAdd() })
onBeforeUnmount(() => window.removeEventListener('resize', syncViewport))
</script>

<style scoped>
.medicine-page { max-width: 1680px; margin: 0 auto; color: #183841; }.page-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 20px; }.eyebrow { color: #0d9474; font-size: 10px; font-weight: 800; letter-spacing: 2px; }.page-hero h1 { margin: 7px 0 5px; color: #102f38; font-size: 27px; letter-spacing: -.5px; }.page-hero p { margin: 0; color: #71868c; font-size: 13px; }.hero-actions { display: flex; gap: 9px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }.metric-grid article { min-width: 0; display: flex; align-items: center; gap: 13px; padding: 17px; border: 1px solid #e5ebec; border-radius: 15px; background: rgba(255,255,255,.94); box-shadow: 0 8px 24px rgba(21,54,63,.035); }.metric-grid article.attention { border-color: #f0d4ce; }.metric-icon { flex: 0 0 43px; width: 43px; height: 43px; display: grid; place-items: center; border-radius: 13px; font-size: 20px; }.metric-icon.teal { color: #167c89; background: #e9f5f6; }.metric-icon.green { color: #0d916d; background: #e8f7f1; }.metric-icon.amber { color: #b77a20; background: #fff4df; }.metric-icon.red { color: #c55c4a; background: #fbece9; }.metric-grid small,.metric-grid strong,.metric-grid em { display: block; }.metric-grid small { color: #70858b; font-size: 11px; }.metric-grid strong { margin-top: 3px; color: #15343d; font-size: 23px; }.metric-grid em { margin-top: 2px; color: #a1adb0; font-size: 10px; font-style: normal; }
.workspace-card { border: 1px solid #e4eaec; border-radius: 16px; }.filter-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding-bottom: 16px; border-bottom: 1px solid #edf1f2; }.filter-main { flex: 1; display: grid; grid-template-columns: minmax(220px, 1.5fr) minmax(180px, 1fr) 130px auto auto; gap: 9px; }.view-hint { display: flex; align-items: center; gap: 7px; color: #8b9b9f; font-size: 11px; white-space: nowrap; }.view-hint span { width: 7px; height: 7px; border-radius: 50%; background: #21b886; box-shadow: 0 0 0 4px #e7f7f1; }.batch-bar { display: flex; align-items: center; gap: 10px; padding: 10px 13px; margin-top: 12px; border: 1px solid #cce8df; border-radius: 10px; color: #31705f; background: #eff9f5; }.batch-bar span { margin-right: auto; color: #78948c; font-size: 11px; }
.medicine-table { margin-top: 8px; }.medicine-cell { display: flex; align-items: center; gap: 11px; padding: 0; border: 0; color: inherit; background: none; cursor: pointer; text-align: left; }.medicine-avatar { flex: 0 0 37px; width: 37px; height: 37px; display: grid; place-items: center; border-radius: 11px; color: #0b8b71; background: linear-gradient(135deg,#e6f7f1,#edf7f8); font-weight: 800; }.medicine-cell strong,.medicine-cell small { display: block; }.medicine-cell strong { color: #173b44; font-size: 13px; }.medicine-cell small,.subline { margin-top: 3px; color: #97a5a8; font-size: 10px; }.medicine-cell:hover strong,.approval-link:hover { color: #0a9272; }.approval-link { padding: 0; border: 0; color: #28728a; background: none; cursor: pointer; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }.spec-text { display: block; color: #3d5a61; }.price { color: #173b44; }.status-pill { display: inline-flex; align-items: center; gap: 6px; padding: 4px 8px; border-radius: 20px; font-size: 11px; white-space: nowrap; }.status-pill i { width: 6px; height: 6px; border-radius: 50%; }.status-pill.enabled { color: #087f61; background: #eaf8f2; }.status-pill.enabled i { background: #19b67e; }.status-pill.disabled { color: #8b6762; background: #f6efed; }.status-pill.disabled i { background: #b98a82; }.table-footer { display: flex; align-items: center; justify-content: space-between; padding-top: 15px; color: #89999d; font-size: 11px; }
.profile-shell { min-height: 100%; color: #203f47; }.profile-header { position: relative; display: flex; align-items: center; gap: 14px; padding: 25px 27px 22px; color: #fff; background: radial-gradient(circle at 80% 0, rgba(83,222,176,.35), transparent 34%), linear-gradient(135deg,#123b45,#126b65); }.drawer-close { position: absolute; top: 13px; right: 13px; width: 31px; height: 31px; display: grid; place-items: center; border: 0; border-radius: 9px; color: #fff; background: rgba(255,255,255,.12); cursor: pointer; }.profile-brand-mark { flex: 0 0 50px; width: 50px; height: 50px; display: grid; place-items: center; border: 1px solid rgba(255,255,255,.18); border-radius: 15px; background: rgba(255,255,255,.12); font-size: 25px; }.profile-title { flex: 1; min-width: 0; }.profile-title span { color: #9bd3cb; font-size: 10px; letter-spacing: 1.5px; }.profile-title h2 { margin: 3px 0; font-size: 21px; }.profile-title p { margin: 0; color: #b8d7d5; font-size: 12px; }.profile-header .status-pill.enabled { color: #caffec; background: rgba(44,218,153,.16); }.profile-kpis { display: grid; grid-template-columns: repeat(4, 1fr); border-bottom: 1px solid #e8edef; }.profile-kpis article { padding: 16px; text-align: center; border-right: 1px solid #e8edef; }.profile-kpis article:last-child { border-right: 0; }.profile-kpis small,.profile-kpis strong,.profile-kpis span { display: block; }.profile-kpis small { color: #87979b; font-size: 10px; }.profile-kpis strong { margin: 5px 0 2px; color: #173b44; font-size: 19px; }.profile-kpis span { color: #a0acae; font-size: 9px; }.profile-kpis .risk strong { color: #d6654f; }.profile-tabs { padding: 0 25px 78px; }.info-section { padding-top: 8px; }.section-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 13px; }.section-title span { color: #a0adaf; font-size: 9px; letter-spacing: 1.3px; }.info-grid { display: grid; grid-template-columns: 1fr 1fr; margin: 0; border: 1px solid #e6ecee; border-radius: 13px; overflow: hidden; }.info-grid div { padding: 13px; border-right: 1px solid #e6ecee; border-bottom: 1px solid #e6ecee; }.info-grid div:nth-child(2n) { border-right: 0; }.info-grid .wide { grid-column: span 2; }.info-grid dt { color: #8b9a9e; font-size: 10px; }.info-grid dd { margin: 5px 0 0; color: #294b53; font-size: 13px; }.profile-note { display: flex; gap: 10px; padding: 13px; margin-top: 15px; border-radius: 11px; color: #47706a; background: #eef8f5; }.profile-note p { margin: 4px 0 0; font-size: 11px; line-height: 1.6; }.relation-card { display: flex; align-items: center; gap: 11px; padding: 12px; margin-bottom: 8px; border: 1px solid #e7edef; border-radius: 12px; }.person-avatar { flex: 0 0 38px; width: 38px; height: 38px; display: grid; place-items: center; border-radius: 50%; color: #0d886e; background: #eaf7f3; font-weight: 800; }.relation-card b { font-size: 13px; }.relation-card p { margin: 4px 0 0; color: #84969a; font-size: 10px; }.stock-state { margin-left: auto; text-align: right; }.stock-state strong,.stock-state small { display: block; }.stock-state strong { color: #1c765f; font-size: 18px; }.stock-state small { color: #9ba7aa; font-size: 9px; }.stock-state.risk strong { color: #d6654f; }.purchase-timeline { padding-top: 8px; }.timeline-card { display: flex; align-items: center; justify-content: space-between; padding: 11px; border: 1px solid #e8edef; border-radius: 11px; }.timeline-card b,.timeline-card span { display: block; }.timeline-card span { margin-top: 3px; color: #8a999d; font-size: 10px; }.timeline-card > strong { color: #13795f; }.profile-footer { position: absolute; left: 0; right: 0; bottom: 0; display: flex; justify-content: flex-end; gap: 8px; padding: 13px 24px; border-top: 1px solid #e7edef; background: rgba(255,255,255,.96); backdrop-filter: blur(10px); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; column-gap: 18px; }.inline-field { display: grid; grid-template-columns: 1fr 94px; gap: 8px; width: 100%; }.inline-field .el-input-number,.form-grid .el-input-number { width: 100%; }.generated-spec { width: 100%; padding: 9px 11px; border-radius: 8px; color: #0b8268; background: #edf8f4; font-weight: 700; }.upload-preview,.upload-placeholder { width: 112px; height: 92px; border-radius: 10px; }.upload-placeholder { display: grid; place-items: center; align-content: center; gap: 5px; border: 1px dashed #bdd2d0; color: #668681; background: #f5faf9; font-size: 11px; }
@media (max-width: 1100px) { .metric-grid { grid-template-columns: 1fr 1fr; }.filter-toolbar { align-items: flex-start; flex-direction: column; }.filter-main { width: 100%; grid-template-columns: 1fr 1fr 130px auto auto; }.view-hint { display: none; } }
@media (max-width: 760px) { .page-hero { align-items: flex-start; flex-direction: column; }.page-hero h1 { font-size: 23px; }.hero-actions { width: 100%; }.hero-actions .el-button { flex: 1; }.metric-grid { grid-template-columns: 1fr 1fr; gap: 8px; }.metric-grid article { padding: 12px; }.metric-icon { display: none; }.metric-grid strong { font-size: 20px; }.filter-main { display: flex; flex-wrap: wrap; }.filter-main > * { flex: 1 1 140px; }.filter-main .el-button { flex: 0 0 auto; }.workspace-card :deep(.el-card__body) { padding: 12px; }.table-footer { align-items: flex-start; flex-direction: column; gap: 10px; }.table-footer > span { display: none; }.profile-kpis { grid-template-columns: 1fr 1fr; }.profile-kpis article:nth-child(2) { border-right: 0; }.profile-kpis article:nth-child(-n+2) { border-bottom: 1px solid #e8edef; }.profile-header { padding: 24px 18px 20px; }.profile-header .status-pill { display: none; }.profile-tabs { padding: 0 14px 76px; }.info-grid { grid-template-columns: 1fr; }.info-grid div,.info-grid div:nth-child(2n) { border-right: 0; }.info-grid .wide { grid-column: span 1; }.form-grid { grid-template-columns: 1fr; } }
</style>
