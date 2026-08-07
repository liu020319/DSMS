USE medicine_system;

-- 登录安全：连续输错5次锁定15分钟；管理员可随时解锁或重置密码。
ALTER TABLE sys_user
  ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数' AFTER status,
  ADD COLUMN IF NOT EXISTS locked_until DATETIME DEFAULT NULL COMMENT '临时锁定截止时间' AFTER failed_login_attempts,
  ADD COLUMN IF NOT EXISTS last_login_time DATETIME DEFAULT NULL COMMENT '最近成功登录时间' AFTER locked_until;

-- 收货状态与物流状态分开，避免“快递显示已送达”被误认为“药品已核验”。
ALTER TABLE family_purchase_order
  ADD COLUMN IF NOT EXISTS receipt_status VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/VERIFIED/EXCEPTION' AFTER logistics_status,
  ADD COLUMN IF NOT EXISTS received_time DATETIME DEFAULT NULL COMMENT '核验通过时间' AFTER receipt_status;

UPDATE family_purchase_order
SET receipt_status = 'VERIFIED', received_time = COALESCE(received_time, update_time)
WHERE logistics_status = 'DELIVERED' AND receipt_status = 'PENDING';

CREATE TABLE IF NOT EXISTS order_receipt_verification (
  verification_id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL COMMENT '安心用药端用户编号',
  check_result VARCHAR(30) NOT NULL COMMENT 'VERIFIED/EXCEPTION',
  photo_url VARCHAR(500) NOT NULL COMMENT '收货药品合照',
  check_json TEXT NOT NULL COMMENT '每种药品的期望值、实收值和比对结果',
  note VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (verification_id),
  KEY idx_receipt_order (order_id, create_time),
  KEY idx_receipt_elder (elder_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单收货逐项核验记录';
