USE medicine_system;

-- 平台角色升级：ADMIN 只保留给唯一超级管理员，普通家属使用 GUARDIAN。
-- 注意：用户指定的真实家庭账号放在本地私有脚本中，不进入 GitHub。
INSERT INTO sys_user (
  username, password, real_name, role, bind_parent_id, status,
  failed_login_attempts, locked_until, create_time, update_time, deleted
)
VALUES (
  'admin',
  '$2a$10$TuITECcS.Ty.C7GdmPbp5evsiS3AMDH9F83OHzFIeotRr45XyxFvK',
  '平台超级管理员', 'ADMIN', NULL, 1,
  0, NULL, NOW(), NOW(), 0
)
ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  real_name = VALUES(real_name),
  role = 'ADMIN',
  bind_parent_id = NULL,
  status = 1,
  failed_login_attempts = 0,
  locked_until = NULL,
  deleted = 0,
  update_time = NOW();

-- 历史上标记为 ADMIN 的家属账号降为家庭守护人，避免拥有全平台权限。
UPDATE sys_user
SET role = 'GUARDIAN', update_time = NOW()
WHERE role = 'ADMIN'
  AND username <> 'admin'
  AND deleted = 0;
