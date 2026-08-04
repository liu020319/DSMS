<template>
  <div>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="用户管理" name="user">
        <el-card>
          <el-button type="success" style="margin-bottom: 15px" @click="handleAddUser">新增用户</el-button>
          <el-table :data="userList" border stripe>
            <el-table-column prop="userId" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="realName" label="真实姓名" width="120" />
            <el-table-column prop="phone" label="手机号" width="130" />
            <el-table-column prop="role" label="角色" width="80">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'success' : 'warning'">{{ row.role === 'ADMIN' ? '子女' : '老人' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="handleEditUser(row)">编辑</el-button>
                <el-button size="small" type="warning" @click="handleResetPwd(row)">重置密码</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="绑定关系管理" name="bind">
        <el-card>
          <el-row :gutter="20">
            <el-col :span="12">
              <h4>子女列表</h4>
              <el-table :data="adminList" border size="small" highlight-current-row @current-change="onAdminSelect">
                <el-table-column prop="userId" label="ID" width="60" />
                <el-table-column prop="realName" label="姓名" />
              </el-table>
            </el-col>
            <el-col :span="12">
              <h4>绑定的老人</h4>
              <el-table :data="elderList" border size="small">
                <el-table-column prop="userId" label="ID" width="60" />
                <el-table-column prop="realName" label="姓名" />
                <el-table-column label="操作" width="80">
                  <template #default="{ row }">
                    <el-button size="small" type="danger" @click="handleUnbind(row)">解绑</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-button type="primary" style="margin-top: 10px" @click="bindDialogVisible = true" :disabled="!selectedAdmin">绑定老人</el-button>
            </el-col>
          </el-row>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userDialogVisible" :title="isEditUser ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" :disabled="isEditUser" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="userForm.realName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="子女(ADMIN)" value="ADMIN" />
            <el-option label="老人(ELDER)" value="ELDER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="bindDialogVisible" title="绑定老人" width="400px">
      <el-select v-model="selectedElderId" placeholder="选择老人" style="width: 100%">
        <el-option v-for="e in allElderList" :key="e.userId" :label="e.realName + '(' + e.username + ')'" :value="e.userId" />
      </el-select>
      <template #footer>
        <el-button @click="bindDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmBind">确定绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getUserList, addUser, updateUser, resetPassword, bindElder, getEldersByParent } from '../../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('user')
const userList = ref([])
const adminList = ref([])
const elderList = ref([])
const allElderList = ref([])
const userDialogVisible = ref(false)
const bindDialogVisible = ref(false)
const isEditUser = ref(false)
const selectedAdmin = ref(null)
const selectedElderId = ref(null)

const userForm = ref({ username: '', realName: '', phone: '', role: 'ELDER' })

const loadUsers = async () => {
  const res = await getUserList()
  userList.value = res.data
  adminList.value = res.data.filter(u => u.role === 'ADMIN')
  allElderList.value = res.data.filter(u => u.role === 'ELDER')
}

const handleAddUser = () => {
  isEditUser.value = false
  userForm.value = { username: '', realName: '', phone: '', role: 'ELDER' }
  userDialogVisible.value = true
}

const handleEditUser = (row) => {
  isEditUser.value = true
  userForm.value = { ...row }
  userDialogVisible.value = true
}

const submitUser = async () => {
  if (isEditUser.value) {
    await updateUser(userForm.value)
  } else {
    await addUser(userForm.value)
  }
  ElMessage.success('操作成功')
  userDialogVisible.value = false
  loadUsers()
}

const handleResetPwd = async (row) => {
  await ElMessageBox.confirm('确定重置该用户密码为123456？', '提示')
  await resetPassword(row.userId, '123456')
  ElMessage.success('密码已重置为123456')
}

const onAdminSelect = async (row) => {
  selectedAdmin.value = row
  if (row) {
    const res = await getEldersByParent(row.userId)
    elderList.value = res.data
  }
}

const confirmBind = async () => {
  if (!selectedAdmin.value || !selectedElderId.value) return
  await bindElder(selectedElderId.value, selectedAdmin.value.userId)
  ElMessage.success('绑定成功')
  bindDialogVisible.value = false
  onAdminSelect(selectedAdmin.value)
}

const handleUnbind = async (row) => {
  await ElMessageBox.confirm('确定解绑？', '提示')
  await updateUser({ userId: row.userId, bindParentId: null })
  ElMessage.success('已解绑')
  onAdminSelect(selectedAdmin.value)
}

onMounted(loadUsers)
</script>
