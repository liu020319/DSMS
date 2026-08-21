#!/usr/bin/env bash

set -Eeuo pipefail

ENV_FILE="/etc/dsms-backend.env"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP="/home/xiaoliu/backups/dsms-backend.env-before-registration-${STAMP}"
TEMP_FILE="$(mktemp)"

cleanup() {
  rm -f -- "${TEMP_FILE}"
  unset INVITE_CODE INVITE_CONFIRM
}
trap cleanup EXIT

if ! sudo test -f "${ENV_FILE}"; then
  echo "找不到生产环境文件：${ENV_FILE}" >&2
  exit 1
fi

read -r -s -p "请设置朋友注册邀请码（8-64 位字母、数字、_ 或 -）：" INVITE_CODE
echo
read -r -s -p "请再次输入相同邀请码：" INVITE_CONFIRM
echo

if [[ "${INVITE_CODE}" != "${INVITE_CONFIRM}" ]]; then
  echo "两次邀请码不一致，没有修改服务器配置。" >&2
  exit 1
fi
if [[ ! "${INVITE_CODE}" =~ ^[A-Za-z0-9_-]{8,64}$ ]]; then
  echo "邀请码格式不正确；请使用 8-64 位字母、数字、_ 或 -。" >&2
  exit 1
fi

umask 077
sudo cat "${ENV_FILE}" | awk '
  !/^PORTAL_REGISTRATION_ENABLED=/ && !/^PORTAL_REGISTRATION_INVITE_CODE=/ { print }
' > "${TEMP_FILE}"
printf 'PORTAL_REGISTRATION_ENABLED=true\n' >> "${TEMP_FILE}"
printf 'PORTAL_REGISTRATION_INVITE_CODE=%s\n' "${INVITE_CODE}" >> "${TEMP_FILE}"

sudo install -d -m 0700 "$(dirname "${BACKUP}")"
sudo cp -a "${ENV_FILE}" "${BACKUP}"
sudo install -o root -g root -m 0600 "${TEMP_FILE}" "${ENV_FILE}"

echo "PORTAL_REGISTRATION_CONFIG_OK"
echo "BACKUP=${BACKUP}"
echo "已开启朋友邀请码注册；邀请码没有打印到终端。下一步安装并重启后端。"
