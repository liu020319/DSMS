package com.medicine.service;

import com.medicine.entity.SysUser;
import com.medicine.entity.UserNotification;
import com.medicine.mapper.SysUserMapper;
import com.medicine.mapper.UserNotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationService {
    @Autowired private UserNotificationMapper notificationMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired(required = false) private JavaMailSender mailSender;
    @Autowired @Qualifier("notificationExecutor") private Executor taskExecutor;
    @Value("${notification.mail.enabled:false}") private boolean mailEnabled;
    @Value("${notification.mail.default-recipient:}") private String defaultRecipient;
    @Value("${spring.mail.username:}") private String mailFrom;

    public UserNotification notify(Long recipientId, String title, String content, String bizType, Long bizId) {
        UserNotification n = new UserNotification();
        n.setRecipientId(recipientId);
        n.setTitle(title);
        n.setContent(content);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setReadStatus(0);
        n.setEmailStatus(mailEnabled ? "PENDING" : "DISABLED");
        notificationMapper.insert(n);
        if (mailEnabled) {
            Runnable dispatch = () -> {
                try {
                    taskExecutor.execute(() -> sendEmail(n.getNotificationId()));
                } catch (RuntimeException ex) {
                    markDeliveryFailure(n.getNotificationId(), "邮件任务队列暂时不可用");
                    log.error("Failed to queue notification email, notificationId={}", n.getNotificationId(), ex);
                }
            };
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { dispatch.run(); }
                });
            } else {
                dispatch.run();
            }
        }
        return n;
    }

    public void sendEmail(Long notificationId) {
        UserNotification n = notificationMapper.selectById(notificationId);
        if (n == null) return;
        try {
            SysUser user = userMapper.selectById(n.getRecipientId());
            String recipient = user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()
                    ? user.getEmail().trim()
                    : (user != null && ("ADMIN".equals(user.getRole()) || "GUARDIAN".equals(user.getRole()))
                    ? defaultRecipient : null);
            if (mailSender == null || mailFrom == null || mailFrom.trim().isEmpty()
                    || recipient == null || recipient.trim().isEmpty()) {
                n.setEmailStatus("SKIPPED");
                n.setEmailError("邮件发送未启用或缺少 MAIL_USERNAME/收件邮箱");
            } else {
                boolean portalNotification = n.getBizType() != null
                        && n.getBizType().startsWith("SOFTWARE_SERVICE_");
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(recipient);
                message.setSubject((portalNotification ? "【小刘云】" : "【安心用药】") + n.getTitle());
                message.setText(n.getContent() + (portalNotification
                        ? "\n\n请登录小刘云软件服务中心查看详情。"
                        : "\n\n请登录家庭慢病用药管理系统查看详情。"));
                sendWithRetry(message, notificationId);
                n.setEmailStatus("SENT");
                n.setEmailError(null);
            }
        } catch (Exception ex) {
            n.setEmailStatus("FAILED");
            n.setEmailError(ex.getMessage() == null ? "邮件发送失败" : ex.getMessage().substring(0, Math.min(500, ex.getMessage().length())));
            log.error("Notification email failed, notificationId={}, errorType={}",
                    notificationId, ex.getClass().getSimpleName());
        }
        notificationMapper.updateById(n);
    }

    public Map<String, Object> mailDiagnostics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", mailEnabled);
        result.put("senderConfigured", mailFrom != null && !mailFrom.trim().isEmpty());
        result.put("recipientConfigured", defaultRecipient != null && !defaultRecipient.trim().isEmpty());
        result.put("ready", mailEnabled && mailSender != null
                && mailFrom != null && !mailFrom.trim().isEmpty()
                && defaultRecipient != null && !defaultRecipient.trim().isEmpty());
        return result;
    }

    public Map<String, Object> sendTestEmail(Long recipientId) {
        UserNotification notification = new UserNotification();
        notification.setRecipientId(recipientId);
        notification.setTitle("邮件提醒链路测试");
        notification.setContent("如果你收到这封邮件，说明 DSMS 的 SMTP 配置、异步通知和收件地址均可用。");
        notification.setBizType("SOFTWARE_SERVICE_MAIL_TEST");
        notification.setReadStatus(0);
        notification.setEmailStatus(mailEnabled ? "PENDING" : "DISABLED");
        notificationMapper.insert(notification);
        if (mailEnabled) sendEmail(notification.getNotificationId());
        UserNotification latest = notificationMapper.selectById(notification.getNotificationId());
        Map<String, Object> result = mailDiagnostics();
        result.put("notificationId", notification.getNotificationId());
        result.put("emailStatus", latest == null ? notification.getEmailStatus() : latest.getEmailStatus());
        result.put("emailError", latest != null && "FAILED".equals(latest.getEmailStatus())
                ? "发送失败，请查看服务器日志" : (latest == null ? null : latest.getEmailError()));
        return result;
    }

    private void sendWithRetry(SimpleMailMessage message, Long notificationId) {
        RuntimeException lastFailure = null;
        long[] waits = {0L, 1000L, 3000L};
        for (int attempt = 0; attempt < waits.length; attempt++) {
            if (waits[attempt] > 0) {
                try {
                    Thread.sleep(waits[attempt]);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("邮件发送线程被中断", ex);
                }
            }
            try {
                mailSender.send(message);
                return;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                log.warn("Notification email attempt failed, notificationId={}, attempt={}, errorType={}",
                        notificationId, attempt + 1, ex.getClass().getSimpleName());
            }
        }
        throw lastFailure == null ? new IllegalStateException("邮件发送失败") : lastFailure;
    }

    private void markDeliveryFailure(Long notificationId, String reason) {
        UserNotification notification = notificationMapper.selectById(notificationId);
        if (notification == null) return;
        notification.setEmailStatus("FAILED");
        notification.setEmailError(reason);
        notificationMapper.updateById(notification);
    }
}
