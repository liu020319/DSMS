USE medicine_system;

INSERT IGNORE INTO `medicine` (`approval_number`, `medicine_name`, `brand_name`, `specification`, `unit_per_box`, `manufacturer`, `reference_price`, `status`) VALUES
('国药准字H20057320', '氨氯地平片', '络活喜', '5mg*28片/盒', 28, '辉瑞制药', 38.00, 1),
('国药准字H20066451', '厄贝沙坦片', '安博维', '150mg*7片/盒', 7, '赛诺菲', 28.00, 1),
('国药准字H20057639', '二甲双胍缓释片', '格华止', '500mg*30片/盒', 30, '中美上海施贵宝', 25.00, 1),
('国药准字H20041394', '格列美脲片', '亚莫利', '2mg*30片/盒', 30, '赛诺菲', 42.00, 1),
('国药准字H20010568', '阿卡波糖片', '拜唐苹', '50mg*30片/盒', 30, '拜耳医药', 65.00, 1),
('国药准字H20065203', '恩替卡韦分散片', '润众', '0.5mg*7片/盒', 7, '正大天晴', 88.00, 1),
('国药准字H20059936', '非布司他片', '风定宁', '40mg*16片/盒', 16, '万特制药', 55.00, 1),
('国药准字H20074056', '碳酸钙D3片', '钙尔奇', '600mg*60片/盒', 60, '惠氏制药', 35.00, 1),
('国药准字H20056976', '骨化三醇软胶囊', '罗盖全', '0.25ug*10粒/盒', 10, '罗氏制药', 72.00, 1),
('国药准字H20060390', '氯沙坦钾片', '科素亚', '50mg*7片/盒', 7, '默沙东', 32.00, 1),
('国药准字H20090143', '替米沙坦片', '美卡素', '80mg*7片/盒', 7, '勃林格殷格翰', 36.00, 1),
('国药准字H20073964', '比索洛尔片', '康忻', '5mg*28片/盒', 28, '默克制药', 29.00, 1),
('国药准字H20058990', '曲美他嗪缓释片', '万爽力', '35mg*30片/盒', 30, '施维雅', 48.00, 1),
('国药准字H20065155', '螺内酯片', '安体舒通', '20mg*100片/盒', 100, '江苏正大丰海', 8.00, 1),
('国药准字H20055057', '呋塞米片', '速尿', '20mg*100片/盒', 100, '上海朝晖药业', 5.00, 1),
('国药准字H20056597', '地高辛片', '地高辛', '0.25mg*30片/盒', 30, '上海新黄河制药', 12.00, 1),
('国药准字H20058968', '华法林钠片', '华法林', '2.5mg*60片/盒', 60, '上海福达制药', 18.00, 1),
('国药准字H20065655', '辛伐他汀片', '舒降之', '20mg*7片/盒', 7, '默沙东', 22.00, 1),
('国药准字H20055398', '非诺贝特胶囊', '力平之', '200mg*10粒/盒', 10, '利博福尼制药', 35.00, 1),
('国药准字H20057591', '依折麦布片', '益适纯', '10mg*5片/盒', 5, '默沙东', 48.00, 1),
('国药准字H20061269', '美托洛尔缓释片', '倍他乐克', '47.5mg*7片/盒', 7, '阿斯利康', 19.00, 1),
('国药准字H20054904', '硝苯地平控释片', '拜新同', '30mg*7片/盒', 7, '拜耳医药', 33.00, 1),
('国药准字H20058907', '培哚普利片', '雅施达', '4mg*28片/盒', 28, '施维雅', 31.00, 1),
('国药准字H20060393', '左氨氯地平片', '施慧达', '2.5mg*14片/盒', 14, '施慧达药业', 26.00, 1),
('国药准字H20055160', '阿托伐他汀钙片', '立普妥', '20mg*7片/盒', 7, '辉瑞制药', 45.00, 1),
('国药准字H20065657', '达格列净片', '安达唐', '10mg*30片/盒', 30, '阿斯利康', 98.00, 1),
('国药准字H20058900', '利拉鲁肽注射液', '诺和力', '18mg*3ml/支', 1, '诺和诺德', 380.00, 1),
('国药准字H20057641', '甘精胰岛素注射液', '来得时', '300IU/支', 1, '赛诺菲', 185.00, 1),
('国药准字H20055399', '门冬胰岛素注射液', '诺和锐', '300IU/支', 1, '诺和诺德', 92.00, 1),
('国药准字H20061270', '西格列汀片', '捷诺维', '100mg*7片/盒', 7, '默沙东', 68.00, 1);

