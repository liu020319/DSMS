<template>
  <router-view v-if="$route.meta.loginPage" />
  <div v-else-if="$route.meta.publicPage" class="public-shell">
    <header class="public-nav">
      <router-link to="/" class="public-brand"><span>LX</span><div><b>小刘云</b><small>BUILD · LEARN · DELIVER</small></div></router-link>
      <nav :class="{ open: publicMenu }">
        <router-link to="/">平台首页</router-link><a href="/kanglian-cloud/">家庭健康</a>
        <router-link to="/finance">个人记账</router-link><router-link v-if="!isLoggedIn" to="/register">朋友注册</router-link><router-link to="/services">开发服务（免登录）</router-link><a href="/">个人博客</a>
      </nav>
      <div class="public-actions"><router-link v-if="!isLoggedIn" to="/login" class="nav-login">登录私有功能</router-link><router-link v-else to="/service-workspace" class="nav-login">服务管理台</router-link><button aria-label="打开导航菜单" @click="publicMenu=!publicMenu">☰</button></div>
    </header>
    <router-view @click="publicMenu=false" />
    <footer class="public-footer"><div><span class="brand-mark">LX</span><div><b>小刘云</b><small>让需求变成可运行、可讲解、可部署的作品。</small></div></div><nav><router-link to="/">平台首页</router-link><router-link to="/services">免登录开发服务</router-link><a href="/kanglian-cloud/">康联云</a><router-link to="/services">在线咨询</router-link></nav><p>独立开发者作品与技术服务平台 · 非学校官方网站</p></footer>
  </div>
  <div v-else class="shell">
    <aside :class="['sidebar', { open: menuOpen }]">
      <div class="brand"><span class="brand-mark">LX</span><div><strong>小刘云</strong><small>DIGITAL WORKSPACE</small></div></div>
      <nav @click="menuOpen = false">
        <router-link to="/"><span>◈</span>数字工作台</router-link>
        <router-link to="/finance"><span>⌁</span>个人记账</router-link>
        <router-link to="/service-workspace"><span>⌘</span>软件服务中心</router-link>
        <a href="/kanglian-cloud/"><span>✚</span>家庭用药系统</a>
        <a href="/"><span>↗</span>个人博客</a>
      </nav>
      <div class="side-foot"><i></i><div><b>服务运行正常</b><small>HTTPS 安全通道已连接</small></div></div>
    </aside>
    <div v-if="menuOpen" class="mask" @click="menuOpen = false"></div>
    <main class="workspace">
      <header class="topbar">
        <button class="menu-button" @click="menuOpen = true">☰</button>
        <div><p>{{ $route.meta.title }}</p><small>{{ today }}</small></div>
        <div class="user"><span>{{ avatar }}</span><div><b>{{ user.realName || user.username || '平台用户' }}</b><small>{{ roleName }}</small></div><button @click="logout">退出</button></div>
      </header>
      <section class="page"><router-view /></section>
      <footer class="site-footer">© 2026 小刘云 · 数字生活与软件服务平台</footer>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const menuOpen = ref(false), publicMenu = ref(false)
const user = computed(() => {
  try { return JSON.parse(localStorage.getItem('userInfo') || '{}') } catch { return {} }
})
const avatar = computed(() => (user.value.realName || user.value.username || '用').slice(0, 1))
const roleName = computed(() => ({ ADMIN: '平台管理员', GUARDIAN: '家庭守护用户', ELDER: '安心用药用户', PORTAL_USER: '个人记账用户' }[user.value.role] || '注册用户'))
const isLoggedIn = computed(() => Boolean(localStorage.getItem('token')))
const today = new Intl.DateTimeFormat('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
const logout = () => {
  localStorage.removeItem('token'); localStorage.removeItem('userInfo'); router.replace('/login')
}
</script>
