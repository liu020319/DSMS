# DSMS 统一门户 R5 紧急修复发版

本次版本只更新两处：

1. Spring Boot 后端 JAR：修复游客提交免费评估失败，并增加朋友邀请码注册接口。
2. `/cloud-hub/` 统一门户：修复手机端横向溢出、路由切换后保留横向滚动位置、工作台和毕业设计服务页裁切，并增加朋友注册页。

本次不替换个人博客首页，也不替换 `/kanglian-cloud/` DSMS 前端，更不需要执行数据库迁移。

对应已评审源码提交：`7e744d0 fix: 修复门户移动端与游客咨询注册`。

## 一、先理解三个修复原理

### 1. 个人记账怎么给朋友用

没有采用付费短信验证码，也没有允许任意账号直接注册。现在使用“朋友邀请码注册”：

- 朋友访问 `https://xiaoliudev.com/cloud-hub/#/register`；
- 输入用户名、密码、显示名称和你私下提供的邀请码；
- 通过已有的人机验证后，后端只创建 `PORTAL_USER` 角色；
- 每个账号只能读取自己的记账数据，不能自动获得 DSMS、管理员或服务工作台权限；
- 邀请码只写在服务器 `/etc/dsms-backend.env`，不写进 GitHub 和前端文件，可随时更换。

这是低成本朋友内测方案。以后若完全公开注册，再增加邮箱验证、找回密码、隐私协议和更强的限流。

### 2. 为什么手机端之前左右被截断

CSS Grid 子元素默认允许按长文本的最小宽度撑开网格；大标题、终端字符串和卡片共同把文档宽度撑到了手机屏幕之外。页面切换时 Vue Router 又保留了原来的横向滚动位置，所以后续页面也像是整体向左偏移。

修复同时处理了根因：网格列改成 `minmax(0, 1fr)`，卡片允许收缩和换行，页面禁止意外横向溢出，并让路由切换始终回到 `left: 0, top: 0`。

### 3. 为什么免费评估提交失败

游客咨询创建成功后，系统还会给管理员写一条站内通知。数据库通知表的 `biz_type` 最长 30 个字符，旧代码写入的值有 31 个字符，MySQL 拒绝通知写入；因为咨询和通知处在同一个事务内，整笔咨询随之回滚。

本次把业务类型改成兼容字段长度的值，并补充了“有管理员时也能完成咨询”的自动化测试。

## 二、把压缩包上传到服务器

把 `DSMS-portal-mobile-registration-20260822-R5.zip` 上传到：

```text
/home/xiaoliu/releases/
```

下面所有命令都在服务器 SSH 终端执行。看到任何与“预期结果”不同的错误就停下，不要继续下一段。

## 三、校验压缩包并解压

```bash
cd /home/xiaoliu/releases
sha256sum DSMS-portal-mobile-registration-20260822-R5.zip
```

- `cd` 是 change directory，切换目录。
- `sha256sum` 计算文件指纹，用于确认上传途中没有损坏。
- 输出必须与本地 `DSMS-portal-mobile-registration-20260822-R5.zip.sha256` 完全一致。

一致后执行：

```bash
unzip -q DSMS-portal-mobile-registration-20260822-R5.zip
cd DSMS-portal-mobile-registration-20260822-R5
sed -i 's/\r$//' SHA256SUMS.txt
sha256sum -c SHA256SUMS.txt
```

- `unzip -q` 解压 ZIP；`-q` 是 quiet，只减少无关输出。
- `sed -i 's/\r$//'` 只把校验清单的 Windows CRLF 行尾转换为 Linux LF，避免隐藏的 `\r` 被当成文件名；发布脚本本身已按 Linux LF 打包。
- `sha256sum -c` 按清单逐个核验发布包内部文件。

预期结果：每个文件后面都是 `OK`。出现 `FAILED` 或 `No such file` 就停下。

## 四、设置朋友邀请码

```bash
cd /home/xiaoliu/releases/DSMS-portal-mobile-registration-20260822-R5
bash deploy/configure-portal-registration.sh
```

