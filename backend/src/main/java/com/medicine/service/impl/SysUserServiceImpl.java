package com.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.common.BusinessException;
import com.medicine.common.BusinessCode;
import com.medicine.common.AccountLockedException;
import com.medicine.dto.LoginDTO;
import com.medicine.dto.RegisterDTO;
import com.medicine.entity.SysUser;
import com.medicine.mapper.SysUserMapper;
import com.medicine.service.SysUserService;
import com.medicine.util.JwtUtil;
import com.medicine.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final Object[] ACCOUNT_LOCKS = new Object[256];
    static {
        for (int i = 0; i < ACCOUNT_LOCKS.length; i++) ACCOUNT_LOCKS[i] = new Object();
    }

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        String normalizedUsername = dto.getUsername().trim();
        Object accountLock = ACCOUNT_LOCKS[(normalizedUsername.hashCode() & Integer.MAX_VALUE) % ACCOUNT_LOCKS.length];
        synchronized (accountLock) {
                SysUser user = getByUsername(normalizedUsername);
                if (user == null) {
                    throw new BusinessException(401, "账号或密码错误");
                }
                if (Integer.valueOf(0).equals(user.getStatus())) {
                    throw new BusinessException(403, "账号已停用，请联系家庭管理员");
                }

                LocalDateTime now = LocalDateTime.now();
                if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                    throw lockedException(user.getLockedUntil(), now);
                }
                if (user.getLockedUntil() != null) {
                    clearLoginFailures(user);
                }

                if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
                    int failedAttempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
                    user.setFailedLoginAttempts(failedAttempts);
                    if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                        user.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                        updateById(user);
                        throw lockedException(user.getLockedUntil(), now);
                    }
                    updateById(user);
                    throw new BusinessException(401, "账号或密码错误，还可尝试" + (MAX_FAILED_ATTEMPTS - failedAttempts) + "次");
                }

                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                user.setLastLoginTime(now);
                updateById(user);
                persistUnlockedState(user.getUserId());

                String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getRole());
                LoginVO vo = new LoginVO();
                vo.setToken(token);
                vo.setUserId(user.getUserId());
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setRole(user.getRole());
                vo.setBindParentId(user.getBindParentId());
                return vo;
        }
    }

    @Override
    public SysUser register(RegisterDTO dto) {
        String normalizedUsername = dto.getUsername().trim();
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, normalizedUsername);
        if (count(wrapper) > 0) {
            throw new BusinessException(BusinessCode.USER_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setBindParentId(dto.getBindParentId());
        user.setStatus(1);
        user.setFailedLoginAttempts(0);
        save(user);
        return user;
    }

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        return getOne(wrapper);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        clearLoginFailures(user);
        updateById(user);
        persistUnlockedState(userId);
    }

    @Override
    public void unlockAccount(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        clearLoginFailures(user);
        updateById(user);
        persistUnlockedState(userId);
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("当前密码不正确");
        }
        if (currentPassword.equals(newPassword)) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        clearLoginFailures(user);
        updateById(user);
        persistUnlockedState(userId);
    }

    @Override
    public List<SysUser> getElderByParentId(Long parentId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getBindParentId, parentId)
               .eq(SysUser::getRole, "ELDER")
               .eq(SysUser::getStatus, 1);
        return list(wrapper);
    }

    private void clearLoginFailures(SysUser user) {
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
    }

    private AccountLockedException lockedException(LocalDateTime lockedUntil, LocalDateTime now) {
        return new AccountLockedException(lockedUntil, Duration.between(now, lockedUntil).getSeconds());
    }

    private void persistUnlockedState(Long userId) {
        update(new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getFailedLoginAttempts, 0)
                .set(SysUser::getLockedUntil, null));
    }
}
