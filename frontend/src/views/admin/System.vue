<template>
  <div class="page">
    <div class="page-head">
      <div><span>家庭与账号</span><h1>成员和登录安全</h1><p>管理家庭成员、绑定关系、密码重置和账号解锁。</p></div>
      <el-button type="primary" size="large" @click="handleAddUser"><el-icon><Plus /></el-icon>{{ isSystemAdmin ? '新增平台账号' : '新增家庭成员' }}</el-button>
    </div>

    <div class="summary-grid">
      <div><small>全部账号</small><b>{{ userList.length }}</b><span>个家庭成员</span></div>
      <div><small>家庭守护端</small><b>{{ adminList.length }}</b><span>负责代购和管理</span></div>
      <div><small>安心用药端</small><b>{{ allCareList.length }}</b><span>查看用药和确认收货</span></div>
      <div :class="{ danger: lockedCount }"><small>临时锁定</small><b>{{ lockedCount }}</b><span>{{ lockedCount ? '需要管理员关注' : '账号状态正常' }}</span></div>
    </div>

    <el-tabs v-model="activeTab" class="content-tabs">
      <el-tab-pane label="账号安全管理" name="user">
        <el-card shadow="never" class="table-card">
          <el-table :data="userList" stripe>
            <el-table-column label="成员" min-width="190">
              <template #default="{ row }"><div class="member"><el-avatar>{{ row.realName?.slice(0,1) || '家' }}</el-avatar><span><b>{{ row.realName }}</b><small>{{ row.username }}</small></span></div></template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号" min-width="130" />
            <el-table-column prop="email" label="通知邮箱" min-width="190"><template #default="{row}">{{ row.email || '未设置' }}</template></el-table-column>
            <el-table-column label="使用端" width="125"><template #default="{row}"><el-tag :type="row.role==='ADMIN'?'danger':row.role==='GUARDIAN'?'success':'primary'">{{ roleText(row.role) }}</el-tag></template></el-table-column>
            <el-table-column label="登录安全" min-width="190">
              <template #default="{row}"><div class="security-state"><el-tag v-if="row.status===0" type="info">已停用</el-tag><el-tag v-else-if="isLocked(row)" type="danger">已临时锁定</el-tag><el-tag v-else type="success">正常</el-tag><small v-if="isLocked(row)">至 {{ row.lockedUntil }}</small><small v-else-if="row.lastLoginTime">最近登录 {{ row.lastLoginTime }}</small><small v-else>尚无登录记录</small></div></template>
            </el-table-column>
            <el-table-column label="操作" :width="isMobile?88:330" :fixed="isMobile?false:'right'">
              <template #default="{row}"><template v-if="!isMobile"><el-button size="small" @click="handleEditUser(row)">编辑</el-button><el-button size="small" type="warning" plain @click="handleResetPwd(row)">重置密码</el-button><el-button v-if="isLocked(row)" size="small" type="danger" @click="handleUnlock(row)">解除锁定</el-button><el-button v-if="isSystemAdmin&&row.role!=='ADMIN'" size="small" type="danger" text @click="handleDelete(row)">删除</el-button></template><el-dropdown v-else trigger="click" @command="command=>handleMobileAction(command,row)"><el-button size="small" type="primary" plain>操作<el-icon><ArrowDown/></el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="edit">编辑</el-dropdown-item><el-dropdown-item command="reset">重置密码</el-dropdown-item><el-dropdown-item v-if="isLocked(row)" command="unlock">解除锁定</el-dropdown-item><el-dropdown-item v-if="isSystemAdmin&&row.role!=='ADMIN'" command="delete" divided>删除</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="家庭绑定关系" name="bind">
        <div class="bind-grid">
          <el-card shadow="never"><template #header><b>选择家庭守护人</b></template><div class="guardian-list"><button v-for="item in adminList" :key="item.userId" :class="{active:selectedAdmin?.userId===item.userId}" @click="onAdminSelect(item)"><el-avatar>{{ item.realName?.slice(0,1) }}</el-avatar><span><b>{{ item.realName }}</b><small>{{ item.username }}</small></span><el-icon><ArrowRight /></el-icon></button></div><el-empty v-if="!adminList.length" description="暂无家庭守护端账号" /></el-card>
          <el-card shadow="never"><template #header><div class="card-title"><span><b>已关联的安心用药账号</b><small v-if="selectedAdmin">当前守护人：{{ selectedAdmin.realName }}</small></span><el-button type="primary" :disabled="!selectedAdmin" @click="bindDialogVisible=true">添加关联</el-button></div></template><div class="linked-list"><div v-for="item in careList" :key="item.userId"><el-avatar>{{ item.realName?.slice(0,1) }}</el-avatar><span><b>{{ item.realName }}</b><small>{{ item.username }}</small></span><el-button type="danger" plain size="small" @click="handleUnbind(item)">解除关联</el-button></div></div><el-empty v-if="selectedAdmin&&!careList.length" description="还没有关联成员" /><el-empty v-if="!selectedAdmin" description="请先选择左侧守护人" /></el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userDialogVisible" :title="isEditUser?'编辑成员资料':'新增家庭成员'" width="520px">
      <el-form :model="userForm" label-position="top" size="large">
        <div class="form-grid"><el-form-item label="登录账号"><el-input v-model="userForm.username" :disabled="isEditUser" autocomplete="off" maxlength="50" /></el-form-item><el-form-item label="姓名"><el-input v-model="userForm.realName" maxlength="50" /></el-form-item></div>
        <el-form-item v-if="!isEditUser" label="初始密码"><el-input v-model="userForm.password" type="password" show-password autocomplete="new-password" maxlength="64" placeholder="8到64位，首次登录后建议修改" /></el-form-item>
        <div class="form-grid"><el-form-item label="手机号"><el-input v-model="userForm.phone" maxlength="20" /></el-form-item><el-form-item label="使用端"><el-select v-model="userForm.role" :disabled="!isSystemAdmin||userForm.role==='ADMIN'"><el-option v-if="isSystemAdmin" label="家庭守护端" value="GUARDIAN"/><el-option label="安心用药端" value="ELDER"/></el-select></el-form-item></div>
        <el-form-item label="通知邮箱"><el-input v-model="userForm.email" maxlength="160" placeholder="接收购药申请和异常提醒" /></el-form-item>
        <el-form-item v-if="isEditUser" label="账号状态"><el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="userDialogVisible=false">取消</el-button><el-button type="primary" @click="submitUser">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="bindDialogVisible" title="关联安心用药账号" width="420px"><p class="dialog-tip">关联后，购药申请、异常和收货信息会发送给 {{ selectedAdmin?.realName }}。</p><el-select v-model="selectedCareId" placeholder="请选择成员" style="width:100%" size="large"><el-option v-for="item in unboundCareList" :key="item.userId" :label="item.realName+'（'+item.username+'）'" :value="item.userId"/></el-select><template #footer><el-button @click="bindDialogVisible=false">取消</el-button><el-button type="primary" @click="confirmBind">确认关联</el-button></template></el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addUser, bindElder, deleteUser, getEldersByParent, getUserList, resetPassword, unbindElder, unlockUser, updateUser } from '../../api/user'
