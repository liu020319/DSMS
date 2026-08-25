#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
TMP_INDEX="$(mktemp)"
TMP_ASSET="$(mktemp)"
trap 'rm -f "${TMP_INDEX}" "${TMP_ASSET}"' EXIT

echo "1/5 检查后端、Nginx 服务和上传限制……"
sudo systemctl is-active --quiet dsms-backend
sudo systemctl is-active --quiet nginx
sudo nginx -t
sudo nginx -T 2>/dev/null | grep -Eq 'client_max_body_size[[:space:]]+16m;'

echo "2/5 检查本机后端健康接口……"
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge >/dev/null

echo "3/5 检查公网 DSMS 首页及入口脚本……"
curl -fsSL "https://${DOMAIN}/kanglian-cloud/" -o "${TMP_INDEX}"
ASSET_PATH="$(grep -oE '/kanglian-cloud/assets/[^" ]+\.js' "${TMP_INDEX}" | head -n 1)"
[[ -n "${ASSET_PATH}" ]] || { echo "无法提取 DSMS 入口脚本。" >&2; exit 1; }
curl -fsSL "https://${DOMAIN}${ASSET_PATH}" -o "${TMP_ASSET}"
test -s "${TMP_ASSET}"

echo "4/5 检查本次功能标记……"
grep -Fq '开始日期' "${TMP_ASSET}"
grep -Fq '付款凭证' "${TMP_ASSET}"
grep -Fq '最后一次确认' "${TMP_ASSET}"

echo "5/5 检查数据库迁移结果……"
read -r -s -p "请输入 MySQL root 密码（输入内容不会显示）: " MYSQL_ROOT_PASSWORD
echo
[[ -n "${MYSQL_ROOT_PASSWORD}" ]] || { echo "密码不能为空。" >&2; exit 1; }
DB_RESULT="$(docker exec -e MYSQL_PWD="${MYSQL_ROOT_PASSWORD}" mysql \
  mysql -N -B medicine_system -e "
    SELECT CONCAT(
      (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='medicine_system' AND TABLE_NAME='purchase_evidence' AND COLUMN_NAME='purchase_id'), ',',
      (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='medicine_system' AND TABLE_NAME='family_fund_transaction' AND COLUMN_NAME='reference_purchase_id')
    );")"
unset MYSQL_ROOT_PASSWORD
[[ "${DB_RESULT}" == "1,1" ]] || { echo "数据库迁移验证失败：${DB_RESULT}" >&2; exit 1; }

echo "DSMS_R8_VERIFY_OK"
echo "PUBLIC_ASSET=${ASSET_PATH}"
echo "DATABASE_COLUMNS=${DB_RESULT}"
