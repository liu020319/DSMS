#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BLOG_ROOT="${PACKAGE_ROOT}/frontend/blog"

grep -Fq '鄂ICP备2026041724号-1' "${BLOG_ROOT}/index.html" || {
  echo "博客产物缺少工信部备案号，拒绝发布。" >&2; exit 1; }
grep -Fq '鄂公网安备42088102000275号' "${BLOG_ROOT}/index.html" || {
  echo "博客产物缺少公安备案号，拒绝发布。" >&2; exit 1; }

bash "${SCRIPT_DIR}/install-blog-v18.sh"

echo "BLOG_R7_INSTALL_OK"
echo "已验证：正式博客包含工信部和公安备案号。"
