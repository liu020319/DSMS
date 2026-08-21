package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.dto.SoftwareServiceStatusDTO;
import com.medicine.entity.SoftwareServiceRequest;
import com.medicine.mapper.SoftwareServiceMilestoneMapper;
import com.medicine.mapper.SoftwareServiceRequestMapper;
import com.medicine.mapper.SoftwareServiceWorkOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SoftwareServiceCenterServiceTest {
    private SoftwareServiceCenterService service;
    private SoftwareServiceRequestMapper requestMapper;

    @BeforeEach
    void setUp() {
        requestMapper = mock(SoftwareServiceRequestMapper.class);
        service = new SoftwareServiceCenterService(requestMapper,
                mock(SoftwareServiceMilestoneMapper.class), mock(SoftwareServiceWorkOrderMapper.class),
                mock(NotificationService.class));
    }

    @Test
    void ordinaryUserCannotSeeAnotherUsersRequest() {
        SoftwareServiceRequest request = request("SUBMITTED");
        request.setRequesterUserId(20L);
        when(requestMapper.selectById(1L)).thenReturn(request);
        assertThrows(BusinessException.class, () -> service.detail(1L, 10L, "GUARDIAN"));
    }

    @Test
    void closedRequestCannotJumpBackToInProgress() {
        when(requestMapper.selectById(1L)).thenReturn(request("CLOSED"));
        SoftwareServiceStatusDTO dto = new SoftwareServiceStatusDTO();
        dto.setStatus("IN_PROGRESS");
        assertThrows(BusinessException.class, () -> service.updateStatus(1L, dto, "ADMIN"));
    }

    @Test
    void nonAdminCannotUpdateRequestStatus() {
        SoftwareServiceStatusDTO dto = new SoftwareServiceStatusDTO();
        dto.setStatus("ASSESSING");
        assertThrows(BusinessException.class, () -> service.updateStatus(1L, dto, "GUARDIAN"));
    }

    private SoftwareServiceRequest request(String status) {
        SoftwareServiceRequest request = new SoftwareServiceRequest();
        request.setRequestId(1L); request.setRequesterUserId(10L); request.setStatus(status);
        return request;
    }
}
