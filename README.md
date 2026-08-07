# DSMS 家庭慢病用药管理系统

本项目由 Vue 3 前端、Spring Boot 后端和 MySQL 组成。药品库存、购药记录和审批数据均保存在 MySQL，不能只部署前端。

## 本地启动

1. 使用 MySQL 8 创建数据库，并执行 `sql/init.sql` 及项目需要的增量 SQL。
2. 配置后端运行变量（不要把真实密码提交到 Git）：

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:mysql://127.0.0.1:3306/medicine_system?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
$env:SPRING_DATASOURCE_USERNAME = '数据库账号'
$env:SPRING_DATASOURCE_PASSWORD = '数据库密码'
$env:JWT_SECRET = '请替换为至少32位的随机字符串'
```

3. 启动后端：

```powershell
cd backend
mvn clean package
java -jar target/medicine-system-1.0.0.jar
```

4. 启动前端：

```powershell
cd frontend
npm ci
npm run dev
```

## 生产部署所需组件

- Linux 云服务器：运行 Java 后端、MySQL、Nginx。
- 域名：给用户一个容易访问的地址，例如 `example.com`；它通过 DNS 解析指向服务器公网 IP。
- HTTPS 证书：用于浏览器安全访问；若改为微信小程序，也必须使用 HTTPS 域名。
- MySQL 数据库：初期可放同一台服务器，生产密码需与代码分离。

建议先使用 GitHub Actions 或手工上传包完成第一次部署，验证后再引入 Docker、自动发布和小程序改造。

详细的上线选择与步骤见后续部署说明。
