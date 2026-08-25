<template>
  <div class="request-page">
    <section class="page-head">
      <div><span class="eyebrow">远程代购</span><h1>告诉家人需要买哪些药</h1><p>先填写，再核对一次。最终提交后内容不能修改，避免家人买错。</p></div>
      <div class="balance" :class="{ negative: balanceAfter < 0 }"><small>当前购药余额</small><strong>¥{{ money(balance) }}</strong></div>
    </section>

    <el-steps :active="step" finish-status="success" simple class="steps"><el-step title="选择药品" /><el-step title="二次确认" /></el-steps>

    <template v-if="step === 0">
      <el-card class="form-card" shadow="never">
        <el-form label-position="top" size="large">
          <el-form-item label="这次为什么需要买药？">
            <el-radio-group v-model="reason" class="reason-grid">
              <el-radio-button value="LOW_STOCK">药快用完了</el-radio-button>
              <el-radio-button value="LOST">药丢失或损坏</el-radio-button>
              <el-radio-button value="NEW_PRESCRIPTION">医生让补购已有药</el-radio-button>
              <el-radio-button value="TRAVEL">外出前备药</el-radio-button>
              <el-radio-button value="OTHER">其他情况</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="给家人的说明（选填）"><el-input v-model="note" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：只剩两天、药盒丢了、希望尽快购买……" /></el-form-item>
        </el-form>
      </el-card>
      <el-alert v-if="reason==='LOST'" title="丢失申请提交后，所填盒数会同时从现有库存中扣除" type="warning" :closable="false" show-icon style="margin-bottom:14px" />

      <div class="medicine-grid">
        <article v-for="p in prescriptions" :key="p.prescriptionId" class="medicine-card" :class="{ selected: selected[p.prescriptionId] }" role="checkbox" :aria-checked="!!selected[p.prescriptionId]" tabindex="0" @click="toggleMedicine(p.prescriptionId)" @keydown.enter.prevent="toggleMedicine(p.prescriptionId)" @keydown.space.prevent="toggleMedicine(p.prescriptionId)">
          <div class="medicine-top"><el-checkbox v-model="selected[p.prescriptionId]" size="large" @click.stop><strong>{{ p.medicineName }}</strong></el-checkbox><el-tag :type="(p.remainingDays || 0) <= 7 ? 'danger' : 'success'">剩{{ p.remainingDays || 0 }}天</el-tag></div>
          <p>{{ p.brandName || '通用药' }} · {{ p.specification }}</p>
          <div class="price-row"><span>参考价 <b>¥{{ money(p.referencePrice) }}/盒</b></span><span>建议补足约30天</span></div>
          <div v-if="selected[p.prescriptionId]" class="quantity" @click.stop><span>需要盒数</span><el-input-number v-model="quantities[p.prescriptionId]" :min="1" :max="99" size="large" /><strong>小计 ¥{{ money((p.referencePrice || 0) * quantities[p.prescriptionId]) }}</strong></div>
        </article>
      </div>
      <el-empty v-if="!prescriptions.length" description="暂无可申请的用药方案，请让家人先完善用药方案" />
    </template>

    <template v-else>
      <el-alert title="请仔细核对：提交后安心用药端不能再修改" type="warning" :closable="false" show-icon class="confirm-alert" />
      <el-card shadow="never" class="confirm-card">
        <div class="confirm-meta"><span>申请原因</span><strong>{{ reasonLabel }}</strong></div>
        <div v-if="note" class="confirm-meta"><span>补充说明</span><strong>{{ note }}</strong></div>
        <div v-for="item in selectedItems" :key="item.prescriptionId" class="confirm-item">
          <div><strong>{{ item.medicineName }}</strong><p>{{ item.brandName }} · {{ item.specification }}</p></div>
          <div class="confirm-price"><span>{{ item.quantityBoxes }}盒 × ¥{{ money(item.referencePrice) }}</span><b>¥{{ money(item.subtotal) }}</b></div>
        </div>
        <div class="settlement">
          <div><span>上次结余/当前余额</span><b>¥{{ money(balance) }}</b></div>
          <div><span>本次参考金额</span><b>- ¥{{ money(estimatedTotal) }}</b></div>
          <div class="after" :class="{ negative: balanceAfter < 0 }"><span>购买后预计余额</span><strong>¥{{ money(balanceAfter) }}</strong></div>
          <p>参考价格仅用于预算，家人实际下单价格会在订单中重新计算，余额允许为负数。</p>
        </div>
      </el-card>
    </template>

    <div class="actions"><el-button v-if="step === 1" size="large" @click="step = 0">返回修改</el-button><el-button v-if="step === 0" type="primary" size="large" :disabled="!selectedItems.length" @click="goConfirm">下一步：核对信息</el-button><el-button v-else type="primary" size="large" :loading="submitting" @click="submit">确认无误，提交申请</el-button></div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPrescriptionByUserId } from '../../api/prescription'
import { submitPurchaseRequest, getFamilyFund } from '../../api/family'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore(); const router = useRouter()
const step = ref(0); const reason = ref('LOW_STOCK'); const note = ref(''); const submitting = ref(false)
const prescriptions = ref([]); const selected = reactive({}); const quantities = reactive({}); const balance = ref(0)
const labels = { LOW_STOCK: '药品快用完', LOST: '药品丢失或损坏', NEW_PRESCRIPTION: '医生让补购已有药', TRAVEL: '外出前备药', OTHER: '其他购药需求' }
const reasonLabel = computed(() => labels[reason.value])
const selectedItems = computed(() => prescriptions.value.filter(p => selected[p.prescriptionId]).map(p => ({ ...p, quantityBoxes: quantities[p.prescriptionId], subtotal: Number(p.referencePrice || 0) * quantities[p.prescriptionId] })))
const estimatedTotal = computed(() => selectedItems.value.reduce((sum, item) => sum + item.subtotal, 0))
const balanceAfter = computed(() => Number(balance.value || 0) - estimatedTotal.value)
const money = value => Number(value || 0).toFixed(2)

