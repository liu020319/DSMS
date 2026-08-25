USE medicine_system;

DROP PROCEDURE IF EXISTS dsms_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE dsms_add_column_if_missing(
  IN p_table_name VARCHAR(64),
  IN p_column_name VARCHAR(64),
  IN p_column_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table_name
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @dsms_ddl = CONCAT(
      'ALTER TABLE `', REPLACE(p_table_name, '`', '``'),
      '` ADD COLUMN `', REPLACE(p_column_name, '`', '``'), '` ',
      p_column_definition
    );
    PREPARE dsms_stmt FROM @dsms_ddl;
    EXECUTE dsms_stmt;
    DEALLOCATE PREPARE dsms_stmt;
  END IF;
END$$
DELIMITER ;

CALL dsms_add_column_if_missing(
  'purchase_evidence', 'purchase_id',
  'BIGINT DEFAULT NULL COMMENT ''关联直接登记的购药记录'' AFTER order_id'
);
CALL dsms_add_column_if_missing(
  'family_fund_transaction', 'reference_purchase_id',
  'BIGINT DEFAULT NULL COMMENT ''关联直接登记的购药记录'' AFTER reference_order_id'
);

ALTER TABLE purchase_evidence MODIFY COLUMN order_id BIGINT NULL;

SET @has_evidence_purchase_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'purchase_evidence'
    AND INDEX_NAME = 'idx_evidence_purchase_time'
);
SET @sql = IF(@has_evidence_purchase_index = 0,
  'ALTER TABLE purchase_evidence ADD KEY idx_evidence_purchase_time (purchase_id, occurred_time, evidence_id)',
  'SELECT 1');
PREPARE dsms_stmt FROM @sql;
EXECUTE dsms_stmt;
DEALLOCATE PREPARE dsms_stmt;

SET @has_fund_purchase_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'family_fund_transaction'
    AND INDEX_NAME = 'idx_fund_purchase'
);
SET @sql = IF(@has_fund_purchase_index = 0,
  'ALTER TABLE family_fund_transaction ADD KEY idx_fund_purchase (reference_purchase_id, transaction_type)',
  'SELECT 1');
PREPARE dsms_stmt FROM @sql;
EXECUTE dsms_stmt;
DEALLOCATE PREPARE dsms_stmt;

DROP PROCEDURE IF EXISTS dsms_add_column_if_missing;
