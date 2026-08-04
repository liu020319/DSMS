SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO medicine (medicine_id, approval_number, medicine_name, brand_name, specification, unit_per_box, manufacturer, reference_price, status, create_time, update_time, deleted) VALUES
(1, 'H20050001', '盐酸地尔硫卓缓释胶囊(Ⅱ)', '合贝爽', '90mg*10', 10, '远大医药', 15.48, 1, NOW(), NOW(), 0),
(2, 'H20050002', '阿司匹林肠溶片', '拜阿司匹灵', '100mg*30', 30, '拜耳', 13.55, 1, NOW(), NOW(), 0),
(3, 'H20050003', '瑞舒伐他汀钙片', '海舒严', '10mg*28', 28, '瀚晖制药有限公司', 5.50, 1, NOW(), NOW(), 0),
(4, 'H20050004', '硫酸氢氯吡格雷片', '帅信', '75mg*7', 7, '乐普药业', 10.77, 1, NOW(), NOW(), 0),
(5, 'H20050005', '泮托拉唑钠肠溶片', '舒可意', '40mg*28', 28, '湖南九典制药有限公司', 21.60, 1, NOW(), NOW(), 0),
(6, 'H20050006', '尼可地尔片', '富格迈', '5mg*30', 30, '中外制药株式会社', 30.00, 1, NOW(), NOW(), 0),
(7, 'H20050007', '单硝酸异山梨酯缓释片', '鲁', '40mg*24', 24, '齐鲁制药', 17.52, 1, NOW(), NOW(), 0),
(8, 'H20050008', '爱维心口服液', '', '10ml*6', 6, '哈尔滨美君制药有限公司', 84.99, 1, NOW(), NOW(), 0),
(9, 'H20050009', '依折麦布片', '欣络康', '10mg*30', 30, '湖南方盛制药有限公司', 33.85, 1, NOW(), NOW(), 0);

INSERT INTO prescription (prescription_id, user_id, medicine_id, daily_times, dosage_per_time, daily_consumption, days_per_box, take_notes, take_timing, dosage_unit, take_frequency_code, take_periods, status, create_time, update_time, deleted) VALUES
(1, 2, 1, 1, 1, 1, 10, '一次90mg口服', '每晨', '粒', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(2, 2, 2, 1, 1, 1, 30, '一次100mg口服空腹', '空腹', '片', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(3, 2, 3, 1, 1, 1, 28, '一次10mg口服', '每晨', '片', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(4, 2, 4, 1, 1, 1, 7, '一次75mg口服', '每晨', '片', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(5, 2, 5, 1, 1, 1, 28, '一次40mg口服餐前', '餐前', '片', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(6, 2, 6, 3, 1, 3, 10, '一次5mg口服', '一日三次', '片', 'DAILY_3_FULL_DAY', '["MORNING","NOON","EVENING"]', 1, NOW(), NOW(), 0),
(7, 2, 7, 1, 1, 1, 24, '一次40mg口服', '每晨', '片', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(8, 2, 8, 2, 1, 2, 3, '一次10ml口服', '早晚', '支', 'DAILY_2_MORNING_EVENING', '["MORNING","EVENING"]', 1, NOW(), NOW(), 0),
(9, 2, 9, 1, 1, 1, 30, '一次10mg口服', '每晨', '片', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0);

INSERT INTO purchase_record (purchase_id, user_id, prescription_id, purchase_date, quantity_boxes, unit_price, total_price, expiry_date, operator_id, create_time, update_time, deleted) VALUES
(1, 2, 1, '2026-04-01', 3, 15.48, 46.44, '2027-06-01', 1, NOW(), NOW(), 0),
(2, 2, 2, '2026-04-01', 1, 13.55, 13.55, '2027-09-01', 1, NOW(), NOW(), 0),
(3, 2, 3, '2026-04-01', 1, 5.50, 5.50, '2027-08-01', 1, NOW(), NOW(), 0),
(4, 2, 4, '2026-04-01', 4, 10.77, 43.08, '2027-05-01', 1, NOW(), NOW(), 0),
(5, 2, 5, '2026-04-01', 1, 21.60, 21.60, '2027-10-01', 1, NOW(), NOW(), 0),
(6, 2, 6, '2026-04-01', 3, 30.00, 90.00, '2027-07-01', 1, NOW(), NOW(), 0),
(7, 2, 7, '2026-04-01', 3, 17.52, 52.56, '2027-12-01', 1, NOW(), NOW(), 0),
(8, 2, 8, '2026-04-01', 10, 84.99, 849.90, '2027-04-01', 1, NOW(), NOW(), 0),
(9, 2, 9, '2026-04-01', 1, 33.85, 33.85, '2027-11-01', 1, NOW(), NOW(), 0);

INSERT INTO stock (stock_id, prescription_id, total_remaining_units, remaining_days, today_deducted_periods, last_deduction_date, version, last_calc_time, expiry_date, create_time, update_time, deleted) VALUES
(1, 1, 30, 30, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-06-01', NOW(), NOW(), 0),
(2, 2, 30, 30, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-09-01', NOW(), NOW(), 0),
(3, 3, 28, 28, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-08-01', NOW(), NOW(), 0),
(4, 4, 28, 28, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-05-01', NOW(), NOW(), 0),
(5, 5, 28, 28, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-10-01', NOW(), NOW(), 0),
(6, 6, 90, 30, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-07-01', NOW(), NOW(), 0),
(7, 7, 72, 72, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-12-01', NOW(), NOW(), 0),
(8, 8, 60, 30, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-04-01', NOW(), NOW(), 0),
(9, 9, 30, 30, '[]', '2026-04-01', 0, '2026-04-01 08:00:00', '2027-11-01', NOW(), NOW(), 0);

SET FOREIGN_KEY_CHECKS = 1;
