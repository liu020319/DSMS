#!/usr/bin/env bash

set -Eeuo pipefail

DSMS_ENV="/etc/dsms-backend.env"
BLOG_ENV="/etc/personal-blog-resume.env"
STAMP="$(date +%Y%m%d-%H%M%S)"
BACKUP_DIR="/home/xiaoliu/backups/mail-config-r6-${STAMP}"
DSMS_TEMP="$(mktemp)"
BLOG_TEMP="$(mktemp)"
CONFIG_INSTALLED=0

cleanup() {
  rm -f -- "${DSMS_TEMP}" "${BLOG_TEMP}"
  unset MAIL_SENDER MAIL_RECIPIENT_INPUT MAIL_APP_PASSWORD_INPUT
}
trap cleanup EXIT

rollback_on_error() {
  local exit_code="${1:-$?}"
  trap - ERR INT TERM
  set +e
  if [[ "${CONFIG_INSTALLED}" -eq 1 ]]; then
    echo "邮件配置未通过验证，正在恢复修改前的两份环境文件……" >&2
    sudo install -o root -g root -m 0600 "${BACKUP_DIR}/dsms-backend.env.before" "${DSMS_ENV}"
    sudo install -o root -g root -m 0600 "${BACKUP_DIR}/personal-blog-resume.env.before" "${BLOG_ENV}"
    sudo systemctl restart dsms-backend
    sudo systemctl restart personal-blog-resume
  fi
  echo "MAIL_CONFIG_R6_FAILED；退出码：${exit_code}" >&2
  exit "${exit_code}"
}
trap 'rollback_on_error $?' ERR
trap 'rollback_on_error 130' INT
trap 'rollback_on_error 143' TERM

for env_file in "${DSMS_ENV}" "${BLOG_ENV}"; do
  if ! sudo test -f "${env_file}"; then
    echo "缺少环境文件：${env_file}；没有修改任何配置。" >&2
    exit 1
  fi
done

read -r -p "请输入 Gmail 发件邮箱：" MAIL_SENDER
read -r -p "请输入接收提醒的邮箱（直接回车则与发件邮箱相同）：" MAIL_RECIPIENT_INPUT
read -r -s -p "请输入 Gmail 应用专用密码（输入内容不会显示）：" MAIL_APP_PASSWORD_INPUT
echo

MAIL_SENDER="${MAIL_SENDER//[[:space:]]/}"
MAIL_RECIPIENT_INPUT="${MAIL_RECIPIENT_INPUT//[[:space:]]/}"
MAIL_APP_PASSWORD_INPUT="${MAIL_APP_PASSWORD_INPUT//[[:space:]]/}"
[[ -n "${MAIL_RECIPIENT_INPUT}" ]] || MAIL_RECIPIENT_INPUT="${MAIL_SENDER}"

if [[ ! "${MAIL_SENDER}" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]] || \
   [[ ! "${MAIL_RECIPIENT_INPUT}" =~ ^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$ ]]; then
  echo "邮箱格式不正确；没有修改任何配置。" >&2
  exit 1
fi
if [[ "${#MAIL_APP_PASSWORD_INPUT}" -lt 12 ]]; then
  echo "应用专用密码长度异常；请使用 Gmail 生成的应用专用密码。" >&2
  exit 1
fi

sudo install -d -m 0700 "${BACKUP_DIR}"
sudo cp -a "${DSMS_ENV}" "${BACKUP_DIR}/dsms-backend.env.before"
sudo cp -a "${BLOG_ENV}" "${BACKUP_DIR}/personal-blog-resume.env.before"

sudo cat "${DSMS_ENV}" | awk '
  !/^(MAIL_ENABLED|MAIL_HOST|MAIL_PORT|MAIL_USERNAME|MAIL_APP_PASSWORD|MAIL_RECIPIENT|MAIL_TO)=/ { print }
' > "${DSMS_TEMP}"
printf '%s\n' \
  'MAIL_ENABLED=true' \
  'MAIL_HOST=smtp.gmail.com' \
  'MAIL_PORT=587' \
  "MAIL_USERNAME=${MAIL_SENDER}" \
  "MAIL_APP_PASSWORD=${MAIL_APP_PASSWORD_INPUT}" \
  "MAIL_RECIPIENT=${MAIL_RECIPIENT_INPUT}" >> "${DSMS_TEMP}"

sudo cat "${BLOG_ENV}" | awk '
  !/^(BLOG_EMAIL_NOTIFICATIONS|BLOG_NOTIFY_EMAIL|BLOG_SMTP_HOST|BLOG_SMTP_PORT|BLOG_SMTP_USERNAME|BLOG_SMTP_PASSWORD|BLOG_SMTP_STARTTLS|BLOG_SMTP_SSL)=/ { print }
' > "${BLOG_TEMP}"
printf '%s\n' \
  'BLOG_EMAIL_NOTIFICATIONS=true' \
  "BLOG_NOTIFY_EMAIL=${MAIL_RECIPIENT_INPUT}" \
  'BLOG_SMTP_HOST=smtp.gmail.com' \
  'BLOG_SMTP_PORT=587' \
  "BLOG_SMTP_USERNAME=${MAIL_SENDER}" \
  "BLOG_SMTP_PASSWORD=${MAIL_APP_PASSWORD_INPUT}" \
  'BLOG_SMTP_STARTTLS=true' \
  'BLOG_SMTP_SSL=false' >> "${BLOG_TEMP}"

sudo install -o root -g root -m 0600 "${DSMS_TEMP}" "${DSMS_ENV}"
sudo install -o root -g root -m 0600 "${BLOG_TEMP}" "${BLOG_ENV}"
CONFIG_INSTALLED=1

sudo systemctl restart dsms-backend
sudo systemctl restart personal-blog-resume

for attempt in $(seq 1 45); do
  if sudo systemctl is-active --quiet dsms-backend && \
     curl -fsS http://127.0.0.1:8088/api/auth/human-challenge >/dev/null 2>&1; then
    break
  fi
  [[ "${attempt}" -lt 45 ]] || { echo "DSMS 后端重启后未就绪。" >&2; false; }
  sleep 2
done
for attempt in $(seq 1 20); do
  if sudo systemctl is-active --quiet personal-blog-resume && \
     curl -fsS http://127.0.0.1:8091/health >/dev/null 2>&1; then
    break
  fi
  [[ "${attempt}" -lt 20 ]] || { echo "博客数据服务重启后未就绪。" >&2; false; }
  sleep 1
done

BLOG_TOKEN="$(sudo sed -n 's/^RESUME_ADMIN_TOKEN=//p' "${BLOG_ENV}" | tail -n 1)"
[[ "${#BLOG_TOKEN}" -ge 32 ]] || { echo "博客管理口令异常，无法执行邮件测试。" >&2; false; }
curl -fsS -X POST -H "Authorization: Bearer ${BLOG_TOKEN}" \
  http://127.0.0.1:8091/admin/email/test >/dev/null
unset BLOG_TOKEN
CONFIG_INSTALLED=0
trap - ERR INT TERM

echo "MAIL_CONFIG_R6_OK"
echo "BACKUP_DIR=${BACKUP_DIR}"
echo "博客测试邮件已提交；DSMS 请登录统一门户管理员工作台点击‘发送测试邮件’完成独立验证。"
