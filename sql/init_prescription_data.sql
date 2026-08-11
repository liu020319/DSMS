SET NAMES utf8mb4;

INSERT INTO prescription (prescription_id, user_id, medicine_id, daily_times, dosage_per_time, dosage_unit, daily_consumption, days_per_box, take_notes, take_timing, take_frequency_code, take_periods, status, create_time, update_time, deleted) VALUES
(1, 2, 1, 1, 1, '粒', 1, 10, '每日1次90毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(2, 2, 2, 1, 1, '片', 1, 30, '每日1次100毫克口服 空腹', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(3, 2, 3, 1, 1, '片', 1, 28, '每日1次10毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(4, 2, 4, 1, 1, '片', 1, 7, '每日1次75毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(5, 2, 5, 1, 1, '片', 1, 28, '每日1次40毫克口服 餐前', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(6, 2, 6, 3, 1, '片', 3, 10, '每日3次5毫克口服', '一日三次', 'DAILY_3_FULL_DAY', '["MORNING","NOON","EVENING"]', 1, NOW(), NOW(), 0),
(7, 2, 7, 1, 1, '片', 1, 24, '每晨40毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0),
(8, 2, 8, 2, 1, '支', 2, 3, '每日2次10毫升口服', '早晚', 'DAILY_2_MORNING_EVENING', '["MORNING","EVENING"]', 1, NOW(), NOW(), 0),
(9, 2, 9, 1, 1, '片', 1, 30, '每日1次10毫克口服', '每晨', 'DAILY_1_MORNING', '["MORNING"]', 1, NOW(), NOW(), 0);

INSERT INTO purchase_record (purchase_id, user_id, prescription_id, purchase_date, quantity_boxes, unit_price, total_price, expiry_date, operator_id, purchase_platform, create_time, update_time, deleted) VALUES
(1, 2, 1, '2026-04-24', 3, 15.48, 46.44, '2027-06-01', 1, NULL, NOW(), NOW(), 0),
(2, 2, 2, '2026-04-24', 1, 13.55, 13.55, '2027-09-01', 1, NULL, NOW(), NOW(), 0),
(3, 2, 3, '2026-04-24', 1, 5.50, 5.50, '2027-08-01', 1, NULL, NOW(), NOW(), 0),
(4, 2, 4, '2026-04-24', 4, 10.77, 43.08, '2027-05-01', 1, NULL, NOW(), NOW(), 0),
(5, 2, 5, '2026-04-24', 1, 21.60, 21.60, '2027-10-01', 1, NULL, NOW(), NOW(), 0),
(6, 2, 6, '2026-04-24', 3, 30.00, 90.00, '2027-07-01', 1, NULL, NOW(), NOW(), 0),
(7, 2, 7, '2026-04-24', 3, 17.52, 52.56, '2027-12-01', 1, NULL, NOW(), NOW(), 0),
(8, 2, 8, '2026-04-24', 10, 84.99, 849.90, '2027-04-01', 1, NULL, NOW(), NOW(), 0),
(9, 2, 9, '2026-04-24', 1, 33.85, 33.85, '2027-11-01', 1, NULL, NOW(), NOW(), 0);

INSERT INTO stock (stock_id, prescription_id, total_remaining_units, remaining_days, last_calc_time, today_deducted_periods, last_deduction_date, expiry_date, version, create_time, update_time, deleted) VALUES
(1, 1, 30, 30, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-06-01', 0, NOW(), NOW(), 0),
(2, 2, 30, 30, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-09-01', 0, NOW(), NOW(), 0),
(3, 3, 28, 28, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-08-01', 0, NOW(), NOW(), 0),
(4, 4, 28, 28, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-05-01', 0, NOW(), NOW(), 0),
(5, 5, 28, 28, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-10-01', 0, NOW(), NOW(), 0),
(6, 6, 90, 30, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-07-01', 0, NOW(), NOW(), 0),
(7, 7, 72, 72, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-12-01', 0, NOW(), NOW(), 0),
(8, 8, 60, 30, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-04-01', 0, NOW(), NOW(), 0),
(9, 9, 30, 30, '2026-04-24 08:00:00', '[]', '2026-04-24', '2027-11-01', 0, NOW(), NOW(), 0);
