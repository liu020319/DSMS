package com.medicine.service;

import com.medicine.entity.SysUser;
import com.medicine.entity.UserNotification;
import com.medicine.mapper.SysUserMapper;
import com.medicine.mapper.UserNotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    private NotificationService service;
    private UserNotificationMapper notificationMapper;
    private SysUserMapper userMapper;
    private JavaMailSender mailSender;
    private AtomicReference<UserNotification> stored;

    @BeforeEach
    void setUp() {
        service = new NotificationService();
        notificationMapper = mock(UserNotificationMapper.class);
        userMapper = mock(SysUserMapper.class);
        mailSender = mock(JavaMailSender.class);
        stored = new AtomicReference<>();

        when(notificationMapper.insert(any(UserNotification.class))).thenAnswer(invocation -> {
            UserNotification notification = invocation.getArgument(0);
            notification.setNotificationId(88L);
            stored.set(notification);
            return 1;
        });
        when(notificationMapper.selectById(88L)).thenAnswer(invocation -> stored.get());
        SysUser admin = new SysUser();
        admin.setUserId(1L);
        admin.setRole("ADMIN");
        when(userMapper.selectById(1L)).thenReturn(admin);

        ReflectionTestUtils.setField(service, "notificationMapper", notificationMapper);
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
        ReflectionTestUtils.setField(service, "mailSender", mailSender);
        ReflectionTestUtils.setField(service, "taskExecutor", (Executor) Runnable::run);
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        ReflectionTestUtils.setField(service, "defaultRecipient", "notify@example.com");
        ReflectionTestUtils.setField(service, "mailFrom", "sender@example.com");
    }

    @Test
    void enabledNotificationIsPersistedAndSentThroughDefaultRecipient() {
        service.notify(1L, "新咨询", "请登录查看", "SOFTWARE_SERVICE_INQUIRY", 9L);

        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(notificationMapper).updateById(stored.get());
        assertEquals("SENT", stored.get().getEmailStatus());
    }

    @Test
    void diagnosticsExposeOnlyConfigurationState() {
        Map<String, Object> result = service.mailDiagnostics();

        assertEquals(Boolean.TRUE, result.get("ready"));
        assertTrue(result.keySet().stream().noneMatch(key -> key.toLowerCase().contains("password")));
    }
}
