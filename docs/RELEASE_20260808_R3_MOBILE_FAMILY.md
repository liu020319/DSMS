# DSMS R3 阿里云手工发版教程

本教程对应发布包 `DSMS-enterprise-console-20260808-r3.zip`。请严格按顺序执行；每一步都写明用途、成功标志和出错后的停止条件。

## 一、需要上传的文件

从本地上传到服务器 `/home/xiaoliu/releases/`：

1. `DSMS-enterprise-console-20260808-r3.zip`：公开代码、JAR、前端 dist、数据库迁移和文档。
2. `20260808_assign_existing_elder_accounts.sql`：私有账号角色与绑定脚本，禁止上传 GitHub。
3. `20260808_seed_existing_elder_scenarios.sql`：两个既有账号的晨午晚演示方案，禁止上传 GitHub。

建议上传后的服务器位置：

```text
/home/xiaoliu/releases/DSMS-enterprise-console-20260808-r3.zip
/home/xiaoliu/releases/20260808_assign_existing_elder_accounts.sql
/home/xiaoliu/releases/20260808_seed_existing_elder_scenarios.sql
```

## 二、解压并校验发布包

```bash
cd /home/xiaoliu/releases
mkdir -p DSMS-enterprise-console-20260808-r3
unzip -q DSMS-enterprise-console-20260808-r3.zip -d DSMS-enterprise-console-20260808-r3
cd DSMS-enterprise-console-20260808-r3
sha256sum -c SHA256SUMS
```

命令解释：

- `cd` 是 change directory，切换当前目录。
- `mkdir` 是 make directory，创建目录；`-p` 表示父目录不存在时一起创建，目录已存在也不报错。
- `unzip` 解压 ZIP；`-q` 是 quiet，减少无关输出；`-d` 是 directory，指定解压位置。
- `sha256sum` 计算 SHA-256 文件摘要；`-c` 是 check，按照 `SHA256SUMS` 逐项核对文件是否损坏。

成功标志：每一行最后都是 `OK`。只要出现 `FAILED`，立即停止，不要继续覆盖服务器文件，应重新上传 ZIP。

## 三、备份当前版本

```bash
mkdir -p /home/xiaoliu/backups/20260808-r3

cp -a \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar \
  /home/xiaoliu/backups/20260808-r3/medicine-system-1.0.0.jar

sudo cp -a \
  /var/www/dsms \
  /home/xiaoliu/backups/20260808-r3/frontend
```

命令解释：

- `cp` 是 copy，复制文件或目录。
- `-a` 是 archive，尽量保留原目录结构、权限和时间。
- `sudo` 是 superuser do，以管理员权限执行后面的命令。`/var/www` 通常只有管理员能修改，所以前端备份需要 `sudo`。
- 反斜杠 `\` 表示命令还没结束，下一行仍属于同一条命令。

成功标志：没有报错，并且下面两条命令都能看到备份：

```bash
ls -lh /home/xiaoliu/backups/20260808-r3/medicine-system-1.0.0.jar
ls -ld /home/xiaoliu/backups/20260808-r3/frontend
```

接着备份数据库：

```bash
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo

docker exec mysql mysqldump \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction \
  --default-character-set=utf8mb4 \
  medicine_system \
  > /home/xiaoliu/backups/20260808-r3/medicine_system.sql

unset MYSQL_ROOT_PASSWORD
ls -lh /home/xiaoliu/backups/20260808-r3/medicine_system.sql
```

命令解释：

- `read` 读取输入；`-s` 是 silent，不回显密码；`-p` 是 prompt，显示提示文字。
- `docker exec` 在正在运行的容器里执行命令；这里的容器名是 `mysql`。
- `mysqldump` 导出数据库备份。
- `-u` 是 user，后面是数据库用户 root；`-p` 是 password，后面紧跟刚输入的密码。
- `--single-transaction` 在 InnoDB 数据库中使用一致性快照，尽量避免长时间锁表。
- `>` 把导出内容写入右侧文件。
- `unset` 从当前终端删除密码变量，减少密码残留。
- `ls -lh` 查看文件；`-l` 是 long format，显示详情；`-h` 是 human readable，以 KB/MB/GB 显示大小。

成功标志：SQL 备份文件不是 0 字节。若导出报错或文件为空，停止发版。

## 四、执行 R3 数据库升级

先进入解压目录并输入一次密码：

```bash
cd /home/xiaoliu/releases/DSMS-enterprise-console-20260808-r3
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo
```

依次执行 5 个脚本。每条命令成功后再执行下一条：

```bash
docker exec -i mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --default-character-set=utf8mb4 \
  medicine_system \
  < sql/migrations/20260808_platform_admin_role.sql
```

作用：建立唯一平台管理员 `admin`，将历史普通管理员角色改为家庭守护人。成功时没有 `ERROR`。

```bash
docker exec -i mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --default-character-set=utf8mb4 \
  medicine_system \
  < /home/xiaoliu/releases/20260808_assign_existing_elder_accounts.sql
