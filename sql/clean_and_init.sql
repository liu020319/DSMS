SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE stock;
TRUNCATE TABLE purchase_record;
TRUNCATE TABLE prescription_history;
TRUNCATE TABLE prescription;
TRUNCATE TABLE approval_task;
TRUNCATE TABLE medicine;
TRUNCATE TABLE sys_log;
TRUNCATE TABLE sys_config;

DELETE FROM sys_user WHERE deleted = 0;

INSERT INTO sys_user (user_id, username, password, real_name, phone, role, status, create_time, update_time, deleted)
VALUES (1, 'admin', '$2a$10$k4mUJFDb4pInBdXAS8qZ9.J2Qjkj/xvNazX7F3G1nq3vp9HeXJyBy', '系统管理员', '13800000001', 'ADMIN', 1, NOW(), NOW(), 0);

INSERT INTO sys_user (user_id, username, password, real_name, phone, role, status, create_time, update_time, deleted)
VALUES (2, 'elder1', '$2a$10$k4mUJFDb4pInBdXAS8qZ9.J2Qjkj/xvNazX7F3G1nq3vp9HeXJyBy', '王大爷', '13800000002', 'ELDER', 1, NOW(), NOW(), 0);

INSERT INTO sys_config (config_key, config_value, config_desc) VALUES
('morning_threshold', '09:00', '晨服扣减阈值时间'),
('noon_threshold', '13:00', '午服扣减阈值时间'),
('evening_threshold', '21:00', '晚服扣减阈值时间');

SET FOREIGN_KEY_CHECKS = 1;
