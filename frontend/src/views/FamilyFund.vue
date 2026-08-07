<template>
  <div class="page">
    <div class="page-head"><div><span>家庭购药资金</span><h1>转账与代购扣款台账</h1><p>每笔钱保留时间、方式和用途，余额仅用于家庭对账。</p></div><el-select v-if="isAdmin" v-model="careUserId" placeholder="选择安心用药成员" size="large" @change="load"><el-option v-for="item in careUsers" :key="item.userId" :label="item.realName+'（'+item.username+'）'" :value="item.userId"/></el-select></div>
    <section :class="['balance-card',{negative:Number(balance)<0}]"><div><small>当前购药余额</small><strong>¥{{ money(balance) }}</strong><p>{{ Number(balance)>=0?'余额充足，可继续用于家庭代购':'余额暂时不足，但不会阻止家庭守护人继续代购' }}</p></div><el-button v-if="isAdmin" type="primary" size="large" :disabled="!careUserId" @click="visible=true">登记家庭转账</el-button></section>
    <el-card shadow="never" class="ledger-card"><template #header><div class="card-title"><b>资金流水</b><span>正数为转入，负数为购药扣款</span></div></template><el-table :data="records" stripe><el-table-column prop="transactionTime" label="时间" min-width="165"/><el-table-column label="类型" width="120"><template #default="{row}"><el-tag :type="Number(row.amount)>=0?'success':'danger'">{{ typeText(row.transactionType) }}</el-tag></template></el-table-column><el-table-column prop="paymentPlatform" label="方式/平台" min-width="120"/><el-table-column prop="note" label="说明" min-width="180"/><el-table-column label="金额" width="145"><template #default="{row}"><b :class="Number(row.amount)>=0?'income':'expense'">{{ Number(row.amount)>=0?'+':'-' }}¥{{ money(Math.abs(row.amount)) }}</b></template></el-table-column></el-table><el-empty v-if="!records.length" description="暂无资金记录"/></el-card>
    <el-dialog v-model="visible" title="登记家庭购药转账" width="520px"><el-form label-position="top" size="large"><el-form-item label="到账金额"><el-input-number v-model="form.amount" :min="1" :precision="2" style="width:100%"/></el-form-item><el-form-item label="转账方式"><el-radio-group v-model="form.paymentPlatform"><el-radio-button value="微信">微信</el-radio-button><el-radio-button value="支付宝">支付宝</el-radio-button><el-radio-button value="银联">银联</el-radio-button><el-radio-button value="现金">现金</el-radio-button></el-radio-group></el-form-item><el-form-item label="到账时间"><el-date-picker v-model="form.transactionTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%"/></el-form-item><el-form-item label="说明"><el-input v-model="form.note" maxlength="500" placeholder="例如：妈妈8月购药款"/></el-form-item></el-form><template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存并发送通知</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { addFamilyFund, getFamilyFund } from '../api/family'
import { getUserList } from '../api/user'
const localNow=()=>{const d=new Date(),p=n=>String(n).padStart(2,'0');return `${d.getFullYear()}-${p(d.getMonth()+1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`}
const store=useUserStore(),isAdmin=store.userInfo.role==='ADMIN',careUsers=ref([]),careUserId=ref(isAdmin?null:store.userInfo.userId),balance=ref(0),records=ref([]),visible=ref(false)
const form=reactive({amount:100,paymentPlatform:'微信',transactionTime:localNow(),note:''})
const money=value=>Number(value||0).toFixed(2),typeText=type=>({TRANSFER:'家庭转账',PURCHASE:'购药扣款',ADJUST:'余额调整'}[type]||type)
const load=async()=>{if(!careUserId.value){records.value=[];balance.value=0;return}const res=await getFamilyFund(careUserId.value);balance.value=res.data.balance;records.value=res.data.records||[]}
const save=async()=>{await addFamilyFund({elderId:careUserId.value,transactionType:'TRANSFER',...form});ElMessage.success('转账已登记并发送通知');visible.value=false;load()}
onMounted(async()=>{if(isAdmin){const res=await getUserList('ELDER');careUsers.value=res.data||[];if(careUsers.value.length)careUserId.value=careUsers.value[0].userId}load()})
</script>

<style scoped>
.page{max-width:1150px;margin:auto}.page-head{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:18px}.page-head>div>span{color:#278064;font-weight:800}.page-head h1{margin:6px 0;color:#173d3b;font-size:30px}.page-head p{margin:0;color:#72827e}.balance-card{display:flex;align-items:center;justify-content:space-between;gap:20px;margin-bottom:16px;padding:26px;border-radius:18px;color:#fff;background:linear-gradient(135deg,#23785d,#3c9873);box-shadow:0 14px 32px rgba(35,120,93,.18)}.balance-card.negative{background:linear-gradient(135deg,#a64545,#d06458)}.balance-card small,.balance-card strong{display:block}.balance-card strong{margin:7px 0;font-size:38px}.balance-card p{margin:0;color:rgba(255,255,255,.78)}.balance-card .el-button{border:0;color:#216b53;background:#fff}.ledger-card{border:1px solid #e2eae7;border-radius:16px}.card-title{display:flex;justify-content:space-between}.card-title span{color:#83918d;font-size:12px}.income{color:#208260}.expense{color:#d34e4e}
@media(max-width:760px){.page-head,.balance-card{align-items:stretch;flex-direction:column}.page-head h1{font-size:25px}.balance-card strong{font-size:32px}.balance-card .el-button{width:100%}.card-title{align-items:flex-start;flex-direction:column;gap:4px}}
</style>