INSERT INTO `prescription` (`user_id`, `medicine_id`, `daily_times`, `dosage_per_time`, `dosage_unit`, `daily_consumption`, `days_per_box`, `take_notes`, `take_timing`, `status`) VALUES
(1, 1, 1, 1, '粒', 1, 7, '每次90mg口服', '每晨', 1),
(1, 2, 1, 1, '片', 1, 30, '每次100mg口服', '空腹', 1),
(1, 3, 1, 1, '片', 1, 28, '每次10mg口服', '晚间', 1),
(1, 4, 1, 1, '片', 1, 7, '每次75mg口服', '每晨', 1),
(1, 5, 1, 1, '片', 1, 28, '每次40mg口服', '餐前', 1),
(1, 6, 3, 1, '片', 3, 10, '每次5mg口服', '餐后', 1),
(1, 7, 1, 1, '片', 1, 24, '每次40mg口服', '每晨', 1),
(1, 8, 1, 1, '片', 1, 30, '每次10mg口服', '空腹', 1),
(1, 9, 2, 1, '支', 2, 3, '每次10ml口服', '餐后', 1),
(1, 19, 1, 1, '片', 1, 7, '每次50mg口服', '每晨', 1),
(1, 21, 1, 1, '片', 1, 28, '每次5mg口服', '每晨', 1),
(1, 31, 1, 1, '片', 1, 7, '每次30mg口服', '每晨', 1),
(3, 10, 1, 1, '片', 1, 28, '每次5mg口服', '每晨', 1),
(3, 11, 1, 1, '片', 1, 7, '每次150mg口服', '每晨', 1),
(3, 12, 1, 1, '片', 1, 30, '每次500mg口服', '餐中', 1),
(3, 2, 1, 1, '片', 1, 30, '每次100mg口服', '空腹', 1),
(3, 30, 1, 1, '片', 1, 7, '每次47.5mg口服', '每晨', 1),
(4, 1, 1, 1, '粒', 1, 7, '每次90mg口服', '每晨', 1),
(4, 17, 1, 1, '片', 1, 60, '每次1片口服', '餐后', 1),
(4, 18, 1, 1, '粒', 1, 10, '每次0.25ug口服', '每晨', 1),
(4, 3, 1, 1, '片', 1, 28, '每次10mg口服', '晚间', 1),
(4, 22, 1, 1, '片', 1, 30, '每次35mg口服', '餐后', 1),
(5, 4, 1, 1, '片', 1, 7, '每次75mg口服', '每晨', 1),
(5, 16, 1, 1, '片', 1, 16, '每次40mg口服', '每晨', 1),
(5, 7, 1, 1, '片', 1, 24, '每次40mg口服', '每晨', 1),
(5, 6, 3, 1, '片', 3, 10, '每次5mg口服', '餐后', 1),
(5, 27, 1, 1, '片', 1, 7, '每次20mg口服', '睡前', 1),
(6, 12, 2, 1, '片', 2, 15, '每次500mg口服', '餐中', 1),
(6, 13, 1, 1, '片', 1, 30, '每次2mg口服', '餐前', 1),
(6, 14, 1, 1, '片', 1, 30, '每次50mg嚼服', '餐前', 1),
(6, 15, 1, 1, '片', 1, 7, '每次0.5mg口服', '空腹', 1),
(6, 36, 1, 1, '片', 1, 30, '每次10mg口服', '每晨', 1);

