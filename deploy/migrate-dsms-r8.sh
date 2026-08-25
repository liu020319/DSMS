#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SQL_FILE="${PACKAGE_ROOT}/sql/20260825_direct_purchase_evidence.sql"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/dsms-r8-migration-${STAMP}"

[[ -f "${SQL_FILE}" ]] || { echo "发布包缺少数据库脚本：${SQL_FILE}" >&2; exit 1; }
docker ps --format '{{.Names}}' | grep -Fxq mysql || { echo "MySQL 容器 mysql 未运行。" >&2; exit 1; }

read -r -s -p "请输入 MySQL root 密码（输入内容不会显示）: " MYSQL_ROOT_PASSWORD
echo
[[ -n "${MYSQL_ROOT_PASSWORD}" ]] || { echo "密码不能为空。" >&2; exit 1; }
trap 'unset MYSQL_ROOT_PASSWORD' EXIT

install -d -m 0700 "${BACKUP_DIR}"
echo "1/3 备份 medicine_system 数据库……"
docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
  mysqldump --single-transaction --routines --triggers medicine_system \
  > "${BACKUP_DIR}/medicine_system-before-r8.sql"
test -s "${BACKUP_DIR}/medicine_system-before-r8.sql"

echo "2/3 增加直接购药记录与凭证、资金流水的关联字段……"
docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" -i mysql \
  mysql --default-character-set=utf8mb4 medicine_system < "${SQL_FILE}"

echo "3/3 验证字段与索引……"
VERIFY_RESULT="$(docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
  mysql -N -B medicine_system -e "
    SELECT CONCAT(
      (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='medicine_system' AND TABLE_NAME='purchase_evidence' AND COLUMN_NAME='purchase_id'), ',',
      (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='medicine_system' AND TABLE_NAME='family_fund_transaction' AND COLUMN_NAME='reference_purchase_id'), ',',
      (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='medicine_system' AND TABLE_NAME='purchase_evidence' AND INDEX_NAME='idx_evidence_purchase_time'), ',',
      (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='medicine_system' AND TABLE_NAME='family_fund_transaction' AND INDEX_NAME='idx_fund_purchase')
    );")"

IFS=',' read -r EVIDENCE_COLUMN FUND_COLUMN EVIDENCE_INDEX FUND_INDEX <<<"${VERIFY_RESULT}"
[[ "${EVIDENCE_COLUMN}" == "1" && "${FUND_COLUMN}" == "1" ]] || {
  echo "R8 字段验证失败：${VERIFY_RESULT}" >&2; exit 1; }
[[ "${EVIDENCE_INDEX}" -ge 1 && "${FUND_INDEX}" -ge 1 ]] || {
  echo "R8 索引验证失败：${VERIFY_RESULT}" >&2; exit 1; }

echo "DSMS_R8_DATABASE_MIGRATION_OK"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "验证结果：${VERIFY_RESULT}"
