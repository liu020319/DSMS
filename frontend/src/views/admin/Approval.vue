<template>
  <div class="page">
    <div class="head"><div><span>家庭协作</span><h1>家庭购药需求</h1><p>核对申请后完成下单、上传截图和物流单号。</p></div><el-badge :value="pending.length"><el-button @click="load">刷新申请</el-button></el-badge></div>
    <el-tabs v-model="tab"><el-tab-pane label="待处理" name="pending"/><el-tab-pane label="全部记录" name="all"/></el-tabs>
    <div class="request-grid">
      <el-card v-for="task in shown" :key="task.taskId" shadow="hover" class="request-card">
        <div class="request-head"><div><el-tag :type="task.status==='PENDING'?'warning':'success'">{{ task.status==='PENDING'?'待下单':task.status==='APPROVED'?'已下单':'已驳回' }}</el-tag><strong>账号ID {{ task.applicantId }} · {{ content(task).reasonLabel || typeText(task.taskType) }}</strong></div><time>{{ task.createTime }}</time></div>
        <template v-if="task.taskType==='PURCHASE_REQUEST'">
          <div class="medicine-line" v-for="item in content(task).items||[]" :key="item.prescriptionId"><div><b>{{ item.medicineName }}</b><span>{{ item.brandName }} · {{ item.specification }}</span></div><div>{{ item.quantityBoxes }}盒<br><strong>约¥{{ money(item.estimatedSubtotal) }}</strong></div></div>
          <div class="summary"><span>提交时余额 ¥{{ money(content(task).balanceSnapshot) }}</span><b>预计总额 ¥{{ money(content(task).estimatedTotal) }}</b></div>
          <p v-if="content(task).note">成员说明：{{ content(task).note }}</p>
          <div class="actions" v-if="task.status==='PENDING'"><el-button type="primary" size="large" @click="openOrder(task)">已购买，登记订单</el-button><el-button type="danger" plain @click="reject(task)">无法购买</el-button></div>
        </template>
        <template v-else><p>原申请类型：{{ typeText(task.taskType) }}</p><div class="actions" v-if="task.status==='PENDING'"><el-button type="success" @click="approve(task)">通过</el-button><el-button type="danger" @click="reject(task)">驳回</el-button></div></template>
      </el-card>
      <el-empty v-if="!shown.length" description="没有待处理申请" />
    </div>

    <el-dialog v-model="visible" title="登记代购订单" width="760px" destroy-on-close>
      <el-form label-position="top" size="large">
        <div class="form-grid"><el-form-item label="购药平台"><el-select v-model="order.purchasePlatform" filterable allow-create><el-option v-for="p in platforms" :key="p" :label="p" :value="p"/></el-select></el-form-item><el-form-item label="线上/线下"><el-radio-group v-model="order.purchaseChannel"><el-radio-button value="ONLINE">线上</el-radio-button><el-radio-button value="OFFLINE">线下</el-radio-button></el-radio-group></el-form-item><el-form-item label="下单时间"><el-date-picker v-model="order.orderTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"/></el-form-item></div>
        <div class="order-item" v-for="item in order.items" :key="item.prescriptionId"><div><b>{{ item.medicineName }}</b><span>{{ item.specification }}</span></div><el-form-item label="盒数"><el-input-number v-model="item.quantityBoxes" :min="1"/></el-form-item><el-form-item label="实际单价"><el-input-number v-model="item.unitPrice" :min="0" :precision="2"/></el-form-item><el-form-item label="有效期"><el-date-picker v-model="item.expiryDate" type="date" value-format="YYYY-MM-DD"/></el-form-item></div>
        <el-divider>订单凭证和物流</el-divider>
        <div class="form-grid"><el-form-item label="快递公司"><el-select v-model="order.carrierCode" @change="carrierChanged"><el-option v-for="c in carriers" :key="c.code" :label="c.name" :value="c.code"/></el-select></el-form-item><el-form-item label="物流单号"><el-input v-model="order.trackingNo" placeholder="下单后可稍后补充"/></el-form-item><el-form-item label="下单截图"><el-upload :show-file-list="false" :http-request="uploadProof" accept="image/*"><el-button :loading="uploading">{{ order.screenshotUrl?'已上传，重新选择':'上传订单截图' }}</el-button></el-upload></el-form-item></div>
        <el-form-item label="备注"><el-input v-model="order.note" type="textarea"/></el-form-item>
        <div class="total">订单实付合计 <strong>¥{{ money(orderTotal) }}</strong></div>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveOrder">保存订单并发送通知</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingList,getAllTasks,approveTask,rejectTask } from '../../api/approval'
