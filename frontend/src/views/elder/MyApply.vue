<template>
  <div class="page">
    <div class="head"><div><h1>我的购药申请</h1><p>提交后的原始内容会固定保存，家人处理后可到“订单物流”查看。</p></div><el-button type="primary" size="large" @click="$router.push('/elder/submit-apply')">新增申请</el-button></div>
    <div class="cards">
      <el-card v-for="task in tasks" :key="task.taskId" shadow="hover" class="task-card">
        <div class="task-head"><div><el-tag :type="statusType(task.status)" size="large">{{ statusText(task.status) }}</el-tag><strong>{{ detail(task).reasonLabel || typeText(task.taskType) }}</strong></div><time>{{ task.createTime }}</time></div>
        <div v-if="task.taskType === 'PURCHASE_REQUEST'" class="items"><div v-for="item in detail(task).items || []" :key="item.prescriptionId"><span>{{ item.medicineName }} · {{ item.specification }}</span><b>{{ item.quantityBoxes }}盒 / 约¥{{ money(item.estimatedSubtotal) }}</b></div></div>
        <p v-if="detail(task).note" class="note">说明：{{ detail(task).note }}</p>
        <div class="task-foot"><span>参考总额 <b>¥{{ money(detail(task).estimatedTotal) }}</b></span><span v-if="task.handlerComment">家人回复：{{ task.handlerComment }}</span></div>
      </el-card>
      <el-empty v-if="!tasks.length" description="还没有申请记录" />
    </div>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { getMyTasks } from '../../api/approval'
import { useUserStore } from '../../stores/user'
const store=useUserStore(); const tasks=ref([]); const cache=new Map()
const detail=t=>{if(!cache.has(t.taskId)){try{cache.set(t.taskId,JSON.parse(t.contentJson||'{}'))}catch{cache.set(t.taskId,{})}}return cache.get(t.taskId)}
const money=v=>Number(v||0).toFixed(2); const typeText=t=>({NEW_MEDICINE:'新增用药',LOSS_ADJUST:'药品丢失',STOCK_CORRECT:'库存修正'}[t]||t)
const statusText=s=>({PENDING:'等待家人处理',APPROVED:'家人已下单',REJECTED:'未能处理'}[s]||s); const statusType=s=>s==='PENDING'?'warning':s==='APPROVED'?'success':'danger'
onMounted(async()=>{const r=await getMyTasks({current:1,size:100,applicantId:store.userInfo.userId});tasks.value=r.data.records||[]})
</script>
<style scoped>.page{max-width:1050px;margin:auto}.head{display:flex;justify-content:space-between;align-items:end;margin-bottom:18px}.head h1{margin:0 0 6px;color:#294b31}.head p{margin:0;color:#738078}.cards{display:grid;gap:14px}.task-card{border-radius:16px}.task-head,.task-head>div,.task-foot,.items>div{display:flex;align-items:center;justify-content:space-between;gap:14px}.task-head>div{justify-content:flex-start}.task-head time{color:#8b958d;font-size:13px}.items{margin:16px 0;padding:12px 16px;background:#f6f9f6;border-radius:12px}.items>div{padding:7px 0}.items b{color:#397747}.note{color:#657168}.task-foot{border-top:1px solid #edf0ed;padding-top:14px}.task-foot b{font-size:19px;color:#397747}@media(max-width:650px){.head{align-items:stretch;flex-direction:column;gap:12px}.task-head,.task-foot,.items>div{align-items:flex-start;flex-direction:column;gap:6px}}</style>
