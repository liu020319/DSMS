#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SQL_FILE="${PACKAGE_ROOT}/sql/20260824_finance_default_accounts.sql"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/dsms-r7-migration-${STAMP}"

[[ -f "${SQL_FILE}" ]] || { echo "发布包缺少数据库脚本：${SQL_FILE}" >&2; exit 1; }
docker ps --format '{{.Names}}' | grep -Fxq mysql || { echo "MySQL 容器 mysql 未运行。" >&2; exit 1; }

read -r -s -p "请输入 MySQL root 密码（输入内容不会显示）: " MYSQL_ROOT_PASSWORD
echo
[[ -n "${MYSQL_ROOT_PASSWORD}" ]] || { echo "密码不能为空。" >&2; exit 1; }
trap 'unset MYSQL_ROOT_PASSWORD' EXIT

install -d -m 0700 "${BACKUP_DIR}"
echo "1/3 备份正式数据库……"
docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
  mysqldump --single-transaction --routines --triggers medicine_system \
  > "${BACKUP_DIR}/medicine_system-before-r7.sql"
test -s "${BACKUP_DIR}/medicine_system-before-r7.sql"

echo "2/3 补齐历史账本的默认付款账户……"
docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" -i mysql \
  mysql --default-character-set=utf8mb4 medicine_system < "${SQL_FILE}"

echo "3/3 验证所有有效账本都至少有一个有效账户……"
MISSING_COUNT="$(docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
  mysql -N -B medicine_system -e "
    SELECT COUNT(*)
    FROM personal_ledger l
    WHERE l.status = 1 AND l.deleted = 0
      AND NOT EXISTS (
        SELECT 1 FROM personal_account a
        WHERE a.ledger_id = l.ledger_id
          AND a.owner_user_id = l.owner_user_id
          AND a.status = 1 AND a.deleted = 0
      );")"

[[ "${MISSING_COUNT}" == "0" ]] || {
  echo "仍有 ${MISSING_COUNT} 个有效账本没有付款账户，停止发版。" >&2
  exit 1
}

echo "DSMS_R7_DATABASE_MIGRATION_OK"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "无付款账户的有效账本数量：${MISSING_COUNT}"
