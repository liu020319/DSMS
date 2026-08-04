package com.medicine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.medicine.entity.Prescription;
import com.medicine.vo.PrescriptionVO;
import java.util.List;

public interface PrescriptionService extends IService<Prescription> {
    void addPrescription(com.medicine.dto.PrescriptionDTO dto);
    void updatePrescription(com.medicine.dto.PrescriptionDTO dto, String changeReason);
    void stopPrescription(Long prescriptionId);
    void enablePrescription(Long prescriptionId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<PrescriptionVO> pageList(int current, int size, Long userId, Long medicineId, String realName);
    List<PrescriptionVO> listByUserId(Long userId);
    PrescriptionVO getDetail(Long prescriptionId);
    List<PrescriptionVO> getHistory(Long prescriptionId);
}
