#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_JAR="${PACKAGE_ROOT}/backend/medicine-system-1.0.0.jar"
TARGET_JAR="/home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar"
SERVICE="dsms-backend"
ENV_FILE="/etc/dsms-backend.env"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/dsms-backend-r8-${STAMP}"
START_TIME=""
SWITCHED=0
HAD_OLD_JAR=0

new_release_logs() {
  if [[ -n "${START_TIME}" ]]; then
    sudo journalctl -u "${SERVICE}" --since "${START_TIME}" -n 200 --no-pager
  else
    sudo journalctl -u "${SERVICE}" -n 200 --no-pager
  fi
}

wait_for_health() {
  local attempt
  for attempt in $(seq 1 "${1:-30}"); do
    if sudo systemctl is-active --quiet "${SERVICE}" && \
      curl -fsS http://127.0.0.1:8088/api/auth/human-challenge >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

rollback_on_error() {
  local exit_code="${1:-$?}"
  trap - ERR INT TERM
  set +e
  echo "R8 后端安装失败，正在保存日志并恢复旧 JAR……" >&2
  new_release_logs | sudo tee "${BACKUP_DIR}/failed-new-release.log" >&2
  if [[ "${SWITCHED}" -eq 1 && "${HAD_OLD_JAR}" -eq 1 ]] && \
    sudo test -f "${BACKUP_DIR}/medicine-system-1.0.0.jar.before"; then
    sudo systemctl stop "${SERVICE}"
    sudo install -o xiaoliu -g xiaoliu -m 0644 \
      "${BACKUP_DIR}/medicine-system-1.0.0.jar.before" "${TARGET_JAR}"
    sudo systemctl start "${SERVICE}"
    wait_for_health 30 && echo "旧后端已自动恢复。" >&2
  fi
  echo "DSMS_R8_BACKEND_INSTALL_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap 'rollback_on_error $?' ERR
trap 'rollback_on_error 130' INT
trap 'rollback_on_error 143' TERM

[[ -f "${SOURCE_JAR}" ]] || { echo "发布包缺少后端 JAR：${SOURCE_JAR}" >&2; exit 1; }
sudo systemctl cat "${SERVICE}" >/dev/null || { echo "找不到 systemd 服务：${SERVICE}" >&2; exit 1; }
sudo test -f "${ENV_FILE}" || { echo "缺少生产环境文件：${ENV_FILE}" >&2; exit 1; }

SOURCE_SHA="$(sha256sum "${SOURCE_JAR}" | awk '{print $1}')"
sudo install -d -m 0700 "${BACKUP_DIR}"
if sudo test -f "${TARGET_JAR}"; then
  HAD_OLD_JAR=1
  sudo cp -a "${TARGET_JAR}" "${BACKUP_DIR}/medicine-system-1.0.0.jar.before"
fi
sudo cp -a "${ENV_FILE}" "${BACKUP_DIR}/dsms-backend.env.before"

sudo systemctl stop "${SERVICE}"
sudo install -o xiaoliu -g xiaoliu -m 0644 "${SOURCE_JAR}" "${TARGET_JAR}"
SWITCHED=1
START_TIME="$(date '+%Y-%m-%d %H:%M:%S')"
sudo systemctl start "${SERVICE}"

SERVICE_READY=0
for attempt in $(seq 1 45); do
  if sudo systemctl is-active --quiet "${SERVICE}" && \
    curl -fsS http://127.0.0.1:8088/api/auth/human-challenge >/dev/null 2>&1; then
    SERVICE_READY=1
    break
  fi
  if (( attempt >= 3 )); then
    RECENT_LOGS="$(new_release_logs 2>&1 || true)"
    if grep -Eq 'APPLICATION FAILED TO START|BeanCreationException|UnsatisfiedDependencyException|Main process exited, code=exited, status=[1-9]' <<<"${RECENT_LOGS}"; then
      echo "检测到明确启动失败，不再继续空等。" >&2
      false
    fi
  fi
  (( attempt % 5 == 0 )) && echo "等待后端启动：已等待 $((attempt * 2)) 秒，最长 90 秒……"
  sleep 2
done

[[ "${SERVICE_READY}" -eq 1 ]] || { echo "90 秒内后端没有达到 active 且接口可用。" >&2; false; }
INSTALLED_SHA="$(sudo sha256sum "${TARGET_JAR}" | awk '{print $1}')"
[[ "${SOURCE_SHA}" == "${INSTALLED_SHA}" ]] || { echo "安装后 JAR 校验值不一致。" >&2; false; }

SWITCHED=0
trap - ERR INT TERM
echo "DSMS_R8_BACKEND_INSTALL_OK"
echo "JAR_SHA256=${INSTALLED_SHA}"
echo "BACKUP_DIR=${BACKUP_DIR}"
