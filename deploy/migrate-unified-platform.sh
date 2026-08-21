#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
OBS_SQL="${PACKAGE_ROOT}/sql/20260822_obs_file_center.sql"
PORTAL_SQL="${PACKAGE_ROOT}/sql/20260822_unified_portal_web.sql"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/unified-database-${STAMP}"

for sql_file in "${OBS_SQL}" "${PORTAL_SQL}"; do
  [[ -f "${sql_file}" ]] || { echo "发布包缺少数据库脚本：${sql_file}" >&2; exit 1; }
done
docker ps --format '{{.Names}}' | grep -Fxq mysql || { echo "MySQL 容器 mysql 未运行。" >&2; exit 1; }

read -r -s -p "请输入 MySQL root 密码（输入内容不会显示）: " MYSQL_ROOT_PASSWORD
echo
[[ -n "${MYSQL_ROOT_PASSWORD}" ]] || { echo "密码不能为空。" >&2; exit 1; }
trap 'unset MYSQL_ROOT_PASSWORD' EXIT

install -d -m 0700 "${BACKUP_DIR}"
echo "1/4 备份 medicine_system 数据库……"
docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
  mysqldump --single-transaction --routines --triggers medicine_system \
  > "${BACKUP_DIR}/medicine_system-before.sql"
test -s "${BACKUP_DIR}/medicine_system-before.sql"

echo "2/4 创建 OBS 文件与购药凭证表……"
docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" -i mysql \
  mysql --default-character-set=utf8mb4 medicine_system < "${OBS_SQL}"

echo "3/4 创建统一门户、个人记账和软件服务表……"
docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" -i mysql \
  mysql --default-character-set=utf8mb4 medicine_system < "${PORTAL_SQL}"

echo "4/4 验证 11 张新增业务表……"
TABLE_COUNT="$(docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
  mysql -N -B medicine_system -e "
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = 'medicine_system'
      AND table_name IN (
        'file_asset','purchase_evidence','personal_ledger','personal_account',
        'personal_transaction','personal_budget','software_service_request',
        'software_service_milestone','software_service_work_order',
        'public_service_inquiry','public_service_message'
      );")"

if [[ "${TABLE_COUNT}" != "11" ]]; then
  echo "数据库表验证失败，预期 11，实际 ${TABLE_COUNT}。停止后续发版。" >&2
  exit 1
fi

echo "UNIFIED_DATABASE_MIGRATION_OK"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "新增业务表数量：${TABLE_COUNT}"
