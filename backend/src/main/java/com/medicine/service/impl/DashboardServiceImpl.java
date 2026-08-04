package com.medicine.service.impl;

import com.medicine.service.DashboardService;
import com.medicine.service.StockDeductionService;
import com.medicine.service.StockService;
import com.medicine.vo.DashboardVO;
import com.medicine.vo.StockVO;
import com.medicine.entity.Medicine;
import com.medicine.entity.Prescription;
import com.medicine.entity.ApprovalTask;
import com.medicine.mapper.MedicineMapper;
import com.medicine.mapper.PrescriptionMapper;
import com.medicine.mapper.ApprovalTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockDeductionService stockDeductionService;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Autowired
    private ApprovalTaskMapper approvalTaskMapper;

    @Override
    public DashboardVO getAdminDashboard() {
        // 首页刷新时调用分时段扣减算法
        stockDeductionService.deductAllWithPeriod();
        DashboardVO vo = new DashboardVO();
        LambdaQueryWrapper<Medicine> mWrapper = new LambdaQueryWrapper<>();
        mWrapper.eq(Medicine::getStatus, 1);
        vo.setTotalMedicines(medicineMapper.selectCount(mWrapper));
        LambdaQueryWrapper<Prescription> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.eq(Prescription::getStatus, 1);
        vo.setActivePrescriptions(prescriptionMapper.selectCount(pWrapper));
        List<StockVO> warningList = stockService.getWarningList(null);
        vo.setWarningCount((long) warningList.size());
        vo.setWarningList(warningList);
        List<StockVO> expiringList = stockService.getExpiringList(null);
        vo.setExpiringCount((long) expiringList.size());
        vo.setExpiringList(expiringList);
        LambdaQueryWrapper<ApprovalTask> aWrapper = new LambdaQueryWrapper<>();
        aWrapper.eq(ApprovalTask::getStatus, "PENDING");
        vo.setPendingApprovalCount(approvalTaskMapper.selectCount(aWrapper));
        return vo;
    }

    @Override
    public DashboardVO getElderDashboard(Long userId) {
        // 首页刷新时调用分时段扣减算法
        stockDeductionService.deductAllByUserIdWithPeriod(userId);
        DashboardVO vo = new DashboardVO();
        LambdaQueryWrapper<Prescription> pWrapper = new LambdaQueryWrapper<>();
        pWrapper.eq(Prescription::getUserId, userId).eq(Prescription::getStatus, 1);
        vo.setActivePrescriptions(prescriptionMapper.selectCount(pWrapper));
        List<StockVO> warningList = stockService.getWarningList(userId);
        vo.setWarningCount((long) warningList.size());
        vo.setWarningList(warningList);
        List<StockVO> expiringList = stockService.getExpiringList(userId);
        vo.setExpiringCount((long) expiringList.size());
        vo.setExpiringList(expiringList);
        LambdaQueryWrapper<ApprovalTask> aWrapper = new LambdaQueryWrapper<>();
        aWrapper.eq(ApprovalTask::getApplicantId, userId).eq(ApprovalTask::getStatus, "PENDING");
        vo.setPendingApprovalCount(approvalTaskMapper.selectCount(aWrapper));
        return vo;
    }
}
