# DSMS R7.1 后端热修复测试报告

## 故障复现结论

- 生产日志：`Unknown handled type in com.medicine.config.FlexibleLocalDateTimeDeserializer`
- 失败阶段：Spring Boot 创建 Jackson ObjectMapper / HTTP JSON 消息转换器
- 服务器保护：原 R7 安装脚本已自动回滚，旧后端恢复健康

## 修复与预防

- 自定义反序列化器改为继承 `StdDeserializer<LocalDateTime>`
- 构造器显式传入 `LocalDateTime.class`
- 新增 `handledType()` 回归断言
- 新增 Spring Boot `Jackson2ObjectMapperBuilder` 构建回归测试
- 新安装脚本检测到明确启动失败后立即保存日志、回滚，不再空等 90 秒
- 安装成功后核对发布 JAR 与服务器 JAR 的 SHA-256

## 本地结果

- 故障专项测试：5 个全部通过
- Maven `clean package`：47 个测试全部通过
- 失败、错误、跳过：0 / 0 / 0
