# DSMS R8 六项修复部署说明

本次发布只更新 DSMS 的数据库、后端 JAR 和 `/kanglian-cloud/` 前端。个人博客首页与 `/cloud-hub/` 不在本次替换范围。

## 一、校验并解压

```bash
cd /home/xiaoliu/releases
sha256sum -c DSMS-business-flow-fixes-20260825-R8.zip.sha256
unzip -q DSMS-business-flow-fixes-20260825-R8.zip
cd DSMS-business-flow-fixes-20260825-R8
sha256sum -c SHA256SUMS.txt
```

- 目的：确认上传的压缩包和包内文件没有损坏或被换掉。
- 正确结果：两次校验都显示 `OK`。
- 停止条件：出现 `FAILED` 时不要继续安装，重新上传发布包。

## 二、执行数据库迁移

```bash
bash deploy/migrate-dsms-r8.sh
```

- 目的：先备份正式数据库，再建立直接购药记录到资金流水、凭证档案的关联字段。
- 正确结果：最后显示 `DSMS_R8_DATABASE_MIGRATION_OK` 和备份目录。
- 停止条件：没有成功标记时不要更新后端。

## 三、放开手机照片上传大小

```bash
bash deploy/configure-upload-limit-r8.sh
```

- 目的：把 Nginx 请求体上限设为 16MB；应用实际只接收不超过 12MB 的真实图片，额外空间用于表单封装开销。
- 正确结果：最后显示 `DSMS_R8_UPLOAD_LIMIT_OK`。
- 停止条件：没有成功标记时不要继续；脚本会恢复原 Nginx 配置。

## 四、更新后端

```bash
bash deploy/install-backend-r8.sh
```

- 目的：备份旧 JAR、替换新 JAR、重启 `dsms-backend` 并检查健康接口。
- 正确结果：最后显示 `DSMS_R8_BACKEND_INSTALL_OK`。
- 停止条件：出现失败标记时停止；脚本会尝试自动恢复旧 JAR，并保存失败日志。

## 五、只更新 DSMS 前端

```bash
bash deploy/install-dsms-frontend-r8.sh
```

- 目的：自动识别博客根目录，只原子替换其中的 `kanglian-cloud` 子目录。
- 正确结果：最后显示 `DSMS_R8_FRONTEND_INSTALL_OK`，同时打印备份目录。
- 停止条件：脚本无法唯一识别博客目录、Nginx 检查失败或入口文件不一致时停止；脚本会恢复旧 DSMS 目录。

## 六、整体验收

```bash
bash deploy/verify-dsms-r8.sh
```

- 目的：同时检查后端、Nginx、公网页面、R8 前端标记和数据库字段。
- 正确结果：最后显示 `DSMS_R8_VERIFY_OK`。
- 停止条件：任何一步失败都不要宣布发布成功，把从失败步骤开始的完整输出发回排查。

浏览器最后使用无痕窗口或强制刷新，依次验收：统计筛选、服用时段标签、老人消息中心、整卡选药、准字号两种输入、直接购药扣款和四类凭证时间线。

## 命令原理

- `sha256sum` 是文件指纹，上传前后相同才说明文件内容一致。
- `unzip` 解压发布包，不会自动安装。
- 数据库迁移先执行，是因为新后端启动后会使用新关联字段。
- `systemctl` 管理 Spring Boot 后端进程；`curl` 直接访问本机接口，绕开浏览器缓存判断后端是否真的可用。
- Nginx 不仅提供网页，也会先接收上传请求；默认请求体上限偏小会直接返回 413，请求甚至到不了 Spring Boot。
- 前端脚本使用“新目录准备完成后再整体换名”，避免用户访问到只复制了一半的页面。
