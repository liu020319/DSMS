<template>
  <div>
    <section class="hero public-hero">
      <div><p class="overline">ONE DOMAIN · MULTIPLE PRODUCTS</p><h1>一个入口，连接生活、健康与创造。</h1><p>无需登录即可了解全部产品与开发服务；只有涉及私人账务、家庭健康和交付档案时，平台才会要求安全登录。</p><div class="hero-actions"><button @click="$router.push('/services')">咨询开发服务</button><a href="/kanglian-cloud/">体验康联云</a></div></div>
      <div class="hero-orbit"><span>DATA</span><i></i><b>3</b><small>业务空间</small></div>
    </section>
    <section class="module-grid">
      <article class="module finance-card" @click="$router.push('/finance')"><span class="module-index">01 · LOGIN / INVITE</span><div class="module-icon">⌁</div><h2>十秒个人记账</h2><p>常用场景一键选，自定义消费地点会自动沉淀；朋友可凭邀请码免费注册，各自账本严格隔离。</p><b>登录或朋友注册后进入 →</b></article>
      <article class="module health-card" @click="go('/kanglian-cloud/')"><span class="module-index">02 · LOGIN</span><div class="module-icon">✚</div><h2>家庭健康协同</h2><p>购药申请、付款凭证、OBS 私有影像、收货核验与家庭消息闭环。</p><b>进入康联云 →</b></article>
      <article class="module service-card" @click="$router.push('/services')"><span class="module-index">03 · PUBLIC · NO LOGIN</span><div class="module-icon">⌘</div><h2>毕业设计指导与开发服务</h2><p>公开了解方案、能力、流程与案例；无需登录、无需注册即可提交咨询并收到专属编号。</p><b>免登录免费咨询 →</b></article>
    </section>
    <section class="architecture-strip"><div><b>统一身份</b><small>JWT + Spring Security</small></div><div><b>最小权限</b><small>RBAC + 数据所有权</small></div><div><b>私有文件</b><small>华为云 OBS + 临时访问</small></div><div><b>可回滚发布</b><small>Nginx 子路径隔离</small></div></section>
  </div>
</template>
<script setup>const go = path => { location.href = path }</script>
