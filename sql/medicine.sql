/*
 Navicat Premium Data Transfer

 Source Server         : 本地数据库
 Source Server Type    : MySQL
 Source Server Version : 80022 (8.0.22)
 Source Host           : localhost:3306
 Source Schema         : medicine_system

 Target Server Type    : MySQL
 Target Server Version : 80022 (8.0.22)
 File Encoding         : 65001

 Date: 25/04/2026 17:18:38
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for medicine
-- ----------------------------
DROP TABLE IF EXISTS `medicine`;
CREATE TABLE `medicine`  (
  `medicine_id` bigint NOT NULL AUTO_INCREMENT COMMENT '药品ID',
  `approval_number` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '国药准字号',
  `medicine_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品通用名',
  `brand_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '品牌名',
  `specification` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规格',
  `unit_per_box` int NOT NULL COMMENT '每盒单位数',
  `box_unit` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '包装单位(片/粒/支等)',
  `manufacturer` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '生产厂家',
  `reference_price` decimal(10, 2) NOT NULL COMMENT '参考价格',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片URL(预留)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
  PRIMARY KEY (`medicine_id`) USING BTREE,
  UNIQUE INDEX `uk_approval_number`(`approval_number` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '药品基础档案表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of medicine
-- ----------------------------
INSERT INTO `medicine` VALUES (1, '国药准字H19990388', '盐酸地尔硫卓缓释胶囊 (Ⅱ)', '合贝爽', '90毫克×10粒', 10, '粒', '远大医药', 15.48, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:53:01', 0);
INSERT INTO `medicine` VALUES (2, '国药准字HJ20160685', '阿司匹林肠溶片', '拜阿司匹灵', '100毫克×30片', 30, '片', '拜耳', 13.55, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:46:36', 0);
INSERT INTO `medicine` VALUES (3, '国药准字H20143338', '瑞舒伐他汀钙片', '海舒严', '10毫克×28片', 28, '片', '瀚晖制药有限公司', 5.60, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:46:59', 0);
INSERT INTO `medicine` VALUES (4, '国药准字H00000001', '硫酸氢氯吡格雷片', '帅信', '75毫克×7片', 7, '片', '乐普药业', 10.77, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:47:36', 0);
INSERT INTO `medicine` VALUES (5, '国药准字H20093501', '泮托拉唑钠肠溶片', '舒可意', '40毫克×28片', 28, '片', '湖南九典制药有限公司', 21.60, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:48:09', 0);
INSERT INTO `medicine` VALUES (6, '国药准字HJ20160539', '尼可地尔片', '喜格迈', '5毫克×30片', 30, '片', '中外制药株式会社', 60.00, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:48:27', 0);
INSERT INTO `medicine` VALUES (7, '国药准字H20066717', '单硝酸异山梨酯缓释片', '齐鲁', '40毫克×24片', 24, '片', '齐鲁制药', 17.52, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:48:47', 0);
INSERT INTO `medicine` VALUES (8, '国药准字Z23020919', '爱维心口服液', '若宏', '10毫克×6支', 6, '支', '哈尔滨美君制药有限公司', 84.99, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:49:09', 0);
INSERT INTO `medicine` VALUES (9, '国药准字H20203468', '依折麦布片', '欣络康', '10毫克×30片', 30, '片', '湖南方盛制药有限公司', 33.85, NULL, 1, '2026-04-25 13:52:53', '2026-04-25 16:49:31', 0);

-- ----------------------------
-- Triggers structure for table medicine
-- ----------------------------
DROP TRIGGER IF EXISTS `trg_medicine_update`;
delimiter ;;
CREATE TRIGGER `trg_medicine_update` AFTER UPDATE ON `medicine` FOR EACH ROW BEGIN
    INSERT INTO sys_log (user_id, operation_type, operation_content, operation_time)
    VALUES (0, 'DB_MEDICINE_UPDATE',
        CONCAT('medicine_id=', NEW.medicine_id, ',name=', NEW.medicine_name, ',status=', NEW.status),
        NOW());
END
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
