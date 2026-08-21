# DSMS 华为云 OBS 文件中心：开发、原理与面试讲解

## 1. 先说业务，不先背代码

子女完成购药后，需要上传问诊截图、下单截图、付款凭证和发票；老人按时间线查看，并在收货后上传药品合照、核对数量、国药准字号、规格和包装。图片不能公开，也不能只保存一个无法鉴权的公网 URL。

本次实现把责任拆成两部分：

- 华为云 OBS 保存图片二进制内容；
- MySQL 的 `file_asset` 保存文件归属、权限、用途、业务关联、SHA-256 和生命周期；
- MySQL 的 `purchase_evidence` 保存某张文件是哪笔订单的哪类购药凭证，以及金额、平台和发生时间。

这不是重复存储。OBS 是“仓库”，数据库是“仓库台账和业务凭证目录”。

## 2. 完整数据流

```text
Vue 选择图片
  -> POST /api/files/images (multipart/form-data)
  -> JWT 登录校验
  -> FileAssetController
  -> FileAssetService 校验 5MB、JPG/PNG/GIF/WEBP 真实文件头、家庭归属
  -> FileStorageRouter 按配置选择 LOCAL 或 OBS
  -> LocalFileStorage / ObsFileStorage 保存二进制
  -> file_asset 写入文件台账
  -> 返回 /api/files/{id}/content
  -> 子女把 fileId 绑定到 purchase_evidence
  -> 老人打开时间线
  -> 后端再次校验家庭权限后流式读取图片
```

必须理解：私有桶中的对象没有匿名公网访问权。页面看到的是后端受保护接口，不是直接暴露的 OBS URL。

## 3. 为什么要引入 SDK 依赖

`backend/pom.xml` 增加：

```xml
<dependency>
    <groupId>com.huaweicloud</groupId>
    <artifactId>esdk-obs-java-bundle</artifactId>
    <version>${huaweicloud.obs.version}</version>
</dependency>
```

- SDK 封装了 HTTP 请求、AK/SK 签名、上传、读取和删除对象；自己手写签名容易出错。
- 使用 `bundle` 版本是为了隔离 SDK 的第三方依赖，降低与 Spring Boot 自带 HTTP 组件冲突的概率。
- 当前固定版本 `3.26.6`，避免每次构建解析到不同版本。

建议背诵：Maven 依赖写在 `pom.xml`，版本固定；SDK 解决“如何正确调用云 API”，不负责本系统的家庭权限。

## 4. 为什么配置文件不写真实 AK/SK

`application.yml` 只写环境变量引用：

```yaml
storage:
  provider: ${FILE_STORAGE_PROVIDER:local}
  object-prefix: ${FILE_OBJECT_PREFIX:dsms/prod}
  obs:
    endpoint: ${OBS_ENDPOINT:https://obs.cn-north-4.myhuaweicloud.com}
    bucket: ${OBS_BUCKET:dsms-file}
    access-key: ${OBS_ACCESS_KEY:}
    secret-key: ${OBS_SECRET_KEY:}
```

`${OBS_ACCESS_KEY:}` 表示：优先读取服务器环境变量 `OBS_ACCESS_KEY`，没有时使用空值。真实密钥只放服务器权限为 `600` 的环境文件。

- AK（Access Key ID）是访问密钥标识；
- SK（Secret Access Key）是签名秘密；
- 后端 SDK 使用 AK/SK 对请求签名；
- Vue、小程序、GitHub、聊天、截图和命令历史中都不能出现 SK。

## 5. 为什么有接口和两个实现

```text
FileStorage
  |- LocalFileStorage
  `- ObsFileStorage
