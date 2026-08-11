# 康联云企业工作台版：保姆级发版手册

适用版本：`DSMS-enterprise-console-20260808-r2.zip`

`r2` 是 2026-08-08 发布包的修正版：修复 MySQL 8.4 混合排序规则报错，并把校验清单统一为 Linux 可直接识别的 LF 换行。

目标服务器：阿里云 Ubuntu，项目目录 `/home/xiaoliu/DSMS`，MySQL 容器名 `mysql`，后端服务名 `dsms-backend`，Nginx 网站目录 `/var/www/dsms`。

本手册假定前一版已经能够通过 `http://8.148.69.75` 访问。如果目录、容器名或服务名与这里不同，不要直接执行替换命令。

---

## 一、这次发布包含什么

- 后端 JAR：`backend/medicine-system-1.0.0.jar`
- 前端正式文件：`frontend/dist/`
- 1000 条购药演示数据：`sql/demo/20260808_seed_enterprise_demo.sql`
- 本发版手册：`docs/RELEASE_20260808_ENTERPRISE_CONSOLE.md`
- 产品设计与简历亮点：`docs/PRODUCT_DESIGN_AND_PORTFOLIO.md`
- 校验文件：`SHA256SUMS.txt`

本次没有把真实数据库密码、手机号、邮箱、JWT 密钥或生产账号初始化脚本放进压缩包。

---

## 二、先认识命令，不要盲目复制

| 写法 | 英文全称或来源 | 本手册中的作用 |
|---|---|---|
| `sudo` | **superuser do** | 临时以系统管理员权限执行一条命令。修改 `/var/www`、管理服务时需要。它不是“登录 root”，只提升这一条命令。 |
| `cd` | **change directory** | 切换当前目录。执行相对路径命令前先进入正确位置。 |
| `pwd` | **print working directory** | 显示当前所在的完整目录，用来防止在错误目录操作。 |
| `ls` | **list** | 列出文件。`ls -lh` 中 `-l` 是详细列表，`-h` 是 human-readable，按 KB/MB 显示大小。 |
| `mkdir -p` | **make directory / parents** | 创建目录；`-p` 会同时创建缺少的父目录，目录已存在时也不报错。 |
| `unzip -o` | **unzip / overwrite** | 解压 ZIP；`-o` 表示覆盖目标目录中的同名文件。只对新的发布目录使用。 |
| `cp -a` | **copy / archive** | 保留目录结构和文件属性复制。路径末尾的 `/.` 表示复制目录里面的全部内容。 |
| `mv` | **move** | 移动或重命名文件。本手册用它把当前前端目录整体改名为备份。 |
| `read -s -p` | **read / silent / prompt** | 读取输入；`-s` 不在屏幕显示密码，`-p` 显示提示语。 |
| `docker exec` | **execute in container** | 在已经运行的 MySQL 容器内执行 `mysql` 或 `mysqldump`。`-i` 保持标准输入开启，以便把 SQL 文件送进去。 |
| `mysql -u root -p...` | **user / password** | `-u` 是数据库 user，指定 root；`-p` 后跟密码。这里的 `-u` 与 `journalctl -u` 不是同一含义。 |
| `mysqldump` | MySQL dump | 把数据库结构和数据导出成可恢复的 SQL 备份。 |
| `systemctl` | **system control** | 管理 systemd 服务。`restart` 重启，`is-active` 检查是否正在运行。 |
| `journalctl` | **journal control/query** | 查看 systemd 日志。`-u dsms-backend` 中 `-u` 是 **unit**，只看这个服务；`-n 80` 中 `-n` 是 **number**，只看最后 80 行。 |
| `--no-pager` | no pager | 不进入分页阅读器，日志直接打印到终端。否则可能出现一个冒号提示，让人误以为卡住；按 `q` 才能退出。 |
| `nginx -t` | test | 只检查 Nginx 配置语法，不重启。看到 `syntax is ok` 和 `test is successful` 才能继续。 |
| `curl` | client URL | 从服务器自己访问接口。`-sS` 表示平时安静、出错时显示错误；`-I` 只看 HTTP 响应头。 |
| `sha256sum -c` | SHA-256 checksum / check | 计算并检查文件指纹，确认上传过程没有损坏文件。 |
| `grep` | global regular expression print | 从输出中筛选关心的文本，例如 HTTP 状态。 |

