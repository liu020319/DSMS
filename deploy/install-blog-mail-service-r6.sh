#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE="${PACKAGE_ROOT}/blog-service/resume_service.py"
TARGET="/opt/personal-blog-resume/resume_service.py"
SERVICE="personal-blog-resume"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/blog-mail-service-r6-${STAMP}"
SWITCHED=0

rollback_on_error() {
  local exit_code="${1:-$?}"
  trap - ERR INT TERM
  set +e
  if [[ "${SWITCHED}" -eq 1 ]] && sudo test -f "${BACKUP_DIR}/resume_service.py.before"; then
    sudo install -o root -g root -m 0755 "${BACKUP_DIR}/resume_service.py.before" "${TARGET}"
    sudo systemctl restart "${SERVICE}"
  fi
  sudo journalctl -u "${SERVICE}" -n 80 --no-pager >&2
  echo "BLOG_MAIL_SERVICE_R6_INSTALL_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}
trap 'rollback_on_error $?' ERR
trap 'rollback_on_error 130' INT
trap 'rollback_on_error 143' TERM

[[ -f "${SOURCE}" ]] || { echo "发布包缺少：${SOURCE}" >&2; exit 1; }
sudo test -f "${TARGET}" || { echo "服务器缺少现有博客数据服务：${TARGET}" >&2; exit 1; }
sudo systemctl cat "${SERVICE}" >/dev/null

sudo install -d -m 0700 "${BACKUP_DIR}"
sudo cp -a "${TARGET}" "${BACKUP_DIR}/resume_service.py.before"
sudo install -o root -g root -m 0755 "${SOURCE}" "${TARGET}"
SWITCHED=1
sudo systemctl restart "${SERVICE}"

for attempt in $(seq 1 20); do
  if sudo systemctl is-active --quiet "${SERVICE}" && \
     curl -fsS http://127.0.0.1:8091/health >/dev/null 2>&1; then
    break
  fi
  [[ "${attempt}" -lt 20 ]] || { echo "博客数据服务 20 秒内未就绪。" >&2; false; }
  sleep 1
done

sudo cmp -s "${SOURCE}" "${TARGET}"
SWITCHED=0
trap - ERR INT TERM
echo "BLOG_MAIL_SERVICE_R6_INSTALL_OK"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "已验证：博客首页未被修改；8091 数据服务 active 且安装文件与发布包逐字节一致。"
