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
import java.util.concurrent.Executor;

@Service
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
            Runnable dispatch = () -> taskExecutor.execute(() -> sendEmail(n.getNotificationId()));
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
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(recipient);
                message.setSubject("【安心用药】" + n.getTitle());
                message.setText(n.getContent() + "\n\n请登录家庭慢病用药管理系统查看详情。");
                mailSender.send(message);
                n.setEmailStatus("SENT");
                n.setEmailError(null);
            }
        } catch (Exception ex) {
            n.setEmailStatus("FAILED");
            n.setEmailError(ex.getMessage() == null ? "邮件发送失败" : ex.getMessage().substring(0, Math.min(500, ex.getMessage().length())));
        }
        notificationMapper.updateById(n);
    }
}