特别说明：`journalctl -u dsms-backend -n 80 --no-pager` 没有 `-noh`。你之前看到的很可能是把 `-n` 与 `--no-pager` 连在一起理解了。

---

## 三、Windows 上把压缩包传到服务器

本地压缩包位置：

```text
D:\CodexWorkFiles\DSMS\output\DSMS-enterprise-console-20260808-r2.zip
```

1. 用 MobaXterm 连接 `8.148.69.75`。
2. 在左侧服务器文件面板进入 `/home/xiaoliu`。
3. 新建目录 `upload`；也可以在终端执行下面两条：

```bash
mkdir -p /home/xiaoliu/upload
ls -ld /home/xiaoliu/upload
```

第一条创建上传目录；第二条确认目录存在。成功时会看到一行以 `d` 开头的目录信息。

4. 把 ZIP 拖到 `/home/xiaoliu/upload/`。
5. 上传完成后执行：

```bash
ls -lh /home/xiaoliu/upload/DSMS-enterprise-console-20260808-r2.zip
```

成功标志：能看到文件名，而且大小不是 `0`。

---

## 四、校验并解压发布包

```bash
mkdir -p /home/xiaoliu/releases/DSMS-enterprise-console-20260808-r2
cd /home/xiaoliu/releases/DSMS-enterprise-console-20260808-r2
unzip -o /home/xiaoliu/upload/DSMS-enterprise-console-20260808-r2.zip
pwd
ls -lh
```

逐行解释：

1. 创建本次独立发布目录，避免直接覆盖正在运行的项目。
2. 进入这个目录；后续 `SHA256SUMS.txt` 中使用的是相对路径。
3. 解压上传包；`-o` 只会覆盖本次发布目录中的同名文件。
4. 打印当前位置，应该是 `/home/xiaoliu/releases/DSMS-enterprise-console-20260808-r2`。
5. 检查解压结果。

校验包内文件：

```bash
# 兼容在 Windows 生成的旧版校验清单：删除每行末尾不可见的 CR 字符。
# 新版发布包已经使用 Linux 的 LF 换行，这一行重复执行也没有副作用。
sed -i 's/\r$//' SHA256SUMS.txt
sha256sum -c SHA256SUMS.txt
```

`sed` 是流式文本编辑器；`-i`（in-place）表示直接修正当前文件。`s/\r$//` 表示删除每一行末尾的 Windows 回车符，不会修改被校验的 JAR、网页或 SQL 文件。

成功标志：每一行末尾都是 `OK`。如果仍有任何一行出现 `FAILED`，不要继续：这说明对应文件确实不完整或不是本次版本，需要重新上传 ZIP。

---

## 五、发布前健康检查

```bash
docker ps --filter name=mysql
sudo systemctl is-active dsms-backend
sudo nginx -t
df -h /
```

解释与成功标准：

1. 检查 MySQL 容器。应该看到 `mysql:8`，状态为 `Up ...`。
2. 检查后端服务。应该只输出 `active`。
3. 检查 Nginx 配置。必须同时看到 `syntax is ok` 和 `test is successful`。
4. 检查系统盘空间。`Avail` 建议至少还有 2 GB，本服务器此前还有约 32 GB。

任意一项不符合就先停止，不要带病发布。

---

## 六、备份数据库、后端和前端

### 6.1 备份数据库

