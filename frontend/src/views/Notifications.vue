<template>
  <div class="page">
    <div class="head">
      <div><span>消息中心</span><h1>家庭用药提醒</h1><p>“是否读过”和“事情办到哪一步”分别展示，不再混在一起。</p></div>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="legend"><span><i class="dot unread-dot"/>未读</span><span><i class="dot pending-dot"/>已读但待处理</span><span><i class="dot done-dot"/>业务已完成</span></div>
    <div class="list">
      <el-card v-for="n in list" :key="n.notificationId" shadow="hover" :class="['notice',{unread:n.readStatus===0}]" @click="read(n)">
        <div class="notice-head">
          <el-badge is-dot :hidden="n.readStatus===1"><strong>{{ n.title }}</strong></el-badge>
          <time>{{ n.createTime }}</time>
        </div>
        <p>{{ n.content }}</p>
        <footer>
          <div class="tags">
            <el-tag size="small" :type="n.readStatus===0?'danger':'info'">{{ n.readStatus===0?'未读':'已读' }}</el-tag>
            <el-tag size="small" :type="n.bizStatusType||'info'">{{ n.bizStatusText||'普通提醒' }}</el-tag>
            <el-tag size="small" effect="plain">{{ bizText(n.bizType) }}</el-tag>
          </div>
          <div class="actions">
            <span v-if="n.emailStatus==='SENT'">邮件已发送</span>
            <span v-else-if="n.emailStatus==='FAILED'" class="fail">邮件发送失败，站内消息不受影响</span>
            <span v-else-if="n.emailStatus==='DISABLED'">邮件待配置，站内消息已送达</span>
            <el-button v-if="targetPath(n)" link type="primary" @click.stop="openBusiness(n)">查看业务</el-button>
          </div>
        </footer>
      </el-card>
      <el-empty v-if="!list.length" description="暂无消息"/>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { getNotifications, markNotificationRead } from '../api/family'

const list=ref([]),router=useRouter(),store=useUserStore()
const load=async()=>{const response=await getNotifications();list.value=response.data||[]}
const read=async notification=>{if(notification.readStatus===0){await markNotificationRead(notification.notificationId);notification.readStatus=1;if(notification.bizStatus==='UNREAD_PENDING'){notification.bizStatus='READ_PENDING';notification.bizStatusText='已读待处理'}window.dispatchEvent(new CustomEvent('dsms-notification-read'))}}
const bizText=type=>({PURCHASE_REQUEST:'购药申请',FAMILY_ORDER:'代购订单',RECEIPT_EXCEPTION:'收货异常',FUND:'购药余额',CHECK_IN:'平安报备',LOW_STOCK:'库存提醒'}[type]||'系统消息')
const targetPath=notification=>{
  if(notification.bizType==='PURCHASE_REQUEST')return store.userInfo.role==='ELDER'?'/elder/my-apply':'/approval'
  if(['FAMILY_ORDER','RECEIPT_EXCEPTION'].includes(notification.bizType))return store.userInfo.role==='ELDER'?'/elder/orders':'/family-orders'
  if(notification.bizType==='FUND')return store.userInfo.role==='ELDER'?'/elder/fund':'/family-fund'
  if(notification.bizType==='LOW_STOCK')return store.userInfo.role==='ELDER'?'/elder/my-medicine':'/risk-center'
  return ''
}
const openBusiness=async notification=>{await read(notification);const path=targetPath(notification);if(path)router.push(path)}
onMounted(load)
</script>

<style scoped>
.page{max-width:950px;margin:auto}.head{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:12px}.head span{color:#388165;font-weight:700}.head h1{margin:5px 0}.head p{margin:0;color:#758179}.legend{display:flex;gap:18px;margin-bottom:15px;color:#6f7d77;font-size:13px}.legend span{display:flex;align-items:center;gap:6px}.dot{width:8px;height:8px;border-radius:50%}.unread-dot{background:#e34d59}.pending-dot{background:#e6a23c}.done-dot{background:#36a36c}.list{display:grid;gap:12px}.notice{cursor:pointer;border-radius:14px}.notice.unread{border-left:5px solid #e34d59;background:linear-gradient(90deg,#fff7f7,#fff 12%)}.notice-head,.notice footer,.tags,.actions{display:flex;align-items:center;justify-content:space-between;gap:10px}.notice time,.notice footer{font-size:13px;color:#89938c}.notice p{color:#56635a;line-height:1.6}.tags,.actions{justify-content:flex-start;flex-wrap:wrap}.fail{color:#d14b4b}@media(max-width:600px){.head,.notice-head,.notice footer{align-items:flex-start;flex-direction:column}.legend{flex-wrap:wrap;gap:8px 15px}.actions{width:100%}}
</style>
