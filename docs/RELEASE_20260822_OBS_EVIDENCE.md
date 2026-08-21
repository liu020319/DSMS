# DSMS OBS 文件中心与购药凭证发版教程

目标环境：阿里云 Ubuntu，MySQL Docker 容器 `mysql`，后端 systemd 服务 `dsms-backend`，后端 JAR `/home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar`，前端目录 `/var/www/dsms`。

本教程不会让 AK/SK 出现在命令历史、Git 或发布包中。任何一步出现本文指定的停止条件，都不要继续下一步。

## 0. 华为云先做安全准备

1. 在 OBS 控制台打开 `dsms-file`。
2. 将桶 ACL/桶策略改为私有，删除匿名用户或所有用户的读写授权。
3. 不要把 `xiaoliudev` 博客桶与 DSMS 文件混用。
4. 创建 IAM 子用户或程序访问凭证，只给 `dsms-file` 必要的对象上传、读取和删除权限；不要使用主账号永久 AK/SK。
5. 不用配置浏览器 CORS。本版本由 Spring Boot 上传和读取，浏览器不直连 OBS。

停止条件：控制台仍显示“公开桶”时，不得上传真实问诊、付款、发票或收货图片。

## 1. 上传并核对发布包

把本地 `DSMS-OBS-evidence-20260822.zip` 拖到：

```text
/home/xiaoliu/releases/DSMS-OBS-evidence-20260822.zip
```

服务器执行：

```bash
cd /home/xiaoliu/releases
sha256sum DSMS-OBS-evidence-20260822.zip
```

- `cd` 是 change directory，切换当前目录；
- `sha256sum` 计算 SHA-256 摘要，必须与本地交付值逐字一致。

解压：

```bash
mkdir -p /home/xiaoliu/releases/DSMS-OBS-evidence-20260822
unzip -q DSMS-OBS-evidence-20260822.zip \
  -d /home/xiaoliu/releases/DSMS-OBS-evidence-20260822
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822
sha256sum -c SHA256SUMS
```

- `mkdir -p` 创建目录及缺少的父目录；已有目录不报错；
- `unzip -q` 中 `-q` 是 quiet，减少非必要输出；
- `-d` 是 directory，指定解压目录；
- `sha256sum -c` 中 `-c` 是 check，读取清单并逐项核对。

成功：清单每一行都是 `OK`。停止：任意 `FAILED`，重新上传，不覆盖线上文件。

## 2. 只读预检

```bash
df -h /
docker ps --filter name=mysql
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx
sudo nginx -t
sudo systemctl cat dsms-backend
sudo nginx -T 2>/dev/null | grep -nE 'server_name|location /kanglian-cloud|location /api|root |alias '
```

- `df` 是 disk free；`-h` 是 human-readable，按 GB/MB 显示；
- `docker ps` 查看运行容器；`--filter` 只显示名称匹配 `mysql` 的容器；
- `systemctl is-active` 查询 systemd 服务运行状态；
- `nginx -t` 只测试配置，不重载；
- `systemctl cat` 显示服务主文件和 drop-in，用来确认 `ExecStart` 与环境配置；
- `nginx -T` 打印完整有效配置，`2>/dev/null` 隐藏标准错误提示，`grep -nE` 按扩展正则筛选并显示行号。

成功：磁盘充足、MySQL 为 `Up`、两个服务为 `active`、Nginx 测试成功，JAR/前端路径与本文一致。停止：任何路径不一致，把输出保存下来，先修改本教程中的路径，不能盲目覆盖。

## 3. 可恢复备份

使用同一个版本时间标识：

```bash
mkdir -p /home/xiaoliu/backups/20260822-obs-evidence

cp -a \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar \
  /home/xiaoliu/backups/20260822-obs-evidence/medicine-system-1.0.0.jar

sudo cp -a \
  /var/www/dsms \
  /home/xiaoliu/backups/20260822-obs-evidence/frontend

sudo systemctl cat dsms-backend \
  > /home/xiaoliu/backups/20260822-obs-evidence/dsms-backend-unit.txt
```

- `cp` 是 copy；`-a` 是 archive，尽量保留权限、时间和目录结构；
- `sudo` 是 superuser do，以管理员权限执行后面的单条命令；
- `>` 把命令输出写入文件。

备份数据库：

```bash
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo

docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysqldump \
  -uroot \
  --single-transaction \
  --default-character-set=utf8mb4 \
  medicine_system \
  > /home/xiaoliu/backups/20260822-obs-evidence/medicine_system.sql

unset MYSQL_ROOT_PASSWORD
ls -lh /home/xiaoliu/backups/20260822-obs-evidence/
```