import { useUserStore } from '../../stores/user'
import { useMobile } from '../../composables/useMobile'

const store=useUserStore(),isSystemAdmin=computed(()=>store.userInfo.role==='ADMIN'),isMobile=useMobile()
const activeTab=ref('user'),userList=ref([]),adminList=ref([]),careList=ref([]),allCareList=ref([]),userDialogVisible=ref(false),bindDialogVisible=ref(false),isEditUser=ref(false),selectedAdmin=ref(null),selectedCareId=ref(null)
const userForm=ref({username:'',password:'',realName:'',phone:'',email:'',role:isSystemAdmin.value?'GUARDIAN':'ELDER',status:1})
const lockedCount=computed(()=>userList.value.filter(isLocked).length)
const unboundCareList=computed(()=>allCareList.value.filter(item=>!item.bindParentId||item.bindParentId===selectedAdmin.value?.userId))
const roleText=role=>({ADMIN:'平台管理员',GUARDIAN:'家庭守护端',ELDER:'安心用药端'}[role]||role)
const isLocked=row=>Boolean(row.lockedUntil&&new Date(row.lockedUntil.replace(' ','T')).getTime()>Date.now())

const loadUsers=async()=>{const res=await getUserList();userList.value=res.data||[];adminList.value=userList.value.filter(u=>['ADMIN','GUARDIAN'].includes(u.role));allCareList.value=userList.value.filter(u=>u.role==='ELDER');if(selectedAdmin.value){selectedAdmin.value=adminList.value.find(u=>u.userId===selectedAdmin.value.userId)||null}}
const handleAddUser=()=>{isEditUser.value=false;userForm.value={username:'',password:'',realName:'',phone:'',email:'',role:isSystemAdmin.value?'GUARDIAN':'ELDER',status:1};userDialogVisible.value=true}
const handleEditUser=row=>{isEditUser.value=true;userForm.value={userId:row.userId,username:row.username,realName:row.realName,phone:row.phone,email:row.email,role:row.role,status:row.status,bindParentId:row.bindParentId};userDialogVisible.value=true}
const submitUser=async()=>{if(!userForm.value.username||!userForm.value.realName)return ElMessage.warning('请填写登录账号和姓名');if(!isEditUser.value&&(!userForm.value.password||userForm.value.password.length<8))return ElMessage.warning('请设置8到64位的初始密码');isEditUser.value?await updateUser(userForm.value):await addUser(userForm.value);ElMessage.success('成员资料已保存');userDialogVisible.value=false;loadUsers()}
const handleResetPwd=async row=>{const{value}=await ElMessageBox.prompt('重置密码后，账号锁定也会自动解除。请输入8到64位的新密码。',`重置 ${row.realName} 的密码`,{inputType:'password',inputPattern:/^.{8,64}$/,inputErrorMessage:'密码长度应为8到64位',confirmButtonText:'确认重置',cancelButtonText:'取消'});await resetPassword(row.userId,value);ElMessage.success('密码已重置，账号锁定已解除');loadUsers()}
const handleUnlock=async row=>{await ElMessageBox.confirm(`确认解除 ${row.realName} 的登录锁定？`,'解除锁定',{type:'warning'});await unlockUser(row.userId);ElMessage.success('账号已解除锁定');loadUsers()}
const handleDelete=async row=>{const familyTip=row.role==='GUARDIAN'?'，并同时删除其绑定的安心用药账号':'';await ElMessageBox.confirm(`确认删除 ${row.realName}${familyTip}？历史业务记录会保留用于审计。`,'删除账号',{type:'error',confirmButtonText:'确认删除',cancelButtonText:'取消'});await deleteUser(row.userId);ElMessage.success('账号已删除');await loadUsers()}
const handleMobileAction=(command,row)=>({edit:()=>handleEditUser(row),reset:()=>handleResetPwd(row),unlock:()=>handleUnlock(row),delete:()=>handleDelete(row)}[command]?.())
const onAdminSelect=async row=>{selectedAdmin.value=row;if(row){const res=await getEldersByParent(row.userId);careList.value=res.data||[]}}
const confirmBind=async()=>{if(!selectedAdmin.value||!selectedCareId.value)return ElMessage.warning('请选择需要关联的成员');await bindElder(selectedCareId.value,selectedAdmin.value.userId);ElMessage.success('家庭关联已建立');bindDialogVisible.value=false;selectedCareId.value=null;await loadUsers();onAdminSelect(selectedAdmin.value)}
const handleUnbind=async row=>{await ElMessageBox.confirm(`解除 ${row.realName} 与 ${selectedAdmin.value.realName} 的家庭关联？`,'解除关联',{type:'warning'});await unbindElder(row.userId);ElMessage.success('关联已解除');await loadUsers();onAdminSelect(selectedAdmin.value)}
onMounted(loadUsers)
</script>

