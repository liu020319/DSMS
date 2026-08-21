#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_PORTAL="${PACKAGE_ROOT}/frontend/cloud-hub"
STAMP="$(date +%Y%m%d-%H%M%S)"
PUBLIC_HOME="$(mktemp)"
VERIFY_HOME="$(mktemp)"
VERIFY_PORTAL="$(mktemp)"
VERIFY_ASSET="$(mktemp)"
SITE_ROOT=""
PORTAL_ROOT=""
PORTAL_STAGE=""
PORTAL_OLD=""
BACKUP_DIR=""
SWITCHED=0
HAD_PORTAL=0

cleanup_temp() {
  rm -f -- "${PUBLIC_HOME}" "${VERIFY_HOME}" "${VERIFY_PORTAL}" "${VERIFY_ASSET}"
}

fetch_local() {
  local path="$1" output="$2"
  if [[ "${PUBLIC_SCHEME}" == "https" ]]; then
    curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}${path}" -o "${output}"
  else
    curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}${path}" -o "${output}"
  fi
}

rollback_on_error() {
  local exit_code="${1:-$?}"
  trap - ERR INT TERM
  set +e
  echo "统一门户安装失败，正在恢复安装前目录……" >&2
  if [[ "${SWITCHED}" -eq 1 ]]; then
    if sudo test -d "${PORTAL_ROOT}"; then
      sudo mv "${PORTAL_ROOT}" "${BACKUP_DIR}/failed-cloud-hub-${STAMP}"
    fi
    if sudo test -d "${PORTAL_OLD}"; then
      sudo mv "${PORTAL_OLD}" "${PORTAL_ROOT}"
    elif sudo test -d "${BACKUP_DIR}/cloud-hub-before"; then
      sudo cp -a "${BACKUP_DIR}/cloud-hub-before" "${PORTAL_ROOT}"
    fi
  fi
  if [[ -n "${PORTAL_STAGE}" ]] && sudo test -d "${PORTAL_STAGE}"; then
    sudo rm -rf -- "${PORTAL_STAGE}"
  fi
  cleanup_temp
  echo "PORTAL_INSTALL_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap cleanup_temp EXIT
trap 'rollback_on_error $?' ERR
trap 'rollback_on_error 130' INT
trap 'rollback_on_error 143' TERM

[[ -f "${SOURCE_PORTAL}/index.html" ]] || { echo "发布包缺少统一门户 index.html。" >&2; exit 1; }
grep -Fq '/cloud-hub/assets/' "${SOURCE_PORTAL}/index.html" || {
  echo "统一门户不是按 /cloud-hub/ 构建，拒绝发布。" >&2; exit 1; }
if grep -Eq '(src|href)="/assets/' "${SOURCE_PORTAL}/index.html"; then
  echo "构建产物仍包含错误根路径 /assets/，拒绝发布。" >&2; exit 1
fi

PORTAL_ASSET_PATH="$(grep -oE '/cloud-hub/assets/[^" ]+\.js' "${SOURCE_PORTAL}/index.html" | head -n 1)"
[[ -n "${PORTAL_ASSET_PATH}" ]] || { echo "无法提取统一门户入口 JavaScript。" >&2; exit 1; }
SOURCE_ASSET="${SOURCE_PORTAL}${PORTAL_ASSET_PATH#/cloud-hub}"
[[ -f "${SOURCE_ASSET}" ]] || { echo "index.html 引用的入口 JavaScript 不存在。" >&2; exit 1; }

if curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="https"
elif curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="http"
else
  echo "无法从本机 Nginx 读取博客首页；没有修改任何文件。" >&2
  exit 1
fi

mapfile -d '' CANDIDATES < <(sudo find /var/www -maxdepth 5 -type f -name index.html -print0)
MATCHES=()
for candidate in "${CANDIDATES[@]}"; do
  if sudo cmp -s "${candidate}" "${PUBLIC_HOME}"; then MATCHES+=("$(dirname "${candidate}")"); fi