- `read -s` 静默读取，密码不回显；`-p` 是 prompt 提示；
- `docker exec -e` 只给容器内本次进程传入临时环境变量，避免密码出现在 MySQL 命令参数中；
- `mysqldump` 导出数据库；`--single-transaction` 为 InnoDB 使用一致性快照；
- `unset` 删除当前终端中的密码变量；
- `ls -lh` 以易读大小显示备份。

停止：数据库文件为 0 字节或 `mysqldump` 报错。

## 4. 执行数据库迁移

```bash
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo

docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql \
  -uroot \
  --default-character-set=utf8mb4 \
  medicine_system \
  < sql/20260822_obs_file_center.sql
```

- `docker exec -i` 的 `-i` 是 interactive，保持标准输入打开，`<` 才能把 SQL 送入容器；
- `mysql` 是容器内客户端；
- `-u` 是 user；密码通过本次 `docker exec` 的临时 `MYSQL_PWD` 传入，不写进 SQL 或发布包；
- `utf8mb4` 支持完整 Unicode。

验证：

```bash
docker exec -i -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql mysql \
  -uroot \
  --default-character-set=utf8mb4 \
  medicine_system \
  -e "SHOW TABLES LIKE 'file_asset'; SHOW TABLES LIKE 'purchase_evidence'; SHOW CREATE TABLE file_asset\G; SHOW CREATE TABLE purchase_evidence\G"

unset MYSQL_ROOT_PASSWORD
```

成功：两张表都显示，字符集为 `utf8mb4`。停止：任何 `ERROR`，不要部署新 JAR。

## 5. 安全保存 OBS 配置

先备份已有环境文件；没有则创建：

```bash
if sudo test -f /etc/dsms-backend.env; then
  sudo cp -a /etc/dsms-backend.env \
    /home/xiaoliu/backups/20260822-obs-evidence/dsms-backend.env
else
  sudo install -o root -g root -m 0600 /dev/null /etc/dsms-backend.env
fi

sudo chmod 600 /etc/dsms-backend.env
sudo nano /etc/dsms-backend.env
```

- `test -f` 判断普通文件是否存在；
- `install` 在这里是创建/复制文件并设置所有者和权限；
- `-o root` 指 owner；`-g root` 指 group；`-m 0600` 指 mode，只有 root 可读写；
- `chmod` 是 change mode；`600` 表示所有者读写，其他人无权限；
- `nano` 是终端文本编辑器，保存按 `Ctrl+O`、回车，退出按 `Ctrl+X`。

在文件末尾加入，等号右侧换成你自己在华为云创建的值：

```text
FILE_STORAGE_PROVIDER=obs
FILE_OBJECT_PREFIX=dsms/prod
OBS_ENDPOINT=https://obs.cn-north-4.myhuaweicloud.com
OBS_BUCKET=dsms-file
OBS_ACCESS_KEY=这里填IAM子用户AK
OBS_SECRET_KEY=这里填IAM子用户SK
IMAGE_MAX_BYTES=5242880
```

不要删除文件里原有的数据库、JWT 或邮件环境变量。

检查权限，不打印内容：

```bash
sudo stat -c '%a %U %G %n' /etc/dsms-backend.env
```

成功应类似：`600 root root /etc/dsms-backend.env`。

让 systemd 读取文件：

```bash
sudo systemctl edit dsms-backend
```

编辑器中加入：

```ini
[Service]
EnvironmentFile=-/etc/dsms-backend.env
```

- `[Service]` 表示覆盖服务段；
- `EnvironmentFile` 指定环境变量文件；
- 路径前的 `-` 表示文件临时缺失时 systemd 不因它直接拒绝加载，但本次部署仍必须保证文件存在。

保存后：

```bash
sudo systemctl daemon-reload
sudo systemctl cat dsms-backend
```

- `daemon-reload` 让 systemd 重新读取 unit/drop-in 配置；它不会自动重启应用。

成功：输出中可看到 `/etc/dsms-backend.env`，但不会打印里面的秘密。

## 6. 安装后端

```bash
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822

sudo systemctl stop dsms-backend
sudo install -o xiaoliu -g xiaoliu -m 0644 \
  backend/medicine-system-1.0.0.jar \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar
sudo systemctl start dsms-backend

sudo systemctl is-active dsms-backend
sudo journalctl -u dsms-backend -n 120 --no-pager
```

- `stop/start` 明确停止旧进程并启动新 JAR；
- `install -o xiaoliu -g xiaoliu -m 0644` 复制 JAR，同时保持服务目录现有账号可维护；
- `journalctl` 查看日志；`-u` 是 unit；`-n 120` 是最后 120 行；`--no-pager` 直接打印，不进入冒号分页界面。