<style scoped>
.page{max-width:1280px;margin:auto}.page-head{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin-bottom:20px}.page-head>div>span{color:#268064;font-weight:800}.page-head h1{margin:6px 0;color:#173d3b;font-size:30px}.page-head p{margin:0;color:#72827e}.summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:20px}.summary-grid>div{padding:18px;border:1px solid #e2ebe7;border-radius:15px;background:#fff}.summary-grid small,.summary-grid b,.summary-grid span{display:block}.summary-grid small{color:#7a8a86}.summary-grid b{margin:7px 0 4px;color:#1e765b;font-size:28px}.summary-grid span{color:#9aa6a3;font-size:12px}.summary-grid .danger b{color:#d55252}.content-tabs{padding:4px 18px 18px;border-radius:17px;background:#fff}.table-card{border:0}.member{display:flex;align-items:center;gap:10px}.member .el-avatar{background:#e5f4ed;color:#21765c}.member b,.member small,.security-state small{display:block}.member small,.security-state small{margin-top:3px;color:#899692;font-size:12px}.security-state .el-tag{margin-right:8px}.bind-grid{display:grid;grid-template-columns:minmax(260px,.75fr) minmax(420px,1.25fr);gap:16px}.guardian-list button{width:100%;display:flex;align-items:center;gap:11px;margin-bottom:8px;padding:12px;border:1px solid #e5ece9;border-radius:12px;color:#405753;background:#fff;text-align:left;cursor:pointer}.guardian-list button.active{border-color:#5aaf90;background:#eef8f3}.guardian-list span{flex:1}.guardian-list b,.guardian-list small,.card-title b,.card-title small,.linked-list b,.linked-list small{display:block}.guardian-list small,.linked-list small,.card-title small{margin-top:3px;color:#899692}.linked-list>div{display:flex;align-items:center;gap:11px;padding:13px 0;border-bottom:1px solid #edf1ef}.linked-list span{flex:1}.card-title{display:flex;align-items:center;justify-content:space-between;gap:12px}.form-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.form-grid :deep(.el-select){width:100%}.dialog-tip{margin-top:0;color:#71807c;line-height:1.6}
@media(max-width:900px){.page-head{align-items:stretch;flex-direction:column}.page-head h1{font-size:25px}.summary-grid{grid-template-columns:1fr 1fr}.bind-grid{grid-template-columns:1fr}.content-tabs{padding:4px 8px 12px}.table-card :deep(.el-card__body){padding:0}.form-grid{grid-template-columns:1fr}}
@media(max-width:520px){.summary-grid{gap:9px}.summary-grid>div{padding:14px}.summary-grid b{font-size:23px}.page-head .el-button{width:100%}}
</style>
