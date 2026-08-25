#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_JAR="${PACKAGE_ROOT}/backend/medicine-system-1.0.0.jar"
TARGET_JAR="/home/xiaoliu/DSMS/backend/target/medicine-system-1.0.0.jar"
SERVICE="dsms-backend"
ENV_FILE="/etc/dsms-backend.env"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/dsms-backend-r7-1-${STAMP}"
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
  local max_attempts="${1:-30}"
  local attempt

  for attempt in $(seq 1 "${max_attempts}"); do
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
  local failed_log="${BACKUP_DIR}/failed-new-release.log"
  trap - ERR INT TERM
  set +e

  echo "R7.1 后端热修复安装失败，正在保存本次启动日志并恢复旧 JAR……" >&2
  if sudo test -d "${BACKUP_DIR}"; then
    new_release_logs | sudo tee "${failed_log}" >&2
  else
    new_release_logs >&2
  fi

  if [[ "${SWITCHED}" -eq 1 && "${HAD_OLD_JAR}" -eq 1 ]] && \
    sudo test -f "${BACKUP_DIR}/medicine-system-1.0.0.jar.before"; then
    sudo systemctl stop "${SERVICE}"
    sudo install -o xiaoliu -g xiaoliu -m 0644 \
      "${BACKUP_DIR}/medicine-system-1.0.0.jar.before" \
      "${TARGET_JAR}"
    sudo systemctl start "${SERVICE}"
    if wait_for_health 30; then
      echo "旧后端已自动恢复，human-challenge 接口已重新可用。" >&2
    else
      echo "旧后端恢复后仍未健康，请立即查看服务日志。" >&2
    fi
  fi

  echo "DSMS_R7_1_BACKEND_HOTFIX_FAILED；退出码：${exit_code}" >&2
  echo "失败日志：${failed_log}" >&2
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
    if grep -Eq \
      'APPLICATION FAILED TO START|BeanCreationException|UnsatisfiedDependencyException|Main process exited, code=exited, status=[1-9]' \
      <<<"${RECENT_LOGS}"; then
      echo "检测到明确的 Spring Boot 启动失败日志，不再空等 90 秒。" >&2
      false
    fi
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

INSTALLED_SHA="$(sudo sha256sum "${TARGET_JAR}" | awk '{print $1}')"
if [[ "${SOURCE_SHA}" != "${INSTALLED_SHA}" ]]; then
  echo "安装后的 JAR 校验值与发布包不一致。" >&2
  false
fi

SWITCHED=0
trap - ERR INT TERM

echo "DSMS_R7_1_BACKEND_HOTFIX_OK"
echo "JAR_SHA256=${INSTALLED_SHA}"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "已验证：${SERVICE} 为 active，human-challenge 接口可访问，安装 JAR 与发布包一致。"
