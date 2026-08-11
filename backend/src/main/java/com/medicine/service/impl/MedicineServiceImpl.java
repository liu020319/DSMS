package com.medicine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.common.BusinessException;
import com.medicine.common.BusinessCode;
import com.medicine.dto.MedicineDTO;
import com.medicine.entity.Medicine;
import com.medicine.mapper.MedicineMapper;
import com.medicine.service.MedicineService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MedicineServiceImpl extends ServiceImpl<MedicineMapper, Medicine> implements MedicineService {

    @Override
    public void addMedicine(MedicineDTO dto) {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getApprovalNumber, dto.getApprovalNumber());
        if (count(wrapper) > 0) {
            throw new BusinessException(BusinessCode.MEDICINE_EXISTS);
        }
        Medicine medicine = new Medicine();
        BeanUtils.copyProperties(dto, medicine);
        if (medicine.getStatus() == null) {
            medicine.setStatus(1);
        }
        save(medicine);
    }

    @Override
    public void updateMedicine(MedicineDTO dto) {
        if (dto.getMedicineId() == null) {
            throw new BusinessException("药品ID不能为空");
        }
        Medicine existing = getById(dto.getMedicineId());
        if (existing == null) {
            throw new BusinessException("药品不存在");
        }
        if (!existing.getApprovalNumber().equals(dto.getApprovalNumber())) {
            LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Medicine::getApprovalNumber, dto.getApprovalNumber());
            if (count(wrapper) > 0) {
                throw new BusinessException(BusinessCode.MEDICINE_EXISTS);
            }
        }
        Medicine medicine = new Medicine();
        BeanUtils.copyProperties(dto, medicine);
        updateById(medicine);
    }

    @Override
    public void disableMedicine(Long medicineId) {
        Medicine medicine = getById(medicineId);
        if (medicine == null) {
            throw new BusinessException("药品不存在");
        }
        medicine.setStatus(medicine.getStatus() == 1 ? 0 : 1);
        updateById(medicine);
    }

    @Override
    public void deleteMedicine(Long medicineId) {
        Medicine medicine = getById(medicineId);
        if (medicine == null) {
            throw new BusinessException("药品不存在");
        }
        removeById(medicineId);
    }

    @Override
    public Page<Medicine> pageList(int current, int size, String keyword, String approvalNumber, Integer status) {
        Page<Medicine> page = new Page<>(current, size);
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Medicine::getMedicineName, keyword)
                    .or().like(Medicine::getBrandName, keyword)
                    .or().like(Medicine::getManufacturer, keyword));
        }
        if (approvalNumber != null && !approvalNumber.isEmpty()) {
            wrapper.like(Medicine::getApprovalNumber, approvalNumber);
        }
        if (status != null) wrapper.eq(Medicine::getStatus, status);
        wrapper.orderByDesc(Medicine::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public List<Medicine> listActive() {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Medicine::getStatus, 1)
               .orderByAsc(Medicine::getMedicineName);
        return list(wrapper);
    }

    @Override
    public Map<String, Object> getOverview() {
        return baseMapper.selectMedicineSummary();
    }

    @Override
    public Map<String, Object> getProfile(Long medicineId) {
        Medicine medicine = getById(medicineId);
        if (medicine == null) throw new BusinessException("药品不存在");
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("medicine", medicine);
        profile.put("stats", baseMapper.selectMedicineProfileStats(medicineId));
        profile.put("relatedUsers", baseMapper.selectRelatedUsers(medicineId));
        profile.put("recentPurchases", baseMapper.selectRecentPurchases(medicineId));
        return profile;
    }
}
