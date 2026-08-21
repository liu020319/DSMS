#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_DIST="${PACKAGE_ROOT}/frontend/dist"
STAMP="$(date +%Y%m%d-%H%M%S)"
PUBLIC_HOME="$(mktemp)"
VERIFY_HOME="$(mktemp)"
VERIFY_DSMS="$(mktemp)"
VERIFY_ASSET="$(mktemp)"
SITE_ROOT=""
DSMS_ROOT=""
BACKUP_DIR=""
STAGE_DIR=""
OLD_DIR=""
SWITCHED=0
HAD_OLD_DSMS=0

cleanup_temp() {
  rm -f "${PUBLIC_HOME}" "${VERIFY_HOME}" "${VERIFY_DSMS}" "${VERIFY_ASSET}"
}

fetch_local() {
  local path="$1"
  local output="$2"

  if [[ "${PUBLIC_SCHEME}" == "https" ]]; then
    curl -kfsSL \
      --resolve "${DOMAIN}:443:127.0.0.1" \
      "https://${DOMAIN}${path}" \
      -o "${output}"
  else
    curl -fsSL \
      --resolve "${DOMAIN}:80:127.0.0.1" \
      "http://${DOMAIN}${path}" \
      -o "${output}"
  fi
}

rollback_on_error() {
  local exit_code=$?
  trap - ERR
  set +e

  echo "DSMS 前端安装失败，正在恢复切换前目录……" >&2
  if [[ "${SWITCHED}" -eq 1 && -n "${DSMS_ROOT}" && -n "${BACKUP_DIR}" ]]; then
    if sudo test -d "${DSMS_ROOT}"; then
      sudo mv "${DSMS_ROOT}" "${BACKUP_DIR}/failed-kanglian-cloud-${STAMP}"
    fi
    if [[ "${HAD_OLD_DSMS}" -eq 1 && -n "${OLD_DIR}" ]] && sudo test -d "${OLD_DIR}"; then
      sudo mv "${OLD_DIR}" "${DSMS_ROOT}"
    elif sudo test -d "${BACKUP_DIR}/kanglian-cloud-before"; then
      sudo cp -a "${BACKUP_DIR}/kanglian-cloud-before" "${DSMS_ROOT}"
    fi
  fi

  if [[ -n "${STAGE_DIR}" ]] && sudo test -e "${STAGE_DIR}"; then
    sudo rm -rf -- "${STAGE_DIR}"
  fi
  cleanup_temp
  echo "DSMS_OBS_FRONTEND_INSTALL_FAILED；已尝试恢复原目录。退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap cleanup_temp EXIT
trap rollback_on_error ERR

for required_file in \
  "${SOURCE_DIST}/index.html" \
  "${SOURCE_DIST}/assets/index-d629ae6a.css"; do
  if [[ ! -f "${required_file}" ]]; then
    echo "发布包不完整，缺少：${required_file}" >&2
    exit 1
  fi
done

if ! grep -Fq '/kanglian-cloud/assets/' "${SOURCE_DIST}/index.html"; then
  echo "前端不是按 /kanglian-cloud/ 基路径构建，拒绝发布。" >&2
  exit 1
fi
if grep -Eq '(src|href)="/assets/' "${SOURCE_DIST}/index.html"; then
  echo "前端仍包含错误的根路径 /assets/，拒绝发布。" >&2
  exit 1
fi

ASSET_PATH="$(grep -oE '/kanglian-cloud/assets/[^" ]+\.js' "${SOURCE_DIST}/index.html" | head -n 1)"
if [[ -z "${ASSET_PATH}" ]]; then
  echo "无法从 index.html 提取入口 JavaScript 路径。" >&2
  exit 1
fi
SOURCE_ASSET="${SOURCE_DIST}${ASSET_PATH#/kanglian-cloud}"
if [[ ! -f "${SOURCE_ASSET}" ]]; then
  echo "index.html 引用的入口文件不存在：${SOURCE_ASSET}" >&2
  exit 1
fi

if curl -kfsSL \
  --resolve "${DOMAIN}:443:127.0.0.1" \
  "https://${DOMAIN}/" \
  -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="https"
elif curl -fsSL \
  --resolve "${DOMAIN}:80:127.0.0.1" \
  "http://${DOMAIN}/" \
  -o "${PUBLIC_HOME}"; then
  PUBLIC_SCHEME="http"
else
  echo "无法通过本机 Nginx 读取个人博客首页，未修改任何文件。" >&2
  exit 1
fi

mapfile -d '' CANDIDATES < <(sudo find /var/www -maxdepth 5 -type f -name index.html -print0)
MATCHES=()
for candidate in "${CANDIDATES[@]}"; do
  if sudo cmp -s "${candidate}" "${PUBLIC_HOME}"; then
    MATCHES+=("$(dirname "${candidate}")")
  fi
