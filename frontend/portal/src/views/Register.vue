<template>
  <main class="portal-register-page">
    <section class="register-intro">
      <p class="overline">INVITE ONLY · NO SMS COST</p>
      <h1>朋友体验账号</h1>
      <p>邀请码注册不依赖付费短信。注册后可使用个人记账；家庭用药和服务管理功能仍按独立角色授权，不会自动开放。</p>
      <dl><div><dt>01</dt><dd><b>数据彼此隔离</b><span>每个账号只看自己的账本与交易</span></dd></div><div><dt>02</dt><dd><b>邀请码可随时更换</b><span>邀请码只保存在服务器环境变量，不写入代码</span></dd></div><div><dt>03</dt><dd><b>无需手机号</b><span>用户名与密码登录，不收集不必要信息</span></dd></div></dl>
    </section>
    <section class="register-card">
      <p class="overline dark">CREATE ACCOUNT</p><h2>创建记账账号</h2><p class="muted">请向站长获取当前朋友邀请码</p>
      <el-alert v-if="errorText" :title="errorText" type="warning" :closable="false" show-icon />
      <el-form label-position="top" @submit.prevent>
        <div class="form-grid"><el-form-item label="用户名"><el-input v-model.trim="form.username" maxlength="50" autocomplete="username" placeholder="4-50位字母、数字或下划线" /></el-form-item><el-form-item label="显示名称"><el-input v-model.trim="form.displayName" maxlength="50" placeholder="例如：小刘的朋友" /></el-form-item></div>
        <el-form-item label="登录密码"><el-input v-model="form.password" maxlength="64" type="password" autocomplete="new-password" show-password placeholder="至少8位，请勿使用简单密码" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="confirmPassword" maxlength="64" type="password" autocomplete="new-password" show-password placeholder="再次输入密码" /></el-form-item>
        <el-form-item label="朋友邀请码"><el-input v-model.trim="form.inviteCode" maxlength="64" type="password" show-password placeholder="向站长获取，不要公开转发" /></el-form-item>
        <button type="button" :class="['verify-box',{done:verified}]" :disabled="verified||verifying" @click="verify"><span>{{verified?'✓':verifying?'…':'◎'}}</span><div><b>{{verified?'真人验证已通过':'点击确认您是真人'}}</b><small>防止批量注册和恶意占用用户名</small></div></button>
        <el-button class="submit-button" type="primary" size="large" :loading="submitting" @click="submit">创建账号</el-button>
      </el-form>
      <div class="register-links"><router-link to="/login">已有账号，返回登录</router-link><router-link to="/">返回平台首页</router-link></div>
    </section>
  </main>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'
import { humanChallengeReadyAt, waitUntilHumanChallengeReady } from '../humanVerification'

const router=useRouter(),confirmPassword=ref(''),verified=ref(false),verifying=ref(false),submitting=ref(false),challengeId=ref(''),challengeReadyAt=ref(0),errorText=ref('')
const form=reactive({username:'',password:'',displayName:'',inviteCode:'',humanToken:''})
const challenge=async()=>{challengeId.value=(await authApi.challenge()).data.challengeId;challengeReadyAt.value=humanChallengeReadyAt()}
const verify=async()=>{verifying.value=true;errorText.value='';try{if(!challengeId.value)await challenge();await waitUntilHumanChallengeReady(challengeReadyAt.value);form.humanToken=(await authApi.verify(challengeId.value)).data.humanToken;verified.value=true}catch(e){errorText.value=e.message||'验证失败';if(e.code!==429){challengeId.value='';challengeReadyAt.value=0;challenge().catch(()=>{})}}finally{verifying.value=false}}
const submit=async()=>{if(!/^[A-Za-z0-9_]{4,50}$/.test(form.username))return ElMessage.warning('用户名只能是4-50位字母、数字或下划线');if(!form.displayName)return ElMessage.warning('请填写显示名称');if(form.password.length<8)return ElMessage.warning('密码至少8位');if(form.password!==confirmPassword.value)return ElMessage.warning('两次输入的密码不一致');if(!form.inviteCode)return ElMessage.warning('请输入朋友邀请码');if(!verified.value)return ElMessage.warning('请先完成人机验证');submitting.value=true;errorText.value='';try{await authApi.portalRegister({...form});ElMessage.success('账号创建成功，请登录');router.replace(`/login?registered=${encodeURIComponent(form.username)}`)}catch(e){errorText.value=e.message||'注册失败';verified.value=false;form.humanToken='';challengeId.value='';challengeReadyAt.value=0;challenge().catch(()=>{})}finally{submitting.value=false}}
onMounted(()=>challenge().catch(()=>{errorText.value='安全验证服务暂时不可用，请刷新重试'}))
</script>
