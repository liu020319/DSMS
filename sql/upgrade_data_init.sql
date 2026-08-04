UPDATE prescription SET take_frequency_code='DAILY_1_MORNING', take_periods='["MORNING"]' WHERE daily_times=1 AND (take_timing IN ('每晨','每晨空腹','空腹','餐前','餐后','餐中') OR take_timing IS NULL);
UPDATE prescription SET take_frequency_code='DAILY_1_EVENING', take_periods='["EVENING"]' WHERE daily_times=1 AND take_timing IN ('晚间','睡前');
UPDATE prescription SET take_frequency_code='DAILY_2_MORNING_EVENING', take_periods='["MORNING","EVENING"]' WHERE daily_times=2;
UPDATE prescription SET take_frequency_code='DAILY_3_FULL_DAY', take_periods='["MORNING","NOON","EVENING"]' WHERE daily_times=3;
UPDATE stock SET today_deducted_periods='[]', last_deduction_date=CURDATE();

CREATE TABLE IF NOT EXISTS sys_config (
  config_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  config_key VARCHAR(50) NOT NULL COMMENT '配置键',
  config_value VARCHAR(100) NOT NULL COMMENT '配置值',
  config_desc VARCHAR(200) DEFAULT NULL COMMENT '配置描述',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT INTO sys_config (config_key, config_value, config_desc) VALUES ('morning_threshold', '09:00', '晨服扣减阈值时间') ON DUPLICATE KEY UPDATE config_value=config_value;
INSERT INTO sys_config (config_key, config_value, config_desc) VALUES ('noon_threshold', '13:00', '午服扣减阈值时间') ON DUPLICATE KEY UPDATE config_value=config_value;
INSERT INTO sys_config (config_key, config_value, config_desc) VALUES ('evening_threshold', '21:00', '晚服扣减阈值时间') ON DUPLICATE KEY UPDATE config_value=config_value;
