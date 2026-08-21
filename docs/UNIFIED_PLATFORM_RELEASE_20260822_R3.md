# 小刘云统一平台 R3 保姆级发版手册

## 1. 本次上线后，四个地址各自负责什么

| 正式地址 | 页面 | 是否登录 | 说明 |
|---|---|---:|---|
| `https://xiaoliudev.com/` | 个人技术博客 | 否 | 文章、项目、个人品牌和新的系统星图 |
| `https://xiaoliudev.com/cloud-hub/` | 小刘云一级入口 | 否 | 查看 DSMS、个人记账和软件服务三个模块 |
| `https://xiaoliudev.com/cloud-hub/#/services` | 软件工程服务展示 | 否 | 浏览能力、提交咨询、凭咨询编号和访问码查询进度 |
| `https://xiaoliudev.com/cloud-hub/#/finance` | 个人记账 | 是 | 每个账号只看自己的账本、流水、消费地点和预算 |
| `https://xiaoliudev.com/kanglian-cloud/` | 康联云 DSMS | 是 | 家庭用药、购药、OBS 凭证、收货核验等 |

不要在网址后增加 `?v=...`。Vite 已通过带哈希的静态资源文件名处理浏览器缓存。

## 2. 上传前先知道这次会改什么

发布包中的脚本把更新拆成四个可独立停止的阶段：

1. 数据库：备份后创建 OBS、个人记账、软件服务和游客咨询表；
2. 后端：备份旧 JAR，安装新 JAR，失败自动恢复；
3. 博客：只替换 `index.html`、`blog-assets/app.js`、`blog-assets/styles.css` 三个文件；
4. 双前端：只替换博客根目录下的 `kanglian-cloud` 和 `cloud-hub` 两个子目录。

任何阶段报 `FAILED` 或 `ERROR` 都停止，不要继续执行下一阶段。

## 3. 把压缩包拖到服务器

本地文件：

```text
D:\CodexWorkFiles\releases\DSMS-unified-platform-20260822-R3.zip
```

拖到服务器：

```text
/home/xiaoliu/releases/DSMS-unified-platform-20260822-R3.zip
```

## 4. 校验压缩包并解压

```bash
cd /home/xiaoliu/releases

sha256sum DSMS-unified-platform-20260822-R3.zip

test ! -e /home/xiaoliu/releases/DSMS-unified-platform-20260822-R3

unzip -q DSMS-unified-platform-20260822-R3.zip

cd /home/xiaoliu/releases/DSMS-unified-platform-20260822-R3

sha256sum -c SHA256SUMS.txt
```

命令解释：

- `cd` 是 change directory，进入目录；
- `sha256sum` 计算文件的 SHA-256 指纹，确认上传过程没有损坏；
- `test ! -e 路径` 检查同名解压目录不存在，成功时不输出任何内容；
- `unzip` 解压 ZIP，`-q` 是 quiet，减少无用输出；
- `sha256sum -c` 中 `-c` 是 check，逐个检查包内文件，必须全部显示 `OK`。

停止条件：压缩包指纹和本文最终交付值不同、同名目录已存在、任何文件显示 `FAILED`。

## 5. 先做只读预检

```bash
df -h /
docker ps --filter name=mysql
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx
sudo nginx -t
curl -kfsSL https://xiaoliudev.com/ | head -n 5
```

解释：

- `df` 是 disk free，`-h` 是 human-readable；根分区至少保留 2 GB；
- `docker ps --filter name=mysql` 只查看 MySQL 容器；状态应为 `Up`；
- `sudo` 是 substitute user do，以管理员权限执行；
- `systemctl` 控制 systemd 服务；`is-active` 只判断服务是否运行；
- `nginx -t` 中 `-t` 是 test，只检查配置，不修改服务；
- `curl -kfsSL`：`-k` 允许本机证书场景，`-f` 遇到 HTTP 错误直接失败，`-sS` 安静但保留错误，`-L` 跟随跳转；
- `head -n 5` 只显示前 5 行。

MySQL 或 Nginx 异常必须停止。后端暂时异常可以由后端安装脚本恢复。

## 6. 检查脚本语法

```bash
bash -n deploy/migrate-unified-platform.sh
bash -n deploy/install-obs-evidence-backend.sh
bash -n deploy/install-blog-v18.sh
bash -n deploy/install-unified-frontends.sh
```

`bash -n` 只解析脚本语法，不执行里面的部署动作。四条命令都应当没有输出；出现行号或 syntax error 就停止。

## 7. 检查生产环境变量，但不要打印秘密

