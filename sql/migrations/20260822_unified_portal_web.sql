USE medicine_system;

-- 个人记账与 DSMS 家庭购药资金台账严格分表：前者归属个人账号，后者归属家庭业务。
CREATE TABLE IF NOT EXISTS personal_ledger (
  ledger_id BIGINT NOT NULL AUTO_INCREMENT,
  owner_user_id BIGINT NOT NULL,
  ledger_name VARCHAR(80) NOT NULL,
  currency_code CHAR(3) NOT NULL DEFAULT 'CNY',
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (ledger_id),
  KEY idx_ledger_owner_status (owner_user_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人记账账本';

CREATE TABLE IF NOT EXISTS personal_account (
  account_id BIGINT NOT NULL AUTO_INCREMENT,
  ledger_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  account_name VARCHAR(80) NOT NULL,
  account_type VARCHAR(30) NOT NULL COMMENT 'CASH/BANK/WECHAT/ALIPAY/OTHER',
  initial_balance DECIMAL(14,2) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (account_id),
  KEY idx_account_owner_ledger (owner_user_id, ledger_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人记账账户';

CREATE TABLE IF NOT EXISTS personal_transaction (
  transaction_id BIGINT NOT NULL AUTO_INCREMENT,
  ledger_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  transaction_type VARCHAR(20) NOT NULL COMMENT 'INCOME/EXPENSE',
  category_name VARCHAR(50) NOT NULL,
  amount DECIMAL(14,2) NOT NULL,
  transaction_time DATETIME NOT NULL,
  counterparty VARCHAR(100) DEFAULT NULL,
  note VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (transaction_id),
  KEY idx_transaction_owner_time (owner_user_id, transaction_time, transaction_id),
  KEY idx_transaction_ledger_time (ledger_id, transaction_time),
  KEY idx_transaction_account_time (account_id, transaction_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人收支流水';

CREATE TABLE IF NOT EXISTS personal_budget (
  budget_id BIGINT NOT NULL AUTO_INCREMENT,
  ledger_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  budget_month CHAR(7) NOT NULL COMMENT 'yyyy-MM',
  category_name VARCHAR(50) NOT NULL,
  budget_amount DECIMAL(14,2) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (budget_id),
  UNIQUE KEY uk_budget_owner_month_category (owner_user_id, ledger_id, budget_month, category_name),
  KEY idx_budget_owner_month (owner_user_id, budget_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人月度分类预算';

-- 软件工程服务中心只描述合规的开发、辅导、调试、部署与咨询服务。
CREATE TABLE IF NOT EXISTS software_service_request (
  request_id BIGINT NOT NULL AUTO_INCREMENT,
  requester_user_id BIGINT NOT NULL,
  service_type VARCHAR(40) NOT NULL COMMENT 'DEVELOPMENT/GUIDANCE/DEBUG/DEPLOYMENT/CONSULTING',
  title VARCHAR(120) NOT NULL,
  requirement_text TEXT NOT NULL,
  technology_stack VARCHAR(300) DEFAULT NULL,
  budget_range VARCHAR(80) DEFAULT NULL,
  expected_date DATE DEFAULT NULL,
  contact_channel VARCHAR(120) DEFAULT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/ASSESSING/QUOTED/IN_PROGRESS/DELIVERED/CLOSED/CANCELLED',
  quote_amount DECIMAL(14,2) DEFAULT NULL,
  manager_note VARCHAR(1000) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (request_id),
  KEY idx_service_requester_time (requester_user_id, create_time, request_id),
  KEY idx_service_status_time (status, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='软件工程服务需求';

CREATE TABLE IF NOT EXISTS software_service_milestone (
  milestone_id BIGINT NOT NULL AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  milestone_name VARCHAR(120) NOT NULL,
  milestone_description VARCHAR(800) DEFAULT NULL,
  planned_date DATE DEFAULT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/BLOCKED',
  sort_no INT NOT NULL DEFAULT 0,
  completed_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (milestone_id),
  KEY idx_milestone_request_sort (request_id, sort_no, milestone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='软件服务里程碑';

CREATE TABLE IF NOT EXISTS software_service_work_order (
  work_order_id BIGINT NOT NULL AUTO_INCREMENT,
  request_id BIGINT NOT NULL,
  requester_user_id BIGINT NOT NULL,
  work_order_type VARCHAR(40) NOT NULL COMMENT 'QUESTION/BUG/DEPLOYMENT/AFTER_SALES',
  subject VARCHAR(120) NOT NULL,
  description TEXT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/PROCESSING/RESOLVED/CLOSED',
  handler_user_id BIGINT DEFAULT NULL,
  resolution_text VARCHAR(1000) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  resolved_time DATETIME DEFAULT NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (work_order_id),
  KEY idx_work_order_requester_time (requester_user_id, create_time, work_order_id),
  KEY idx_work_order_request_status (request_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='软件服务沟通与售后工单';

-- 游客无需注册即可提交咨询；详细联系方式只保存在后端，不在公开接口回显。
CREATE TABLE IF NOT EXISTS public_service_inquiry (
  inquiry_id BIGINT NOT NULL AUTO_INCREMENT,
  inquiry_no VARCHAR(32) NOT NULL,
  contact_name VARCHAR(80) NOT NULL,
  contact_value VARCHAR(160) NOT NULL,
  service_type VARCHAR(40) NOT NULL COMMENT 'GUIDANCE/DEVELOPMENT/DEBUG/DEPLOYMENT/CONSULTING',
  project_type VARCHAR(80) DEFAULT NULL,
  inquiry_text TEXT NOT NULL,
  source_path VARCHAR(120) NOT NULL DEFAULT '/cloud-hub/',
  status VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/CONTACTED/CLOSED',
  public_access_hash CHAR(64) NOT NULL COMMENT '游客访问码的SHA-256摘要，不保存访问码原文',
  client_fingerprint CHAR(64) DEFAULT NULL COMMENT '客户端地址不可逆摘要，仅用于风控审计',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (inquiry_id),
  UNIQUE KEY uk_public_inquiry_no (inquiry_no),
  KEY idx_public_inquiry_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客软件服务咨询';

CREATE TABLE IF NOT EXISTS public_service_message (
  message_id BIGINT NOT NULL AUTO_INCREMENT,
  inquiry_id BIGINT NOT NULL,
  sender_type VARCHAR(20) NOT NULL COMMENT 'VISITOR/ADMIN',
  sender_user_id BIGINT DEFAULT NULL,
  message_text VARCHAR(3000) NOT NULL,
  visible_to_visitor TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (message_id),
  KEY idx_public_message_inquiry_time (inquiry_id, create_time, message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游客咨询沟通记录';