```

作用：把两个既有真实账号设置为安心用药端并绑定给平台管理员。成功时会显示两行账号、角色、绑定编号和状态。

```bash
docker exec -i mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --default-character-set=utf8mb4 \
  medicine_system \
  < sql/migrations/20260808_mobile_family_enhancement.sql
```

作用：给家庭订单增加预计到货时间。脚本可重复执行，不会重复加列。

```bash
docker exec -i mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --default-character-set=utf8mb4 \
  medicine_system \
  < sql/demo/20260808_seed_enterprise_demo.sql
```

作用：生成药品、方案、库存、1000 条购药记录和 120 条演示消息。旧版本已执行过时会安全跳过重复插入，但仍会补齐晨午晚时段。

```bash
docker exec -i mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --default-character-set=utf8mb4 \
  medicine_system \
  < /home/xiaoliu/releases/20260808_seed_existing_elder_scenarios.sql

unset MYSQL_ROOT_PASSWORD
```

作用：保证两个既有安心用药账号都有晨间、午间、晚间方案和库存。成功时每个账号应显示方案数和库存数。

参数解释：

- `docker exec -i` 中 `-i` 是 interactive，保持标准输入打开，才能把 `< SQL文件` 的内容送进容器。
- `mysql` 是容器内的 MySQL 命令行客户端。
- `<` 把右侧 SQL 文件作为左侧 mysql 命令的输入。
- `--default-character-set=utf8mb4` 指定完整 UTF-8，避免中文乱码。

任何脚本出现 `ERROR` 时立即停止，保存完整错误文字，不要继续部署 JAR。

## 五、安装后端 JAR

```bash
sudo systemctl stop dsms-backend

sudo install -m 0644 \
  backend/medicine-system-1.0.0.jar \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar

sudo systemctl start dsms-backend
sudo systemctl is-active dsms-backend
```

命令解释：

- `systemctl` 是 systemd 服务控制工具。
- `stop` 停止服务，避免覆盖运行中的版本时状态不清楚；`start` 启动新版本。
- `install` 在这里不是安装软件包，而是“复制文件并设置权限”；`-m 0644` 表示所有者可读写，其他用户只读。
- `is-active` 检查服务是否正在运行。

成功标志：最后输出 `active`。如果不是 `active`，查看日志：

```bash
sudo journalctl -u dsms-backend -n 100 --no-pager
```

- `journalctl` 查看 systemd 日志。
- `-u` 是 unit，只看 `dsms-backend` 这个服务单元。
- `-n 100` 是 number，显示最后 100 行。
- `--no-pager` 不进入分页器，日志直接打印完，不会卡在冒号界面。

## 六、安装前端 dist

```bash
sudo mkdir -p /var/www/dsms
sudo cp -a frontend/dist/. /var/www/dsms/
sudo nginx -t
sudo systemctl reload nginx
```

命令解释：

- `frontend/dist/.` 中的 `.` 表示复制 dist 里面的全部内容，而不是再套一层 dist 目录。
- `nginx -t` 中 `-t` 是 test，只检查 Nginx 配置语法，不会修改配置。
- `systemctl reload nginx` 平滑重新加载配置，不会像 restart 那样主动中断已有连接。

成功标志：`nginx -t` 显示 `syntax is ok` 和 `test is successful`，随后 reload 没有报错。

## 七、服务器本机健康检查

```bash
curl -i http://127.0.0.1:8088/api/auth/human-challenge
curl -I http://127.0.0.1/
sudo systemctl is-active dsms-backend
sudo systemctl is-active nginx
```

命令解释：

- `curl` 发起 HTTP 请求。
- `-i` 是 include，把响应头和响应体一起显示。
- `-I` 是 head，只读取响应头，适合检查前端首页是否能返回。
- `127.0.0.1` 是服务器自己，不经过阿里云公网和安全组。

成功标志：后端返回 HTTP 200 和人机挑战数据；前端返回 HTTP 200；两个服务均为 `active`。

## 八、浏览器验收

1. 先在电脑无痕窗口访问公网地址，清除旧缓存影响。
2. 用 `admin/admin` 登录。浏览器可能因为该密码过于常见而提示泄露，这是弱口令警告；验收完成后应立即修改。
3. 再用两个安心用药账号分别检查今日用药、订单与收货、购药余额、消息中心。
4. 用手机检查新增方案、购药列表、资金弹窗和收货核验弹窗。
5. 按《TEST_REPORT_20260808_R3.md》第 7 节逐项验收。

## 九、回滚方法

如果新版本启动失败，先恢复后端和前端：

```bash
sudo systemctl stop dsms-backend
sudo cp -a \
  /home/xiaoliu/backups/20260808-r3/medicine-system-1.0.0.jar \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar

sudo cp -a \
  /home/xiaoliu/backups/20260808-r3/frontend/. \
  /var/www/dsms/

sudo systemctl start dsms-backend
sudo nginx -t
sudo systemctl reload nginx
```

数据库回滚会覆盖发版后产生的新数据，风险高，不要直接执行。只有确认必须整体恢复时，才先停后端，再使用第三步的 `medicine_system.sql`；执行前应再备份一次当前数据库。
