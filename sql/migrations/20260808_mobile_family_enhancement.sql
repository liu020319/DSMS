USE medicine_system;

-- R3：预计到货时间。脚本可重复执行，适配 MySQL 8.4。
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
  'family_purchase_order', 'expected_arrival_time',
  'DATETIME DEFAULT NULL COMMENT ''家属填写的预计到货时间'' AFTER order_time'
);

DROP PROCEDURE IF EXISTS dsms_add_column_if_missing;
