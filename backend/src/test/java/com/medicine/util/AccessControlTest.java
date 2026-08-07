package com.medicine.util;

import com.medicine.common.BusinessException;
import com.medicine.entity.SysUser;
import com.medicine.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccessControlTest {
    private AccessControl accessControl;
    private SysUserMapper mapper;

    @BeforeEach
    void setUp() {
        accessControl = new AccessControl();
        mapper = mock(SysUserMapper.class);
        ReflectionTestUtils.setField(accessControl, "sysUserMapper", mapper);
    }

    @Test
    void platformAdminCanQueryAllFamilies() {
        MockHttpServletRequest request = request(1L, "ADMIN");
        assertNull(accessControl.scopedUserIds(request, null));
        assertDoesNotThrow(() -> accessControl.requireOwnerOrAdmin(request, 999L));
        verifyNoInteractions(mapper);
    }

    @Test
    void elderCanOnlyAccessOwnData() {
        MockHttpServletRequest request = request(20L, "ELDER");
        assertDoesNotThrow(() -> accessControl.requireOwnerOrAdmin(request, 20L));
        assertThrows(BusinessException.class, () -> accessControl.requireOwnerOrAdmin(request, 21L));
    }

    @Test
    void guardianScopeContainsOnlyBoundElders() {
        SysUser first = new SysUser(); first.setUserId(30L);
        SysUser second = new SysUser(); second.setUserId(31L);
        when(mapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        MockHttpServletRequest request = request(10L, "GUARDIAN");
        assertEquals(Arrays.asList(30L, 31L), accessControl.scopedUserIds(request, null));
    }

    @Test
    void guardianWithoutFamilyGetsEmptyScope() {
        when(mapper.selectList(any())).thenReturn(Collections.emptyList());
        assertTrue(accessControl.scopedUserIds(request(10L, "GUARDIAN"), null).isEmpty());
    }

    private MockHttpServletRequest request(Long userId, String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);
        return request;
    }
}
