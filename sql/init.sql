-- ============================================================
-- 家庭长期慢病用药安全管理系统 - 数据库建表脚本
-- 数据库: medicine_system
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS medicine_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE medicine_system;

-- 1.药品基础档案表
DROP TABLE IF EXISTS `medicine`;
CREATE TABLE `medicine` (
  `medicine_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `approval_number` VARCHAR(50) NOT NULL COMMENT '国药准字号',
  `medicine_name` VARCHAR(100) NOT NULL COMMENT '药品通用名',
  `brand_name` VARCHAR(100) NOT NULL COMMENT '品牌名',
  `specification` VARCHAR(200) NOT NULL COMMENT '规格',
  `unit_per_box` INT NOT NULL COMMENT '每盒单位数',
  `manufacturer` VARCHAR(200) NOT NULL COMMENT '生产厂家',
  `reference_price` DECIMAL(10,2) NOT NULL COMMENT '参考价格',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL(预留)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`medicine_id`),
  UNIQUE KEY `uk_approval_number` (`approval_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品基础档案表';

-- 2.个人用药方案表
DROP TABLE IF EXISTS `prescription`;
CREATE TABLE `prescription` (
  `prescription_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '方案ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `medicine_id` BIGINT NOT NULL COMMENT '药品ID',
  `daily_times` INT NOT NULL COMMENT '每日服药次数',
  `dosage_per_time` INT NOT NULL COMMENT '每次用量(单位数)',
  `daily_consumption` INT NOT NULL COMMENT '每日消耗量(单位数)',
  `days_per_box` INT NOT NULL COMMENT '单盒可吃天数',
  `take_notes` VARCHAR(500) DEFAULT NULL COMMENT '服药备注',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1在用 0停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`prescription_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_medicine_id` (`medicine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人用药方案表';

-- 3.库存核心数据表
DROP TABLE IF EXISTS `stock`;
CREATE TABLE `stock` (
  `stock_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `prescription_id` BIGINT NOT NULL COMMENT '方案ID',
  `last_calc_time` DATETIME NOT NULL COMMENT '上次计算时间',
  `total_remaining_units` INT NOT NULL DEFAULT 0 COMMENT '剩余总单位数',
  `remaining_days` INT NOT NULL DEFAULT 0 COMMENT '剩余可吃天数',
  `expiry_date` DATE NOT NULL COMMENT '最近有效期',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`stock_id`),
  UNIQUE KEY `uk_prescription_id` (`prescription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存核心数据表';

-- 4.用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `role` VARCHAR(20) NOT NULL COMMENT '角色 ADMIN子女 ELDER老人',
  `bind_parent_id` BIGINT DEFAULT NULL COMMENT '绑定的子女ID(老人关联子女)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_bind_parent_id` (`bind_parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 5.审批申请表
DROP TABLE IF EXISTS `approval_task`;
CREATE TABLE `approval_task` (
  `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `applicant_id` BIGINT NOT NULL COMMENT '申请人ID',
  `handler_id` BIGINT NOT NULL COMMENT '审批人ID',
  `task_type` VARCHAR(50) NOT NULL COMMENT '任务类型 NEW_MEDICINE/LOSS_ADJUST/STOCK_CORRECT',
  `content_json` TEXT NOT NULL COMMENT '申请内容JSON',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态 PENDING/APPROVED/REJECTED',
  `handler_comment` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`task_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_handler_id` (`handler_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批申请表';

-- 6.购药记录表
DROP TABLE IF EXISTS `purchase_record`;
CREATE TABLE `purchase_record` (
  `purchase_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购药记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `prescription_id` BIGINT NOT NULL COMMENT '方案ID',
  `purchase_date` DATE NOT NULL COMMENT '购药日期',
  `quantity_boxes` INT NOT NULL COMMENT '购买盒数',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `total_price` DECIMAL(10,2) NOT NULL COMMENT '总价',
  `expiry_date` DATE NOT NULL COMMENT '有效期',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`purchase_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_prescription_id` (`prescription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购药记录表';

-- 7.操作日志表
DROP TABLE IF EXISTS `sys_log`;
CREATE TABLE `sys_log` (
  `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT NOT NULL COMMENT '操作用户ID',
  `operation_type` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `operation_content` TEXT NOT NULL COMMENT '操作内容',
  `operation_ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
  `operation_time` DATETIME NOT NULL COMMENT '操作时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`log_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 8.用药方案历史版本表(补充优化项)
DROP TABLE IF EXISTS `prescription_history`;
CREATE TABLE `prescription_history` (
  `history_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史ID',
  `prescription_id` BIGINT NOT NULL COMMENT '方案ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `medicine_id` BIGINT NOT NULL COMMENT '药品ID',
  `daily_times` INT NOT NULL COMMENT '每日服药次数',
  `dosage_per_time` INT NOT NULL COMMENT '每次用量',
  `daily_consumption` INT NOT NULL COMMENT '每日消耗量',
  `days_per_box` INT NOT NULL COMMENT '单盒可吃天数',
  `take_notes` VARCHAR(500) DEFAULT NULL COMMENT '服药备注',
  `change_reason` VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`history_id`),
  KEY `idx_prescription_id` (`prescription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用药方案历史版本表';

-- 初始管理员账号(密码: admin123, BCrypt加密)
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `role`, `bind_parent_id`, `status`)
VALUES ('admin', '$2a$10$RiHbs75Bfa6QSIv.laxe3.TC/h2pWHy7c1mPzVUVTixqt7YDj06We', '系统管理员', '13800000000', 'ADMIN', NULL, 1);

INSERT INTO `sys_user` (`username`, `password`, `real_name`, `phone`, `role`, `bind_parent_id`, `status`)
VALUES ('elder1', '$2a$10$rPn4xdvYhZGFDCzLcx.wV.JYYdLHmEevUVjFGkoO54hZ2xxG/B/sO', '测试老人', '13900000001', 'ELDER', 1, 1);

-- ============================================================
-- 测试药品数据（7种慢病用药）
-- ============================================================
INSERT INTO `medicine` (`approval_number`, `medicine_name`, `brand_name`, `specification`, `unit_per_box`, `manufacturer`, `reference_price`, `status`) VALUES
('国药准字H20040092', '缬沙坦胶囊', '库魏玛', '90mg×7粒/盒', 7, '诺华制药', 35.00, 1),
('国药准字J20170007', '阿司匹林肠溶片', '拜阿司匹林', '100mg×30片/盒', 30, '拜耳医药', 18.50, 1),
('国药准字H20080240', '瑞舒伐他汀钙片', '海舒颜', '10mg×28片/盒', 28, '鲁南贝特制药有限公司', 6.00, 1),
('国药准字H20123105', '硫酸氢氯吡格雷片', '率性', '75mg×7片/盒', 7, '乐普药业', 10.00, 1),
('国药准字H20093668', '泮托拉唑钠肠溶片', '舒可逸', '40mg×28片/盒', 28, '湖南九典制药有限公司', 22.00, 1),
('国药准字J20180006', '尼可地尔片', '喜格迈', '5mg×30片/盒', 30, '中外制药株式会社', 60.00, 1),
('国药准字H20050220', '单硝酸异山梨酯缓释片', '齐鲁', '40mg×24片/盒', 24, '齐鲁制药', 18.00, 1);

-- ============================================================
-- 测试用药方案（admin用户，userId=1）
-- ============================================================
INSERT INTO `prescription` (`user_id`, `medicine_id`, `daily_times`, `dosage_per_time`, `daily_consumption`, `days_per_box`, `take_notes`, `status`) VALUES
(1, 1, 1, 1, 1, 7, '每日一次，每次90mg口服', 1),
(1, 2, 1, 1, 1, 30, '每日一次，每次100mg口服，空腹', 1),
(1, 3, 1, 1, 1, 28, '每日一次，每次10mg口服，晚间服用', 1),
(1, 4, 1, 1, 1, 7, '每日一次，每次75mg口服', 1),
(1, 5, 1, 1, 1, 28, '每日一次，每次40mg口服，餐前服用', 1),
(1, 6, 3, 1, 3, 10, '每日三次，每次5mg口服', 1),
(1, 7, 1, 1, 1, 24, '每晨一次，每次40mg口服', 1);

-- ============================================================
-- 测试库存数据（关联用药方案）
-- ============================================================
INSERT INTO `stock` (`prescription_id`, `last_calc_time`, `total_remaining_units`, `remaining_days`, `expiry_date`) VALUES
(1, NOW(), 21, 21, '2027-04-23'),
(2, NOW(), 30, 30, '2027-04-23'),
(3, NOW(), 28, 28, '2027-04-23'),
(4, NOW(), 28, 28, '2027-04-23'),
(5, NOW(), 28, 28, '2027-04-23'),
(6, NOW(), 90, 30, '2027-04-23'),
(7, NOW(), 72, 72, '2027-04-23');

-- ============================================================
-- 测试购药记录
-- ============================================================
INSERT INTO `purchase_record` (`user_id`, `prescription_id`, `purchase_date`, `quantity_boxes`, `unit_price`, `total_price`, `expiry_date`, `operator_id`) VALUES
(1, 1, '2026-04-20', 3, 35.00, 105.00, '2027-04-23', 1),
(1, 2, '2026-04-20', 1, 18.50, 18.50, '2027-04-23', 1),
(1, 3, '2026-04-20', 1, 6.00, 6.00, '2027-04-23', 1),
(1, 4, '2026-04-20', 4, 10.00, 40.00, '2027-04-23', 1),
(1, 5, '2026-04-20', 1, 22.00, 22.00, '2027-04-23', 1),
(1, 6, '2026-04-20', 3, 60.00, 180.00, '2027-04-23', 1),
(1, 7, '2026-04-20', 3, 18.00, 54.00, '2027-04-23', 1);

-- ============================================================
-- 数据库操作日志触发器（防止直接篡改关键数据）
-- ============================================================

DELIMITER //

CREATE TRIGGER trg_stock_update AFTER UPDATE ON stock
FOR EACH ROW
BEGIN
    INSERT INTO sys_log (user_id, operation_type, operation_content, operation_time)
    VALUES (0, 'DB_STOCK_UPDATE',
        CONCAT('stock_id=', NEW.stock_id, ',remaining_units=', NEW.total_remaining_units, ',remaining_days=', NEW.remaining_days),
        NOW());
END //

CREATE TRIGGER trg_prescription_update AFTER UPDATE ON prescription
FOR EACH ROW
BEGIN
    INSERT INTO sys_log (user_id, operation_type, operation_content, operation_time)
    VALUES (0, 'DB_PRESCRIPTION_UPDATE',
        CONCAT('prescription_id=', NEW.prescription_id, ',daily_consumption=', NEW.daily_consumption),
        NOW());
END //

CREATE TRIGGER trg_purchase_insert AFTER INSERT ON purchase_record
FOR EACH ROW
BEGIN
    INSERT INTO sys_log (user_id, operation_type, operation_content, operation_time)
    VALUES (NEW.operator_id, 'DB_PURCHASE_INSERT',
        CONCAT('purchase_id=', NEW.purchase_id, ',boxes=', NEW.quantity_boxes, ',total=', NEW.total_price),
        NOW());
END //

CREATE TRIGGER trg_medicine_update AFTER UPDATE ON medicine
FOR EACH ROW
BEGIN
    INSERT INTO sys_log (user_id, operation_type, operation_content, operation_time)
    VALUES (0, 'DB_MEDICINE_UPDATE',
        CONCAT('medicine_id=', NEW.medicine_id, ',name=', NEW.medicine_name, ',status=', NEW.status),
        NOW());
END //

DELIMITER ;
