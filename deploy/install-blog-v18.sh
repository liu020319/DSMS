#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SOURCE_BLOG="${PACKAGE_ROOT}/frontend/blog"
STAMP="$(date +%Y%m%d-%H%M%S)"
PUBLIC_HOME="$(mktemp)"
VERIFY_HOME="$(mktemp)"
SITE_ROOT=""
BACKUP_DIR=""
SWITCHED=0

cleanup() { rm -f "${PUBLIC_HOME}" "${VERIFY_HOME}"; }

rollback() {
  local exit_code=$?
  trap - ERR
  set +e
  if [[ "${SWITCHED}" -eq 1 && -n "${SITE_ROOT}" && -n "${BACKUP_DIR}" ]]; then
    echo "博客 V18 安装失败，正在恢复 3 个原文件……" >&2
    sudo cp -a "${BACKUP_DIR}/index.html" "${SITE_ROOT}/index.html"
    sudo cp -a "${BACKUP_DIR}/app.js" "${SITE_ROOT}/blog-assets/app.js"
    sudo cp -a "${BACKUP_DIR}/styles.css" "${SITE_ROOT}/blog-assets/styles.css"
  fi
  cleanup
  echo "BLOG_V18_INSTALL_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}

trap cleanup EXIT
trap rollback ERR

for required in index.html blog-assets/app.js blog-assets/styles.css; do
  [[ -f "${SOURCE_BLOG}/${required}" ]] || { echo "发布包缺少：frontend/blog/${required}" >&2; exit 1; }
done
grep -Fq 'styles.css?release=v19' "${SOURCE_BLOG}/index.html" || { echo "index.html 不是本轮 V19 博客产物，拒绝发布。" >&2; exit 1; }
grep -Fq 'SYSTEM CONSTELLATION' "${SOURCE_BLOG}/blog-assets/app.js" || { echo "app.js 缺少系统星图，拒绝发布。" >&2; exit 1; }

if curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  SCHEME="https"; PORT=443
elif curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}/" -o "${PUBLIC_HOME}"; then
  SCHEME="http"; PORT=80
else
  echo "本机 Nginx 无法读取博客首页；没有修改任何文件。" >&2; exit 1
fi

mapfile -d '' CANDIDATES < <(sudo find /var/www -maxdepth 5 -type f -name index.html -print0)
MATCHES=()
for candidate in "${CANDIDATES[@]}"; do
  if sudo cmp -s "${candidate}" "${PUBLIC_HOME}"; then MATCHES+=("$(dirname "${candidate}")"); fi
done
[[ "${#MATCHES[@]}" -eq 1 ]] || { echo "无法唯一识别当前博客根目录；匹配数：${#MATCHES[@]}" >&2; exit 1; }
SITE_ROOT="$(readlink -f "${MATCHES[0]}")"
[[ "${SITE_ROOT}" == /var/www/* ]] || { echo "博客根目录不在 /var/www：${SITE_ROOT}" >&2; exit 1; }
sudo test -f "${SITE_ROOT}/blog-assets/app.js"
sudo test -d "${SITE_ROOT}/kanglian-cloud"

BACKUP_DIR="/home/xiaoliu/backups/blog-v18-${STAMP}"
sudo install -d -m 0700 "${BACKUP_DIR}"
sudo cp -a "${SITE_ROOT}/index.html" "${BACKUP_DIR}/index.html"
sudo cp -a "${SITE_ROOT}/blog-assets/app.js" "${BACKUP_DIR}/app.js"
sudo cp -a "${SITE_ROOT}/blog-assets/styles.css" "${BACKUP_DIR}/styles.css"

sudo install -m 0644 "${SOURCE_BLOG}/index.html" "${SITE_ROOT}/.index-v18-next-${STAMP}"
sudo install -m 0644 "${SOURCE_BLOG}/blog-assets/app.js" "${SITE_ROOT}/blog-assets/.app-v18-next-${STAMP}"
sudo install -m 0644 "${SOURCE_BLOG}/blog-assets/styles.css" "${SITE_ROOT}/blog-assets/.styles-v18-next-${STAMP}"
sudo cmp -s "${SOURCE_BLOG}/index.html" "${SITE_ROOT}/.index-v18-next-${STAMP}"
sudo cmp -s "${SOURCE_BLOG}/blog-assets/app.js" "${SITE_ROOT}/blog-assets/.app-v18-next-${STAMP}"
sudo cmp -s "${SOURCE_BLOG}/blog-assets/styles.css" "${SITE_ROOT}/blog-assets/.styles-v18-next-${STAMP}"

SWITCHED=1
sudo mv "${SITE_ROOT}/.index-v18-next-${STAMP}" "${SITE_ROOT}/index.html"
sudo mv "${SITE_ROOT}/blog-assets/.app-v18-next-${STAMP}" "${SITE_ROOT}/blog-assets/app.js"
sudo mv "${SITE_ROOT}/blog-assets/.styles-v18-next-${STAMP}" "${SITE_ROOT}/blog-assets/styles.css"
sudo nginx -t

if [[ "${SCHEME}" == "https" ]]; then
  curl -kfsSL --resolve "${DOMAIN}:${PORT}:127.0.0.1" "${SCHEME}://${DOMAIN}/" -o "${VERIFY_HOME}"
else
  curl -fsSL --resolve "${DOMAIN}:${PORT}:127.0.0.1" "${SCHEME}://${DOMAIN}/" -o "${VERIFY_HOME}"
fi
cmp -s "${SOURCE_BLOG}/index.html" "${VERIFY_HOME}"
sudo cmp -s "${SOURCE_BLOG}/blog-assets/app.js" "${SITE_ROOT}/blog-assets/app.js"
sudo cmp -s "${SOURCE_BLOG}/blog-assets/styles.css" "${SITE_ROOT}/blog-assets/styles.css"

SWITCHED=0; trap - ERR
cleanup; trap - EXIT
echo "BLOG_V18_INSTALL_OK"
echo "SITE_ROOT=${SITE_ROOT}"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "只更新了 index.html、blog-assets/app.js、blog-assets/styles.css；DSMS 与统一门户目录未改动。"
