# DSMS R7.1 后端启动热修复发版手册

## 这次修复什么

R7 的 `FlexibleLocalDateTimeDeserializer` 能正确解析浏览器 ISO 时间和历史空格时间，
但没有向 Spring Boot/Jackson 明确声明其处理类型。后端因此在创建 JSON 消息转换器时启动失败，
R7 安装脚本随后自动恢复了旧 JAR。

R7.1 将该类改为 `StdDeserializer<LocalDateTime>`，并显式注册 `LocalDateTime.class`。
同时增加两项回归验证：处理类型断言、Spring Boot ObjectMapper 构建验证。

## 服务器安装顺序

在本地把下列两个文件上传到 `/home/xiaoliu/releases/`：

- `DSMS-backend-R7.1-hotfix-20260825.zip`
- `DSMS-backend-R7.1-hotfix-20260825.zip.sha256`

然后执行：

```bash
cd /home/xiaoliu/releases
sha256sum -c DSMS-backend-R7.1-hotfix-20260825.zip.sha256
unzip -q DSMS-backend-R7.1-hotfix-20260825.zip
cd /home/xiaoliu/releases/DSMS-backend-R7.1-hotfix-20260825
bash deploy/install-backend-r7-1-hotfix.sh
```

必须看到：

```text
DSMS_R7_1_BACKEND_HOTFIX_OK
```

如果看到 `DSMS_R7_1_BACKEND_HOTFIX_FAILED`，不要继续执行正式数据初始化；
脚本会恢复旧 JAR，并把本轮失败日志保存在打印出的备份目录中。

## 安装后验证

```bash
sudo systemctl is-active dsms-backend
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge
sha256sum backend/medicine-system-1.0.0.jar
sudo sha256sum /home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar
```

预期：服务输出 `active`，接口返回包含 `challengeId` 的 JSON，两个 JAR 校验值完全相同。

本包只替换后端 JAR，不重复安装已经成功的博客、平台前端和 DSMS 前端，也不修改数据库。
