#!/usr/bin/env bash

set -Eeuo pipefail

DOMAIN="xiaoliudev.com"
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf -- "${TEMP_DIR}"' EXIT

fetch_local() {
  local path="$1" output="$2"
  if curl -kfsSL --resolve "${DOMAIN}:443:127.0.0.1" "https://${DOMAIN}${path}" -o "${output}"; then
    return 0
  fi
  curl -fsSL --resolve "${DOMAIN}:80:127.0.0.1" "http://${DOMAIN}${path}" -o "${output}"
}

echo "1/6 检查服务状态和后端接口……"
sudo systemctl is-active --quiet dsms-backend
sudo systemctl is-active --quiet personal-blog-resume
sudo systemctl is-active --quiet nginx
curl -fsS http://127.0.0.1:8088/api/auth/human-challenge > "${TEMP_DIR}/challenge.json"
grep -Fq 'challengeId' "${TEMP_DIR}/challenge.json"

echo "2/6 检查三个正式入口……"
fetch_local "/" "${TEMP_DIR}/blog.html"
fetch_local "/kanglian-cloud/" "${TEMP_DIR}/dsms.html"
fetch_local "/cloud-hub/" "${TEMP_DIR}/portal.html"
grep -Fq 'blog-assets' "${TEMP_DIR}/blog.html"
grep -Fq '/kanglian-cloud/assets/' "${TEMP_DIR}/dsms.html"
grep -Fq '/cloud-hub/assets/' "${TEMP_DIR}/portal.html"

echo "3/6 检查本轮入口脚本真实可访问……"
DSMS_ASSET="$(grep -oE '/kanglian-cloud/assets/[^" ]+\.js' "${TEMP_DIR}/dsms.html" | head -n 1)"
PORTAL_ASSET="$(grep -oE '/cloud-hub/assets/[^" ]+\.js' "${TEMP_DIR}/portal.html" | head -n 1)"
[[ -n "${DSMS_ASSET}" && -n "${PORTAL_ASSET}" ]]
fetch_local "${DSMS_ASSET}" "${TEMP_DIR}/dsms.js"
fetch_local "${PORTAL_ASSET}" "${TEMP_DIR}/portal.js"

echo "4/6 检查备案和新版功能标记……"
grep -Fq '鄂ICP备2026041724号-1' "${TEMP_DIR}/blog.html"
grep -Fq '鄂公网安备42088102000275号' "${TEMP_DIR}/blog.html"
sudo grep -RFlq '创建付款账户' /var/www
sudo grep -RFlq '鄂公网安备42088102000275号' /var/www
sudo grep -RFlq '保存物流并提醒安心用药端' /var/www

echo "5/6 检查邮件配置存在性（不输出密码）……"
for setting in '^MAIL_ENABLED=true[[:space:]]*$' '^MAIL_USERNAME=.+' '^MAIL_APP_PASSWORD=.+' '^MAIL_RECIPIENT=.+'; do
  sudo grep -Eq "${setting}" /etc/dsms-backend.env
done
for setting in '^BLOG_EMAIL_NOTIFICATIONS=true[[:space:]]*$' '^BLOG_SMTP_USERNAME=.+' '^BLOG_SMTP_PASSWORD=.+' '^BLOG_NOTIFY_EMAIL=.+'; do
  sudo grep -Eq "${setting}" /etc/personal-blog-resume.env
done

echo "6/6 检查 Nginx 与公网 HTTPS……"
sudo nginx -t
curl -fsSI "https://${DOMAIN}/" >/dev/null
curl -fsSI "https://${DOMAIN}/kanglian-cloud/" >/dev/null
curl -fsSI "https://${DOMAIN}/cloud-hub/" >/dev/null

echo "DSMS_FORMAL_R7_VERIFY_OK"
echo "dsms-backend=$(sudo systemctl is-active dsms-backend)"
echo "personal-blog-resume=$(sudo systemctl is-active personal-blog-resume)"
echo "nginx=$(sudo systemctl is-active nginx)"
