#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
DSMS_ENV="/etc/dsms-backend.env"
BLOG_ENV="/etc/personal-blog-resume.env"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf -- "${TEMP_DIR}"' EXIT

fetch_local() {
  local path="$1"
  local output="$2"
  if curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}${path}" -o "${output}"; then
    return 0
  fi
  curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}${path}" -o "${output}"
}

echo "1/7 检查两个后端服务……"
sudo systemctl is-active --quiet dsms-backend
sudo systemctl is-active --quiet personal-blog-resume
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge > "${TEMP_DIR}/challenge.json"
curl -fsS http://127.0.0.1:8091/health > "${TEMP_DIR}/blog-health.json"
grep -Fq 'challengeId' "${TEMP_DIR}/challenge.json"

echo "2/7 检查邮件环境变量（只检查存在性，不输出秘密）……"
for setting in '^MAIL_ENABLED=true[[:space:]]*$' '^MAIL_USERNAME=.+' '^MAIL_APP_PASSWORD=.+' '^MAIL_RECIPIENT=.+'; do
  sudo grep -Eq "${setting}" "${DSMS_ENV}"
done
for setting in '^BLOG_EMAIL_NOTIFICATIONS=true[[:space:]]*$' '^BLOG_SMTP_USERNAME=.+' '^BLOG_SMTP_PASSWORD=.+' '^BLOG_NOTIFY_EMAIL=.+'; do
  sudo grep -Eq "${setting}" "${BLOG_ENV}"
done

echo "3/7 检查博客、DSMS、统一门户三个入口……"
fetch_local "/" "${TEMP_DIR}/blog.html"
fetch_local "/kanglian-cloud/" "${TEMP_DIR}/dsms.html"
fetch_local "/cloud-hub/" "${TEMP_DIR}/portal.html"
grep -Fq 'blog-assets' "${TEMP_DIR}/blog.html"
grep -Fq '/kanglian-cloud/assets/' "${TEMP_DIR}/dsms.html"
grep -Fq '/cloud-hub/assets/' "${TEMP_DIR}/portal.html"

echo "4/7 检查两个前端入口脚本可访问……"
DSMS_ASSET="$(grep -oE '/kanglian-cloud/assets/[^" ]+\.js' "${TEMP_DIR}/dsms.html" | head -n 1)"
PORTAL_ASSET="$(grep -oE '/cloud-hub/assets/[^" ]+\.js' "${TEMP_DIR}/portal.html" | head -n 1)"
[[ -n "${DSMS_ASSET}" && -n "${PORTAL_ASSET}" ]]
fetch_local "${DSMS_ASSET}" "${TEMP_DIR}/dsms.js"
fetch_local "${PORTAL_ASSET}" "${TEMP_DIR}/portal.js"

echo "5/7 检查新版功能标记……"
grep -Fq '费用凭证档案' "${TEMP_DIR}/dsms.js" || \
  sudo grep -RFlq '费用凭证档案' /var/www
grep -Fq '一个入口，连接生活、健康与创造' "${TEMP_DIR}/portal.js" || \
  sudo grep -RFlq '一个入口，连接生活、健康与创造' /var/www
grep -Fq '邮件提醒链路' "${TEMP_DIR}/portal.js" || \
  sudo grep -RFlq '邮件提醒链路' /var/www

echo "6/7 检查 Nginx 配置与公网状态码……"
sudo nginx -t
curl -fsSI "https://${DOMAIN}/" >/dev/null
curl -fsSI "https://${DOMAIN}/kanglian-cloud/" >/dev/null
curl -fsSI "https://${DOMAIN}/cloud-hub/" >/dev/null

echo "7/7 输出不含秘密的服务摘要……"
echo "dsms-backend=$(sudo systemctl is-active dsms-backend)"
echo "personal-blog-resume=$(sudo systemctl is-active personal-blog-resume)"
echo "nginx=$(sudo systemctl is-active nginx)"
echo "FULL_PLATFORM_R6_VERIFY_OK"
