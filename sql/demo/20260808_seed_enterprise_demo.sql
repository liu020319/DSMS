USE medicine_system;

-- ============================================================
-- 康联云企业演示数据集 v1
-- 作用：为统计分析、药品画像、风险中心和凭证档案生成可浏览数据。
-- 安全：不包含真实手机号、邮箱或密码；自动关联数据库内现有家庭账号。
-- 幂等：同一版本只执行一次，再次执行会跳过，不会重复生成 1000 条记录。
-- ============================================================

CREATE TABLE IF NOT EXISTS dsms_demo_seed_history (
  seed_version VARCHAR(50) NOT NULL,
  record_count INT NOT NULL DEFAULT 0,
  executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (seed_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演示数据版本记录';

DROP PROCEDURE IF EXISTS dsms_seed_enterprise_demo;
DELIMITER $$
CREATE PROCEDURE dsms_seed_enterprise_demo()
main: BEGIN
  DECLARE v_seed_version VARCHAR(50) DEFAULT '20260808_ENTERPRISE_DEMO_V1';
  DECLARE v_exists INT DEFAULT 0;
  DECLARE v_admin_id BIGINT;
  DECLARE v_member_id BIGINT;
  DECLARE v_index INT DEFAULT 1;
  DECLARE v_prescription_count INT DEFAULT 0;
  DECLARE v_prescription_id BIGINT;
  DECLARE v_price DECIMAL(10,2);
  DECLARE v_quantity INT;
  DECLARE v_purchase_date DATE;
  DECLARE v_purchase_time DATETIME;
  DECLARE v_platform VARCHAR(100);
  DECLARE v_channel VARCHAR(20);

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_dsms_demo_prescription;
    RESIGNAL;
  END;

  -- 使用二进制比较，避免旧库 utf8mb4_general_ci 与 MySQL 8.4 默认
  -- utf8mb4_0900_ai_ci 在字段和存储过程变量比较时触发 1267。
  SELECT COUNT(*) INTO v_exists
  FROM dsms_demo_seed_history
  WHERE BINARY seed_version = BINARY v_seed_version;
  IF v_exists > 0 THEN
    SELECT '演示数据已存在，本次安全跳过；不会重复插入。' AS message,
           record_count, executed_at
    FROM dsms_demo_seed_history
    WHERE BINARY seed_version = BINARY v_seed_version;
    LEAVE main;
  END IF;

  SELECT COUNT(*) INTO v_exists
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_record'
    AND COLUMN_NAME IN ('purchase_time', 'purchase_platform', 'purchase_channel', 'proof_url', 'receipt_status');
  IF v_exists < 5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '数据库版本过旧：请先执行 20260807 两个业务迁移脚本';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_notification'
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '缺少 user_notification 表：请先执行家庭购药业务迁移脚本';
  END IF;

  -- 演示数据必须落在同一个真实家庭边界中，不能随意拼接两个用户。
  SELECT user_id, bind_parent_id INTO v_member_id, v_admin_id
  FROM sys_user
  WHERE role = 'ELDER' AND bind_parent_id IS NOT NULL AND status = 1 AND deleted = 0
  ORDER BY user_id LIMIT 1;

  IF v_admin_id IS NULL OR v_member_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '需要至少一个已绑定家庭守护人的 ELDER 账号后才能生成演示数据';
  END IF;

  START TRANSACTION;

  INSERT INTO medicine
    (approval_number, medicine_name, brand_name, specification, unit_per_box, manufacturer, reference_price, status, deleted)
  VALUES
    ('国药准字H90000001','苯磺酸氨氯地平片','络活示例','5毫克×28片',28,'康联示例制药一厂',23.80,1,0),
    ('国药准字H90000002','厄贝沙坦片','安博示例','150毫克×28片',28,'康联示例制药二厂',31.60,1,0),
    ('国药准字H90000003','盐酸二甲双胍缓释片','格华示例','500毫克×30片',30,'康联示例制药三厂',18.90,1,0),
    ('国药准字H90000004','阿托伐他汀钙片','立普示例','20毫克×28片',28,'康联示例制药四厂',42.50,1,0),
    ('国药准字H90000005','瑞舒伐他汀钙片','可定示例','10毫克×28片',28,'康联示例制药五厂',36.20,1,0),
    ('国药准字H90000006','阿司匹林肠溶片','拜阿示例','100毫克×30片',30,'康联示例制药六厂',19.80,1,0),
    ('国药准字H90000007','硫酸氢氯吡格雷片','波立示例','75毫克×28片',28,'康联示例制药七厂',68.00,1,0),
    ('国药准字H90000008','琥珀酸美托洛尔缓释片','倍他示例','47.5毫克×28片',28,'康联示例制药八厂',32.40,1,0),
    ('国药准字H90000009','缬沙坦胶囊','代文示例','80毫克×28粒',28,'康联示例制药九厂',39.90,1,0),
    ('国药准字H90000010','替米沙坦片','美卡示例','40毫克×28片',28,'康联示例制药十厂',28.60,1,0),
    ('国药准字H90000011','达格列净片','安达示例','10毫克×14片',14,'康联示例制药十一厂',61.50,1,0),
    ('国药准字H90000012','恩格列净片','欧唐示例','10毫克×30片',30,'康联示例制药十二厂',88.00,1,0),
    ('国药准字H90000013','非布司他片','优立示例','40毫克×16片',16,'康联示例制药十三厂',48.80,1,0),
    ('国药准字H90000014','甲钴胺片','弥可示例','0.5毫克×20片',20,'康联示例制药十四厂',24.30,1,0),
    ('国药准字H90000015','泮托拉唑钠肠溶片','潘妥示例','40毫克×28片',28,'康联示例制药十五厂',27.90,1,0),
    ('国药准字H90000016','奥美拉唑肠溶胶囊','洛赛示例','20毫克×28粒',28,'康联示例制药十六厂',22.60,1,0),
    ('国药准字H90000017','单硝酸异山梨酯缓释片','依姆示例','40毫克×24片',24,'康联示例制药十七厂',35.70,1,0),
    ('国药准字H90000018','尼可地尔片','喜格示例','5毫克×30片',30,'康联示例制药十八厂',73.80,1,0),
    ('国药准字H90000019','维生素D滴剂','悦而示例','400单位×30粒',30,'康联示例制药十九厂',29.50,1,0),
    ('国药准字H90000020','碳酸钙D3片','钙尔示例','600毫克×30片',30,'康联示例制药二十厂',45.00,1,0)
  ON DUPLICATE KEY UPDATE
    medicine_name = VALUES(medicine_name), brand_name = VALUES(brand_name),
    specification = VALUES(specification), unit_per_box = VALUES(unit_per_box),
    manufacturer = VALUES(manufacturer), reference_price = VALUES(reference_price),
    status = 1, deleted = 0;

  INSERT INTO prescription
    (user_id, medicine_id, daily_times, dosage_per_time, dosage_unit,
     daily_consumption, days_per_box, take_notes, take_timing,
     take_frequency_code, take_periods, status, deleted)
  SELECT v_member_id, m.medicine_id,
         CASE WHEN MOD(m.medicine_id, 4) = 0 THEN 3 ELSE 1 END,
         1,
         '片',
         CASE WHEN MOD(m.medicine_id, 4) = 0 THEN 3 ELSE 1 END,
         FLOOR(m.unit_per_box / CASE WHEN MOD(m.medicine_id, 4) = 0 THEN 3 ELSE 1 END),
         CONCAT('企业演示方案：', CASE MOD(m.medicine_id, 4)
           WHEN 0 THEN '早中晚各一次' WHEN 1 THEN '晨间一次'
           WHEN 2 THEN '午间一次' ELSE '晚间一次' END),
         CASE MOD(m.medicine_id, 4)
           WHEN 0 THEN '早中晚' WHEN 1 THEN '晨间'
           WHEN 2 THEN '午间' ELSE '晚间' END,
         CASE MOD(m.medicine_id, 4)
           WHEN 0 THEN 'DAILY_3_FULL_DAY' WHEN 1 THEN 'DAILY_1_MORNING'
           WHEN 2 THEN 'DAILY_1_NOON' ELSE 'DAILY_1_EVENING' END,
         CASE MOD(m.medicine_id, 4)
           WHEN 0 THEN '["MORNING","NOON","EVENING"]'
           WHEN 1 THEN '["MORNING"]'
           WHEN 2 THEN '["NOON"]'
           ELSE '["EVENING"]' END,
         1, 0
  FROM medicine m
  WHERE m.approval_number LIKE '国药准字H9000%'
    AND NOT EXISTS (
      SELECT 1 FROM prescription p
      WHERE p.user_id = v_member_id AND p.medicine_id = m.medicine_id AND p.deleted = 0
    );

  INSERT INTO stock
    (prescription_id, last_calc_time, total_remaining_units, remaining_days, expiry_date, deleted)
  SELECT p.prescription_id, NOW(),
         CASE WHEN MOD(p.medicine_id, 7) = 0 THEN 3 ELSE 30 + MOD(p.medicine_id, 60) END,
         CASE WHEN MOD(p.medicine_id, 7) = 0 THEN 3 WHEN MOD(p.medicine_id, 5) = 0 THEN 6 ELSE 24 + MOD(p.medicine_id, 20) END,
         CASE WHEN MOD(p.medicine_id, 6) = 0 THEN DATE_ADD(CURDATE(), INTERVAL 20 DAY) ELSE DATE_ADD(CURDATE(), INTERVAL 420 DAY) END,
         0
  FROM prescription p
  JOIN medicine m ON p.medicine_id = m.medicine_id
  WHERE p.user_id = v_member_id AND p.deleted = 0 AND m.approval_number LIKE '国药准字H9000%'
    AND NOT EXISTS (SELECT 1 FROM stock s WHERE s.prescription_id = p.prescription_id AND s.deleted = 0);

  DROP TEMPORARY TABLE IF EXISTS tmp_dsms_demo_prescription;
  CREATE TEMPORARY TABLE tmp_dsms_demo_prescription (
    seq_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    reference_price DECIMAL(10,2) NOT NULL
  );
  INSERT INTO tmp_dsms_demo_prescription (prescription_id, reference_price)
  SELECT p.prescription_id, m.reference_price
  FROM prescription p JOIN medicine m ON p.medicine_id = m.medicine_id
  WHERE p.user_id = v_member_id AND p.deleted = 0 AND m.approval_number LIKE '国药准字H9000%'
  ORDER BY p.prescription_id;
  SELECT COUNT(*) INTO v_prescription_count FROM tmp_dsms_demo_prescription;

  WHILE v_index <= 1000 DO
    SELECT prescription_id, reference_price INTO v_prescription_id, v_price
    FROM tmp_dsms_demo_prescription
    WHERE seq_id = MOD(v_index - 1, v_prescription_count) + 1;

    SET v_quantity = MOD(v_index * 7, 5) + 1;
    SET v_purchase_date = DATE_SUB(CURDATE(), INTERVAL MOD(v_index * 17, 1095) DAY);
    SET v_purchase_time = TIMESTAMP(v_purchase_date, MAKETIME(7 + MOD(v_index * 5, 15), MOD(v_index * 11, 60), 0));
    SET v_price = ROUND(v_price * (0.86 + MOD(v_index, 9) * 0.025), 2);
    SET v_channel = IF(MOD(v_index, 10) IN (8, 9), 'OFFLINE', 'ONLINE');
    SET v_platform = CASE MOD(v_index, 10)
      WHEN 0 THEN '京东健康' WHEN 1 THEN '阿里健康' WHEN 2 THEN '美团买药'
      WHEN 3 THEN '叮当快药' WHEN 4 THEN '饿了么买药' WHEN 5 THEN '京东健康'
      WHEN 6 THEN '益丰大药房' WHEN 7 THEN '高济健康' WHEN 8 THEN '医院药房'
      ELSE '社区药店' END;

    INSERT INTO purchase_record
      (user_id, prescription_id, purchase_date, purchase_time, quantity_boxes, unit_price,
       total_price, expiry_date, operator_id, purchase_platform, purchase_channel,
       proof_url, receipt_status, deleted)
    VALUES
      (v_member_id, v_prescription_id, v_purchase_date, v_purchase_time, v_quantity, v_price,
       ROUND(v_price * v_quantity, 2), DATE_ADD(v_purchase_date, INTERVAL 540 + MOD(v_index, 365) DAY),
       v_admin_id, v_platform, v_channel,
       IF(MOD(v_index, 5) = 0, NULL, CONCAT('demo://voucher/', v_seed_version, '/', v_index)),
       IF(MOD(v_index, 13) = 0, 0, 1), 0);
    SET v_index = v_index + 1;
  END WHILE;

  SET v_index = 1;
  WHILE v_index <= 120 DO
    INSERT INTO user_notification
      (recipient_id, title, content, biz_type, biz_id, read_status, email_status, create_time, deleted)
    VALUES
      (IF(MOD(v_index, 2) = 0, v_admin_id, v_member_id),
       CASE MOD(v_index, 4)
         WHEN 0 THEN '库存风险提醒' WHEN 1 THEN '购药记录已归档'
         WHEN 2 THEN '家庭协同进度更新' ELSE '费用凭证检查提醒' END,
       CONCAT('企业演示消息 #', v_index, '：用于体验消息筛选、业务跳转与已读状态。'),
       CASE MOD(v_index, 4)
         WHEN 0 THEN 'LOW_STOCK' WHEN 1 THEN 'PURCHASE'
         WHEN 2 THEN 'FAMILY_ORDER' ELSE 'EVIDENCE' END,
       v_index, IF(v_index > 12, 1, 0), 'DISABLED',
       DATE_SUB(NOW(), INTERVAL v_index * 6 HOUR), 0);
    SET v_index = v_index + 1;
  END WHILE;

  INSERT INTO dsms_demo_seed_history (seed_version, record_count, note)
  VALUES (v_seed_version, 1180, '20药品+20方案+20库存+1000购药记录+120消息');

  DROP TEMPORARY TABLE IF EXISTS tmp_dsms_demo_prescription;
  COMMIT;
  SELECT '企业演示数据生成成功' AS message,
         1000 AS purchase_record_count,
         20 AS medicine_count,
         120 AS notification_count,
         v_member_id AS linked_member_id;
END$$
DELIMITER ;

CALL dsms_seed_enterprise_demo();
DROP PROCEDURE IF EXISTS dsms_seed_enterprise_demo;

-- 即使 V1 以前执行过，也补齐晨间、午间、晚间三个时段，重复执行不会重复造数据。
UPDATE prescription p
JOIN medicine m ON m.medicine_id = p.medicine_id
SET p.daily_times = CASE WHEN MOD(m.medicine_id, 4) = 0 THEN 3 ELSE 1 END,
    p.daily_consumption = CASE WHEN MOD(m.medicine_id, 4) = 0 THEN 3 ELSE 1 END,
    p.days_per_box = FLOOR(m.unit_per_box / CASE WHEN MOD(m.medicine_id, 4) = 0 THEN 3 ELSE 1 END),
    p.dosage_unit = '片',
    p.take_timing = CASE MOD(m.medicine_id, 4)
      WHEN 0 THEN '早中晚' WHEN 1 THEN '晨间' WHEN 2 THEN '午间' ELSE '晚间' END,
    p.take_frequency_code = CASE MOD(m.medicine_id, 4)
      WHEN 0 THEN 'DAILY_3_FULL_DAY' WHEN 1 THEN 'DAILY_1_MORNING'
      WHEN 2 THEN 'DAILY_1_NOON' ELSE 'DAILY_1_EVENING' END,
    p.take_periods = CASE MOD(m.medicine_id, 4)
      WHEN 0 THEN '["MORNING","NOON","EVENING"]'
      WHEN 1 THEN '["MORNING"]'
      WHEN 2 THEN '["NOON"]'
      ELSE '["EVENING"]' END
WHERE m.approval_number LIKE '国药准字H9000%'
  AND p.deleted = 0;

SELECT COUNT(*) AS all_purchase_records FROM purchase_record WHERE deleted = 0;
SELECT COUNT(*) AS all_demo_medicines FROM medicine WHERE deleted = 0 AND approval_number LIKE '国药准字H9000%';