```bash
sudo grep -E '^(FILE_STORAGE_PROVIDER|OBS_ENDPOINT|OBS_BUCKET|MAIL_ENABLED|MAIL_TO)=' /etc/dsms-backend.env

sudo grep -q '^OBS_ACCESS_KEY=.' /etc/dsms-backend.env && echo 'OBS_ACCESS_KEY 已配置'
sudo grep -q '^OBS_SECRET_KEY=.' /etc/dsms-backend.env && echo 'OBS_SECRET_KEY 已配置'

sudo stat -c '%a %U %G %n' /etc/dsms-backend.env
```

预期：OBS 桶为 `dsms-file`，AK/SK 均显示“已配置”，环境文件权限是 `600 root root`。`grep -q` 的 `-q` 是 quiet，只判断是否存在，不显示密钥。不要把 AK/SK 贴到聊天、截图或 GitHub。

邮件提醒依赖现有邮件配置。如果 `MAIL_ENABLED=false`，站内通知仍会生成，但不会发送邮件；这不阻塞其他功能上线。

## 8. 数据库迁移

```bash
cd /home/xiaoliu/releases/DSMS-unified-platform-20260822-R3

bash deploy/migrate-unified-platform.sh
```

脚本会隐藏读取 MySQL root 密码，先使用 `mysqldump --single-transaction` 备份，再执行两份 `CREATE TABLE IF NOT EXISTS` SQL，最后验证 11 张表。

成功标志：

```text
UNIFIED_DATABASE_MIGRATION_OK
新增业务表数量：11
BACKUP_DIR=...
```

## 9. 安装后端

```bash
bash deploy/install-obs-evidence-backend.sh
```

脚本会：停止服务、备份旧 JAR、安装新 JAR、启动服务，并最多等待 90 秒；只有 systemd 为 `active` 且安全验证接口可访问才成功。

成功标志：

```text
DSMS_OBS_BACKEND_INSTALL_OK
```

若失败，脚本自动恢复旧 JAR，并用 `journalctl -u dsms-backend -n 100 --no-pager` 显示最近 100 行服务日志。`journalctl` 是 systemd 日志查看工具；`-u` 指定 unit，`-n` 指定行数，`--no-pager` 防止进入翻页界面。

## 10. 安装博客 V18

```bash
bash deploy/install-blog-v18.sh
```

脚本从 Nginx 当前返回的博客首页反查真实目录，只替换三个静态文件。不会移动 `kanglian-cloud` 或 `cloud-hub`。

成功标志：

```text
BLOG_V18_INSTALL_OK
```

## 11. 安装 DSMS 和一级门户

```bash
bash deploy/install-unified-frontends.sh
```

该脚本会备份并原子切换两个子目录，验证博客首页没有变化，并逐字节比较 DSMS、统一门户的首页和入口 JavaScript。

成功标志：

```text
UNIFIED_FRONTENDS_INSTALL_OK
```

## 12. 服务器命令验收

```bash
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx

curl -fsS http://127.0.0.1:8088/api/auth/human-challenge

curl -kfsSL https://xiaoliudev.com/ | grep -F 'styles.css?release=v18'
curl -kfsSL https://xiaoliudev.com/cloud-hub/ | grep -F '/cloud-hub/assets/'
curl -kfsSL https://xiaoliudev.com/kanglian-cloud/ | grep -F '/kanglian-cloud/assets/'
```

两项服务必须是 `active`，三个 `grep` 都必须回显匹配内容。

## 13. 浏览器完整验收

电脑按 `Ctrl+F5`，手机清除该网站缓存，然后依次测试：

1. 博客首页能看到“一个入口，连接四种真实能力”；
2. 一级门户无需登录能打开；
3. 软件服务页无需登录能提交测试咨询，得到咨询编号和访问码；
4. 用编号和访问码能查进度；故意输错访问码不能看到联系方式；
5. 管理员登录软件服务中心能看到游客咨询并回复；
6. 朋友账号进入个人记账，只能看到自己的账本；
7. 新增“水果 / 榴莲摊”后刷新，再登记时“榴莲摊”出现在常用地点；
8. 月度分类、地点、每日脉冲和预算百分比同步变化；
9. DSMS 上传一张无隐私的测试付款截图，刷新后仍能查看；
10. 另一个家庭账号不能读取该文件。

## 14. 什么才算真正部署成功

本地 Maven、Vite 构建成功只证明“代码能打包”。只有数据库迁移、systemd、Nginx、四个公网入口和权限场景全部通过，才算上线完成。
