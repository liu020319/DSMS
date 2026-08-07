<template>
  <div class="login-page">
    <section class="brand-panel">
      <div class="brand-content">
        <div class="brand-badge"><el-icon><FirstAidKit /></el-icon></div>
        <p class="eyebrow">FAMILY MEDICATION CARE</p>
        <h1>安康药管家</h1>
        <p class="brand-subtitle">让每一次买药、收药和用药，都有人认真守护。</p>
        <div class="role-cards">
          <div><el-icon><House /></el-icon><span><b>家庭守护端</b><small>代购、物流、资金与异常处理</small></span></div>
          <div><el-icon><Sunny /></el-icon><span><b>安心用药端</b><small>用药、申请、收货与平安反馈</small></span></div>
        </div>
        <ul>
          <li><el-icon><CircleCheck /></el-icon>购药申请与家庭消息实时联动</li>
          <li><el-icon><CircleCheck /></el-icon>数量、国药准字号和收货照片逐项核验</li>
          <li><el-icon><CircleCheck /></el-icon>费用凭证、库存和用药计划统一管理</li>
        </ul>
      </div>
      <div class="soft-circle circle-one"></div><div class="soft-circle circle-two"></div>
    </section>

    <main class="form-panel">
      <div class="login-card">
        <div class="mobile-brand"><span>安</span><div><b>安康药管家</b><small>家庭慢病用药安全管理</small></div></div>
        <header><span>安全登录</span><h2>欢迎回来</h2><p>请使用家庭管理员为您设置的账号登录</p></header>

        <el-alert v-if="loginError" :title="loginError" :type="lockRemaining > 0 ? 'error' : 'warning'" :closable="false" show-icon class="login-alert" />

        <el-form ref="formRef" :model="loginForm" :rules="rules" label-position="top" size="large" @submit.prevent>
          <el-form-item label="登录账号" prop="username">
            <el-input v-model.trim="loginForm.username" name="username" autocomplete="username" maxlength="50" placeholder="请输入手机号或用户名" prefix-icon="User" clearable />
          </el-form-item>
          <el-form-item label="登录密码" prop="password">
            <el-input v-model="loginForm.password" name="password" autocomplete="current-password" maxlength="64" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
          </el-form-item>

          <button type="button" :class="['human-check', humanState]" :disabled="humanState === 'verifying' || lockRemaining > 0" @click="handleHumanVerify">
            <span class="check-icon"><el-icon v-if="humanState === 'verified'"><CircleCheckFilled /></el-icon><el-icon v-else-if="humanState === 'verifying'" class="is-loading"><Loading /></el-icon><el-icon v-else><Pointer /></el-icon></span>
            <span><b>{{ humanText }}</b><small>{{ humanState === 'verified' ? '本次验证2分钟内有效' : '防止自动程序反复尝试登录' }}</small></span>
            <el-icon class="arrow"><ArrowRight /></el-icon>
          </button>

          <el-button type="primary" native-type="submit" class="login-button" :loading="loading" :disabled="humanState !== 'verified' || lockRemaining > 0" @click="handleLogin">
            {{ lockRemaining > 0 ? `账号锁定中 ${countdownText}` : '安全登录' }}
          </el-button>
        </el-form>

        <div class="account-help">
          <el-icon><Warning /></el-icon>
          <p><b>忘记密码或账号被锁定？</b><span>请联系家庭管理员重置密码或解除锁定。连续输错5次，账号将锁定15分钟。</span></p>
        </div>
        <footer><el-icon><Lock /></el-icon>登录信息需通过 HTTPS 加密传输，请勿在公共设备保存密码</footer>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { createHumanChallenge, login, verifyHumanChallenge } from '../api/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)
const humanState = ref('idle')
const challengeId = ref('')
const loginError = ref('')
const lockRemaining = ref(0)
let countdownTimer

