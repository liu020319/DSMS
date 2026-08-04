<template>
  <div class="login-page">
    <div class="login-left">
      <div class="login-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="#fff" fill-opacity="0.2"/>
            <path d="M24 8C15.16 8 8 15.16 8 24s7.16 16 16 16 16-7.16 16-16S32.84 8 24 8zm0 4c2.21 0 4 1.79 4 4s-1.79 4-4 4-4-1.79-4-4 1.79-4 4-4zm0 22.4c-4 0-7.52-2.06-9.6-5.17.05-3.18 6.4-4.93 9.6-4.93 3.18 0 9.55 1.75 9.6 4.93-2.08 3.11-5.6 5.17-9.6 5.17z" fill="#fff"/>
          </svg>
        </div>
        <h1 class="brand-title">家庭慢病用药安全管理系统</h1>
        <p class="brand-subtitle">精准用药 · 守护家人 · 安心每一刻</p>
      </div>
      <div class="login-features">
        <div class="feature-item">
          <div class="feature-dot"></div>
          <span>分时段精准库存扣减，早中晚按时段自动计算</span>
        </div>
        <div class="feature-item">
          <div class="feature-dot"></div>
          <span>国药准字号严格校验，杜绝买错药风险</span>
        </div>
        <div class="feature-item">
          <div class="feature-dot"></div>
          <span>老人录入子女审批，双重核对保安全</span>
        </div>
        <div class="feature-item">
          <div class="feature-dot"></div>
          <span>库存不足智能预警，购药提醒不遗漏</span>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-form-wrap">
        <h2 class="form-title">欢迎登录</h2>
        <p class="form-desc">请输入您的账号信息</p>
        <el-form :model="loginForm" :rules="rules" ref="formRef" label-width="0" size="large">
          <el-form-item prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
          </el-form-item>
        </el-form>
        <div class="login-hint">
          <span>管理员: admin / 123456</span>
          <el-divider direction="vertical" />
          <span>王大爷: elder2 / 123456</span>
          <el-divider direction="vertical" />
          <span>李奶奶: elder3 / 123456</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { login } from '../api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await login(loginForm)
    userStore.setLogin(res.data)
    ElMessage.success('登录成功')
    if (res.data.role === 'ADMIN') {
      router.push('/')
    } else {
      router.push('/elder')
    }
  } catch (e) {
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  overflow: hidden;
}
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px;
  position: relative;
}
.login-left::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at 30% 50%, rgba(46, 204, 113, 0.08) 0%, transparent 50%),
              radial-gradient(circle at 70% 80%, rgba(52, 152, 219, 0.06) 0%, transparent 40%);
  animation: float 20s ease-in-out infinite;
}
@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-20px, -20px); }
}
.login-brand {
  position: relative;
  z-index: 1;
  margin-bottom: 50px;
}
.brand-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 24px;
}
.brand-icon svg {
  width: 100%;
  height: 100%;
}
.brand-title {
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px;
  letter-spacing: 2px;
}
.brand-subtitle {
  color: rgba(255,255,255,0.6);
  font-size: 16px;
  margin: 0;
  letter-spacing: 4px;
}
.login-features {
  position: relative;
  z-index: 1;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  color: rgba(255,255,255,0.75);
  font-size: 15px;
  margin-bottom: 20px;
}
.feature-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2ecc71;
  flex-shrink: 0;
}
.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}
.login-form-wrap {
  width: 360px;
}
.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}
.form-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 36px;
}
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(135deg, #0f2027, #203a43);
  border: none;
}
.login-btn:hover {
  background: linear-gradient(135deg, #1a3a4a, #2c5364);
}
.login-hint {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  margin-top: 24px;
}
</style>
