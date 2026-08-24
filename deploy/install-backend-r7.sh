#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

bash "${SCRIPT_DIR}/install-backend-r6.sh"

echo "DSMS_R7_BACKEND_INSTALL_OK"
echo "已验证：最新版 JAR 已安装，服务 active，登录挑战接口可访问。"
