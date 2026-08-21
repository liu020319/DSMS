package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.dto.PortalRegisterDTO;
import com.medicine.dto.RegisterDTO;
import com.medicine.entity.SysUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortalRegistrationServiceTest {
    private SysUserService userService;
    private HumanVerificationService verificationService;
    private PortalRegistrationService service;

    @BeforeEach
    void setUp() {
        userService = mock(SysUserService.class);
        verificationService = mock(HumanVerificationService.class);
        service = new PortalRegistrationService(userService, verificationService);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "configuredInviteCode", "friend-2026");
    }

    @Test
    void invitationRegistrationCreatesFinanceOnlyPortalUser() {
        SysUser saved = new SysUser(); saved.setUserId(9L); saved.setPassword("encoded");
        when(userService.register(org.mockito.ArgumentMatchers.any())).thenReturn(saved);

        SysUser result = service.register(dto("friend-2026"), "203.0.113.9");

        ArgumentCaptor<RegisterDTO> captor = ArgumentCaptor.forClass(RegisterDTO.class);
        verify(userService).register(captor.capture());
        verify(verificationService).consume("human-token", "203.0.113.9");
        assertEquals("PORTAL_USER", captor.getValue().getRole());
        assertEquals("朋友甲", captor.getValue().getRealName());
        assertNull(result.getPassword());
    }

    @Test
    void rejectsWrongInviteCodeWithoutCreatingAccount() {
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.register(dto("wrong-code"), "203.0.113.10"));
        assertEquals(403, error.getCode());
    }

    private PortalRegisterDTO dto(String inviteCode) {
        PortalRegisterDTO dto = new PortalRegisterDTO();
        dto.setUsername("friend001"); dto.setPassword("Password123");
        dto.setDisplayName("朋友甲"); dto.setInviteCode(inviteCode);
        dto.setHumanToken("human-token");
        return dto;
    }
}