const loginForm = reactive({ username: '', password: '', humanToken: '' })
const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
}
const humanText = computed(() => humanState.value === 'verified' ? '验证通过' : humanState.value === 'verifying' ? '正在验证…' : '点击确认您是真人')
const countdownText = computed(() => {
  const minutes = Math.floor(lockRemaining.value / 60)
  const seconds = String(lockRemaining.value % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})

const refreshChallenge = async () => {
  const res = await createHumanChallenge()
  challengeId.value = res.data.challengeId
}

const handleHumanVerify = async () => {
  if (humanState.value === 'verified') return
  humanState.value = 'verifying'
  loginError.value = ''
  try {
    if (!challengeId.value) await refreshChallenge()
    const res = await verifyHumanChallenge(challengeId.value)
    loginForm.humanToken = res.data.humanToken
    humanState.value = 'verified'
  } catch (error) {
    humanState.value = 'idle'
    challengeId.value = ''
    loginError.value = error.message || '验证失败，请重新点击'
    try { await refreshChallenge() } catch (e) {}
  }
}

const startCountdown = seconds => {
  clearInterval(countdownTimer)
  lockRemaining.value = Math.max(1, Number(seconds || 900))
  countdownTimer = setInterval(() => {
    lockRemaining.value -= 1
    if (lockRemaining.value <= 0) {
      clearInterval(countdownTimer)
      loginError.value = '锁定时间已结束，请重新完成人机验证后登录'
      humanState.value = 'idle'
      refreshChallenge().catch(() => {})
    }
  }, 1000)
}

const resetHumanVerification = async () => {
  loginForm.humanToken = ''
  humanState.value = 'idle'
  challengeId.value = ''
  try { await refreshChallenge() } catch (e) {}
}

const handleLogin = async () => {
  if (lockRemaining.value > 0) return
  if (humanState.value !== 'verified') return ElMessage.warning('请先点击完成人机验证')
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  loginError.value = ''
  try {
    const res = await login({ ...loginForm })
    userStore.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push(res.data.role === 'ADMIN' ? '/' : '/elder')
  } catch (error) {
    loginError.value = error.message || '登录失败，请检查账号和密码'
    if (error.code === 423) startCountdown(error.data?.remainingSeconds)
    await resetHumanVerification()
  } finally {
    loading.value = false
  }
}

onMounted(() => refreshChallenge().catch(() => { loginError.value = '安全验证暂时不可用，请刷新页面重试' }))
onBeforeUnmount(() => clearInterval(countdownTimer))
</script>

<style scoped>
.login-page{min-height:100vh;display:grid;grid-template-columns:minmax(480px,1.08fr) minmax(480px,.92fr);background:#f4f7f6}.brand-panel{position:relative;overflow:hidden;display:flex;align-items:center;padding:72px clamp(48px,7vw,112px);color:#fff;background:linear-gradient(145deg,#12363a 0%,#1d5950 55%,#28745b 100%)}.brand-content{position:relative;z-index:2;max-width:610px}.brand-badge{width:68px;height:68px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.28);border-radius:20px;background:rgba(255,255,255,.13);font-size:34px;box-shadow:0 16px 40px rgba(7,35,31,.2)}.eyebrow{margin:28px 0 9px;color:#8ee3bd;font-size:12px;font-weight:800;letter-spacing:3px}.brand-panel h1{margin:0;font-size:48px;letter-spacing:3px}.brand-subtitle{max-width:520px;margin:18px 0 32px;color:rgba(255,255,255,.78);font-size:19px;line-height:1.8}.role-cards{display:grid;grid-template-columns:1fr 1fr;gap:14px}.role-cards>div{display:flex;align-items:center;gap:13px;padding:17px;border:1px solid rgba(255,255,255,.15);border-radius:15px;background:rgba(255,255,255,.08);backdrop-filter:blur(8px)}.role-cards .el-icon{font-size:25px;color:#8ee3bd}.role-cards b,.role-cards small{display:block}.role-cards b{font-size:16px}.role-cards small{margin-top:5px;color:rgba(255,255,255,.62);font-size:12px}.brand-panel ul{list-style:none;margin:30px 0 0;padding:0}.brand-panel li{display:flex;align-items:center;gap:10px;margin:14px 0;color:rgba(255,255,255,.82)}.brand-panel li .el-icon{color:#8ee3bd}.soft-circle{position:absolute;border-radius:50%;border:1px solid rgba(255,255,255,.08)}.circle-one{width:460px;height:460px;right:-190px;top:-120px}.circle-two{width:330px;height:330px;left:-170px;bottom:-110px}.form-panel{display:grid;place-items:center;padding:48px}.login-card{width:min(430px,100%)}.mobile-brand{display:none}.login-card header>span{color:#1d795d;font-size:13px;font-weight:800;letter-spacing:2px}.login-card h2{margin:8px 0 9px;color:#183b39;font-size:35px}.login-card header p{margin:0 0 28px;color:#7c8b88}.login-alert{margin-bottom:18px;border-radius:10px}.login-card :deep(.el-form-item__label){color:#344d4a;font-weight:700}.login-card :deep(.el-input__wrapper){min-height:50px;border-radius:11px;box-shadow:0 0 0 1px #dce5e2 inset}.login-card :deep(.el-input__wrapper.is-focus){box-shadow:0 0 0 1px #258766 inset}.human-check{width:100%;display:flex;align-items:center;gap:12px;margin:5px 0 18px;padding:12px 14px;border:1px solid #d8e3df;border-radius:12px;color:#536965;background:#f8faf9;text-align:left;cursor:pointer;transition:.2s}.human-check:hover{border-color:#5aaa8e;background:#f1f8f5}.human-check.verified{border-color:#69b89a;color:#176c51;background:#ecf8f2}.human-check:disabled{cursor:not-allowed}.human-check .check-icon{width:34px;height:34px;display:grid;place-items:center;border-radius:50%;color:#fff;background:#809b94;font-size:18px}.human-check.verified .check-icon{background:#29a573}.human-check b,.human-check small{display:block}.human-check small{margin-top:3px;color:#8b9996}.human-check .arrow{margin-left:auto}.login-button{width:100%;height:50px;border:0;border-radius:11px;background:linear-gradient(135deg,#1b7259,#2b9370);font-size:16px;font-weight:750;box-shadow:0 11px 24px rgba(31,126,95,.2)}.account-help{display:flex;gap:11px;margin-top:22px;padding:14px;border-radius:12px;color:#6d5d3d;background:#fff9ec}.account-help>.el-icon{margin-top:2px;color:#c28b27;font-size:19px}.account-help p{margin:0}.account-help b,.account-help span{display:block}.account-help b{font-size:13px}.account-help span{margin-top:5px;font-size:12px;line-height:1.55}.login-card footer{display:flex;align-items:center;justify-content:center;gap:6px;margin-top:22px;color:#9aa5a2;font-size:11px}
@media(max-width:900px){.login-page{display:block;background:linear-gradient(180deg,#17443f 0,#1e6752 220px,#f4f7f6 220px)}.brand-panel{display:none}.form-panel{display:block;padding:22px 16px 36px}.login-card{width:100%;max-width:500px;margin:auto;padding:24px 20px;border-radius:22px;background:#fff;box-shadow:0 18px 45px rgba(14,54,45,.16)}.mobile-brand{display:flex;align-items:center;gap:11px;margin-bottom:27px}.mobile-brand>span{width:43px;height:43px;display:grid;place-items:center;border-radius:13px;color:#fff;background:linear-gradient(135deg,#1d795d,#4aa584);font-size:21px;font-weight:800}.mobile-brand b,.mobile-brand small{display:block}.mobile-brand b{color:#1a4941;font-size:18px}.mobile-brand small{margin-top:3px;color:#82928e;font-size:11px}.login-card h2{font-size:28px}.login-card header>span{display:none}.login-card header p{margin-bottom:22px}.account-help{align-items:flex-start}.login-card footer{line-height:1.5;text-align:center}}
@media(max-width:380px){.form-panel{padding:14px 10px 24px}.login-card{padding:21px 15px}.login-card footer{font-size:10px}}
</style>
