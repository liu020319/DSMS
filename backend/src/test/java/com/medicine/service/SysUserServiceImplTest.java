package com.medicine.service;

import com.medicine.common.AccountLockedException;
import com.medicine.common.BusinessException;
import com.medicine.dto.LoginDTO;
import com.medicine.entity.SysUser;
import com.medicine.mapper.SysUserMapper;
import com.medicine.service.impl.SysUserServiceImpl;
import com.medicine.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SysUserServiceImplTest {
    private SysUserServiceImpl service;
    private SysUser user;

    @BeforeEach
    void setUp() {
        service = spy(new SysUserServiceImpl());
        SysUserMapper mapper = mock(SysUserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", encoder);
        ReflectionTestUtils.setField(service, "jwtUtil", mock(JwtUtil.class));

        user = new SysUser();
        user.setUserId(10L);
        user.setUsername("family-user");
        user.setPassword("bcrypt-hash");
        user.setRole("ELDER");
        user.setStatus(1);
        user.setFailedLoginAttempts(0);

        doReturn(user).when(service).getByUsername("family-user");
        when(mapper.updateById(any(SysUser.class))).thenReturn(1);
        when(encoder.matches(anyString(), anyString())).thenReturn(false);
    }

    @Test
    void fifthWrongPasswordLocksAccountForFifteenMinutes() {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("family-user");
        dto.setPassword("wrong-password");
        dto.setHumanToken("already-verified-by-controller");

        for (int i = 0; i < 4; i++) {
            BusinessException error = assertThrows(BusinessException.class, () -> service.login(dto));
            assertTrue(error.getMessage().contains("还可尝试"));
        }
        AccountLockedException locked = assertThrows(AccountLockedException.class, () -> service.login(dto));
        assertNotNull(user.getLockedUntil());
        assertTrue(locked.getRemainingSeconds() >= 14 * 60);
        assertEquals(5, user.getFailedLoginAttempts());
    }
}
