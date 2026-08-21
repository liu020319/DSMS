#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_DSMS="${PACKAGE_ROOT}/frontend/dsms"
SOURCE_PORTAL="${PACKAGE_ROOT}/frontend/cloud-hub"
STAMP="$(date +%Y%m%d-%H%M%S)"
PUBLIC_HOME="$(mktemp)"
VERIFY_HOME="$(mktemp)"
VERIFY_DSMS="$(mktemp)"
VERIFY_PORTAL="$(mktemp)"
VERIFY_DSMS_ASSET="$(mktemp)"
VERIFY_PORTAL_ASSET="$(mktemp)"
SITE_ROOT=""
BACKUP_DIR=""
DSMS_ROOT=""
PORTAL_ROOT=""
DSMS_STAGE=""
PORTAL_STAGE=""
DSMS_OLD=""
PORTAL_OLD=""
DSMS_SWITCHED=0
PORTAL_SWITCHED=0
HAD_DSMS=0
HAD_PORTAL=0

cleanup_temp() {
  rm -f "${PUBLIC_HOME}" "${VERIFY_HOME}" "${VERIFY_DSMS}" "${VERIFY_PORTAL}" \
    "${VERIFY_DSMS_ASSET}" "${VERIFY_PORTAL_ASSET}"
}

fetch_local() {
  local path="$1"
  local output="$2"
  if [[ "${PUBLIC_SCHEME}" == "https" ]]; then
    curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}${path}" -o "${output}"
  else
    curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}${path}" -o "${output}"
  fi
}

restore_frontend() {
  local switched="$1" root="$2" old="$3" backup="$4" failed_name="$5"
  if [[ "${switched}" -ne 1 ]]; then return; fi
  if sudo test -d "${root}"; then sudo mv "${root}" "${BACKUP_DIR}/${failed_name}-${STAMP}"; fi
  if sudo test -d "${old}"; then
    sudo mv "${old}" "${root}"
  elif sudo test -d "${backup}"; then
    sudo cp -a "${backup}" "${root}"
  fi
}

rollback_on_error() {
  local exit_code=$?
  trap - ERR
  set +e
  echo "统一前端安装失败，正在恢复博客下的两个子系统……" >&2
  restore_frontend "${PORTAL_SWITCHED}" "${PORTAL_ROOT}" "${PORTAL_OLD}" \
    "${BACKUP_DIR}/cloud-hub-before" "failed-cloud-hub"
  restore_frontend "${DSMS_SWITCHED}" "${DSMS_ROOT}" "${DSMS_OLD}" \
    "${BACKUP_DIR}/kanglian-cloud-before" "failed-kanglian-cloud"
  [[ -n "${DSMS_STAGE}" ]] && sudo test -e "${DSMS_STAGE}" && sudo rm -rf -- "${DSMS_STAGE}"
  [[ -n "${PORTAL_STAGE}" ]] && sudo test -e "${PORTAL_STAGE}" && sudo rm -rf -- "${PORTAL_STAGE}"
  cleanup_temp
  echo "UNIFIED_FRONTENDS_INSTALL_FAILED；已尝试恢复原目录。退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap cleanup_temp EXIT
trap rollback_on_error ERR

for required in "${SOURCE_DSMS}/index.html" "${SOURCE_PORTAL}/index.html"; do
  [[ -f "${required}" ]] || { echo "发布包不完整，缺少：${required}" >&2; exit 1; }
done
grep -Fq '/kanglian-cloud/assets/' "${SOURCE_DSMS}/index.html" || {
  echo "DSMS 不是按 /kanglian-cloud/ 构建，拒绝发布。" >&2; exit 1; }
grep -Fq '/cloud-hub/assets/' "${SOURCE_PORTAL}/index.html" || {
  echo "统一门户不是按 /cloud-hub/ 构建，拒绝发布。" >&2; exit 1; }
if grep -Eq '(src|href)="/assets/' "${SOURCE_DSMS}/index.html" "${SOURCE_PORTAL}/index.html"; then
  echo "构建产物仍包含错误根路径 /assets/，拒绝发布。" >&2; exit 1
fi

DSMS_ASSET_PATH="$(grep -oE '/kanglian-cloud/assets/[^" ]+\.js' "${SOURCE_DSMS}/index.html" | head -n 1)"
PORTAL_ASSET_PATH="$(grep -oE '/cloud-hub/assets/[^" ]+\.js' "${SOURCE_PORTAL}/index.html" | head -n 1)"
[[ -n "${DSMS_ASSET_PATH}" && -n "${PORTAL_ASSET_PATH}" ]] || {
  echo "无法提取前端入口 JavaScript。" >&2; exit 1; }
SOURCE_DSMS_ASSET="${SOURCE_DSMS}${DSMS_ASSET_PATH#/kanglian-cloud}"
SOURCE_PORTAL_ASSET="${SOURCE_PORTAL}${PORTAL_ASSET_PATH#/cloud-hub}"
[[ -f "${SOURCE_DSMS_ASSET}" && -f "${SOURCE_PORTAL_ASSET}" ]] || {
  echo "index.html 引用的入口 JavaScript 不存在。" >&2; exit 1; }

if curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="https"
elif curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="http"
else
  echo "无法从本机 Nginx 读取博客首页；没有修改任何文件。" >&2; exit 1
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
        ACTIVE_MATCHES+=("${resolved_match}"); break
      fi
    done
  done
  [[ "${#ACTIVE_MATCHES[@]}" -eq 1 ]] && MATCHES=("${ACTIVE_MATCHES[0]}")
fi
[[ "${#MATCHES[@]}" -eq 1 ]] || { echo "无法唯一识别博客目录；匹配数：${#MATCHES[@]}" >&2; exit 1; }

SITE_ROOT="$(readlink -f "${MATCHES[0]}")"
[[ "${SITE_ROOT}" == /var/www/* ]] || { echo "博客目录不在 /var/www 下：${SITE_ROOT}" >&2; exit 1; }
sudo test -f "${SITE_ROOT}/blog-assets/app.js" || { echo "博客目录缺少 blog-assets/app.js。" >&2; exit 1; }

DSMS_ROOT="${SITE_ROOT}/kanglian-cloud"
PORTAL_ROOT="${SITE_ROOT}/cloud-hub"
BACKUP_DIR="/home/xiaoliu/backups/unified-frontends-${STAMP}"
DSMS_STAGE="${SITE_ROOT}/.kanglian-cloud-next-${STAMP}"
PORTAL_STAGE="${SITE_ROOT}/.cloud-hub-next-${STAMP}"
DSMS_OLD="${SITE_ROOT}/.kanglian-cloud-old-${STAMP}"
PORTAL_OLD="${SITE_ROOT}/.cloud-hub-old-${STAMP}"

echo "博客根目录保持不动：${SITE_ROOT}"
echo "DSMS 安装目录：${DSMS_ROOT}"
echo "统一门户安装目录：${PORTAL_ROOT}"
echo "备份目录：${BACKUP_DIR}"

sudo install -d -m 0700 "${BACKUP_DIR}"
if sudo test -d "${DSMS_ROOT}"; then HAD_DSMS=1; sudo cp -a "${DSMS_ROOT}" "${BACKUP_DIR}/kanglian-cloud-before"; fi
if sudo test -d "${PORTAL_ROOT}"; then HAD_PORTAL=1; sudo cp -a "${PORTAL_ROOT}" "${BACKUP_DIR}/cloud-hub-before"; fi

for pair in "${SOURCE_DSMS}|${DSMS_STAGE}" "${SOURCE_PORTAL}|${PORTAL_STAGE}"; do
  source_dir="${pair%%|*}"; stage_dir="${pair##*|}"
  sudo install -d -m 0755 "${stage_dir}"
  sudo cp -a "${source_dir}/." "${stage_dir}/"
  sudo find "${stage_dir}" -type d -exec chmod 0755 {} +
  sudo find "${stage_dir}" -type f -exec chmod 0644 {} +
  sudo cmp -s "${source_dir}/index.html" "${stage_dir}/index.html"
done

if [[ "${HAD_DSMS}" -eq 1 ]]; then sudo mv "${DSMS_ROOT}" "${DSMS_OLD}"; fi
sudo mv "${DSMS_STAGE}" "${DSMS_ROOT}"; DSMS_SWITCHED=1
if [[ "${HAD_PORTAL}" -eq 1 ]]; then sudo mv "${PORTAL_ROOT}" "${PORTAL_OLD}"; fi
sudo mv "${PORTAL_STAGE}" "${PORTAL_ROOT}"; PORTAL_SWITCHED=1

sudo nginx -t
fetch_local "/" "${VERIFY_HOME}"
fetch_local "/kanglian-cloud/" "${VERIFY_DSMS}"
fetch_local "/cloud-hub/" "${VERIFY_PORTAL}"
fetch_local "${DSMS_ASSET_PATH}" "${VERIFY_DSMS_ASSET}"
fetch_local "${PORTAL_ASSET_PATH}" "${VERIFY_PORTAL_ASSET}"
cmp -s "${PUBLIC_HOME}" "${VERIFY_HOME}"
cmp -s "${SOURCE_DSMS}/index.html" "${VERIFY_DSMS}"
cmp -s "${SOURCE_PORTAL}/index.html" "${VERIFY_PORTAL}"
cmp -s "${SOURCE_DSMS_ASSET}" "${VERIFY_DSMS_ASSET}"
cmp -s "${SOURCE_PORTAL_ASSET}" "${VERIFY_PORTAL_ASSET}"

DSMS_SWITCHED=0; PORTAL_SWITCHED=0; trap - ERR
[[ "${HAD_DSMS}" -eq 1 ]] && sudo test -d "${DSMS_OLD}" && sudo mv "${DSMS_OLD}" "${BACKUP_DIR}/kanglian-cloud-before-atomic"
[[ "${HAD_PORTAL}" -eq 1 ]] && sudo test -d "${PORTAL_OLD}" && sudo mv "${PORTAL_OLD}" "${BACKUP_DIR}/cloud-hub-before-atomic"
cleanup_temp; trap - EXIT

echo "UNIFIED_FRONTENDS_INSTALL_OK"
echo "SITE_ROOT=${SITE_ROOT}"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "已验证：博客首页未变化，DSMS 和统一门户首页及入口脚本与发布包逐字节一致。"
