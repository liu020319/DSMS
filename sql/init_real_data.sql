-- =============================================
-- 正式数据初始化脚本
-- 清空所有业务数据，保留管理员+刘大爷
-- 录入9种真实药品、用药方案、购药记录、库存
-- =============================================

SET NAMES utf8mb4;

-- 1. 清空所有业务数据
TRUNCATE TABLE sys_log;
TRUNCATE TABLE stock;
TRUNCATE TABLE purchase_record;
TRUNCATE TABLE prescription_history;
TRUNCATE TABLE prescription;
TRUNCATE TABLE medicine;

-- 2. 重置用户表，只保留管理员和刘大爷
TRUNCATE TABLE sys_user;
INSERT INTO sys_user (user_id, username, password, real_name, phone, role, bind_parent_id, status, create_time, update_time, deleted) VALUES
(1, 'admin', '$2a$10$k4mUJFDb4pInBdXAS8qZ9.J2Qjkj/xvNazX7F3G1nq3vp9HeXJyBy', '管理员', '13800000000', 'ADMIN', NULL, 1, NOW(), NOW(), 0),
(2, 'liuhao', '$2a$10$k4mUJFDb4pInBdXAS8qZ9.J2Qjkj/xvNazX7F3G1nq3vp9HeXJyBy', '刘浩', '13900000001', 'CHILD', NULL, 1, NOW(), NOW(), 0),
(3, 'elder1', '$2a$10$k4mUJFDb4pInBdXAS8qZ9.J2Qjkj/xvNazX7F3G1nq3vp9HeXJyBy', '刘大爷', '13700000001', 'ELDER', 2, 1, NOW(), NOW(), 0);

-- 3. 录入9种真实药品
INSERT INTO medicine (medicine_id, approval_number, medicine_name, brand_name, specification, unit_per_box, manufacturer, reference_price, status, create_time, update_time, deleted) VALUES
(1, '国药准字H20050001', '盐酸地尔硫卓缓释胶囊(Ⅱ)', '合贝爽', '90mg×10', 10, '远大医药', 15.48, 1, NOW(), NOW(), 0),
(2, '国药准字H20050002', '阿司匹林肠溶片', '拜阿司匹灵', '100mg×30', 30, '拜耳', 13.55, 1, NOW(), NOW(), 0),
(3, '国药准字H20050003', '瑞舒伐他汀钙片', '海舒严', '10mg×28', 28, '瀚晖制药有限公司', 5.50, 1, NOW(), NOW(), 0),
(4, '国药准字H20050004', '硫酸氢氯吡格雷片', '帅信', '75mg×7', 7, '乐普药业', 10.77, 1, NOW(), NOW(), 0),
(5, '国药准字H20050005', '泮托拉唑钠肠溶片', '舒可意', '40mg×28', 28, '湖南九典制药有限公司', 21.60, 1, NOW(), NOW(), 0),
(6, '国药准字H20050006', '尼可地尔片', '富格迈', '5mg×30', 30, '中外制药株式会社', 30.00, 1, NOW(), NOW(), 0),
(7, '国药准字H20050007', '单硝酸异山梨酯缓释片', '鲁', '40mg×24', 24, '齐鲁制药', 17.52, 1, NOW(), NOW(), 0),
(8, '国药准字H20050008', '爱维心口服液', '', '10ml×6', 6, '哈尔滨美君制药有限公司', 84.99, 1, NOW(), NOW(), 0),
(9, '国药准字H20050009', '依折麦布片', '欣络康', '10mg×30', 30, '湖南方盛制药有限公司', 33.85, 1, NOW(), NOW(), 0);

