#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_JAR="${PACKAGE_ROOT}/backend/medicine-system-1.0.0.jar"
TARGET_JAR="/home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar"
SERVICE="dsms-backend"
ENV_FILE="/etc/dsms-backend.env"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/dsms-obs-backend-${STAMP}"
SWITCHED=0
HAD_OLD_JAR=0

rollback_on_error() {
  local exit_code="${1:-$?}"
  trap - ERR INT TERM
  set +e

  echo "DSMS 后端安装失败，正在恢复安装前 JAR……" >&2
  if [[ "${SWITCHED}" -eq 1 && "${HAD_OLD_JAR}" -eq 1 ]] && \
    sudo test -f "${BACKUP_DIR}/medicine-system-1.0.0.jar.before"; then
    sudo systemctl stop "${SERVICE}"
    sudo install -o xiaoliu -g xiaoliu -m 0644 \
      "${BACKUP_DIR}/medicine-system-1.0.0.jar.before" \
      "${TARGET_JAR}"
    sudo systemctl start "${SERVICE}"
  fi

  sudo journalctl -u "${SERVICE}" -n 100 --no-pager >&2
  echo "DSMS_OBS_BACKEND_INSTALL_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap 'rollback_on_error $?' ERR
trap 'rollback_on_error 130' INT
trap 'rollback_on_error 143' TERM

if [[ ! -f "${SOURCE_JAR}" ]]; then
  echo "发布包缺少后端 JAR：${SOURCE_JAR}" >&2
  exit 1
fi
if ! sudo systemctl cat "${SERVICE}" >/dev/null; then
  echo "找不到 systemd 服务：${SERVICE}" >&2
  exit 1
fi
if ! sudo test -f "${ENV_FILE}"; then
  echo "缺少生产环境文件：${ENV_FILE}" >&2
  exit 1
fi

# 只检查变量是否存在，不打印 AK/SK 内容。
if sudo grep -Eq '^FILE_STORAGE_PROVIDER=obs[[:space:]]*$' "${ENV_FILE}"; then
  for secret_name in OBS_ACCESS_KEY OBS_SECRET_KEY OBS_BUCKET OBS_ENDPOINT; do
    if ! sudo grep -Eq "^${secret_name}=.+" "${ENV_FILE}"; then
      echo "OBS 模式缺少环境变量：${secret_name}" >&2
      exit 1
    fi
  done
fi

sudo install -d -m 0700 "${BACKUP_DIR}"
if sudo test -f "${TARGET_JAR}"; then
  HAD_OLD_JAR=1
  sudo cp -a "${TARGET_JAR}" "${BACKUP_DIR}/medicine-system-1.0.0.jar.before"
fi
sudo cp -a "${ENV_FILE}" "${BACKUP_DIR}/dsms-backend.env.before"

sudo systemctl stop "${SERVICE}"
sudo install -o xiaoliu -g xiaoliu -m 0644 "${SOURCE_JAR}" "${TARGET_JAR}"
SWITCHED=1
sudo systemctl start "${SERVICE}"

SERVICE_READY=0
for attempt in $(seq 1 45); do
  if sudo systemctl is-active --quiet "${SERVICE}" && \
    curl -fsS http://127.0.0.1:8088/api/auth/human-challenge >/dev/null 2>&1; then
    SERVICE_READY=1
    break
  fi
  if (( attempt % 5 == 0 )); then
    echo "等待后端启动：已等待 $((attempt * 2)) 秒，最长等待 90 秒……"
  fi
  sleep 2
done

if [[ "${SERVICE_READY}" -ne 1 ]]; then
  echo "90 秒内后端没有同时达到 active 和接口可用状态。" >&2
  false
fi

SWITCHED=0
trap - ERR INT TERM

echo "DSMS_OBS_BACKEND_INSTALL_OK"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "已验证：${SERVICE} 为 active，human-challenge 接口可访问。"
