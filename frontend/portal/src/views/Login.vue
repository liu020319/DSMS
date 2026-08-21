<template>
  <div class="portal-login">
    <section class="login-story">
      <div class="login-logo">LX</div><p class="overline">ONE ACCOUNT · THREE CAPABILITIES</p>
      <h1>把生活数据与<br><em>创造力</em>放在一起</h1>
      <p>同一个账号进入个人记账、家庭健康协同与软件工程服务；数据按业务域隔离，权限始终由后端校验。</p>
      <div class="story-grid"><span>01<b>独立数据边界</b></span><span>02<b>全端响应式体验</b></span><span>03<b>服务过程可追踪</b></span></div>
    </section>
    <section class="login-form-wrap">
      <div class="login-form-card">
        <p class="overline dark">SECURE ACCESS</p><h2>欢迎回来</h2><p class="muted">登录您的数字工作空间</p>
        <el-alert v-if="errorText" :title="errorText" type="warning" :closable="false" show-icon />
        <el-form :model="form" @submit.prevent>
          <label>账号</label><el-input v-model.trim="form.username" size="large" maxlength="50" autocomplete="username" placeholder="手机号或用户名" />
          <label>密码</label><el-input v-model="form.password" size="large" maxlength="64" type="password" autocomplete="current-password" show-password placeholder="请输入密码" @keyup.enter="submit" />
          <button type="button" :class="['verify-box', { done: verified }]" @click="verify" :disabled="verifying || verified">
            <span>{{ verified ? '✓' : verifying ? '…' : '◎' }}</span><div><b>{{ verified ? '真人验证已通过' : verifying ? '正在完成安全验证' : '点击确认您是真人' }}</b><small>防止自动程序反复尝试登录</small></div>
          </button>
          <el-button class="submit-button" type="primary" size="large" :loading="loading" @click="submit">进入工作空间</el-button>
        </el-form>
        <div class="login-links"><router-link to="/register">朋友邀请码注册</router-link><a href="/">返回个人博客</a><a href="/kanglian-cloud/">家庭用药系统</a></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '', humanToken: '' })
const challengeId = ref(''), verified = ref(false), verifying = ref(false), loading = ref(false), errorText = ref('')
const challenge = async () => { const res = await authApi.challenge(); challengeId.value = res.data.challengeId }
const verify = async () => {
  if (verified.value) return
  verifying.value = true; errorText.value = ''
  try {
    if (!challengeId.value) await challenge()
    const res = await authApi.verify(challengeId.value)
    form.humanToken = res.data.humanToken; verified.value = true
  } catch (e) { errorText.value = e.message || '安全验证失败'; challengeId.value = ''; challenge().catch(() => {}) }
  finally { verifying.value = false }
}
const submit = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请输入账号和密码')
  if (!verified.value) return ElMessage.warning('请先点击完成人机验证')
  loading.value = true; errorText.value = ''
  try {
    const res = await authApi.login({ ...form }); localStorage.setItem('token', res.data.token); localStorage.setItem('userInfo', JSON.stringify(res.data)); router.replace(String(route.query.redirect || '/'))
  } catch (e) {
    errorText.value = e.message || '登录失败'; verified.value = false; form.humanToken = ''; challengeId.value = ''; challenge().catch(() => {})
  } finally { loading.value = false }
}
onMounted(() => { if (route.query.registered) form.username = String(route.query.registered); challenge().catch(() => { errorText.value = '安全验证服务暂时不可用，请刷新重试' }) })
</script>