done
if [[ "${#MATCHES[@]}" -gt 1 ]]; then
  mapfile -t CONFIGURED_ROOTS < <(sudo nginx -T 2>/dev/null | awk '$1 == "root" { gsub(/;/, "", $2); print $2 }' | sort -u)
  ACTIVE_MATCHES=()
  for match in "${MATCHES[@]}"; do
    resolved_match="$(readlink -f "${match}")"
    for configured_root in "${CONFIGURED_ROOTS[@]}"; do
      if resolved_root="$(readlink -f "${configured_root}" 2>/dev/null)" && [[ "${resolved_match}" == "${resolved_root}" ]]; then
        ACTIVE_MATCHES+=("${resolved_match}")
        break
      fi
    done
  done
  [[ "${#ACTIVE_MATCHES[@]}" -eq 1 ]] && MATCHES=("${ACTIVE_MATCHES[0]}")
fi
[[ "${#MATCHES[@]}" -eq 1 ]] || { echo "无法唯一识别博客目录；匹配数：${#MATCHES[@]}" >&2; exit 1; }

SITE_ROOT="$(readlink -f "${MATCHES[0]}")"
[[ "${SITE_ROOT}" == /var/www/* ]] || { echo "博客目录不在 /var/www 下：${SITE_ROOT}" >&2; exit 1; }
sudo test -f "${SITE_ROOT}/blog-assets/app.js" || { echo "博客目录缺少 blog-assets/app.js。" >&2; exit 1; }

PORTAL_ROOT="${SITE_ROOT}/cloud-hub"
PORTAL_STAGE="${SITE_ROOT}/.cloud-hub-next-${STAMP}"
PORTAL_OLD="${SITE_ROOT}/.cloud-hub-old-${STAMP}"
BACKUP_DIR="/home/xiaoliu/backups/cloud-hub-r5-${STAMP}"

echo "博客根目录保持不动：${SITE_ROOT}"
echo "本次只安装：${PORTAL_ROOT}"
echo "备份目录：${BACKUP_DIR}"

sudo install -d -m 0700 "${BACKUP_DIR}"
if sudo test -d "${PORTAL_ROOT}"; then
  HAD_PORTAL=1
  sudo cp -a "${PORTAL_ROOT}" "${BACKUP_DIR}/cloud-hub-before"
fi
sudo install -d -m 0755 "${PORTAL_STAGE}"
sudo cp -a "${SOURCE_PORTAL}/." "${PORTAL_STAGE}/"
sudo find "${PORTAL_STAGE}" -type d -exec chmod 0755 {} +
sudo find "${PORTAL_STAGE}" -type f -exec chmod 0644 {} +
sudo cmp -s "${SOURCE_PORTAL}/index.html" "${PORTAL_STAGE}/index.html"

if [[ "${HAD_PORTAL}" -eq 1 ]]; then sudo mv "${PORTAL_ROOT}" "${PORTAL_OLD}"; fi
sudo mv "${PORTAL_STAGE}" "${PORTAL_ROOT}"
SWITCHED=1

sudo nginx -t
fetch_local "/" "${VERIFY_HOME}"
fetch_local "/cloud-hub/" "${VERIFY_PORTAL}"
fetch_local "${PORTAL_ASSET_PATH}" "${VERIFY_ASSET}"
cmp -s "${PUBLIC_HOME}" "${VERIFY_HOME}"
cmp -s "${SOURCE_PORTAL}/index.html" "${VERIFY_PORTAL}"
cmp -s "${SOURCE_ASSET}" "${VERIFY_ASSET}"

SWITCHED=0
trap - ERR INT TERM
if [[ "${HAD_PORTAL}" -eq 1 ]] && sudo test -d "${PORTAL_OLD}"; then
  sudo mv "${PORTAL_OLD}" "${BACKUP_DIR}/cloud-hub-before-atomic"
fi
cleanup_temp
trap - EXIT

echo "PORTAL_INSTALL_OK"
echo "SITE_ROOT=${SITE_ROOT}"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "已验证：博客首页未变化；统一门户首页和入口脚本与发布包逐字节一致。"
