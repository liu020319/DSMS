-- ============================================================
-- 分时段精准扣减库存 - 数据库增量脚本
-- 仅新增字段和表，不修改/删除原有字段
-- ============================================================

USE medicine_system;

-- 1.prescription用药方案表新增字段
ALTER TABLE `prescription`
  ADD COLUMN `take_frequency_code` VARCHAR(50) NOT NULL DEFAULT 'DAILY_1_MORNING' COMMENT '服用频次枚举值' AFTER `take_notes`,
  ADD COLUMN `take_periods` JSON NOT NULL COMMENT '服用时段数组' AFTER `take_frequency_code`;

-- 2.stock库存表新增字段
ALTER TABLE `stock`
  ADD COLUMN `today_deducted_periods` JSON NOT NULL COMMENT '当日已扣减时段' AFTER `remaining_days`,
  ADD COLUMN `last_deduction_date` DATE NOT NULL COMMENT '上次扣减日期' AFTER `today_deducted_periods`,
  ADD COLUMN `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER `last_deduction_date`;

-- 3.初始化已有prescription数据的take_frequency_code和take_periods
UPDATE `prescription` SET
  `take_frequency_code` = 'DAILY_1_MORNING',
  `take_periods` = '["MORNING"]'
WHERE `daily_times` = 1 AND (`take_timing` IN ('每晨','每晨空腹','空腹','餐前','餐后','餐中') OR `take_timing` IS NULL);

UPDATE `prescription` SET
  `take_frequency_code` = 'DAILY_1_EVENING',
  `take_periods` = '["EVENING"]'
WHERE `daily_times` = 1 AND (`take_timing` IN ('晚间','睡前'));

UPDATE `prescription` SET
  `take_frequency_code` = 'DAILY_2_MORNING_EVENING',
  `take_periods` = '["MORNING","EVENING"]'
WHERE `daily_times` = 2;

UPDATE `prescription` SET
  `take_frequency_code` = 'DAILY_3_FULL_DAY',
  `take_periods` = '["MORNING","NOON","EVENING"]'
WHERE `daily_times` = 3;

-- 4.初始化已有stock数据的today_deducted_periods和last_deduction_date
UPDATE `stock` SET
  `today_deducted_periods` = '[]',
  `last_deduction_date` = CURDATE();

-- 5.新增sys_config系统配置表
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `config_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(100) NOT NULL COMMENT '配置值',
  `config_desc` VARCHAR(200) DEFAULT NULL COMMENT '配置描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`config_id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 6.初始化时段阈值配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_desc`) VALUES
('morning_threshold', '09:00', '晨服扣减阈值时间'),
('noon_threshold', '13:00', '午服扣减阈值时间'),
('evening_threshold', '21:00', '晚服扣减阈值时间');
