USE medicine_system;

-- 文件本体在本地磁盘或 OBS；本表是权限、业务归属、校验和生命周期台账。
CREATE TABLE IF NOT EXISTS file_asset (
  file_id BIGINT NOT NULL AUTO_INCREMENT,
  storage_provider VARCHAR(20) NOT NULL COMMENT 'LOCAL/OBS',
  bucket_name VARCHAR(128) DEFAULT NULL COMMENT 'OBS桶；本地存储为空',
  object_key VARCHAR(500) NOT NULL COMMENT '随机对象键，不包含姓名手机号等隐私',
  original_name VARCHAR(255) NOT NULL COMMENT '原始文件名，仅用于界面展示',
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  sha256 CHAR(64) NOT NULL COMMENT '内容摘要，用于完整性校验和去重',
  file_category VARCHAR(50) NOT NULL COMMENT 'MEDICINE_IMAGE/ORDER_SCREENSHOT等',
  access_scope VARCHAR(20) NOT NULL DEFAULT 'FAMILY' COMMENT 'AUTHENTICATED/FAMILY/OWNER',
  business_type VARCHAR(50) DEFAULT NULL,
  business_id BIGINT DEFAULT NULL,
  owner_user_id BIGINT NOT NULL,
  family_id BIGINT DEFAULT NULL COMMENT '家庭守护人账号ID；平台公共药品图片为空',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DELETED/QUARANTINED',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  delete_time DATETIME DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (file_id),
  UNIQUE KEY uk_file_object (storage_provider, object_key),
  KEY idx_file_sha256 (sha256),
  KEY idx_file_family_category (family_id, file_category, create_time),
  KEY idx_file_business (business_type, business_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一文件资产与权限台账';

-- 一笔订单可以有问诊截图、付款截图、发票等多份凭证，不能继续只放一个 proof_url。
CREATE TABLE IF NOT EXISTS purchase_evidence (
  evidence_id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT DEFAULT NULL,
  purchase_id BIGINT DEFAULT NULL COMMENT '关联直接登记的购药记录',
  elder_id BIGINT NOT NULL,
  parent_id BIGINT NOT NULL,
  evidence_type VARCHAR(30) NOT NULL COMMENT 'CONSULTATION/PAYMENT/INVOICE/ORDER_SCREENSHOT',
  file_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  occurred_time DATETIME NOT NULL COMMENT '凭证对应的业务发生时间',
  amount DECIMAL(12,2) DEFAULT NULL,
  purchase_platform VARCHAR(100) DEFAULT NULL,
  note VARCHAR(500) DEFAULT NULL,
  created_by BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (evidence_id),
  KEY idx_evidence_order_time (order_id, occurred_time, evidence_id),
  KEY idx_evidence_purchase_time (purchase_id, occurred_time, evidence_id),
  KEY idx_evidence_elder_time (elder_id, occurred_time, evidence_id),
  KEY idx_evidence_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购药凭证时间线';
