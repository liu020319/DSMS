<template>
  <main class="service-landing">
    <div class="scroll-progress" :style="{ width: scrollPercent + '%' }"></div>
    <section class="service-hero">
      <div class="tech-grid"></div><div class="fiber fiber-a"></div><div class="fiber fiber-b"></div>
      <div class="service-hero-copy">
        <p class="overline">FROM CAMPUS IDEA TO RUNNING SOFTWARE</p>
        <h1>让毕业设计<br>从流程图走向<em>上线</em></h1>
        <p>面向大学生与小微团队，提供毕业设计开发指导、需求拆解、代码讲解、调试测试、云端部署和软件定制。每一步都要能运行、能解释、能复现。</p>
        <div class="hero-actions"><button @click="scrollContact">免费评估需求</button><button class="ghost" @click="scrollCapabilities">查看交付能力</button></div>
        <div class="campus-note"><span>WUHAN · JIANGXIA</span><b>应用实践 × 产教融合 × 学以致用</b><small>独立开发者服务，非武汉纺织大学外经贸学院官方平台</small></div>
      </div>
      <div class="code-orbit"><div class="orbit-core"><small>FULL STACK</small><b>0 → 1</b><span>DELIVERY</span></div><i class="node n1">JAVA</i><i class="node n2">VUE</i><i class="node n3">MYSQL</i><i class="node n4">LINUX</i></div>
    </section>

    <section class="trust-marquee"><div><span v-for="item in stack" :key="item">{{item}}</span><span v-for="item in stack" :key="item+'copy'">{{item}}</span></div></section>

    <section ref="capabilitiesRef" class="landing-section">
      <header class="section-heading"><p class="overline dark">CAPABILITY MATRIX</p><h2>从“我有一个想法”到“服务器真的跑起来”</h2><p>不是只交一份看不懂的代码，而是把业务、技术、测试和部署串成完整闭环。</p></header>
      <div class="capability-grid"><article v-for="(item,index) in capabilities" :key="item.title"><span>0{{index+1}}</span><div>{{item.icon}}</div><h3>{{item.title}}</h3><p>{{item.desc}}</p><ul><li v-for="point in item.points" :key="point">{{point}}</li></ul></article></div>
    </section>

    <section class="proof-section">
      <div class="proof-copy"><p class="overline">REAL PROJECT · REAL DELIVERY</p><h2>不是模板换皮，<br>是能接受追问的项目。</h2><p>以“康联云”家庭慢病用药安全管理系统为代表，从前后端开发、数据建模、权限设计，到华为云 OBS 私有文件、Nginx 子路径部署与回滚，形成真实上线链路。</p><a href="/kanglian-cloud/">查看在线项目 →</a></div>
      <div class="proof-terminal"><header><i></i><i></i><i></i><span>release-check.sh</span></header><div class="terminal-body"><p v-for="line in terminalLines" :key="line.text" :class="line.kind"><b v-if="line.prefix">$</b> {{line.text}}</p></div></div>
    </section>

    <section class="landing-section"><header class="section-heading"><p class="overline dark">DELIVERY PROCESS</p><h2>每个节点都有产物，不靠一句“差不多好了”</h2></header><div class="process-line"><article v-for="(step,index) in process" :key="step.title"><span>{{String(index+1).padStart(2,'0')}}</span><h3>{{step.title}}</h3><p>{{step.desc}}</p></article></div></section>

    <section class="boundary-section"><div><p class="overline">ACADEMIC INTEGRITY</p><h2>指导你做出来，也教你讲明白。</h2></div><p>支持选题分析、系统设计、代码开发指导、缺陷修复、测试、部署和答辩技术讲解；不提供论文代写、考试作弊、伪造数据或冒名提交。真正经得住答辩的项目，必须由你理解并参与。</p></section>

    <section ref="contactRef" class="contact-section">
      <div class="contact-copy"><p class="overline">START A PROJECT</p><h2>把你的题目或想法<br>发给我看看。</h2><p>无需注册，也不需要短信验证码。系统会生成咨询编号和随机访问码，用它们查询进度或继续留言。</p><dl><div><dt>01</dt><dd><b>隐私保护</b><span>联系方式仅用于本次沟通，不公开展示</span></dd></div><div><dt>02</dt><dd><b>拒绝模糊报价</b><span>先拆功能、工期和风险，再给建议</span></dd></div><div><dt>03</dt><dd><b>可学习交付</b><span>代码、文档、测试和部署说明一并交付</span></dd></div></dl></div>
      <div class="inquiry-card">
        <div v-if="success.inquiryNo" class="inquiry-success"><span>✓</span><h3>咨询已收到</h3><p>请立即保存下面两项，访问码不会再次公开显示。</p><label>咨询编号</label><b>{{success.inquiryNo}}</b><label>随机访问码</label><b>{{success.accessCode}}</b><button @click="copyCredentials">复制编号和访问码</button><small>提交和留言已经通过邮件链路提醒站长。</small><button class="ghost-button" @click="resetForm">继续提交</button></div>
        <el-form v-else label-position="top"><div class="form-grid"><el-form-item label="怎么称呼你"><el-input v-model="form.contactName" maxlength="80" placeholder="姓名或昵称" /></el-form-item><el-form-item label="联系方式"><el-input v-model="form.contactValue" maxlength="160" placeholder="微信号 / QQ / 邮箱" /></el-form-item><el-form-item label="想咨询什么"><el-select v-model="form.serviceType" class="full"><el-option v-for="(name,key) in serviceNames" :key="key" :label="name" :value="key" /></el-select></el-form-item><el-form-item label="项目方向"><el-input v-model="form.projectType" maxlength="80" placeholder="Java Web、小程序、管理系统…" /></el-form-item></div><el-form-item label="请描述题目、现状和希望解决的问题"><el-input v-model="form.inquiryText" type="textarea" :rows="6" minlength="10" maxlength="3000" show-word-limit placeholder="例如：已有需求文档，需要完成 Spring Boot + Vue 项目，并希望理解部署流程……" /></el-form-item><button type="button" :class="['public-verify',{done:verified}]" @click="verify" :disabled="verified||verifying"><span>{{verified?'✓':verifying?'…':'◎'}}</span><div><b>{{verified?'真人验证已通过':'点击确认您是真人'}}</b><small>用于阻止垃圾留言和自动提交</small></div></button><button type="button" class="inquiry-submit" :disabled="submitting" @click="submit">{{submitting?'正在提交…':'提交免费评估 →'}}</button><p class="privacy-text">请勿填写密码、身份证号、云密钥等敏感数据。提交即同意仅为需求沟通保存以上信息。</p></el-form>
      </div>
    </section>

    <section class="query-section"><div><p class="overline dark">NO ACCOUNT REQUIRED</p><h2>已有咨询？直接查询进度</h2><p>输入咨询编号和随机访问码即可查看站长回复、处理状态并继续留言，不需要创建账号。</p></div><div class="query-box"><el-input v-model.trim="query.inquiryNo" placeholder="咨询编号" /><el-input v-model.trim="query.accessCode" type="password" show-password placeholder="随机访问码" /><el-button type="primary" @click="queryProgress">查询进度</el-button></div></section>
    <el-drawer v-model="progressDrawer" title="咨询进度中心" :size="drawerSize"><template v-if="progress.inquiryNo"><div class="progress-title"><span>{{serviceNames[progress.serviceType]}}</span><h2>{{progress.inquiryNo}}</h2><i>{{statusNames[progress.status]}}</i><p>{{progress.inquiryText}}</p></div><div class="message-stream"><article v-for="m in progress.messages" :key="m.messageId" :class="m.senderType.toLowerCase()"><b>{{m.senderType==='ADMIN'?'站长回复':'我的留言'}}</b><p>{{m.messageText}}</p><small>{{m.createTime}}</small></article><el-empty v-if="!progress.messages?.length" description="暂时没有新回复" /></div><div v-if="progress.status!=='CLOSED'" class="visitor-reply"><el-input v-model="reply.messageText" type="textarea" :rows="4" maxlength="3000" placeholder="补充需求或回复消息"/><button type="button" :class="['public-verify',{done:replyVerified}]" @click="verifyReply" :disabled="replyVerified||replyVerifying"><span>{{replyVerified?'✓':'◎'}}</span><div><b>{{replyVerified?'验证通过':'点击完成人机验证'}}</b></div></button><el-button type="primary" @click="addMessage">发送留言</el-button></div></template></el-drawer>
  </main>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { authApi, publicServiceApi, reportError } from '../api'

