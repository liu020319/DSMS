<template>
  <div class="security-page">
    <section class="security-hero">
      <div class="security-icon"><el-icon><Lock /></el-icon></div>
      <div>
        <h2>账号与安全</h2>
        <p>定期更新登录密码，避免使用手机号、生日或连续数字作为长期密码。</p>
      </div>
    </section>

    <el-card class="security-card" shadow="never">
      <template #header>
        <div class="card-title">
          <span>修改登录密码</span>
          <el-tag type="success" effect="plain">BCrypt 加密保存</el-tag>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="form.currentPassword" type="password" show-password autocomplete="current-password" placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" placeholder="至少8位，建议包含数字和字母" />
          <div class="strength-row">
            <span>密码强度</span>
            <span :class="['strength-value', strengthClass]">{{ strengthLabel }}</span>
          </div>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="请再次输入新密码" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" class="submit-button" :loading="loading" @click="submit">保存新密码</el-button>
      </el-form>
    </el-card>

    <el-alert
      title="密码修改成功后会退出当前账号，请使用新密码重新登录。"
      type="info"
      :closable="false"
      show-icon
    />
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '../api/user'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })

const strengthScore = computed(() => {
  const value = form.newPassword
  let score = 0
  if (value.length >= 8) score++
  if (/[A-Za-z]/.test(value)) score++
  if (/\d/.test(value)) score++
  if (/[^A-Za-z0-9]/.test(value)) score++
  return score
})
const strengthLabel = computed(() => ['未设置', '较弱', '一般', '良好', '很强'][strengthScore.value])
const strengthClass = computed(() => `strength-${strengthScore.value}`)

const validateConfirm = (_, value, callback) => {
  if (value !== form.newPassword) callback(new Error('两次输入的新密码不一致'))
  else callback()
}

const rules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度应为8到64位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const submit = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    await changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    router.replace('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.security-page { max-width: 720px; margin: 0 auto; }
.security-hero { display: flex; gap: 18px; align-items: center; padding: 26px; margin-bottom: 18px; border-radius: 18px; color: #fff; background: linear-gradient(135deg, #143642, #246b5e); box-shadow: 0 14px 36px rgba(20, 54, 66, .18); }
.security-hero h2 { margin: 0 0 8px; font-size: 24px; }
.security-hero p { margin: 0; color: rgba(255,255,255,.75); line-height: 1.7; }
.security-icon { width: 52px; height: 52px; flex: 0 0 52px; display: grid; place-items: center; border-radius: 15px; background: rgba(255,255,255,.16); font-size: 26px; }
.security-card { margin-bottom: 16px; border-radius: 16px; }
.card-title { display: flex; justify-content: space-between; align-items: center; font-weight: 700; }
.strength-row { width: 100%; display: flex; justify-content: space-between; margin-top: 8px; color: #8a94a6; font-size: 12px; }
.strength-value { font-weight: 700; }
.strength-1 { color: #f56c6c; }.strength-2 { color: #e6a23c; }.strength-3 { color: #409eff; }.strength-4 { color: #67c23a; }
.submit-button { width: 100%; height: 44px; }
@media (max-width: 640px) {
  .security-hero { align-items: flex-start; padding: 20px; border-radius: 14px; }
  .security-hero h2 { font-size: 21px; }
  .security-hero p { font-size: 13px; }
}
</style>
