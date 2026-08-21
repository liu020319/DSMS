# DSMS 全平台 R6 保姆级发版手册

## 1. 本次发版解决什么

R6 同时发布并校验三条独立链路，不能只替换其中一个目录：

1. `/`：个人博客，静态首页保持不变，只升级后台邮件发送服务。
2. `/kanglian-cloud/`：最新 DSMS，包含 OBS 购药凭证、手机导航底部不重叠等修复。
3. `/cloud-hub/`：统一门户、个人记账、免登录开发咨询、管理工作台和邮件诊断。

本次还修复：

- 手机输入框聚焦后 iPhone 自动放大，导致上下左右看不全。
- 人机验证第一次过快后挑战被提前作废，继续点击永远失败。
- 博客评论/留言和开发咨询邮件失败时静默丢失。
- DSMS 邮件收件人环境变量名称与部署文档不一致。
- 只更新统一门户、没有更新 DSMS，导致生产仍显示旧版菜单问题。

## 2. 上传文件

把本机文件：

```text
DSMS-full-platform-20260822-R6.zip
DSMS-full-platform-20260822-R6.zip.sha256
```

上传到服务器：

```text
/home/xiaoliu/releases/
```

`zip` 是发布包，`.sha256` 是发布包指纹。指纹相同才能证明上传过程中没有损坏或被替换。

## 3. 校验并解压

```bash
cd /home/xiaoliu/releases
sha256sum -c DSMS-full-platform-20260822-R6.zip.sha256
unzip -q DSMS-full-platform-20260822-R6.zip
cd /home/xiaoliu/releases/DSMS-full-platform-20260822-R6
sha256sum -c SHA256SUMS
```

命令解释：

- `cd` 是 change directory，切换目录。
- `sha256sum -c` 中 `-c` 是 check，按清单重新计算并核对摘要。
- `unzip -q` 中 `-q` 是 quiet，只在出错时输出，避免终端被文件列表淹没。

预期结果：每一项均为 `OK`。只要出现 `FAILED`，立即停止，不执行后续安装。

## 4. 数据库迁移

```bash
bash deploy/migrate-unified-platform.sh
```

脚本会：

1. 隐藏输入 MySQL root 密码。
2. 用 `mysqldump` 备份整个 `medicine_system`。
3. 幂等执行 OBS 和统一门户迁移。
4. 核对 11 张新增业务表。

成功标志：

```text
UNIFIED_DATABASE_MIGRATION_OK
新增业务表数量：11
```

若没有看到成功标志，停止发版并保留完整报错。

## 5. 配置朋友邀请码

```bash
bash deploy/configure-portal-registration-r5.sh
```

朋友邀请码代替短信验证码，不产生短信费用。脚本把邀请码写入 root 才能读取的 `/etc/dsms-backend.env`，不会提交 GitHub。

成功标志：

```text
PORTAL_REGISTRATION_CONFIG_OK
```

## 6. 升级博客邮件后台

```bash
bash deploy/install-blog-mail-service-r6.sh
```

这个脚本只替换 `/opt/personal-blog-resume/resume_service.py`，不会覆盖博客 `/` 的 HTML、CSS 或 JavaScript。它使用 `systemctl restart` 重启 Python 后台，并访问 `127.0.0.1:8091/health` 验证。

成功标志：

```text
BLOG_MAIL_SERVICE_R6_INSTALL_OK
```

## 7. 统一配置邮件提醒

```bash
bash deploy/configure-mail-r6.sh
```

依次输入 Gmail 发件邮箱、接收提醒邮箱和 Gmail 应用专用密码。输入密码时终端不显示字符是正常的。

原理：普通 Gmail 登录密码不应交给程序。应用专用密码是给 SMTP 客户端使用的独立凭证，可单独撤销。脚本同时配置：

- DSMS/开发咨询：`/etc/dsms-backend.env`
- 博客评论/留言：`/etc/personal-blog-resume.env`

