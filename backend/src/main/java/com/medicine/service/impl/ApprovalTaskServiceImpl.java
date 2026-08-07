package com.medicine.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medicine.common.BusinessException;
import com.medicine.common.BusinessCode;
import com.medicine.dto.ApprovalTaskDTO;
import com.medicine.dto.PrescriptionDTO;
import com.medicine.dto.PurchaseRecordDTO;
import com.medicine.entity.ApprovalTask;
import com.medicine.mapper.ApprovalTaskMapper;
import com.medicine.service.ApprovalTaskService;
import com.medicine.service.PrescriptionService;
import com.medicine.service.PurchaseRecordService;
import com.medicine.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ApprovalTaskServiceImpl extends ServiceImpl<ApprovalTaskMapper, ApprovalTask> implements ApprovalTaskService {

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private PurchaseRecordService purchaseRecordService;

    @Autowired
    private StockService stockService;

    @Override
    public void submitTask(ApprovalTaskDTO dto) {
        LambdaQueryWrapper<ApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalTask::getApplicantId, dto.getApplicantId())
               .eq(ApprovalTask::getTaskType, dto.getTaskType())
               .eq(ApprovalTask::getContentJson, dto.getContentJson())
               .eq(ApprovalTask::getStatus, "PENDING");
        if (count(wrapper) > 0) {
            throw new BusinessException(BusinessCode.APPROVAL_PENDING);
        }
        ApprovalTask task = new ApprovalTask();
        task.setApplicantId(dto.getApplicantId());
        task.setHandlerId(dto.getHandlerId());
        task.setTaskType(dto.getTaskType());
        task.setContentJson(dto.getContentJson());
        task.setStatus("PENDING");
        save(task);
    }

    @Override
    @Transactional
    public void approveTask(Long taskId, String comment, Long handlerId) {
        ApprovalTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("审批任务不存在");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("该任务已处理");
        }
        validateHandler(task, handlerId);
        task.setStatus("APPROVED");
        task.setHandlerComment(comment);
        updateById(task);
        executeApprovedAction(task);
    }

    @Override
    public void rejectTask(Long taskId, String comment, Long handlerId) {
        ApprovalTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("审批任务不存在");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("该任务已处理");
        }
        validateHandler(task, handlerId);
        task.setStatus("REJECTED");
        task.setHandlerComment(comment);
        updateById(task);
    }

    @Override
    @Transactional
    public void modifyAndApproveTask(Long taskId, ApprovalTaskDTO modifiedDto, String comment, Long handlerId) {
        ApprovalTask task = getById(taskId);
        if (task == null) {
            throw new BusinessException("审批任务不存在");
        }
        if (!"PENDING".equals(task.getStatus())) {
            throw new BusinessException("该任务已处理");
        }
        validateHandler(task, handlerId);
        if (modifiedDto.getContentJson() != null) {
            task.setContentJson(modifiedDto.getContentJson());
        }
        task.setStatus("APPROVED");
        task.setHandlerComment(comment);
        updateById(task);
        executeApprovedAction(task);
    }

    @Override
    public Page<ApprovalTask> pagePending(int current, int size, Long handlerId) {
        Page<ApprovalTask> page = new Page<>(current, size);
        LambdaQueryWrapper<ApprovalTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApprovalTask::getStatus, "PENDING");
        if (handlerId != null) {
            wrapper.eq(ApprovalTask::getHandlerId, handlerId);
        }
        wrapper.orderByDesc(ApprovalTask::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public Page<ApprovalTask> pageByApplicant(int current, int size, Long applicantId) {
        Page<ApprovalTask> page = new Page<>(current, size);
        LambdaQueryWrapper<ApprovalTask> wrapper = new LambdaQueryWrapper<>();
        if (applicantId != null) {
            wrapper.eq(ApprovalTask::getApplicantId, applicantId);
        }
        wrapper.orderByDesc(ApprovalTask::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public Page<ApprovalTask> pageAll(int current, int size, Long handlerId) {
        Page<ApprovalTask> page = new Page<>(current, size);
        LambdaQueryWrapper<ApprovalTask> wrapper = new LambdaQueryWrapper<>();
        if (handlerId != null) wrapper.eq(ApprovalTask::getHandlerId, handlerId);
        wrapper.orderByDesc(ApprovalTask::getCreateTime);
        return page(page, wrapper);
    }

    private void executeApprovedAction(ApprovalTask task) {
        JSONObject content = JSONUtil.parseObj(task.getContentJson());
        switch (task.getTaskType()) {
            case "NEW_MEDICINE":
                handleNewMedicine(content, task);
                break;
            case "LOSS_ADJUST":
                handleLossAdjust(content);
                break;
            case "STOCK_CORRECT":
                handleStockCorrect(content);
                break;
            default:
                throw new BusinessException("未知的审批类型: " + task.getTaskType());
        }
    }

    private void handleNewMedicine(JSONObject content, ApprovalTask task) {
        PrescriptionDTO dto = new PrescriptionDTO();
        dto.setUserId(content.getLong("userId"));
        dto.setMedicineId(content.getLong("medicineId"));
        dto.setDailyTimes(content.getInt("dailyTimes"));
        dto.setDosagePerTime(content.getInt("dosagePerTime"));
        dto.setTakeNotes(content.getStr("takeNotes"));
        prescriptionService.addPrescription(dto);
        if (content.containsKey("quantityBoxes") && content.getInt("quantityBoxes") > 0) {
            PurchaseRecordDTO purchaseDTO = new PurchaseRecordDTO();
            purchaseDTO.setUserId(dto.getUserId());
            purchaseDTO.setPrescriptionId(dto.getPrescriptionId());
            purchaseDTO.setPurchaseDate(LocalDate.now());
            purchaseDTO.setQuantityBoxes(content.getInt("quantityBoxes"));
            purchaseDTO.setUnitPrice(content.getBigDecimal("unitPrice") != null ? content.getBigDecimal("unitPrice") : BigDecimal.ZERO);
            String expiryStr = content.getStr("expiryDate");
            purchaseDTO.setExpiryDate(expiryStr != null ? LocalDate.parse(expiryStr) : LocalDate.now().plusYears(2));
            purchaseDTO.setOperatorId(task.getHandlerId() != null ? task.getHandlerId() : dto.getUserId());
            purchaseDTO.setReceiptStatus(1);
            purchaseRecordService.addPurchaseRecord(purchaseDTO);
        }
    }

    private void handleLossAdjust(JSONObject content) {
        Long prescriptionId = content.getLong("prescriptionId");
        Integer lossBoxes = content.getInt("lossBoxes");
        stockService.deductStockOnLoss(prescriptionId, lossBoxes);
    }

    private void handleStockCorrect(JSONObject content) {
        Long prescriptionId = content.getLong("prescriptionId");
        Integer correctBoxes = content.getInt("correctBoxes");
        String expiryStr = content.getStr("expiryDate");
        LocalDate expiryDate = expiryStr != null ? LocalDate.parse(expiryStr) : LocalDate.now().plusYears(2);
        stockService.addStockOnPurchase(prescriptionId, correctBoxes, expiryDate);
    }

    private void validateHandler(ApprovalTask task, Long handlerId) {
        if (handlerId == null || (task.getHandlerId() != null && !handlerId.equals(task.getHandlerId()))) {
            throw new BusinessException("无权处理该审批任务");
        }
    }
}