```bash
mkdir -p /home/xiaoliu/backups/20260808-enterprise-console
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo
docker exec mysql mysqldump \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction \
  --routines \
  --triggers \
  medicine_system \
  > /home/xiaoliu/backups/20260808-enterprise-console/medicine_system_before_release.sql
unset MYSQL_ROOT_PASSWORD
ls -lh /home/xiaoliu/backups/20260808-enterprise-console/medicine_system_before_release.sql
```

解释：

- 第一行建立本次备份目录。
- `read -s` 让密码输入不回显；`echo` 只是换行。
- `mysqldump` 导出数据库。
- `--single-transaction` 在 InnoDB 下尽量获得一致快照，并减少长时间锁表。
- `--routines` 保存存储过程，`--triggers` 保存触发器。
- `>` 把导出结果写入服务器文件；它会覆盖同名文件，所以本手册使用独立日期目录。
- `unset` 从当前终端变量中删除数据库密码。

成功标志：最后显示的 SQL 文件不是 `0` 字节。可再执行：

```bash
head -n 5 /home/xiaoliu/backups/20260808-enterprise-console/medicine_system_before_release.sql
```

`head -n 5` 只查看开头五行，应该能看到 MySQL dump 说明文字。

### 6.2 备份当前后端 JAR

```bash
cp -a \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar \
  /home/xiaoliu/backups/20260808-enterprise-console/medicine-system-1.0.0.jar
```

这只是复制当前 JAR，不停止服务。

### 6.3 备份当前前端

```bash
sudo cp -a \
  /var/www/dsms \
  /home/xiaoliu/backups/20260808-enterprise-console/frontend
```

`sudo` 是因为 `/var/www` 通常归 root 管理。备份目标位于明确的日期目录，不会碰其他系统文件。

---

## 七、导入 1000 条演示购药数据

演示数据不是系统启动的必要条件；它只是让你能体验统计和风险页面。建议先备份，再导入。

```bash
cd /home/xiaoliu/releases/DSMS-enterprise-console-20260808-r2
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo
docker exec -i mysql mysql \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --default-character-set=utf8mb4 \
  medicine_system \
  < sql/demo/20260808_seed_enterprise_demo.sql
unset MYSQL_ROOT_PASSWORD
```

解释：

- `docker exec -i` 保持输入通道，让 `<` 后面的 SQL 文件进入容器内 MySQL。
- `--default-character-set=utf8mb4` 防止中文药品名称乱码。
- `<` 表示把服务器上的 SQL 文件作为数据库客户端的输入。
- 脚本会自动选择一个启用的家庭管理员和一个启用的安心用药成员，不需要在公开 SQL 中写手机号。
- 脚本使用事务，出错会回滚；使用版本记录，同一版本再次执行会提示跳过。
- 修正版用二进制方式比较演示数据版本号，因此兼容旧库的 `utf8mb4_general_ci` 和 MySQL 8.4 的 `utf8mb4_0900_ai_ci`，不需要修改现有数据库的排序规则。

成功标志应包含：

```text
企业演示数据生成成功
purchase_record_count  1000
all_demo_medicines     20
```

如果提示“数据库版本过旧”，先检查之前两个 `20260807` 迁移脚本是否已经执行。不要删除版本表强制重跑。

### 验证数据量

```bash
read -s -p "请输入MySQL root密码: " MYSQL_ROOT_PASSWORD
echo
docker exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" medicine_system \
  -e "SELECT COUNT(*) AS purchase_count FROM purchase_record WHERE deleted=0; SELECT COUNT(*) AS demo_medicine_count FROM medicine WHERE deleted=0 AND approval_number LIKE '国药准字H9000%';"
unset MYSQL_ROOT_PASSWORD
```

这里数据库 `mysql` 命令中的 `-e` 是 **execute**，直接执行后面的 SQL 并打印结果。

---

## 八、替换后端 JAR

```bash
cp -a \
  /home/xiaoliu/releases/DSMS-enterprise-console-20260808-r2/backend/medicine-system-1.0.0.jar \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar
sudo systemctl restart dsms-backend
sudo systemctl is-active dsms-backend
```

