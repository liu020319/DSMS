#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_DSMS="${PACKAGE_ROOT}/frontend/dsms"
STAMP="$(date +%Y%m%d-%H%M%S)"
PUBLIC_HOME="$(mktemp)"
VERIFY_DSMS="$(mktemp)"
VERIFY_ASSET="$(mktemp)"
SITE_ROOT=""
DSMS_ROOT=""
STAGE=""
OLD=""
BACKUP_DIR=""
SWITCHED=0
HAD_OLD=0

cleanup_temp() { rm -f "${PUBLIC_HOME}" "${VERIFY_DSMS}" "${VERIFY_ASSET}"; }

fetch_local() {
  local path="$1" output="$2"
  if [[ "${PUBLIC_SCHEME}" == "https" ]]; then
    curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}${path}" -o "${output}"
  else
    curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}${path}" -o "${output}"
  fi
}

rollback_on_error() {
  local exit_code=$?
  trap - ERR
  set +e
  echo "R8 DSMS 前端安装失败，正在恢复旧目录……" >&2
  if [[ "${SWITCHED}" -eq 1 ]]; then
    sudo test -d "${DSMS_ROOT}" && sudo mv "${DSMS_ROOT}" "${BACKUP_DIR}/failed-kanglian-cloud-${STAMP}"
    sudo test -d "${OLD}" && sudo mv "${OLD}" "${DSMS_ROOT}"
  fi
  [[ -n "${STAGE}" ]] && sudo test -e "${STAGE}" && sudo rm -rf -- "${STAGE}"
  cleanup_temp
  echo "DSMS_R8_FRONTEND_INSTALL_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap cleanup_temp EXIT
trap rollback_on_error ERR

[[ -f "${SOURCE_DSMS}/index.html" ]] || { echo "发布包缺少 DSMS index.html。" >&2; exit 1; }
grep -Fq '/kanglian-cloud/assets/' "${SOURCE_DSMS}/index.html" || { echo "前端不是按 /kanglian-cloud/ 构建。" >&2; exit 1; }
if grep -Eq '(src|href)="/assets/' "${SOURCE_DSMS}/index.html"; then
  echo "前端包含错误根路径 /assets/。" >&2; exit 1
fi
ASSET_PATH="$(grep -oE '/kanglian-cloud/assets/[^" ]+\.js' "${SOURCE_DSMS}/index.html" | head -n 1)"
SOURCE_ASSET="${SOURCE_DSMS}${ASSET_PATH#/kanglian-cloud}"
[[ -n "${ASSET_PATH}" && -f "${SOURCE_ASSET}" ]] || { echo "入口脚本不存在。" >&2; exit 1; }

if curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="https"
elif curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="http"
else
  echo "无法读取博客首页；未修改服务器。" >&2; exit 1
fi

mapfile -d '' CANDIDATES < <(sudo find /var/www -maxdepth 5 -type f -name index.html -print0)
MATCHES=()
for candidate in "${CANDIDATES[@]}"; do
  sudo cmp -s "${candidate}" "${PUBLIC_HOME}" && MATCHES+=("$(dirname "${candidate}")")
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
[[ "${SITE_ROOT}" == /var/www/* ]] || { echo "博客目录不在 /var/www 下。" >&2; exit 1; }
sudo test -f "${SITE_ROOT}/blog-assets/app.js" || { echo "识别到的目录不是博客根目录。" >&2; exit 1; }

DSMS_ROOT="${SITE_ROOT}/kanglian-cloud"
STAGE="${SITE_ROOT}/.kanglian-cloud-next-${STAMP}"
OLD="${SITE_ROOT}/.kanglian-cloud-old-${STAMP}"
BACKUP_DIR="/home/xiaoliu/backups/dsms-frontend-r8-${STAMP}"

echo "博客首页保持不动：${SITE_ROOT}"
echo "仅替换 DSMS：${DSMS_ROOT}"
sudo install -d -m 0700 "${BACKUP_DIR}"
if sudo test -d "${DSMS_ROOT}"; then
  HAD_OLD=1
  sudo cp -a "${DSMS_ROOT}" "${BACKUP_DIR}/kanglian-cloud-before"
fi
sudo install -d -m 0755 "${STAGE}"
sudo cp -a "${SOURCE_DSMS}/." "${STAGE}/"
sudo find "${STAGE}" -type d -exec chmod 0755 {} +
sudo find "${STAGE}" -type f -exec chmod 0644 {} +
sudo cmp -s "${SOURCE_DSMS}/index.html" "${STAGE}/index.html"

[[ "${HAD_OLD}" -eq 0 ]] || sudo mv "${DSMS_ROOT}" "${OLD}"
sudo mv "${STAGE}" "${DSMS_ROOT}"
SWITCHED=1
sudo nginx -t
fetch_local "/kanglian-cloud/" "${VERIFY_DSMS}"
fetch_local "${ASSET_PATH}" "${VERIFY_ASSET}"
cmp -s "${SOURCE_DSMS}/index.html" "${VERIFY_DSMS}"
cmp -s "${SOURCE_ASSET}" "${VERIFY_ASSET}"

SWITCHED=0
trap - ERR
[[ "${HAD_OLD}" -eq 0 ]] || sudo mv "${OLD}" "${BACKUP_DIR}/kanglian-cloud-before-atomic"
cleanup_temp
trap - EXIT
echo "DSMS_R8_FRONTEND_INSTALL_OK"
echo "SITE_ROOT=${SITE_ROOT}"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "已验证：博客首页未替换，DSMS 首页及入口脚本与发布包一致。"
