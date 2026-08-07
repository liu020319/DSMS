package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.Medicine;
import java.util.List;

public interface MedicineService extends IService<Medicine> {
    void addMedicine(com.medicine.dto.MedicineDTO dto);
    void updateMedicine(com.medicine.dto.MedicineDTO dto);
    void disableMedicine(Long medicineId);
    void deleteMedicine(Long medicineId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<Medicine> pageList(int current, int size, String keyword, String approvalNumber);
    List<Medicine> listActive();
}