解释：

1. 把已经在本地构建并测试的 JAR 复制到 systemd 服务原来读取的位置。
2. `restart` 让 Java 进程重新读取新 JAR。这个过程通常有数秒短暂中断。
3. `is-active` 检查服务状态，必须返回 `active`。

查看最近 80 行日志：

```bash
sudo journalctl -u dsms-backend -n 80 --no-pager
```

成功标志：靠近最后能看到 `Started MedicineSystemApplication` 或 `Tomcat started on port(s): 8088`，且没有新的 `APPLICATION FAILED TO START`。

后端本机接口检查：

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" \
  http://127.0.0.1:8088/api/auth/human-challenge
```

解释：

- `-o /dev/null` 丢弃响应正文，只检查结果。
- `-w` 是 **write-out**，输出 HTTP 状态码。
- 期望看到 `HTTP 200`。

---

## 九、替换前端文件

本步骤先把旧目录整体改名为临时回滚目录，再建立新目录。目标路径是明确的 `/var/www/dsms`，不要改成 `/var/www` 或 `/`。

```bash
sudo mv /var/www/dsms /var/www/dsms_previous_20260808
sudo mkdir -p /var/www/dsms
sudo cp -a \
  /home/xiaoliu/releases/DSMS-enterprise-console-20260808-r2/frontend/dist/. \
  /var/www/dsms/
sudo nginx -t
sudo systemctl reload nginx
```

解释：

1. `mv` 把旧网站目录改名，保留一个快速回滚副本。
2. 创建新的空网站目录。
3. `dist/.` 中的 `.` 表示复制 `dist` 里面的所有文件，包括隐藏文件，而不是再套一层 `dist`。
4. 先 `nginx -t` 检查配置。
5. `reload` 平滑重载 Nginx，通常不会像 `restart` 那样直接中断现有连接。

检查首页：

```bash
curl -sS -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1/
curl -sS http://127.0.0.1/ | grep -o '<title>[^<]*</title>'
```

第一条期望 `HTTP 200`。第二条从 HTML 中提取标题，用来确认 Nginx 确实读到了新前端。

---

## 十、浏览器验收清单

打开：`http://8.148.69.75`

先按 `Ctrl + F5` 强制刷新，避免浏览器继续使用旧 JavaScript 缓存。

家庭协同端依次检查：

1. 登录页品牌显示“康联云”。
2. 左侧导航按四个业务域分组。
3. 点击左上角折叠按钮，侧栏从 248px 收到 76px；刷新页面后仍保留选择。
4. 顶部不再重复显示药品档案、用药方案等四个按钮，而是显示面包屑、全局搜索、消息和快速新建。
5. 按 `Ctrl + K`，能打开全局功能搜索。
6. 首页显示角色化欢迎区、家庭健康分、风险队列、快捷任务和能力地图。
7. 进入“药事协同 → 药品档案”，能看到总数、启用数、平均价和待完善数。
8. 点击药品名称或国药准字号，右侧打开“药品业务画像”。
9. 在抽屉中切换“基础档案、关联用药、购药轨迹”。
10. 进入风险中心，能看到高风险、库存预警和临期批次。
11. 进入费用凭证档案，能按凭证完整性筛选。
12. 进入统计分析，年/月/周/日、平台、渠道和时段都有数据。
13. 进入数据质量，能看到质量评分和治理任务。

手机端检查：

1. 用手机访问同一网址并登录。
2. 顶部出现菜单按钮，左侧导航不应挤压正文。
3. 药品 KPI 为两列，筛选栏自动换行。
4. 药品详情抽屉占满手机宽度，字段不横向溢出。
5. 表格允许横向滑动，页面本身不应整体错位。

---

## 十一、出现问题时怎么判断

### 11.1 页面打不开或 502

```bash
sudo systemctl is-active dsms-backend
sudo journalctl -u dsms-backend -n 120 --no-pager
sudo nginx -t
curl -sS -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:8088/api/auth/human-challenge
```

