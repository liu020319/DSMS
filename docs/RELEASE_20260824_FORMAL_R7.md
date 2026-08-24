# DSMS 正式启用 R7 保姆级发版手册

## 1. 本次发布边界

R7 必须同时使用同一轮最新版源码构建的四部分：

1. `/`：个人博客，补齐工信部和公安备案号，并修复首页卡片文字重叠。
2. `/cloud-hub/`：统一门户，修复个人记账无付款账户、账户删除、游客咨询删除和备案展示。
3. `/kanglian-cloud/`：DSMS，修复物流时间格式导致的“系统异常”，并展示备案信息。
4. `medicine-system-1.0.0.jar`：与两个前端同时构建的 Spring Boot 后端。

正式账号和通知邮箱位于单独的私有包，绝不进入 GitHub 和公开 R7 压缩包。

## 2. 上传文件

将以下文件放到服务器目录 `/home/xiaoliu/releases/`：

```text
DSMS-formal-production-20260824-R7.zip
DSMS-formal-production-20260824-R7.zip.sha256
DSMS-private-production-reset-20260824.zip
DSMS-private-production-reset-20260824.zip.sha256
```

- `.zip` 是发布内容。
- `.sha256` 是文件指纹，证明本机文件与服务器收到的文件一致。
- 私有包包含正式账号资料，只能保存在私有服务器目录，不能发到 GitHub。

## 3. 校验和解压

```bash
cd /home/xiaoliu/releases
sha256sum -c DSMS-formal-production-20260824-R7.zip.sha256
sha256sum -c DSMS-private-production-reset-20260824.zip.sha256
unzip -q DSMS-formal-production-20260824-R7.zip
unzip -q DSMS-private-production-reset-20260824.zip
cd /home/xiaoliu/releases/DSMS-formal-production-20260824-R7
sha256sum -c SHA256SUMS
```

命令解释：

- `cd` 是 change directory，切换目录。
- `sha256sum -c` 中 `-c` 是 check，按清单核对摘要。
- `unzip -q` 中 `-q` 是 quiet，只在出错时输出。

预期：所有文件均为 `OK`。出现一个 `FAILED` 就停止，不安装。

## 4. 先做数据库非破坏性升级

```bash
bash deploy/migrate-formal-r7.sh
```

脚本先用 `mysqldump` 完整备份，再为历史个人账本补齐默认付款账户。它不会清空 DSMS。

成功标志：

```text
DSMS_R7_DATABASE_MIGRATION_OK
无付款账户的有效账本数量：0
```

## 5. 安装个人博客

```bash
bash deploy/install-blog-r7.sh
```

脚本只替换博客的 `index.html`、`blog-assets/app.js`、`blog-assets/styles.css`，不会覆盖 `/cloud-hub/` 和 `/kanglian-cloud/`。

成功标志：`BLOG_R7_INSTALL_OK`。

## 6. 安装后端

```bash
bash deploy/install-backend-r7.sh
```

脚本会备份旧 JAR、停止 `dsms-backend`、替换为 R7 JAR、重新启动并等待接口可用；失败会恢复旧 JAR。

- `systemctl` 是 systemd 的服务控制工具。
- `systemctl stop` 停止服务，`start` 启动服务，`is-active` 检查运行状态。
- `journalctl -u dsms-backend` 中 `-u` 是 unit，表示只看这个服务的日志。

成功标志：`DSMS_R7_BACKEND_INSTALL_OK`。

## 7. 原子替换两个业务前端

```bash
bash deploy/install-frontends-r7.sh
```

脚本先把文件复制到临时目录并校验，再用一次目录重命名切换。这样访问者不会看到“只复制了一半”的页面。

成功标志：`DSMS_R7_FRONTENDS_INSTALL_OK`。

## 8. 执行 DSMS 正式数据初始化

确认没有用户正在录入数据后执行：

```bash
cd /home/xiaoliu/releases/DSMS-private-production-reset-20260824
chmod 700 deploy-production-reset.sh
bash deploy-production-reset.sh
```

- `chmod` 是 change mode，修改文件权限。
- `700` 表示所有者可读、可写、可执行，其他人没有权限。
- 脚本要求手动输入 `RESET-DSMS`，随后再次备份数据库才会清理。

成功标志：

```text
DSMS_PRODUCTION_RESET_OK
八类正式业务剩余记录：0
```

初始化后的正式账号、初始密码、家庭绑定和通知邮箱只记录在私有包说明中，本公开发版文档不保存真实身份信息。

## 9. 自动终检

```bash
cd /home/xiaoliu/releases/DSMS-formal-production-20260824-R7
bash deploy/verify-formal-r7.sh
```

成功标志只有一个：`DSMS_FORMAL_R7_VERIFY_OK`。

## 10. 浏览器人工验收

用手机无痕窗口和电脑分别检查：

```text
https://xiaoliudev.com/
https://xiaoliudev.com/cloud-hub/#/finance
https://xiaoliudev.com/cloud-hub/#/services
https://xiaoliudev.com/kanglian-cloud/#/login
```

必须逐项确认：

1. 三个模块底部均显示两项备案号。
2. 博客四张入口卡片的说明文字与按钮不重叠。
3. 新建个人账本后自动出现“日常账户”，可直接记一笔；空账户可删除，有流水的账户禁止删除。
4. 管理员可删除测试游客咨询。
5. 私有包配置的最高管理员能登录并拥有最高权限；绑定的安心用药端账号能登录且只能看到自己的家庭数据。
6. 管理员保存物流信息不再提示笼统“系统异常”。
7. 手机展开 DSMS 长菜单时，“系统运行正常”不覆盖菜单。
8. 分别发送博客留言、游客咨询和 DSMS 通知测试，确认邮箱实际收到邮件。

## 11. 失败时停止条件

任何脚本没有打印对应成功标志，都不要继续下一步。执行并复制完整输出：

```bash
sudo systemctl status dsms-backend --no-pager
sudo journalctl -u dsms-backend -n 120 --no-pager
sudo systemctl status personal-blog-resume --no-pager
sudo journalctl -u personal-blog-resume -n 120 --no-pager
sudo nginx -t
```

`--no-pager` 表示直接输出，不进入翻页界面；`-n 120` 表示最近 120 行。

## 12. 为什么 DSMS 不开放公开注册

DSMS 涉及家庭关系、慢病用药和付款凭证。公开注册会增加错误绑定、越权查看和垃圾账号风险。正式流程保持为：最高管理员创建家庭守护端和安心用药端账号，再完成绑定。个人记账的朋友注册属于另一套身份域，不与 DSMS 混用。