INSERT INTO `stock` (`prescription_id`, `last_calc_time`, `total_remaining_units`, `remaining_days`, `expiry_date`)
SELECT p.prescription_id, NOW(),
       FLOOR(RAND() * 60 + 10),
       FLOOR(RAND() * 60 + 10),
       DATE_ADD(CURDATE(), INTERVAL FLOOR(RAND() * 730 + 180) DAY)
FROM prescription p WHERE p.deleted = 0 AND p.status = 1
AND NOT EXISTS (SELECT 1 FROM stock s WHERE s.prescription_id = p.prescription_id AND s.deleted = 0);

DROP PROCEDURE IF EXISTS generate_test_data;
DELIMITER //
CREATE PROCEDURE generate_test_data()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE v_user_id BIGINT;
    DECLARE v_prescription_id BIGINT;
    DECLARE v_medicine_id BIGINT;
    DECLARE v_unit_per_box INT;
    DECLARE v_unit_price DECIMAL(10,2);
    DECLARE v_boxes INT;
    DECLARE v_total_price DECIMAL(10,2);
    DECLARE v_purchase_date DATE;
    DECLARE v_expiry_date DATE;
    DECLARE v_platform VARCHAR(50);
    
    SET i = 0;
    WHILE i < 1050 DO
        SET v_user_id = FLOOR(RAND() * 6) + 1;
        SELECT prescription_id, medicine_id INTO v_prescription_id, v_medicine_id
        FROM prescription WHERE user_id = v_user_id AND deleted = 0 AND status = 1 ORDER BY RAND() LIMIT 1;
        IF v_prescription_id IS NOT NULL THEN
            SELECT unit_per_box, reference_price INTO v_unit_per_box, v_unit_price
            FROM medicine WHERE medicine_id = v_medicine_id AND deleted = 0;
            IF v_unit_per_box IS NOT NULL THEN
                SET v_boxes = FLOOR(RAND() * 5) + 1;
                SET v_total_price = v_unit_price * v_boxes;
                SET v_purchase_date = DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 730) DAY);
                SET v_expiry_date = DATE_ADD(v_purchase_date, INTERVAL FLOOR(RAND() * 730 + 365) DAY);
                CASE FLOOR(RAND() * 8)
                    WHEN 0 THEN SET v_platform = '美团买药';
                    WHEN 1 THEN SET v_platform = '饿了么买药';
                    WHEN 2 THEN SET v_platform = '京东健康';
                    WHEN 3 THEN SET v_platform = '阿里健康';
                    WHEN 4 THEN SET v_platform = '叮当快药';
                    WHEN 5 THEN SET v_platform = '社区药店';
                    WHEN 6 THEN SET v_platform = '医院药房';
                    ELSE SET v_platform = '益丰大药房';
                END CASE;
                INSERT INTO purchase_record (user_id, prescription_id, purchase_date, quantity_boxes, unit_price, total_price, expiry_date, operator_id, purchase_platform)
                VALUES (v_user_id, v_prescription_id, v_purchase_date, v_boxes, v_unit_price, v_total_price, v_expiry_date, v_user_id, v_platform);
            END IF;
        END IF;
        SET i = i + 1;
        SET v_prescription_id = NULL;
    END WHILE;
END //
DELIMITER ;
CALL generate_test_data();
DROP PROCEDURE IF EXISTS generate_test_data;

SELECT '药品' AS item, COUNT(*) AS cnt FROM medicine WHERE deleted=0
UNION ALL SELECT '方案', COUNT(*) FROM prescription WHERE deleted=0
UNION ALL SELECT '库存', COUNT(*) FROM stock WHERE deleted=0
UNION ALL SELECT '购药', COUNT(*) FROM purchase_record WHERE deleted=0
UNION ALL SELECT '用户', COUNT(*) FROM sys_user WHERE deleted=0;
