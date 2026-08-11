package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.dto.HumanChallengeVO;
import com.medicine.dto.HumanVerifyVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HumanVerificationService {
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(2);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(2);
    private static final long MINIMUM_REACTION_MILLIS = 600L;

    private final Map<String, VerificationState> challenges = new ConcurrentHashMap<>();
    private final Map<String, VerificationState> tokens = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> challengeRates = new ConcurrentHashMap<>();

    public HumanChallengeVO createChallenge(String clientAddress) {
        cleanupExpired();
        enforceChallengeRateLimit(clientAddress);
        LocalDateTime now = LocalDateTime.now();
        String challengeId = UUID.randomUUID().toString();
        challenges.put(challengeId, new VerificationState(clientAddress, now, now.plus(CHALLENGE_TTL)));
        return new HumanChallengeVO(challengeId, now.plus(CHALLENGE_TTL));
    }

    public HumanVerifyVO verify(String challengeId, String clientAddress) {
        cleanupExpired();
        VerificationState state = challenges.remove(challengeId);
        LocalDateTime now = LocalDateTime.now();
        if (state == null || state.expiresAt.isBefore(now) || !state.clientAddress.equals(clientAddress)) {
            throw new BusinessException(400, "验证已失效，请重新点击验证");
        }
        if (Duration.between(state.createdAt, now).toMillis() < MINIMUM_REACTION_MILLIS) {
            throw new BusinessException(429, "操作过快，请稍后重新验证");
        }
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = now.plus(TOKEN_TTL);
        tokens.put(token, new VerificationState(clientAddress, now, expiresAt));
        return new HumanVerifyVO(token, expiresAt);
    }

    public void consume(String token, String clientAddress) {
        cleanupExpired();
        VerificationState state = tokens.remove(token);
        if (state == null || state.expiresAt.isBefore(LocalDateTime.now()) || !state.clientAddress.equals(clientAddress)) {
            throw new BusinessException(400, "人机验证已过期，请重新验证");
        }
    }

    private void cleanupExpired() {
        LocalDateTime now = LocalDateTime.now();
        challenges.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
        tokens.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
        long cutoff = System.currentTimeMillis() - 120_000L;
        challengeRates.entrySet().removeIf(entry -> entry.getValue().windowStartedAt < cutoff);
    }

    private synchronized void enforceChallengeRateLimit(String clientAddress) {
        long now = System.currentTimeMillis();
        RateWindow window = challengeRates.get(clientAddress);
        if (window == null || now - window.windowStartedAt >= 60_000L) {
            challengeRates.put(clientAddress, new RateWindow(now, 1));
            return;
        }
        if (window.count >= 30) throw new BusinessException(429, "安全验证请求过于频繁，请稍后再试");
        window.count++;
    }

    private static class VerificationState {
        private final String clientAddress;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;

        private VerificationState(String clientAddress, LocalDateTime createdAt, LocalDateTime expiresAt) {
            this.clientAddress = clientAddress;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }

    private static class RateWindow {
        private final long windowStartedAt;
        private int count;

        private RateWindow(long windowStartedAt, int count) {
            this.windowStartedAt = windowStartedAt;
            this.count = count;
        }
    }
}
