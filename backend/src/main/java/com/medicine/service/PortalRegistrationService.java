package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.dto.PortalRegisterDTO;
import com.medicine.dto.RegisterDTO;
import com.medicine.entity.SysUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PortalRegistrationService {
    private static final int MAX_REGISTRATIONS_PER_HOUR = 8;

    private final SysUserService sysUserService;
    private final HumanVerificationService verificationService;
    private final Map<String, RateWindow> registrationRates = new ConcurrentHashMap<>();

    @Value("${portal.registration.enabled:false}")
    private boolean enabled;
    @Value("${portal.registration.invite-code:}")
    private String configuredInviteCode;

    public PortalRegistrationService(SysUserService sysUserService,
                                     HumanVerificationService verificationService) {
        this.sysUserService = sysUserService;
        this.verificationService = verificationService;
    }

    public SysUser register(PortalRegisterDTO dto, String clientAddress) {
        if (!enabled || configuredInviteCode == null || configuredInviteCode.trim().isEmpty()) {
            throw new BusinessException(403, "朋友注册暂未开放，请联系站长获取账号");
        }
        verificationService.consume(dto.getHumanToken(), clientAddress);
        enforceRateLimit(clientAddress);
        if (!MessageDigest.isEqual(digest(configuredInviteCode.trim()), digest(dto.getInviteCode().trim()))) {
            throw new BusinessException(403, "邀请码不正确或已经更换");
        }

        RegisterDTO register = new RegisterDTO();
        register.setUsername(dto.getUsername());
        register.setPassword(dto.getPassword());
        register.setRealName(dto.getDisplayName().trim());
        register.setRole("PORTAL_USER");
        SysUser user = sysUserService.register(register);
        user.setPassword(null);
        return user;
    }

    private byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("无法校验邀请码", e);
        }
    }

    private synchronized void enforceRateLimit(String clientAddress) {
        long now = System.currentTimeMillis();
        registrationRates.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= 3_600_000L);
        RateWindow window = registrationRates.get(clientAddress);
        if (window == null || now - window.startedAt >= 3_600_000L) {
            registrationRates.put(clientAddress, new RateWindow(now));
            return;
        }
        if (window.count.incrementAndGet() > MAX_REGISTRATIONS_PER_HOUR) {
            throw new BusinessException(429, "注册尝试过于频繁，请一小时后再试");
        }
    }

    private static class RateWindow {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger(1);
        private RateWindow(long startedAt) { this.startedAt = startedAt; }
    }
}
