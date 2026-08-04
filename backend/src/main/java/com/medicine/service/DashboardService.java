package com.medicine.service;

import com.medicine.vo.DashboardVO;

public interface DashboardService {
    DashboardVO getAdminDashboard();
    DashboardVO getElderDashboard(Long userId);
}