import { createFamilyOrder } from '../../api/family'
import request from '../../utils/request'
const tab=ref('pending'),pending=ref([]),all=ref([]),visible=ref(false),current=ref(null),saving=ref(false),uploading=ref(false),cache=new Map()
const platforms=['京东健康','京东','阿里健康','美团买药','饿了么买药','医院','线下药店','其他'];const carriers=[{code:'jd',name:'京东物流'},{code:'zhongtong',name:'中通快递'},{code:'shentong',name:'申通快递'},{code:'yuantong',name:'圆通速递'},{code:'jtexpress',name:'极兔速递'},{code:'other',name:'其他'}]
const order=reactive({taskId:null,purchasePlatform:'京东健康',purchaseChannel:'ONLINE',orderTime:'',screenshotUrl:'',carrierCode:'jd',carrierName:'京东物流',trackingNo:'',note:'',items:[]})
const shown=computed(()=>tab.value==='pending'?pending.value:all.value);const orderTotal=computed(()=>order.items.reduce((s,i)=>s+Number(i.unitPrice||0)*Number(i.quantityBoxes||0),0));const money=v=>Number(v||0).toFixed(2)
const content=t=>{if(!cache.has(t.taskId)){try{cache.set(t.taskId,JSON.parse(t.contentJson||'{}'))}catch{cache.set(t.taskId,{})}}return cache.get(t.taskId)};const typeText=t=>({NEW_MEDICINE:'新增用药',LOSS_ADJUST:'药品丢失',STOCK_CORRECT:'库存修正'}[t]||t)
const load=async()=>{const [p,a]=await Promise.all([getPendingList({current:1,size:100}),getAllTasks({current:1,size:100})]);pending.value=p.data.records||[];all.value=a.data.records||[]}
const tomorrowYear=()=>{const d=new Date();d.setFullYear(d.getFullYear()+2);return d.toISOString().slice(0,10)}
const nowText=()=>{const d=new Date(),pad=n=>String(n).padStart(2,'0');return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:00`}
const openOrder=t=>{current.value=t;const c=content(t);Object.assign(order,{taskId:t.taskId,purchasePlatform:'京东健康',purchaseChannel:'ONLINE',orderTime:nowText(),screenshotUrl:'',carrierCode:'jd',carrierName:'京东物流',trackingNo:'',note:'',items:(c.items||[]).map(i=>({...i,unitPrice:Number(i.estimatedUnitPrice||0),expiryDate:tomorrowYear()}))});visible.value=true}
const carrierChanged=code=>{order.carrierName=carriers.find(c=>c.code===code)?.name||'其他'}
const uploadProof=async opt=>{uploading.value=true;try{const fd=new FormData();fd.append('file',opt.file);const r=await request.post('/upload/image',fd);order.screenshotUrl=r.data;ElMessage.success('截图已上传')}finally{uploading.value=false}}
const saveOrder=async()=>{if(!order.purchasePlatform||!order.orderTime)return ElMessage.warning('请填写平台和下单时间');if(order.items.some(i=>!i.expiryDate))return ElMessage.warning('请填写每种药品的有效期');saving.value=true;try{await createFamilyOrder({...order,items:order.items.map(i=>({prescriptionId:i.prescriptionId,quantityBoxes:i.quantityBoxes,unitPrice:i.unitPrice,expiryDate:i.expiryDate}))});ElMessage.success('订单已登记，安心用药端已收到提醒');visible.value=false;load()}finally{saving.value=false}}
const reject=async t=>{const {value}=await ElMessageBox.prompt('请填写无法购买的原因','驳回申请',{inputValidator:v=>!!v||'请填写原因'});await rejectTask(t.taskId,value);ElMessage.success('已发送通知');load()};const approve=async t=>{await approveTask(t.taskId,'已通过');load()}
onMounted(load)
</script>
<style scoped>.page{max-width:1200px;margin:auto}.head{display:flex;justify-content:space-between;align-items:end}.head span{color:#34836a;font-weight:700}.head h1{margin:5px 0}.head p{color:#718078}.request-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:15px}.request-card{border-radius:16px}.request-head,.request-head>div,.medicine-line,.summary,.actions{display:flex;align-items:center;justify-content:space-between;gap:12px}.request-head>div{justify-content:flex-start}.request-head time{color:#89948d;font-size:13px}.medicine-line{padding:13px 0;border-bottom:1px solid #edf0ed}.medicine-line span,.order-item span{display:block;color:#7a867e;font-size:13px;margin-top:4px}.medicine-line>div:last-child{text-align:right}.summary{margin:14px 0;padding:12px;border-radius:10px;background:#f1f8f4}.summary b{color:#34764c}.actions{justify-content:flex-end}.form-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.form-grid :deep(.el-select),.form-grid :deep(.el-date-editor){width:100%}.order-item{display:grid;grid-template-columns:1.4fr .8fr 1fr 1.2fr;gap:12px;align-items:end;padding:12px 0;border-bottom:1px solid #eee}.order-item .el-form-item{margin-bottom:0}.total{text-align:right;padding:14px;font-size:16px;background:#eef8f1;border-radius:12px}.total strong{font-size:26px;color:#34764c}@media(max-width:800px){.request-grid{grid-template-columns:1fr}.form-grid,.order-item{grid-template-columns:1fr}.request-head,.summary{align-items:flex-start;flex-direction:column}}</style>