const toggleMedicine = prescriptionId => { selected[prescriptionId] = !selected[prescriptionId] }
const goConfirm = () => { if (!selectedItems.value.length) return ElMessage.warning('请至少选择一种药品'); step.value = 1; window.scrollTo({ top: 0, behavior: 'smooth' }) }
const submit = async () => {
  if (!userStore.userInfo.bindParentId) return ElMessage.error('尚未绑定家庭守护人账号，不能提交')
  await ElMessageBox.confirm('提交后不能修改，确定药品和盒数都正确吗？', '最后一次确认', { confirmButtonText: '确定提交', cancelButtonText: '再检查一下', type: 'warning' })
  submitting.value = true
  try {
    await submitPurchaseRequest({ elderId: userStore.userInfo.userId, parentId: userStore.userInfo.bindParentId, reason: reason.value, note: note.value, confirmed: true, items: selectedItems.value.map(i => ({ prescriptionId: i.prescriptionId, quantityBoxes: i.quantityBoxes })) })
    ElMessage.success('已提交，家庭守护端和邮箱提醒会同步生成')
    router.push('/elder/my-apply')
  } finally { submitting.value = false }
}

onMounted(async () => {
  const userId = userStore.userInfo.userId
  const [pRes, fundRes] = await Promise.all([getPrescriptionByUserId(userId), getFamilyFund(userId)])
  prescriptions.value = pRes.data || []; balance.value = fundRes.data.balance || 0
  prescriptions.value.forEach(p => { const missing = Math.max(0, 30 * Number(p.dailyConsumption || 1) - Number(p.totalRemainingUnits || 0)); quantities[p.prescriptionId] = Math.max(1, Math.ceil(missing / Number(p.unitPerBox || 1))); selected[p.prescriptionId] = (p.remainingDays || 0) <= 7 })
})
</script>

<style scoped>
.medicine-card{cursor:pointer}.medicine-card:hover,.medicine-card:focus-visible{border-color:#94b99a;outline:none;box-shadow:0 6px 18px rgba(72,123,81,.08)}.medicine-card.selected{background:#f8fcf9}
.request-page{max-width:1050px;margin:0 auto}.page-head{display:flex;justify-content:space-between;gap:24px;align-items:flex-end;margin-bottom:20px}.page-head h1{margin:5px 0 8px;font-size:30px;color:#24472d}.page-head p{margin:0;color:#6c7d70}.eyebrow{color:#4f8a5b;font-weight:700}.balance{min-width:180px;padding:16px 20px;border-radius:16px;background:#edf8ef;color:#2f7a43}.balance small,.balance strong{display:block}.balance strong{font-size:26px;margin-top:4px}.balance.negative,.negative{background:#fff0f0!important;color:#c84b4b!important}.steps{margin-bottom:18px;border-radius:12px}.form-card{margin-bottom:18px}.reason-grid{display:flex;flex-wrap:wrap;gap:8px}.medicine-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.medicine-card{border:2px solid #e6ece7;border-radius:16px;padding:18px;background:#fff;transition:.2s}.medicine-card.selected{border-color:#67a873;box-shadow:0 8px 24px rgba(72,123,81,.1)}.medicine-top,.price-row,.quantity,.confirm-item,.confirm-meta,.settlement>div{display:flex;justify-content:space-between;align-items:center;gap:14px}.medicine-card p,.confirm-item p{color:#7b887e;margin:8px 0}.price-row{font-size:14px;color:#66736a}.quantity{margin-top:15px;padding-top:14px;border-top:1px dashed #dce5de}.confirm-alert{margin-bottom:15px}.confirm-card{border-radius:16px}.confirm-meta{padding:12px 0;border-bottom:1px solid #edf0ed}.confirm-item{padding:18px 0;border-bottom:1px solid #edf0ed}.confirm-price{text-align:right}.confirm-price span,.confirm-price b{display:block}.confirm-price b{font-size:18px;color:#3d7c49;margin-top:5px}.settlement{margin-top:18px;padding:18px;border-radius:14px;background:#f5f8f5}.settlement>div{padding:6px 0}.settlement .after{margin-top:8px;padding:14px;border-radius:10px;background:#e8f5eb;color:#2e7140}.settlement p{font-size:13px;color:#7c887e;margin:10px 0 0}.confirm-check{margin-top:18px;white-space:normal;height:auto}.actions{display:flex;justify-content:flex-end;gap:10px;margin-top:20px}.actions .el-button{min-width:170px}
@media(max-width:720px){.page-head{align-items:stretch;flex-direction:column}.page-head h1{font-size:24px}.balance{min-width:0}.medicine-grid{grid-template-columns:1fr}.reason-grid{display:grid;grid-template-columns:1fr 1fr;width:100%}.reason-grid :deep(.el-radio-button__inner){width:100%;white-space:normal;min-height:48px}.quantity{align-items:flex-start;flex-wrap:wrap}.confirm-item{align-items:flex-start}.actions{position:sticky;bottom:8px;padding:10px;border-radius:14px;background:rgba(255,255,255,.95);z-index:2}.actions .el-button{flex:1;min-width:0}}
</style>