```

业务服务只依赖 `FileStorage` 接口。开发机默认 `local`，不需要云密钥也能启动和测试；服务器设置 `FILE_STORAGE_PROVIDER=obs` 后切到 OBS。以后换 OSS、MinIO 时增加实现，不需要重写 Controller 和凭证业务。

这是依赖倒置和策略模式的实际应用。面试不要只说模式名称，要先说它解决了“本地测试、云端生产、供应商解耦和应急回退”。

## 6. 为什么对象键随机生成

示例：

```text
dsms/prod/family-10/payment/2026/08/49c3...d2.png
```

对象键包含环境、家庭编号、用途和年月，最后使用 UUID。它不包含姓名、手机号、药名或国药准字号，避免 URL/日志泄露隐私，也避免同名文件覆盖。

OBS 控制台中的“文件夹”是对象键前缀，不是 Linux 那种真实目录。

## 7. `file_asset` 字段为什么存在

| 字段 | 原因 |
|---|---|
| `storage_provider/bucket_name/object_key` | 找到物理文件 |
| `original_name/content_type/file_size` | 展示和响应元数据 |
| `sha256` | 完整性校验，未来可去重 |
| `file_category` | 区分付款、发票、收货照片等用途，防止混用 |
| `owner_user_id/family_id/access_scope` | 服务端权限校验 |
| `business_type/business_id` | 关联订单、收货或其他业务 |
| `status/delete_time/deleted` | 生命周期、逻辑删除和审计 |

为什么不只存 URL：URL 不能说明谁有权看、属于哪家、用于什么、是否删除、内容是否变化。

## 8. 一致性和失败补偿

上传流程先写对象，再写数据库。若数据库插入失败，代码立即删除刚上传的对象，减少“OBS 中有图片但数据库没有记录”的孤儿文件。

这是补偿事务，不是分布式事务。进程若恰好在两步之间崩溃仍可能留下孤儿对象；更高阶方案是定时对账、Outbox 或对象隔离区。当前项目先采用与体量匹配的方案，不为了简历硬加 MQ。

## 9. 购药凭证与权限

`purchase_evidence` 支持：

- `CONSULTATION`：问诊记录；
- `ORDER_SCREENSHOT`：下单截图；
- `PAYMENT`：付款凭证；
- `INVOICE`：发票凭证。

子女或平台管理员可登记，本家庭老人和守护人可查看。后端同时检查：

1. 角色是否允许；
2. 当前账号是否属于订单对应家庭；
3. 文件类型是否与凭证类型一致；
4. 文件是否已经绑定另一笔业务。

这是“RBAC 角色权限 + 数据范围权限”。前端隐藏按钮不等于安全，真正的限制必须在服务端。

## 10. 自己完成本地构建

### 后端

```powershell
cd D:\CodexWorkFiles\DSMS\dsms-obs-file-center\backend
mvn '-Dmaven.repo.local=D:\CodexWorkFiles\.m2-repository' clean package
```

- `mvn`：Maven 启动命令；
- `-D`：定义 Maven/Java 系统属性；
- `maven.repo.local`：使用工作区依赖缓存，不修改全局 Maven；
- `clean`：删除旧 `target`；
- `package`：编译、测试并生成 JAR。

成功：出现 `BUILD SUCCESS`，并存在 `target/medicine-system-1.0.0.jar`。失败：停在第一条 `[ERROR]` 处理，不能上传旧 JAR 冒充新包。

### 前端

```powershell
cd D:\CodexWorkFiles\DSMS\dsms-obs-file-center\frontend
npm ci
npm run build
```

- `npm ci`：严格按 `package-lock.json` 干净安装；
- `npm run build`：执行 `package.json` 中的 `vite build`；
- `dist`：Nginx 实际部署的静态 HTML/CSS/JS。

本机旧 npm 6 曾出现 `cb() never called!`，属于 npm 自身在安装收尾阶段失败。本次在核对核心版本完全一致后复用了原项目 `node_modules` 目录联接完成构建。后续应在项目内升级构建工具链或使用固定 Node 容器，不要未经评估升级服务器全局 Node。

## 11. 面试回答

### 30 秒

“我在家庭用药系统中设计了统一私有文件中心。华为 OBS 保存图片字节，MySQL 文件台账保存家庭归属、用途、业务关联、SHA-256 和生命周期。后端通过存储接口在本地与 OBS 之间切换，上传时校验真实图片类型并生成随机对象键，读取时再次做 RBAC 和家庭数据范围校验。AK/SK 只放服务器环境变量，不进入前端和 Git。购药订单支持问诊、付款、发票和下单截图时间线。”

### 常见追问

1. 为什么不用公网 URL？答：无法做家庭权限、审计、用途和生命周期管理。
2. 为什么不让前端直传？答：当前小图片体量优先后端统一鉴权；大流量时再引入带大小、类型和过期条件的临时签名直传。
3. 数据库失败怎么办？答：普通失败做对象补偿删除；进程崩溃窗口后续用定时对账治理。
4. 本地测试证明 OBS 可用吗？答：不能。本地测试证明接口、校验和本地驱动；OBS 还要在服务器用最小权限密钥做上传、受保护读取和删除验证。

## 12. 必须理解 / 建议背诵 / 现场会查

- 必须理解：上传和读取数据流；OBS 与 MySQL 分工；家庭权限；AK/SK 为什么不能进前端。
- 建议背诵：`FileStorage` 抽象、随机对象键、SHA-256、补偿删除、RBAC + 数据范围。
- 现场会查：SDK 精确版本、endpoint 拼写、IAM 策略 JSON。

自测题：不看文档，用自己的话从“子女点击上传付款截图”讲到“老人打开时间线看到图片”，并说出其中三次安全校验。