成功：`active`，日志出现 `Tomcat started on port(s): 8088` 和 `Started MedicineSystemApplication`，没有新的 `APPLICATION FAILED TO START`、OBS 配置异常或数据库表不存在。

停止：服务不是 `active`。立即执行第 11 节后端回滚，不安装前端。

## 7. 本机后端检查

```bash
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge
echo
```

- `curl` 发 HTTP 请求；
- `-f` 遇到 HTTP 4xx/5xx 返回失败；
- `-sS` 正常时减少进度输出，失败时仍显示错误。

成功：返回 JSON 挑战数据。它只证明后端基础接口启动，OBS 仍需登录后的上传测试。

## 8. 原子替换前端

不要把新文件直接覆盖到旧目录中，否则旧、新哈希资源混合可能白屏。先准备完整新目录：

```bash
cd /home/xiaoliu/releases/DSMS-OBS-evidence-20260822

sudo mkdir -p /var/www/dsms_next_20260822
sudo cp -a frontend/dist/. /var/www/dsms_next_20260822/
sudo chown -R root:root /var/www/dsms_next_20260822
sudo find /var/www/dsms_next_20260822 -type d -exec chmod 755 {} \;
sudo find /var/www/dsms_next_20260822 -type f -exec chmod 644 {} \;

sudo test -f /var/www/dsms_next_20260822/index.html
```

- `chown` 是 change owner；`-R` 是 recursive，只对明确的新目录递归；
- `find -type d/f` 分别处理目录和文件；
- `-exec ... {} \;` 对每个找到的项目执行命令；
- 目录需要 `755` 才允许 Nginx 穿越，普通文件用 `644`。

切换：

```bash
sudo test ! -e /var/www/dsms_previous_20260822
sudo mv /var/www/dsms /var/www/dsms_previous_20260822
sudo mv /var/www/dsms_next_20260822 /var/www/dsms
sudo nginx -t
sudo systemctl reload nginx
```

- `mv` 是 move/rename；同一文件系统内改名速度快，可保留旧目录回滚；
- `nginx -t` 的 `-t` 是 test；
- `reload` 平滑重载配置，不主动中断现有连接。

停止：第一条 `test ! -e` 失败说明同名旧备份已存在，先检查，不要覆盖；`nginx -t` 失败时先把目录切回。

## 9. 本机与公网验收

```bash
curl -I http://127.0.0.1/
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx
```

根据当前记录，DSMS 公网入口为：

```text
https://xiaoliudev.com/kanglian-cloud/
```

发版前以第 2 步实际 Nginx 输出为准，不要擅自给网址加 `?v=...` 或改为 IP 路径。

浏览器完整验收：

1. 守护端登录，打开一笔订单；
2. 分别上传问诊、付款、发票；
3. 刷新后仍在时间线；
4. 安心用药端登录，同订单能查看图片；
5. 安心用药端上传收货照片并完成核验；
6. 再用其他家庭账号直接访问文件地址，应返回 403；
7. 未登录无痕窗口打开文件地址，应被拒绝；
8. 华为云控制台确认新对象位于随机前缀，且桶仍为私有；
9. 手机端检查卡片、补充凭证弹窗和收货弹窗；
10. 清浏览器缓存或强制刷新，不改正式 URL。

## 10. 成功边界

只有 SQL、服务、Nginx、守护端上传、安心端查看、跨家庭拒绝、重启后仍可读取全部通过，才能称“OBS 文件中心已部署上线”。JAR/`dist` 在本地构建成功不等于服务器部署完成。

## 11. 回滚

后端：

```bash
sudo systemctl stop dsms-backend
sudo install -o xiaoliu -g xiaoliu -m 0644 \
  /home/xiaoliu/backups/20260822-obs-evidence/medicine-system-1.0.0.jar \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar
sudo systemctl start dsms-backend
sudo systemctl is-active dsms-backend
```

前端：

```bash
sudo test -d /var/www/dsms_previous_20260822
sudo mv /var/www/dsms /var/www/dsms_failed_20260822
sudo mv /var/www/dsms_previous_20260822 /var/www/dsms
sudo nginx -t
sudo systemctl reload nginx
```

OBS 配置：若旧 JAR 不使用它，可以保留环境文件；若需恢复旧 drop-in，使用 `sudo systemctl edit dsms-backend` 删除本次新增行，然后 `sudo systemctl daemon-reload` 和重启。

数据库：本次只新增两张表，旧 JAR 会忽略它们，通常不需要恢复整个数据库。不要轻易导入旧全库备份，因为会覆盖发版后新增业务数据。

## 12. 你下次独立练习

不看本文，自己解释并执行本地两条构建命令；然后回答：为什么服务器要先执行 SQL，再启动新 JAR；为什么前端要整体切换 `dist`，不能只复制一个 `index.html`。