done

if [[ "${#MATCHES[@]}" -gt 1 ]]; then
  mapfile -t CONFIGURED_ROOTS < <(
    sudo nginx -T 2>/dev/null |
      awk '$1 == "root" { gsub(/;/, "", $2); print $2 }' |
      sort -u
  )
  ACTIVE_MATCHES=()
  for match in "${MATCHES[@]}"; do
    resolved_match="$(readlink -f "${match}")"
    for configured_root in "${CONFIGURED_ROOTS[@]}"; do
      if resolved_root="$(readlink -f "${configured_root}" 2>/dev/null)" && \
        [[ "${resolved_match}" == "${resolved_root}" ]]; then
        ACTIVE_MATCHES+=("${resolved_match}")
        break
      fi
    done
  done
  if [[ "${#ACTIVE_MATCHES[@]}" -eq 1 ]]; then
    MATCHES=("${ACTIVE_MATCHES[0]}")
  fi
fi

if [[ "${#MATCHES[@]}" -ne 1 ]]; then
  echo "无法唯一识别个人博客真实目录，匹配数量：${#MATCHES[@]}。未修改任何文件。" >&2
  if [[ "${#MATCHES[@]}" -gt 0 ]]; then
    printf '候选目录：%s\n' "${MATCHES[@]}" >&2
  fi
  exit 1
fi

SITE_ROOT="$(readlink -f "${MATCHES[0]}")"
if [[ "${SITE_ROOT}" != /var/www/* ]]; then
  echo "识别出的博客目录不在 /var/www/ 下，拒绝继续：${SITE_ROOT}" >&2
  exit 1
fi
if ! sudo test -f "${SITE_ROOT}/blog-assets/app.js"; then
  echo "识别目录缺少个人博客资源 blog-assets/app.js，拒绝继续。" >&2
  exit 1
fi

DSMS_ROOT="${SITE_ROOT}/kanglian-cloud"
BACKUP_DIR="/home/xiaoliu/backups/dsms-obs-frontend-${STAMP}"
STAGE_DIR="${SITE_ROOT}/.kanglian-cloud-next-${STAMP}"
OLD_DIR="${SITE_ROOT}/.kanglian-cloud-old-${STAMP}"

echo "个人博客目录：${SITE_ROOT}"
echo "DSMS 只会安装到：${DSMS_ROOT}"
echo "备份目录：${BACKUP_DIR}"

sudo install -d -m 0700 "${BACKUP_DIR}"
if sudo test -d "${DSMS_ROOT}"; then
  HAD_OLD_DSMS=1
  sudo cp -a "${DSMS_ROOT}" "${BACKUP_DIR}/kanglian-cloud-before"
fi

sudo install -d -m 0755 "${STAGE_DIR}"
sudo cp -a "${SOURCE_DIST}/." "${STAGE_DIR}/"
sudo find "${STAGE_DIR}" -type d -exec chmod 0755 {} +
sudo find "${STAGE_DIR}" -type f -exec chmod 0644 {} +
sudo cmp -s "${SOURCE_DIST}/index.html" "${STAGE_DIR}/index.html"

if [[ "${HAD_OLD_DSMS}" -eq 1 ]]; then
  sudo mv "${DSMS_ROOT}" "${OLD_DIR}"
fi
sudo mv "${STAGE_DIR}" "${DSMS_ROOT}"
SWITCHED=1

sudo nginx -t

fetch_local "/" "${VERIFY_HOME}"
fetch_local "/kanglian-cloud/" "${VERIFY_DSMS}"
fetch_local "${ASSET_PATH}" "${VERIFY_ASSET}"

cmp -s "${PUBLIC_HOME}" "${VERIFY_HOME}"
cmp -s "${SOURCE_DIST}/index.html" "${VERIFY_DSMS}"
cmp -s "${SOURCE_ASSET}" "${VERIFY_ASSET}"

SWITCHED=0
trap - ERR
if [[ "${HAD_OLD_DSMS}" -eq 1 ]] && sudo test -d "${OLD_DIR}"; then
  sudo mv "${OLD_DIR}" "${BACKUP_DIR}/kanglian-cloud-before-atomic"
fi

cleanup_temp
trap - EXIT

echo "DSMS_OBS_FRONTEND_INSTALL_OK"
echo "SITE_ROOT=${SITE_ROOT}"
echo "DSMS_ROOT=${DSMS_ROOT}"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "已验证：博客首页未改变，DSMS 首页和入口 JavaScript 与发布包逐字节一致。"
