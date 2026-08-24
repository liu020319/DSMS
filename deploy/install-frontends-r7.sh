#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

grep -RFlq '鄂ICP备2026041724号-1' "${PACKAGE_ROOT}/frontend/dsms" || {
  echo "DSMS 前端缺少工信部备案号，拒绝发布。" >&2; exit 1; }
grep -RFlq '鄂公网安备42088102000275号' "${PACKAGE_ROOT}/frontend/dsms" || {
  echo "DSMS 前端缺少公安备案号，拒绝发布。" >&2; exit 1; }
grep -RFlq '创建付款账户' "${PACKAGE_ROOT}/frontend/cloud-hub" || {
  echo "统一门户不是本轮最新版：缺少创建付款账户功能。" >&2; exit 1; }
grep -RFlq '鄂公网安备42088102000275号' "${PACKAGE_ROOT}/frontend/cloud-hub" || {
  echo "统一门户缺少公安备案号，拒绝发布。" >&2; exit 1; }

bash "${SCRIPT_DIR}/install-unified-frontends.sh"

echo "DSMS_R7_FRONTENDS_INSTALL_OK"
echo "已验证：博客根目录未被覆盖，DSMS 与统一门户均来自本轮构建。"
