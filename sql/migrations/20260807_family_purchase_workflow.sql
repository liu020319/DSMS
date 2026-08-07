USE medicine_system;

ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS email VARCHAR(160) DEFAULT NULL COMMENT '通知邮箱' AFTER phone;

ALTER TABLE purchase_record
  ADD COLUMN IF NOT EXISTS purchase_time DATETIME DEFAULT NULL COMMENT '实际下单时间' AFTER purchase_date,
  ADD COLUMN IF NOT EXISTS purchase_channel VARCHAR(20) DEFAULT NULL COMMENT 'ONLINE/OFFLINE' AFTER purchase_platform,
  ADD COLUMN IF NOT EXISTS order_id BIGINT DEFAULT NULL COMMENT '关联代购订单' AFTER purchase_channel,
  ADD COLUMN IF NOT EXISTS proof_url VARCHAR(500) DEFAULT NULL COMMENT '订单截图或票据' AFTER order_id;

UPDATE purchase_record
SET purchase_time = CONCAT(purchase_date, ' 12:00:00')
WHERE purchase_time IS NULL;

CREATE TABLE IF NOT EXISTS family_purchase_order (
  order_id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL COMMENT '关联老人提交的申请',
  elder_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL,
  item_json TEXT NOT NULL COMMENT '实际购买药品和价格快照',
  purchase_platform VARCHAR(100) NOT NULL,
  purchase_channel VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
  order_time DATETIME NOT NULL,
  actual_total DECIMAL(12,2) NOT NULL DEFAULT 0,
  screenshot_url VARCHAR(500) DEFAULT NULL,
  carrier_code VARCHAR(30) DEFAULT NULL,
  carrier_name VARCHAR(60) DEFAULT NULL,
  tracking_no VARCHAR(80) DEFAULT NULL,
  logistics_status VARCHAR(30) NOT NULL DEFAULT 'ORDERED',
  note VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (order_id),
  UNIQUE KEY uk_task_id (task_id),
  KEY idx_order_elder (elder_id),
  KEY idx_order_parent (parent_id),
  KEY idx_tracking_no (tracking_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭远程代购订单';

CREATE TABLE IF NOT EXISTS logistics_event (
  event_id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  status_code VARCHAR(30) NOT NULL,
  status_text VARCHAR(100) NOT NULL,
  detail VARCHAR(500) DEFAULT NULL,
  occurred_time DATETIME NOT NULL,
  source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (event_id),
  KEY idx_logistics_order (order_id, occurred_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹';

CREATE TABLE IF NOT EXISTS family_fund_transaction (
  transaction_id BIGINT NOT NULL AUTO_INCREMENT,
  elder_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL,
  transaction_type VARCHAR(20) NOT NULL COMMENT 'TRANSFER/PURCHASE/ADJUST',
  amount DECIMAL(12,2) NOT NULL COMMENT '转入为正，购药为负',
  payment_platform VARCHAR(30) DEFAULT NULL COMMENT '微信/支付宝/银联/现金/其他',
  transaction_time DATETIME NOT NULL,
  reference_order_id BIGINT DEFAULT NULL,
  proof_url VARCHAR(500) DEFAULT NULL,
  note VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (transaction_id),
  UNIQUE KEY uk_fund_order (reference_order_id, transaction_type),
  KEY idx_fund_elder (elder_id, transaction_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人购药资金台账';

CREATE TABLE IF NOT EXISTS user_notification (
  notification_id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  biz_type VARCHAR(30) DEFAULT NULL,
  biz_id BIGINT DEFAULT NULL,
  read_status TINYINT NOT NULL DEFAULT 0,
  email_status VARCHAR(20) NOT NULL DEFAULT 'DISABLED',
  email_error VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_time DATETIME DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (notification_id),
  KEY idx_notify_recipient (recipient_id, read_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内与邮件通知';