-- 4. 录入9条用药方案（刘大爷 user_id=3）
-- 频次映射：
--   每日1次晨服 → DAILY_1_MORNING, dailyTimes=1, periods=["MORNING"]
--   每日3次     → DAILY_3_FULL_DAY, dailyTimes=3, periods=["MORNING","NOON","EVENING"]
--   每日2次早晚 → DAILY_2_MORNING_EVENING, dailyTimes=2, periods=["MORNING","EVENING"]
--   每晨       → DAILY_1_MORNING, dailyTimes=1, periods=["MORNING"]
INSERT INTO prescription (prescription_id, user_id, medicine_id, daily_times, dosage_per_time, dosage_unit, daily_consumption, days_per_box, take_notes, take_timing, take_frequency_code, take_periods, status, create_time, update_time, deleted) VALUES
(1, 3, 1, 1, 1, '粒', 1, 10, '每日1次90毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(2, 3, 2, 1, 1, '片', 1, 30, '每日1次100毫克口服 空腹', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(3, 3, 3, 1, 1, '片', 1, 28, '每日1次10毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(4, 3, 4, 1, 1, '片', 1, 7, '每日1次75毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(5, 3, 5, 1, 1, '片', 1, 28, '每日1次40毫克口服 餐前', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(6, 3, 6, 3, 1, '片', 3, 10, '每日3次5毫克口服', '一日三次', 'DAILY_3_FULL_DAY', '["MORNING","NOON","EVENING"]', 1, NOW(), NOW(), 0),
(7, 3, 7, 1, 1, '片', 1, 24, '每晨40毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(8, 3, 8, 2, 1, '支', 2, 3, '每日2次10毫升口服', '早晚', 'DAILY_2_MORNING_EVENING', '["MORNING","EVENING"]', 1, NOW(), NOW(), 0),
(9, 3, 9, 1, 1, '片', 1, 30, '每日1次10毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0);

-- 5. 录入9条购药记录（4月1日购入）
INSERT INTO purchase_record (purchase_id, user_id, prescription_id, purchase_date, quantity_boxes, unit_price, total_price, expiry_date, operator_id, purchase_platform, create_time, update_time, deleted) VALUES
(1, 3, 1, '2026-04-01', 3, 15.48, 46.44, '2027-06-01', 1, NULL, NOW(), NOW(), 0),
(2, 3, 2, '2026-04-01', 1, 13.55, 13.55, '2027-09-01', 1, NULL, NOW(), NOW(), 0),
(3, 3, 3, '2026-04-01', 1, 5.50, 5.50, '2027-08-01', 1, NULL, NOW(), NOW(), 0),
(4, 3, 4, '2026-04-01', 4, 10.77, 43.08, '2027-05-01', 1, NULL, NOW(), NOW(), 0),
(5, 3, 5, '2026-04-01', 1, 21.60, 21.60, '2027-10-01', 1, NULL, NOW(), NOW(), 0),
(6, 3, 6, '2026-04-01', 3, 30.00, 90.00, '2027-07-01', 1, NULL, NOW(), NOW(), 0),
(7, 3, 7, '2026-04-01', 3, 17.52, 52.56, '2027-12-01', 1, NULL, NOW(), NOW(), 0),
(8, 3, 8, '2026-04-01', 10, 84.99, 849.90, '2027-04-01', 1, NULL, NOW(), NOW(), 0),
(9, 3, 9, '2026-04-01', 1, 33.85, 33.85, '2027-11-01', 1, NULL, NOW(), NOW(), 0);

-- 6. 初始化9条库存记录
-- 初始库存 = 购买盒数 × 每盒单位数
-- last_calc_time 设为4月1日08:00，表示从4月1日开始计算
-- today_deducted_periods = '[]' 表示4月1日晨服前尚未扣减
INSERT INTO stock (stock_id, prescription_id, total_remaining_units, remaining_days, last_calc_time, today_deducted_periods, last_deduction_date, expiry_date, version, create_time, update_time, deleted) VALUES
(1, 1, 30, 30, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-06-01', 0, NOW(), NOW(), 0),
(2, 2, 30, 30, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-09-01', 0, NOW(), NOW(), 0),
(3, 3, 28, 28, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-08-01', 0, NOW(), NOW(), 0),
(4, 4, 28, 28, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-05-01', 0, NOW(), NOW(), 0),
(5, 5, 28, 28, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-10-01', 0, NOW(), NOW(), 0),
(6, 6, 90, 30, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-07-01', 0, NOW(), NOW(), 0),
(7, 7, 72, 72, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-12-01', 0, NOW(), NOW(), 0),
(8, 8, 60, 30, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-04-01', 0, NOW(), NOW(), 0),
(9, 9, 30, 30, '2026-04-01 08:00:00', '[]', '2026-04-01', '2027-11-01', 0, NOW(), NOW(), 0);
