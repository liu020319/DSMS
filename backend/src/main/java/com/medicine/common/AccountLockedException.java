package com.medicine.common;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends RuntimeException {
    private final LocalDateTime lockedUntil;
    private final long remainingSeconds;

    public AccountLockedException(LocalDateTime lockedUntil, long remainingSeconds) {
        super("密码连续输错次数过多，账号已暂时锁定");
        this.lockedUntil = lockedUntil;
        this.remainingSeconds = Math.max(1L, remainingSeconds);
    }
}
