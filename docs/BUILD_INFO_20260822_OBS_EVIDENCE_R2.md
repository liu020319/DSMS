# DSMS OBS 凭证中心 R2 构建信息

- 源分支：`feature/obs-file-center`
- 基础业务提交：`5f729bf`（OBS 私有文件中心与购药凭证时间线）
- R2 修复：Vite 生产基路径改为 `/kanglian-cloud/`；新增博客/DSMS 边界安全部署脚本。
- 前端环境：Node.js `14.21.3`、npm `6.14.18`、Vite `4.5.14`。
- 前端结果：`npm run build` 成功，2275 个模块转换完成；构建产物不再包含 `src="/assets` 或 `href="/assets`。
- 后端环境：Java `1.8.0_202`、Maven `3.6.3`、Spring Boot `2.7.18`。
- 后端结果：使用项目本地 Maven 仓库执行 `mvn clean package`，19 个测试，0 失败、0 错误、0 跳过，`BUILD SUCCESS`。
- 部署脚本：两份 Bash 脚本均通过 `bash -n` 语法检查。
- 安全边界：发布包不包含生产 AK/SK；服务器和 OBS 端到端上传、读取及跨家庭拒绝仍须部署后验证。