配置文件权限为 `0600`：所有者可读写，其他用户无权限。

成功标志：

```text
MAIL_CONFIG_R6_OK
```

同时检查收件箱是否收到博客测试邮件。没有收到就停止，不要继续假装邮件链路正常。

## 8. 安装最新后端

```bash
bash deploy/install-backend-r6.sh
```

脚本先检查 OBS、邀请码和邮件变量，只检查“是否存在”，不会打印 AK、SK 或邮件密码；然后备份旧 JAR、替换、启动并最多等待 90 秒。

`systemctl` 是 systemd 的服务控制命令：

- `systemctl stop dsms-backend`：停止服务。
- `systemctl start dsms-backend`：启动服务。
- `systemctl is-active`：判断服务是否处于 active。
- `journalctl -u dsms-backend`：`-u` 是 unit，查看指定服务日志。

成功标志：

```text
DSMS_R6_BACKEND_INSTALL_OK
```

## 9. 原子替换两个前端

```bash
bash deploy/install-unified-frontends.sh
```

脚本不会猜固定博客目录，而是通过当前 Nginx 实际返回的首页定位站点根目录。随后只替换：

```text
<博客目录>/kanglian-cloud
<博客目录>/cloud-hub
```

先复制到临时目录，校验后通过 `mv` 同盘重命名一次切换，这叫原子替换：访问者不会看到只复制了一半的目录。最后逐字节比较首页和入口脚本，并确认博客首页前后完全相同。

成功标志：

```text
UNIFIED_FRONTENDS_INSTALL_OK
```

## 10. 全平台终检

```bash
bash deploy/verify-full-platform-r6.sh
```

成功标志只有一个：

```text
FULL_PLATFORM_R6_VERIFY_OK
```

脚本验证：

- `dsms-backend`、`personal-blog-resume`、Nginx 状态。
- DSMS 人机验证接口和博客健康接口。
- 三个正式入口和两个前端入口脚本。
- DSMS 费用凭证、统一门户、邮件诊断三个新版标记。
- 邮件环境变量存在，但绝不输出密码。

## 11. 浏览器人工验收

手机使用无痕窗口或清理站点缓存后依次打开：

```text
https://xiaoliudev.com/
https://xiaoliudev.com/cloud-hub/#/
https://xiaoliudev.com/cloud-hub/#/services
https://xiaoliudev.com/cloud-hub/#/register
https://xiaoliudev.com/kanglian-cloud/#/login
```

必须验证：

1. 博客首页仍是原来的个人博客。
2. 开发服务无需登录可浏览、提交咨询、凭编号和访问码查询。
3. 注册页明确显示邀请码注册。
4. 点击每个输入框，页面不会放大或出现横向裁切。
5. DSMS 展开长菜单时，“系统运行正常”位于滚动内容之后，不覆盖菜单。
6. DSMS 管理员工作台显示 OBS 费用凭证功能。
7. 管理员进入“软件服务中心”，邮件链路显示已配置，并点击发送测试邮件。
8. 博客再提交一条测试留言，确认第二条邮件也能收到。

## 12. 任何一步失败怎么办

不要连续重跑，也不要手工删生产目录。执行：

```bash
sudo systemctl status dsms-backend --no-pager
sudo journalctl -u dsms-backend -n 120 --no-pager
sudo systemctl status personal-blog-resume --no-pager
sudo journalctl -u personal-blog-resume -n 120 --no-pager
sudo nginx -t
```

参数解释：

- `sudo` 是 substitute user do，以管理员权限执行当前命令。
- `--no-pager` 不进入可上下翻页的 `less`，便于直接复制完整输出。
- `-n 120` 是只取最近 120 行。
- `nginx -t` 中 `-t` 是 test，只检查配置语法，不重载服务。

把从命令开始到提示符结束的完整输出发来。安装脚本本身会在切换失败时恢复旧 JAR 或旧目录。
