package com.medicine.service;

import com.medicine.vo.DashboardVO;

public interface DashboardService {
    DashboardVO getAdminDashboard(java.util.List<Long> allowedUserIds, Long handlerId);
    DashboardVO getElderDashboard(Long userId);
}