const capabilitiesRef=ref(),contactRef=ref(),verified=ref(false),verifying=ref(false),submitting=ref(false),challengeId=ref(''),success=reactive({inquiryNo:'',accessCode:''}),scrollPercent=ref(0),viewportWidth=ref(window.innerWidth)
const progressDrawer=ref(false),progress=ref({}),replyVerified=ref(false),replyVerifying=ref(false),replyChallengeId=ref('')
const query=reactive({inquiryNo:'',accessCode:''}),reply=reactive({messageText:'',humanToken:''})
const drawerSize=computed(()=>viewportWidth.value<720?'100%':'min(680px,76vw)')
const stack=['SPRING BOOT','SPRING SECURITY','JWT / RBAC','VUE 3','MYSQL 8','HUAWEI OBS','NGINX','LINUX / SYSTEMD','MAVEN','VITE']
const capabilities=[{icon:'⌘',title:'毕业设计开发指导',desc:'围绕真实选题完成需求、架构、数据和模块拆解。',points:['选题可行性与工作量评估','ER图、接口与权限设计','代码讲解与答辩技术梳理']},{icon:'◫',title:'全栈项目开发',desc:'管理系统、小程序后端与响应式网站从零搭建。',points:['Spring Boot + Vue 前后端','MySQL 建模与业务闭环','PC / 手机同一套自适应页面']},{icon:'⚡',title:'调试测试与优化',desc:'不只修表面报错，同时检查边界、权限和隐藏缺陷。',points:['接口、服务、构建全量测试','数据越权与文件安全检查','性能瓶颈与移动端适配']},{icon:'↗',title:'部署上线与运维',desc:'把本地项目真正运行在云服务器，并能安全回滚。',points:['Maven / Vite 生产打包','Nginx、HTTPS、systemd','校验值、备份与回滚文档']}]
const process=[{title:'需求诊断',desc:'明确用户、场景、必须功能、边界和验收标准。'},{title:'方案与报价',desc:'拆解模块、技术栈、里程碑、工期和风险。'},{title:'迭代开发',desc:'按可运行版本推进，每个阶段都能演示。'},{title:'测试评审',desc:'功能、权限、移动端、构建和安全自查。'},{title:'部署交付',desc:'上线、验证、文档、讲解与后续支持。'}]
const terminalLines=[{text:'mvn clean package',prefix:true},{text:'BUILD SUCCESS',kind:'ok'},{text:'npm run build',prefix:true},{text:'built in 18.42s',kind:'ok'},{text:'sudo nginx -t',prefix:true},{text:'syntax is ok',kind:'ok'},{text:'DEPLOYMENT_VERIFIED ✓',kind:'verified'}]
const serviceNames={GUIDANCE:'毕业设计开发指导',DEVELOPMENT:'软件定制开发',DEBUG:'调试与缺陷修复',DEPLOYMENT:'部署上线与运维',CONSULTING:'技术方案咨询'},statusNames={NEW:'已收到',CONTACTED:'沟通中',CLOSED:'已结束'}
const form=reactive({contactName:'',contactValue:'',serviceType:'GUIDANCE',projectType:'',inquiryText:'',sourcePath:'/cloud-hub/#/services',humanToken:''})
const scrollContact=()=>contactRef.value?.scrollIntoView({behavior:'smooth'}),scrollCapabilities=()=>capabilitiesRef.value?.scrollIntoView({behavior:'smooth'})
const createChallenge=async target=>{const r=await authApi.challenge();target.value=r.data.challengeId}
const verify=async()=>{verifying.value=true;try{if(!challengeId.value)await createChallenge(challengeId);const r=await authApi.verify(challengeId.value);form.humanToken=r.data.humanToken;verified.value=true}catch(e){reportError(e,'验证失败');challengeId.value='';createChallenge(challengeId).catch(()=>{})}finally{verifying.value=false}}
const submit=async()=>{if(!form.contactName.trim()||!form.contactValue.trim())return ElMessage.warning('请填写称呼和联系方式');if(form.inquiryText.trim().length<10)return ElMessage.warning('请至少用10个字描述需求');if(!verified.value)return ElMessage.warning('请先完成人机验证');submitting.value=true;try{const r=await publicServiceApi.submitInquiry({...form});Object.assign(success,r.data)}catch(e){reportError(e);verified.value=false;form.humanToken='';challengeId.value='';createChallenge(challengeId).catch(()=>{})}finally{submitting.value=false}}
const copyCredentials=async()=>{await navigator.clipboard.writeText(`咨询编号：${success.inquiryNo}\n访问码：${success.accessCode}`);ElMessage.success('已复制，请妥善保存')}
const resetForm=()=>{Object.assign(form,{contactName:'',contactValue:'',serviceType:'GUIDANCE',projectType:'',inquiryText:'',sourcePath:'/cloud-hub/#/services',humanToken:''});Object.assign(success,{inquiryNo:'',accessCode:''});verified.value=false;challengeId.value='';createChallenge(challengeId).catch(()=>{})}
const queryProgress=async()=>{if(!query.inquiryNo||!query.accessCode)return ElMessage.warning('请填写咨询编号和访问码');try{progress.value=(await publicServiceApi.queryInquiry(query)).data||{};progressDrawer.value=true}catch(e){reportError(e)}}
const verifyReply=async()=>{replyVerifying.value=true;try{if(!replyChallengeId.value)await createChallenge(replyChallengeId);const r=await authApi.verify(replyChallengeId.value);reply.humanToken=r.data.humanToken;replyVerified.value=true}catch(e){reportError(e)}finally{replyVerifying.value=false}}
const addMessage=async()=>{if(reply.messageText.trim().length<2)return ElMessage.warning('请填写留言内容');if(!replyVerified.value)return ElMessage.warning('请先完成人机验证');try{await publicServiceApi.addMessage({inquiryNo:query.inquiryNo,accessCode:query.accessCode,messageText:reply.messageText,humanToken:reply.humanToken});reply.messageText='';reply.humanToken='';replyVerified.value=false;replyChallengeId.value='';await queryProgress();ElMessage.success('留言已发送并邮件提醒站长')}catch(e){reportError(e)}}
const onScroll=()=>{const max=document.documentElement.scrollHeight-innerHeight;scrollPercent.value=max>0?scrollY/max*100:0}
const onResize=()=>{viewportWidth.value=window.innerWidth}
onMounted(()=>{createChallenge(challengeId).catch(()=>{});addEventListener('scroll',onScroll,{passive:true});addEventListener('resize',onResize,{passive:true})})
onBeforeUnmount(()=>{removeEventListener('scroll',onScroll);removeEventListener('resize',onResize)})
</script>
