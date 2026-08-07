USE medicine_system;
DROP PROCEDURE IF EXISTS gen_more;
DELIMITER //
CREATE PROCEDURE gen_more()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE v_user_id BIGINT;
    DECLARE v_prescription_id BIGINT;
    DECLARE v_medicine_id BIGINT;
    DECLARE v_unit_price DECIMAL(10,2);
    DECLARE v_boxes INT;
    DECLARE v_total_price DECIMAL(10,2);
    DECLARE v_purchase_date DATE;
    DECLARE v_expiry_date DATE;
    DECLARE v_platform VARCHAR(50);
    WHILE i < 200 DO
        SET v_user_id = FLOOR(RAND() * 6) + 1;
        SELECT prescription_id, medicine_id INTO v_prescription_id, v_medicine_id FROM prescription WHERE user_id = v_user_id AND deleted = 0 AND status = 1 ORDER BY RAND() LIMIT 1;
        IF v_prescription_id IS NOT NULL THEN
            SELECT reference_price INTO v_unit_price FROM medicine WHERE medicine_id = v_medicine_id AND deleted = 0;
            IF v_unit_price IS NOT NULL THEN
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
                INSERT INTO purchase_record (user_id, prescription_id, purchase_date, quantity_boxes, unit_price, total_price, expiry_date, operator_id, purchase_platform) VALUES (v_user_id, v_prescription_id, v_purchase_date, v_boxes, v_unit_price, v_total_price, v_expiry_date, v_user_id, v_platform);
            END IF;
        END IF;
        SET i = i + 1;
        SET v_prescription_id = NULL;
    END WHILE;
END //
DELIMITER ;
CALL gen_more();
DROP PROCEDURE IF EXISTS gen_more;
SELECT COUNT(*) as total FROM purchase_record WHERE deleted=0;
