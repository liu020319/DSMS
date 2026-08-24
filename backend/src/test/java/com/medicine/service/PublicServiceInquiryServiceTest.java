package com.medicine.service;

import com.medicine.dto.PublicServiceInquiryDTO;
import com.medicine.entity.PublicServiceInquiry;
import com.medicine.entity.PublicServiceMessage;
import com.medicine.entity.SysUser;
import com.medicine.mapper.PublicServiceInquiryMapper;
import com.medicine.mapper.PublicServiceMessageMapper;
import com.medicine.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicServiceInquiryServiceTest {
    private PublicServiceInquiryService service;
    private PublicServiceInquiryMapper inquiryMapper;
    private PublicServiceMessageMapper messageMapper;
    private HumanVerificationService verificationService;
    private SysUserMapper userMapper;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        inquiryMapper = mock(PublicServiceInquiryMapper.class);
        messageMapper = mock(PublicServiceMessageMapper.class);
        userMapper = mock(SysUserMapper.class);
        verificationService = mock(HumanVerificationService.class);
        notificationService = mock(NotificationService.class);
        service = new PublicServiceInquiryService(inquiryMapper, messageMapper, userMapper,
                notificationService, verificationService);
        when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(inquiryMapper.insert(any(PublicServiceInquiry.class))).thenAnswer(invocation -> {
            invocation.<PublicServiceInquiry>getArgument(0).setInquiryId(8L);
            return 1;
        });
    }

    @Test
    void anonymousSubmitReturnsSecretButStoresOnlyHash() {
        Map<String, String> result = service.submit(dto(), "127.0.0.1");
        ArgumentCaptor<PublicServiceInquiry> captor = ArgumentCaptor.forClass(PublicServiceInquiry.class);
        verify(inquiryMapper).insert(captor.capture());
        verify(verificationService).consume("human-token", "127.0.0.1");

        String rawCode = result.get("accessCode");
        assertTrue(rawCode.length() >= 20);
        assertEquals(64, captor.getValue().getPublicAccessHash().length());
        assertNotEquals(rawCode, captor.getValue().getPublicAccessHash());
    }

    @Test
    void publicViewDoesNotExposeContactOrAccessHash() {
        Map<String, String> created = service.submit(dto(), "10.0.0.2");
        ArgumentCaptor<PublicServiceInquiry> captor = ArgumentCaptor.forClass(PublicServiceInquiry.class);
        verify(inquiryMapper).insert(captor.capture());
        when(inquiryMapper.selectOne(any())).thenReturn(captor.getValue());
        PublicServiceMessage message = new PublicServiceMessage();
        message.setMessageId(3L); message.setSenderType("ADMIN"); message.setSenderUserId(99L);
        message.setMessageText("已收到，会尽快联系。");
        when(messageMapper.selectList(any())).thenReturn(Collections.singletonList(message));

        Map<String, Object> view = service.publicDetail(
                created.get("inquiryNo"), created.get("accessCode"), "10.0.0.2");
        assertFalse(view.containsKey("contactValue"));
        assertFalse(view.containsKey("publicAccessHash"));
        assertEquals("GUIDANCE", view.get("serviceType"));
        Map<String, Object> publicMessage = ((List<Map<String, Object>>) view.get("messages")).get(0);
        assertFalse(publicMessage.containsKey("senderUserId"));
    }

    @Test
    void anonymousSubmitUsesNotificationTypeThatFitsExistingColumn() {
        SysUser first = new SysUser(); first.setUserId(1L);
        SysUser second = new SysUser(); second.setUserId(2L);
        when(userMapper.selectList(any())).thenReturn(java.util.Arrays.asList(first, second));

        service.submit(dto(), "10.0.0.8");

        String bizType = "SOFTWARE_SERVICE_INQUIRY";
        assertTrue(bizType.length() <= 30, "通知业务类型必须兼容 user_notification.biz_type VARCHAR(30)");
        verify(notificationService, times(1)).notify(
                org.mockito.ArgumentMatchers.eq(1L), any(), any(),
                org.mockito.ArgumentMatchers.eq(bizType), org.mockito.ArgumentMatchers.eq(8L));
        verify(notificationService, times(1)).notify(
                org.mockito.ArgumentMatchers.eq(2L), any(), any(),
                org.mockito.ArgumentMatchers.eq(bizType), org.mockito.ArgumentMatchers.eq(8L));
    }

    @Test
    void adminDeleteRemovesMessagesBeforeInquiry() {
        PublicServiceInquiry inquiry = new PublicServiceInquiry();
        inquiry.setInquiryId(8L);
        when(inquiryMapper.selectById(8L)).thenReturn(inquiry);

        service.adminDelete(8L, "ADMIN");

        verify(messageMapper).delete(any());
        verify(inquiryMapper).deleteById(8L);
    }

    @Test
    void nonAdminCannotDeletePublicInquiry() {
        assertThrows(com.medicine.common.BusinessException.class,
                () -> service.adminDelete(8L, "PORTAL_USER"));
    }

    private PublicServiceInquiryDTO dto() {
        PublicServiceInquiryDTO dto = new PublicServiceInquiryDTO();
        dto.setContactName("同学"); dto.setContactValue("example@example.com");
        dto.setServiceType("GUIDANCE"); dto.setProjectType("Java管理系统");
        dto.setInquiryText("希望获得需求分析、代码讲解和部署指导。\n");
        dto.setSourcePath("/cloud-hub/#/services"); dto.setHumanToken("human-token");
        return dto;
    }
}
