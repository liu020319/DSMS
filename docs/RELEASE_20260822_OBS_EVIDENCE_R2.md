# DSMS OBS 凭证中心 R2 发版手册

## 这次修了什么

R1 的业务代码、SQL 和 OBS 后端实现可以使用，但发布方式有两个错误：

1. Vite 默认按网站根目录 `/` 构建，生成 `/assets/...`，与 DSMS 的真实入口 `/kanglian-cloud/` 不匹配。
2. R1 文档把整个 `/var/www/dsms` 当成 DSMS 前端替换，实际该目录同时承载个人博客根页面，导致博客被覆盖。

R2 已把 Vite `base` 改为 `/kanglian-cloud/`，并提供两份带备份、校验和失败回滚的脚本：

- 后端只更新 `dsms-backend` 使用的 JAR；
- 前端自动识别个人博客真实根目录，只替换其 `kanglian-cloud` 子目录；
- 前端切换后逐字节校验博客首页未变化、DSMS 首页和入口 JS 正确。

## 0. 先恢复个人博客

如果还没有执行 `personal-blog-v17-asset-repair-20260822.zip`，必须先按它的文档恢复博客，并看到：

```text
BLOG_ASSETS_REPAIR_OK
```

不要再次运行旧的 `rollback-after-review.sh`，它只检查首页标识，不能证明 JS/CSS 正确。

## 1. 上传、校验、解压 R2

把 `DSMS-OBS-evidence-20260822-r2.zip` 拖到 `/home/xiaoliu/releases/`，然后执行：

```bash
cd /home/xiaoliu/releases
sha256sum DSMS-OBS-evidence-20260822-r2.zip
test ! -e /home/xiaoliu/releases/DSMS-OBS-evidence-20260822-r2
unzip -q DSMS-OBS-evidence-20260822-r2.zip
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822-r2
sha256sum -c SHA256SUMS
```

- `sha256sum` 计算文件指纹，必须与交付值一致；
- `test ! -e` 确认不会覆盖同名解压目录；成功时没有输出；
- `unzip -q` 解压，`-q` 是 quiet；
- `sha256sum -c` 中 `-c` 是 check，所有行必须为 `OK`。

停止条件：压缩包哈希不同、同名目录已存在或出现 `FAILED`。

## 2. 服务器只读预检

```bash
df -h /
docker ps --filter name=mysql
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx
sudo nginx -t
curl -kfsSL https://xiaoliudev.com/ | head -n 5
curl -kfsSL https://xiaoliudev.com/kanglian-cloud/ | head -n 8
```

预期：磁盘充足、MySQL 为 `Up`、Nginx 为 `active`、`nginx -t` 成功，根路径是个人博客，子路径是家庭用药系统或其旧版本。

后端如果暂时不是 `active`，仍可继续安装 R2 后端；MySQL 或 Nginx 异常则必须停止。

## 3. 数据库迁移

该 SQL 只使用 `CREATE TABLE IF NOT EXISTS` 创建 `file_asset` 和 `purchase_evidence`，已经执行过也可以再次校验式执行。

```bash
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822-r2
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo

docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql \
  -uroot \
  --default-character-set=utf8mb4 \
  medicine_system \
  < sql/20260822_obs_file_center.sql

docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql \
  -uroot \
  medicine_system \
  -e "SHOW TABLES LIKE 'file_asset'; SHOW TABLES LIKE 'purchase_evidence';"

unset MYSQL_ROOT_PASSWORD
```

- `read -s` 静默读取，不显示密码；
- `docker exec -i` 保持标准输入，使 `< SQL文件` 能进入容器；
- `-e MYSQL_PWD=...` 只给本次容器进程传密码；
- `unset` 从当前终端删除变量。

成功：显示两张表。停止：任意 `ERROR`。

## 4. 检查 OBS 环境变量

只显示非秘密配置：

```bash
sudo grep -E '^(FILE_STORAGE_PROVIDER|FILE_OBJECT_PREFIX|OBS_ENDPOINT|OBS_BUCKET|IMAGE_MAX_BYTES)=' /etc/dsms-backend.env
```

只判断 AK/SK 是否存在，不打印内容：

```bash
sudo grep -q '^OBS_ACCESS_KEY=.' /etc/dsms-backend.env && echo 'OBS_ACCESS_KEY 已配置'
sudo grep -q '^OBS_SECRET_KEY=.' /etc/dsms-backend.env && echo 'OBS_SECRET_KEY 已配置'
sudo stat -c '%a %U %G %n' /etc/dsms-backend.env
```

预期：provider 为 `obs`、bucket 为 `dsms-file`、AK/SK 均显示已配置、权限为 `600 root root`。不要把 AK/SK 发送到聊天、截图或 Git。

## 5. 安装后端

```bash
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822-r2
bash deploy/install-obs-evidence-backend.sh
```

脚本会备份当前 JAR，安装新 JAR，等待最多 90 秒，并同时检查：

- `dsms-backend` 为 `active`；
- `http://127.0.0.1:8088/api/auth/human-challenge` 可访问。

成功标志：

```text
DSMS_OBS_BACKEND_INSTALL_OK
BACKUP_DIR=...
```

失败时脚本自动恢复旧 JAR并打印最近 100 行日志。出现 `FAILED` 时不要安装前端。

## 6. 安装前端到正确子路径

```bash
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822-r2
bash deploy/install-obs-evidence-frontend.sh
```

脚本自动识别当前 Nginx 正在使用的博客目录，只安装到：

```text
博客真实目录/kanglian-cloud
```

不会替换博客根目录。成功标志：

```text
DSMS_OBS_FRONTEND_INSTALL_OK
SITE_ROOT=...
DSMS_ROOT=.../kanglian-cloud
BACKUP_DIR=...
```

失败时自动恢复原 DSMS 子目录。

## 7. 公网验收

```bash
curl -kfsSL https://xiaoliudev.com/ | grep -F '小刘 · 个人技术博客'
curl -kfsSL https://xiaoliudev.com/kanglian-cloud/ | grep -F '/kanglian-cloud/assets/'
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx
```

然后电脑按 `Ctrl+F5`，手机清除该站点缓存，使用正式地址：

```text
https://xiaoliudev.com/kanglian-cloud/#/login
```

不要给正式网址增加 `?v=...`。

登录后测试：守护端进入“费用凭证档案”或订单凭证入口，上传问诊截图、付款截图、发票；刷新后确认时间线仍存在，再用安心用药端查看。必须使用测试图片，不要先传真实隐私材料。

## 8. 成功边界

只有以下内容全部成立才算上线：

1. 个人博客根页面、样式和导航正常；
2. `/kanglian-cloud/` 加载的 JS/CSS 都来自同一子路径；
3. 后端为 `active`，登录接口正常；
4. 上传后 OBS 私有桶出现对象，数据库 `file_asset` 有记录；
5. 同家庭可查看，未登录和其他家庭访问被拒绝；
6. 重启服务后仍能查看凭证。

本地构建成功不能代替服务器和 OBS 端到端验证。