- `bash` 明确让 Bash 解释器执行脚本，因此不依赖脚本是否带可执行位。
- 脚本用 `read -s` 隐藏输入内容；终端不会显示邀请码。
- 脚本先备份 `/etc/dsms-backend.env`，然后写入注册开关和邀请码。
- 邀请码请使用 8-64 位字母、数字、下划线或短横线，不要用生日、手机号和常见密码。

预期结果：

```text
PORTAL_REGISTRATION_CONFIG_OK
```

并记下输出的 `BACKUP=` 路径。没有看到成功标记就停下。

## 五、安装后端

```bash
cd /home/xiaoliu/releases/DSMS-portal-mobile-registration-20260822-R5
bash deploy/install-backend.sh
```

脚本内部的关键命令含义：

- `sudo`：superuser do，以管理员权限操作 systemd 和生产 JAR；
- `systemctl stop/start dsms-backend`：让 systemd 停止或启动 Spring Boot 服务；
- `install -o xiaoliu -g xiaoliu -m 0644`：复制 JAR，同时设置 owner、group 和 mode；`0644` 表示所有者可读写，其他用户只读；
- `curl -fsS`：调用本机健康接口；`-f` 遇到 HTTP 错误就失败，`-sS` 隐藏进度但保留错误；
- `journalctl -u dsms-backend`：安装失败时读取该 systemd 服务的日志；
- 脚本会等待最多 90 秒，不能因为前几秒 `Connection refused` 就手动中断。

预期结果：

```text
DSMS_R5_BACKEND_INSTALL_OK
```

安装失败或按下 Ctrl+C 时，脚本会尝试恢复旧 JAR。没有成功标记就不要安装前端。

## 六、安装统一门户前端

```bash
cd /home/xiaoliu/releases/DSMS-portal-mobile-registration-20260822-R5
bash deploy/install-portal.sh
```

脚本不会凭目录名猜网站根目录。它会先从本机 Nginx 读取当前博客首页，再在 `/var/www` 中找到与该首页逐字节一致的真实目录，然后只原子替换其 `cloud-hub` 子目录。

预期结果：

```text
PORTAL_INSTALL_OK
```

脚本还会验证：

- `nginx -t` 语法正确；
- 个人博客首页安装前后逐字节一致；
- `/cloud-hub/` 的 `index.html` 与入口 JS 和发布包逐字节一致。

## 七、服务器本机验收

```bash
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge
curl -kfsSI --resolve xiaoliudev.com:443:127.0.0.1 https://xiaoliudev.com/
curl -kfsSI --resolve xiaoliudev.com:443:127.0.0.1 https://xiaoliudev.com/cloud-hub/
```

- `is-active` 应输出 `active`；
- `--resolve` 临时把域名指向 `127.0.0.1`，验证本机 Nginx，而不依赖外部 DNS；
- `-I` 只读取响应头；`-k` 仅用于服务器本机按 IP 回环验证证书链，公众浏览器仍正常校验证书。

## 八、浏览器验收清单

手机和电脑都强制刷新后依次检查：

1. `https://xiaoliudev.com/`：仍是个人博客；
2. `https://xiaoliudev.com/cloud-hub/#/`：平台首页完整，无横向滚动；
3. `#/services`：免登录可浏览，免费评估表单可打开；
4. `#/register`：可使用朋友邀请码注册；
5. 新账号登录后进入 `#/finance`，能新增一条测试账目且只能看到自己的数据；
6. `#/service-workspace`：有权限时显示完整工作台，手机端不再左右裁切；
7. 提交一条测试咨询后得到咨询编号和查询口令；再用编号、手机号后四位和口令查询。

第 7 项会真实写入测试咨询并给管理员产生通知，属于生产数据验收，请使用明显的“R5验收测试”标题，测试后由管理员按正常流程处理。

## 九、回滚说明

两个安装脚本在失败时会自动回滚。若安装成功后仍要人工回滚，先不要猜路径：使用脚本成功输出的 `SITE_ROOT=`、`BACKUP_DIR=` 和后端 `BACKUP_DIR=`，核对目录后再恢复。把这些三行输出发给 Codex，可按你的真实路径生成精确回滚命令。

## 十、成功边界

本地构建和测试成功只证明发布包可构建；服务器脚本成功只证明后端、Nginx 和静态文件切换成功。只有第八节手机与电脑的完整业务验收都通过，才算本次生产发版完成。
