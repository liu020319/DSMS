#!/usr/bin/env bash

set -Eeuo pipefail

CONF_FILE="/etc/nginx/conf.d/dsms-upload-size.conf"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/dsms-nginx-r8-${STAMP}"
HAD_OLD=0

rollback_on_error() {
  local exit_code=$?
  trap - ERR
  set +e
  echo "上传大小配置失败，正在恢复旧 Nginx 配置……" >&2
  if [[ "${HAD_OLD}" -eq 1 ]]; then
    sudo install -m 0644 "${BACKUP_DIR}/dsms-upload-size.conf.before" "${CONF_FILE}"
  else
    sudo rm -f -- "${CONF_FILE}"
  fi
  sudo nginx -t && sudo systemctl reload nginx
  echo "DSMS_R8_UPLOAD_LIMIT_CONFIG_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap rollback_on_error ERR
sudo install -d -m 0700 "${BACKUP_DIR}"
if sudo test -f "${CONF_FILE}"; then
  HAD_OLD=1
  sudo cp -a "${CONF_FILE}" "${BACKUP_DIR}/dsms-upload-size.conf.before"
fi

TEMP_CONF="$(mktemp)"
trap 'rm -f "${TEMP_CONF}"' EXIT
printf '%s\n' \
  '# DSMS uploads: application accepts up to 12MB; leave multipart overhead.' \
  'client_max_body_size 16m;' > "${TEMP_CONF}"
sudo install -m 0644 "${TEMP_CONF}" "${CONF_FILE}"
sudo nginx -t
sudo systemctl reload nginx
sudo nginx -T 2>/dev/null | grep -Eq 'client_max_body_size[[:space:]]+16m;'

trap - ERR
echo "DSMS_R8_UPLOAD_LIMIT_OK"
echo "NGINX_CONFIG=${CONF_FILE}"
echo "BACKUP_DIR=${BACKUP_DIR}"