- 后端不是 `active`：优先看 Java 日志。
- 后端本机是 200，但公网是 502：检查 Nginx 反向代理配置。
- Nginx 测试失败：不要 reload，先恢复原配置或修正错误。

### 11.2 页面仍是旧版

```bash
ls -lh /var/www/dsms/index.html
grep -R "康联云" /var/www/dsms/assets | head
```

如果能搜到“康联云”，服务器文件是新的，通常只需浏览器 `Ctrl + F5` 或清除该站点缓存。

### 11.3 数据导入后看不到 1000 条

先执行第七节的数据库计数命令。数据库有数据但页面没有时，检查当前登录的家庭管理员是否绑定到脚本自动选择的安心用药成员；统计分析选择“全部成员”也能看到全量数据。

---

## 十二、完整回滚

只有本次新版本确实影响使用时才回滚。

### 12.1 回滚后端

```bash
cp -a \
  /home/xiaoliu/backups/20260808-enterprise-console/medicine-system-1.0.0.jar \
  /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar
sudo systemctl restart dsms-backend
sudo systemctl is-active dsms-backend
```

### 12.2 回滚前端

先确认两个目录都存在：

```bash
ls -ld /var/www/dsms /var/www/dsms_previous_20260808
```

确认无误后：

```bash
sudo mv /var/www/dsms /var/www/dsms_failed_20260808
sudo mv /var/www/dsms_previous_20260808 /var/www/dsms
sudo nginx -t
sudo systemctl reload nginx
```

这组命令没有删除目录，失败版本仍保留为 `/var/www/dsms_failed_20260808`，方便排查。

### 12.3 是否回滚数据库

演示数据不会改变真实账号密码，但会增加统计记录。一般不建议因为页面问题回滚整个数据库，只回滚前后端即可。

如果确实需要恢复整库，必须先停止后端，并明确接受“发布后新增业务数据会丢失”的后果。恢复整库是高风险操作，不应直接照抄执行；请先根据备份时间确认影响范围。

---

## 十三、以后每次开发后的标准发版流程

以后不需要重新摸索，固定按下面顺序：

1. 本地修改源代码。
2. 后端执行 `mvn test`，先确认测试通过。
3. 后端执行 `mvn clean package`，生成 JAR。
4. 前端执行 `npm ci`，按锁文件安装依赖；没有依赖变化时可以跳过。
5. 前端执行 `npm run build`，生成 `dist`。
6. 检查 Git 改动，确认没有密码、手机号、邮箱和私有配置。
7. 提交并推送到 GitHub 的公开同步分支。
8. 生成包含 JAR、dist、SQL、文档和 SHA-256 的发布 ZIP。
9. 上传到服务器新的 releases 目录。
10. 健康检查、备份数据库/后端/前端。
11. 按版本顺序执行数据库增量脚本。
12. 替换 JAR、重启后端、检查日志和本机 API。
13. 替换 dist、测试 Nginx、平滑 reload。
14. 电脑和手机完成业务验收。
15. 保留至少一个可用旧版本，确认稳定后再清理较老备份。

### Maven 命令解释

```bash
mvn clean package
```

- `mvn` 是 Maven 命令。
- `clean` 删除上次的 `target` 构建产物，防止旧文件混入。
- `package` 编译、运行测试并打包成 JAR。
- 如果只想先跑测试，使用 `mvn test`。
- `-DskipTests` 表示跳过测试，不适合正式发版；本项目发版默认不使用。

### npm 命令解释

```bash
npm ci
npm run build
```

- `npm` 是 Node Package Manager。
- `ci` 是 clean install，严格按照 `package-lock.json` 安装，适合持续集成和可重复构建。
- `npm run build` 执行 `package.json` 中的 build 脚本，本项目实际调用 Vite 生成正式压缩资源。
- `npm run dev` 只用于开发调试，不能把开发服务器当作线上部署。
