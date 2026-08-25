package com.medicine.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.common.BusinessException;
import com.medicine.dto.*;
import com.medicine.entity.*;
import com.medicine.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FamilyCareService {
    @Autowired private ApprovalTaskMapper approvalTaskMapper;
    @Autowired private PrescriptionMapper prescriptionMapper;
    @Autowired private MedicineMapper medicineMapper;
    @Autowired private SysUserMapper userMapper;
    @Autowired private FamilyPurchaseOrderMapper orderMapper;
    @Autowired private LogisticsEventMapper logisticsMapper;
    @Autowired private FamilyFundTransactionMapper fundMapper;
    @Autowired private UserNotificationMapper notificationMapper;
    @Autowired private OrderReceiptVerificationMapper receiptVerificationMapper;
    @Autowired private PurchaseRecordMapper purchaseRecordMapper;
    @Autowired private PurchaseRecordService purchaseRecordService;
    @Autowired private StockService stockService;
    @Autowired private NotificationService notificationService;
    @Autowired private FileAssetService fileAssetService;
    @Autowired private PurchaseEvidenceService purchaseEvidenceService;
    @Value("${logistics.query-url-template:}") private String logisticsUrlTemplate;

    @Transactional(rollbackFor = Exception.class)
    public ApprovalTask submitPurchaseRequest(PurchaseRequestDTO dto) {
        SysUser elder = userMapper.selectById(dto.getElderId());
        if (elder == null || !"ELDER".equals(elder.getRole())) throw new BusinessException("老人账号不存在");
        if (!dto.getParentId().equals(elder.getBindParentId())) throw new BusinessException("申请接收人不是已绑定的子女账号");
        if (approvalTaskMapper.selectCount(new LambdaQueryWrapper<ApprovalTask>()
                .eq(ApprovalTask::getApplicantId, dto.getElderId())
                .eq(ApprovalTask::getTaskType, "PURCHASE_REQUEST")
                .eq(ApprovalTask::getStatus, "PENDING")) > 0) {
            throw new BusinessException("已有一笔购药申请等待子女处理，请勿重复提交");
        }

        JSONArray itemArray = new JSONArray();
        BigDecimal estimatedTotal = BigDecimal.ZERO;
        Set<Long> uniqueIds = new HashSet<>();
        for (PurchaseRequestItemDTO requestItem : dto.getItems()) {
            if (!uniqueIds.add(requestItem.getPrescriptionId())) throw new BusinessException("同一种药品不能重复添加");
            Prescription p = prescriptionMapper.selectById(requestItem.getPrescriptionId());
            if (p == null || !dto.getElderId().equals(p.getUserId()) || !Integer.valueOf(1).equals(p.getStatus())) {
                throw new BusinessException("用药方案不存在或不属于当前老人");
            }
            Medicine medicine = medicineMapper.selectById(p.getMedicineId());
            if (medicine == null || !Integer.valueOf(1).equals(medicine.getStatus())) throw new BusinessException("药品不存在或已停用");
            BigDecimal unitPrice = medicine.getReferencePrice() == null ? BigDecimal.ZERO : medicine.getReferencePrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(requestItem.getQuantityBoxes()));
            estimatedTotal = estimatedTotal.add(subtotal);
            JSONObject item = new JSONObject();
            item.set("prescriptionId", p.getPrescriptionId());
            item.set("medicineId", medicine.getMedicineId());
            item.set("medicineName", medicine.getMedicineName());
            item.set("brandName", medicine.getBrandName());
            item.set("approvalNumber", medicine.getApprovalNumber());
            item.set("specification", medicine.getSpecification());
            item.set("quantityBoxes", requestItem.getQuantityBoxes());
            item.set("estimatedUnitPrice", unitPrice);
            item.set("estimatedSubtotal", subtotal);
            itemArray.add(item);
        }

        JSONObject content = new JSONObject();
        content.set("reason", dto.getReason());
        content.set("reasonLabel", reasonLabel(dto.getReason()));
        content.set("note", dto.getNote());
        content.set("items", itemArray);
        content.set("estimatedTotal", estimatedTotal);
        content.set("balanceSnapshot", getBalance(dto.getElderId()));
        content.set("confirmed", true);
        content.set("confirmedAt", LocalDateTime.now().toString());

        ApprovalTask task = new ApprovalTask();
        task.setApplicantId(dto.getElderId());
        task.setHandlerId(dto.getParentId());
        task.setTaskType("PURCHASE_REQUEST");
        task.setContentJson(content.toString());
        task.setStatus("PENDING");
        approvalTaskMapper.insert(task);

        if ("LOST".equals(dto.getReason())) {
            for (PurchaseRequestItemDTO requestItem : dto.getItems()) {
                stockService.deductStockOnLoss(requestItem.getPrescriptionId(), requestItem.getQuantityBoxes());
            }
        }

        String title = elder.getRealName() + "提交了购药需求";
        String body = "账号" + elder.getUsername() + "提交了" + itemArray.size() + "种药品的代购申请，参考金额约¥" + estimatedTotal + "。";
        notificationService.notify(dto.getParentId(), title, body, "PURCHASE_REQUEST", task.getTaskId());
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyPurchaseOrder createOrder(FamilyOrderDTO dto, Long operatorId, boolean systemAdmin) {
        ApprovalTask task = approvalTaskMapper.selectById(dto.getTaskId());
        if (task == null || !"PURCHASE_REQUEST".equals(task.getTaskType())) throw new BusinessException("购药申请不存在");
        if (!"PENDING".equals(task.getStatus())) throw new BusinessException("该申请已经处理");
        if (!systemAdmin && !operatorId.equals(task.getHandlerId())) throw new BusinessException("无权处理其他家庭的申请");
        Long parentId = task.getHandlerId();
        if (orderMapper.selectCount(new LambdaQueryWrapper<FamilyPurchaseOrder>().eq(FamilyPurchaseOrder::getTaskId, dto.getTaskId())) > 0) {
            throw new BusinessException("该申请已经生成订单");
        }

        JSONObject requestContent = JSONUtil.parseObj(task.getContentJson());
        Set<Long> requestedIds = new HashSet<>();
        JSONArray requestedItems = requestContent.getJSONArray("items");
        for (int i = 0; i < requestedItems.size(); i++) requestedIds.add(requestedItems.getJSONObject(i).getLong("prescriptionId"));

        JSONArray actualItems = new JSONArray();
        BigDecimal actualTotal = BigDecimal.ZERO;
        Set<Long> orderedPrescriptionIds = new HashSet<>();
        for (OrderItemDTO itemDto : dto.getItems()) {
            if (!orderedPrescriptionIds.add(itemDto.getPrescriptionId())) throw new BusinessException("同一种药品不能重复下单");
            if (!requestedIds.contains(itemDto.getPrescriptionId())) throw new BusinessException("订单包含未申请的药品");
            Prescription p = prescriptionMapper.selectById(itemDto.getPrescriptionId());
            Medicine m = p == null ? null : medicineMapper.selectById(p.getMedicineId());
            if (p == null || m == null || !task.getApplicantId().equals(p.getUserId())) throw new BusinessException("订单药品信息无效");
            BigDecimal subtotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantityBoxes()));
            actualTotal = actualTotal.add(subtotal);
            JSONObject actual = new JSONObject();
            actual.set("prescriptionId", p.getPrescriptionId());
            actual.set("medicineName", m.getMedicineName());
            actual.set("brandName", m.getBrandName());
            actual.set("specification", m.getSpecification());
            actual.set("approvalNumber", m.getApprovalNumber());
            actual.set("quantityBoxes", itemDto.getQuantityBoxes());
            actual.set("unitPrice", itemDto.getUnitPrice());
            actual.set("subtotal", subtotal);
            actual.set("expiryDate", itemDto.getExpiryDate().toString());
            actualItems.add(actual);
        }

        FamilyPurchaseOrder order = new FamilyPurchaseOrder();
        order.setTaskId(task.getTaskId());
        order.setElderId(task.getApplicantId());
        order.setParentId(parentId);
        order.setItemJson(actualItems.toString());
        order.setPurchasePlatform(dto.getPurchasePlatform());
        order.setPurchaseChannel(dto.getPurchaseChannel());
        order.setOrderTime(dto.getOrderTime());
        order.setExpectedArrivalTime(dto.getExpectedArrivalTime());
        order.setActualTotal(actualTotal);
        order.setScreenshotUrl(dto.getScreenshotUrl());
        order.setCarrierCode(dto.getCarrierCode());
        order.setCarrierName(dto.getCarrierName());
        order.setTrackingNo(dto.getTrackingNo());
        order.setLogisticsStatus("ORDERED");
        order.setReceiptStatus("PENDING");
        order.setNote(dto.getNote());
        orderMapper.insert(order);

        for (OrderItemDTO itemDto : dto.getItems()) {
            PurchaseRecordDTO purchase = new PurchaseRecordDTO();
            purchase.setUserId(task.getApplicantId());
            purchase.setPrescriptionId(itemDto.getPrescriptionId());
            purchase.setPurchaseDate(dto.getOrderTime().toLocalDate());
            purchase.setPurchaseTime(dto.getOrderTime());
            purchase.setQuantityBoxes(itemDto.getQuantityBoxes());
            purchase.setUnitPrice(itemDto.getUnitPrice());
            purchase.setExpiryDate(itemDto.getExpiryDate());
            purchase.setOperatorId(operatorId);
            purchase.setPurchasePlatform(dto.getPurchasePlatform());
            purchase.setPurchaseChannel(dto.getPurchaseChannel());
            purchase.setOrderId(order.getOrderId());
            purchase.setProofUrl(dto.getScreenshotUrl());
            // 家庭代购无论线上或线下，都必须由收货人完成逐项核验后才能入库。
            purchase.setReceiptStatus(0);
            purchaseRecordService.addPurchaseRecord(purchase);
        }

        FamilyFundTransaction expense = new FamilyFundTransaction();
        expense.setElderId(task.getApplicantId());
        expense.setParentId(parentId);
        expense.setTransactionType("PURCHASE");
        expense.setAmount(actualTotal.negate());
        expense.setPaymentPlatform(dto.getPurchasePlatform());
        expense.setTransactionTime(dto.getOrderTime());
        expense.setReferenceOrderId(order.getOrderId());
        expense.setProofUrl(dto.getScreenshotUrl());
        expense.setNote("代购订单扣款");
        fundMapper.insert(expense);

        addLogisticsEvent(order.getOrderId(), "ORDERED", "子女已完成下单", dto.getPurchasePlatform() + "，订单金额¥" + actualTotal, dto.getOrderTime(), "SYSTEM");
        Long orderScreenshotId = fileIdFromContentUrl(dto.getScreenshotUrl());
        if (orderScreenshotId != null) {
            PurchaseEvidenceDTO evidence = new PurchaseEvidenceDTO();
            evidence.setEvidenceType("ORDER_SCREENSHOT");
            evidence.setFileId(orderScreenshotId);
            evidence.setTitle("购药下单截图");
            evidence.setOccurredTime(dto.getOrderTime());
            evidence.setAmount(actualTotal);
            evidence.setPurchasePlatform(dto.getPurchasePlatform());
            evidence.setNote("创建家庭代购订单时自动归档");
            purchaseEvidenceService.add(order.getOrderId(), evidence, operatorId,
                    systemAdmin ? "ADMIN" : "GUARDIAN");
        }
        task.setStatus("APPROVED");
        task.setHandlerComment("已代购并生成订单" + order.getOrderId());
        approvalTaskMapper.updateById(task);
        notificationService.notify(task.getApplicantId(), "子女已为你下单", "本次共购买" + actualItems.size() + "种药品，实付¥" + actualTotal + "，可在订单物流中查看进度。", "FAMILY_ORDER", order.getOrderId());
        return order;
    }

    public List<FamilyPurchaseOrder> listOrders(Long elderId, Long parentId) {
        LambdaQueryWrapper<FamilyPurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (elderId != null) wrapper.eq(FamilyPurchaseOrder::getElderId, elderId);
        if (parentId != null) wrapper.eq(FamilyPurchaseOrder::getParentId, parentId);
        wrapper.orderByDesc(FamilyPurchaseOrder::getOrderTime);
        return orderMapper.selectList(wrapper);
    }

    public Map<String, Object> orderDetail(Long orderId) {
        FamilyPurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        List<LogisticsEvent> events = logisticsMapper.selectList(new LambdaQueryWrapper<LogisticsEvent>()
                .eq(LogisticsEvent::getOrderId, orderId).orderByDesc(LogisticsEvent::getOccurredTime));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", JSONUtil.parseArray(order.getItemJson()));
        result.put("events", events);
        result.put("receiptVerifications", receiptVerificationMapper.selectList(
                new LambdaQueryWrapper<OrderReceiptVerification>()
                        .eq(OrderReceiptVerification::getOrderId, orderId)
                        .orderByDesc(OrderReceiptVerification::getCreateTime)));
        result.put("trackingQueryUrl", buildTrackingUrl(order));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateLogistics(Long orderId, LogisticsUpdateDTO dto, Long operatorId, boolean systemAdmin) {
        FamilyPurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null || (!systemAdmin && !operatorId.equals(order.getParentId()))) throw new BusinessException("订单不存在或无权操作");
        if ("VERIFIED".equals(order.getReceiptStatus()) && !"DELIVERED".equals(dto.getStatusCode())) {
            throw new BusinessException("订单已完成收货核验，物流状态不能再回退");
        }
        order.setLogisticsStatus(dto.getStatusCode());
        if (dto.getCarrierCode() != null) order.setCarrierCode(dto.getCarrierCode());
        if (dto.getCarrierName() != null) order.setCarrierName(dto.getCarrierName());
        if (dto.getTrackingNo() != null) order.setTrackingNo(dto.getTrackingNo());
        orderMapper.updateById(order);
        addLogisticsEvent(orderId, dto.getStatusCode(), dto.getStatusText(), dto.getDetail(), dto.getOccurredTime(), "MANUAL");
        notificationService.notify(order.getElderId(), "订单物流有更新", dto.getStatusText() + (dto.getDetail() == null ? "" : "：" + dto.getDetail()), "FAMILY_ORDER", orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderReceiptVerification verifyReceipt(Long orderId, ReceiptVerificationDTO dto, Long elderId) {
        FamilyPurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null || !elderId.equals(order.getElderId())) throw new BusinessException("订单不存在或无权操作");
        if ("VERIFIED".equals(order.getReceiptStatus())) throw new BusinessException("该订单已经完成收货核验");
        if ("EXCEPTION".equals(order.getReceiptStatus())) throw new BusinessException("收货异常正在由家属处理，请处理后再重新核验");
        Long receiptPhotoId = fileIdFromContentUrl(dto.getPhotoUrl());
        if (receiptPhotoId != null) {
            fileAssetService.attachBusiness(receiptPhotoId, "RECEIPT_PHOTO",
                    "ORDER_RECEIPT", orderId, order.getParentId(), elderId, "ELDER");
        } else if (dto.getPhotoUrl() == null || !dto.getPhotoUrl().startsWith("/api/uploads/")) {
            throw new BusinessException("请上传系统保存的收货照片");
        }

        JSONArray expectedItems = JSONUtil.parseArray(order.getItemJson());
        Map<Long, ReceiptItemCheckDTO> submitted = new LinkedHashMap<>();
        for (ReceiptItemCheckDTO check : dto.getItems()) {
            if (submitted.put(check.getPrescriptionId(), check) != null) throw new BusinessException("同一种药品不能重复核验");
        }
        if (submitted.size() != expectedItems.size()) throw new BusinessException("请完整核对订单中的每一种药品");

        boolean allMatched = true;
        JSONArray checkItems = new JSONArray();
        List<String> problems = new ArrayList<>();
        for (int i = 0; i < expectedItems.size(); i++) {
            JSONObject expected = expectedItems.getJSONObject(i);
            Long prescriptionId = expected.getLong("prescriptionId");
            ReceiptItemCheckDTO actual = submitted.remove(prescriptionId);
            if (actual == null) throw new BusinessException("核验药品与订单不一致");

            String expectedApproval = expected.getStr("approvalNumber");
            if (expectedApproval == null || expectedApproval.trim().isEmpty()) {
                Prescription prescription = prescriptionMapper.selectById(prescriptionId);
                Medicine medicine = prescription == null ? null : medicineMapper.selectById(prescription.getMedicineId());
                expectedApproval = medicine == null ? "" : medicine.getApprovalNumber();
            }
            int expectedQuantity = expected.getInt("quantityBoxes", 0);
            boolean quantityMatched = expectedQuantity == actual.getReceivedQuantityBoxes();
            boolean approvalMatched = normalizeApprovalNumber(expectedApproval).equals(normalizeApprovalNumber(actual.getApprovalNumber()));
            boolean packageMatched = Boolean.TRUE.equals(actual.getPackageIntact());
            boolean specificationMatched = Boolean.TRUE.equals(actual.getSpecificationConfirmed());
            boolean itemMatched = quantityMatched && approvalMatched && packageMatched && specificationMatched;
            allMatched = allMatched && itemMatched;

            JSONObject itemCheck = new JSONObject();
            itemCheck.set("prescriptionId", prescriptionId);
            itemCheck.set("medicineName", expected.getStr("medicineName"));
            itemCheck.set("expectedQuantityBoxes", expectedQuantity);
            itemCheck.set("receivedQuantityBoxes", actual.getReceivedQuantityBoxes());
            itemCheck.set("quantityMatched", quantityMatched);
            itemCheck.set("expectedApprovalNumber", expectedApproval);
            itemCheck.set("receivedApprovalNumber", actual.getApprovalNumber().trim());
            itemCheck.set("approvalMatched", approvalMatched);
            itemCheck.set("packageIntact", packageMatched);
            itemCheck.set("expectedSpecification", expected.getStr("specification"));
            itemCheck.set("specificationConfirmed", specificationMatched);
            itemCheck.set("matched", itemMatched);
            checkItems.add(itemCheck);

            if (!itemMatched) {
                List<String> itemProblems = new ArrayList<>();
                if (!quantityMatched) itemProblems.add("数量不符");
                if (!approvalMatched) itemProblems.add("国药准字号不符");
                if (!packageMatched) itemProblems.add("包装破损");
                if (!specificationMatched) itemProblems.add("规格或剂量不符");
                problems.add(expected.getStr("medicineName") + "：" + String.join("、", itemProblems));
            }
        }
        if (!submitted.isEmpty()) throw new BusinessException("核验中包含订单以外的药品");

        OrderReceiptVerification verification = new OrderReceiptVerification();
        verification.setOrderId(orderId);
        verification.setElderId(elderId);
        verification.setCheckResult(allMatched ? "VERIFIED" : "EXCEPTION");
        verification.setPhotoUrl(dto.getPhotoUrl());
        verification.setCheckJson(checkItems.toString());
        verification.setNote(dto.getNote());
        receiptVerificationMapper.insert(verification);

        if (allMatched) {
            List<PurchaseRecord> records = purchaseRecordMapper.selectList(
                    new LambdaQueryWrapper<PurchaseRecord>().eq(PurchaseRecord::getOrderId, orderId));
            for (PurchaseRecord record : records) {
                if (!Integer.valueOf(1).equals(record.getReceiptStatus())) purchaseRecordService.confirmFamilyReceipt(record.getPurchaseId());
            }
            order.setReceiptStatus("VERIFIED");
            order.setReceivedTime(LocalDateTime.now());
            order.setLogisticsStatus("DELIVERED");
            orderMapper.updateById(order);
            addLogisticsEvent(orderId, "DELIVERED", "收货核验通过", "数量、国药准字号和包装均已核对，药品已计入库存", LocalDateTime.now(), "SYSTEM");
            notificationService.notify(order.getParentId(), "药品已安全收到", "订单" + orderId + "已完成逐项核验，数量和国药准字号一致。", "FAMILY_ORDER", orderId);
        } else {
            order.setReceiptStatus("EXCEPTION");
            orderMapper.updateById(order);
            String problemText = String.join("；", problems);
            addLogisticsEvent(orderId, "RECEIPT_EXCEPTION", "收货核验发现异常", problemText, LocalDateTime.now(), "SYSTEM");
            notificationService.notify(order.getParentId(), "收货核验发现异常，请尽快处理", "订单" + orderId + "存在问题：" + problemText + "。药品尚未计入库存。", "RECEIPT_EXCEPTION", orderId);
        }
        return verification;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reopenReceiptVerification(Long orderId, Long operatorId, boolean systemAdmin) {
        FamilyPurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null || (!systemAdmin && !operatorId.equals(order.getParentId()))) throw new BusinessException("订单不存在或无权操作");
        if (!"EXCEPTION".equals(order.getReceiptStatus())) throw new BusinessException("只有收货异常订单可以重新开启核验");
        order.setReceiptStatus("PENDING");
        orderMapper.updateById(order);
        addLogisticsEvent(orderId, "RECEIPT_REOPENED", "家属已处理收货异常", "请收到补发或更换药品后重新核验", LocalDateTime.now(), "SYSTEM");
        notificationService.notify(order.getElderId(), "请重新核验收到的药品", "家属已处理订单" + orderId + "的收货异常，请收到正确药品后重新拍照核验。", "FAMILY_ORDER", orderId);
    }

    public BigDecimal getBalance(Long elderId) {
        BigDecimal balance = fundMapper.selectBalance(elderId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    public List<FamilyFundTransaction> listFunds(Long elderId) {
        return fundMapper.selectList(new LambdaQueryWrapper<FamilyFundTransaction>()
                .eq(FamilyFundTransaction::getElderId, elderId).orderByDesc(FamilyFundTransaction::getTransactionTime));
    }

    public FamilyFundTransaction addFund(FundTransactionDTO dto, Long operatorId, boolean systemAdmin) {
        SysUser elder = userMapper.selectById(dto.getElderId());
        if (elder == null || elder.getBindParentId() == null
                || (!systemAdmin && !operatorId.equals(elder.getBindParentId()))) {
            throw new BusinessException("安心用药账号不存在或未绑定当前家庭守护人");
        }
        Long parentId = elder.getBindParentId();
        if (!"TRANSFER".equals(dto.getTransactionType()) && !"ADJUST".equals(dto.getTransactionType())) throw new BusinessException("仅允许登记转账或余额调整");
        FamilyFundTransaction item = new FamilyFundTransaction();
        item.setElderId(dto.getElderId()); item.setParentId(parentId); item.setTransactionType(dto.getTransactionType());
        item.setAmount(dto.getAmount()); item.setPaymentPlatform(dto.getPaymentPlatform()); item.setTransactionTime(dto.getTransactionTime());
        item.setProofUrl(dto.getProofUrl()); item.setNote(dto.getNote());
        fundMapper.insert(item);
        notificationService.notify(dto.getElderId(), "购药资金有新记录", "子女登记了¥" + dto.getAmount() + "的" + ("TRANSFER".equals(dto.getTransactionType()) ? "转账" : "余额调整") + "，当前余额¥" + getBalance(dto.getElderId()), "FUND", item.getTransactionId());
        return item;
    }

    public List<Map<String, Object>> listNotifications(Long recipientId) {
        List<UserNotification> notifications = notificationMapper.selectList(
                new LambdaQueryWrapper<UserNotification>()
                        .eq(UserNotification::getRecipientId, recipientId)
                        .orderByDesc(UserNotification::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (UserNotification notification : notifications) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("notificationId", notification.getNotificationId());
            item.put("recipientId", notification.getRecipientId());
            item.put("title", notification.getTitle());
            item.put("content", notification.getContent());
            item.put("bizType", notification.getBizType());
            item.put("bizId", notification.getBizId());
            item.put("readStatus", notification.getReadStatus());
            item.put("emailStatus", notification.getEmailStatus());
            item.put("createTime", notification.getCreateTime());
            item.put("readTime", notification.getReadTime());
            enrichBusinessProgress(item, notification);
            result.add(item);
        }
        return result;
    }

    private void enrichBusinessProgress(Map<String, Object> target, UserNotification notification) {
        String status = notification.getReadStatus() != null && notification.getReadStatus() == 1
                ? "READ" : "UNREAD";
        String text = "READ".equals(status) ? "已读" : "未读";
        String type = "READ".equals(status) ? "info" : "danger";

        if ("PURCHASE_REQUEST".equals(notification.getBizType()) && notification.getBizId() != null) {
            ApprovalTask task = approvalTaskMapper.selectById(notification.getBizId());
            if (task != null && "PENDING".equals(task.getStatus())) {
                status = "READ".equals(status) ? "READ_PENDING" : "UNREAD_PENDING";
                text = "READ_PENDING".equals(status) ? "已读待处理" : "未读待处理";
                type = "warning";
            } else if (task != null && "REJECTED".equals(task.getStatus())) {
                status = "REJECTED"; text = "已驳回"; type = "danger";
            } else if (task != null && "APPROVED".equals(task.getStatus())) {
                FamilyPurchaseOrder order = orderMapper.selectOne(new LambdaQueryWrapper<FamilyPurchaseOrder>()
                        .eq(FamilyPurchaseOrder::getTaskId, task.getTaskId()).last("LIMIT 1"));
                if (order == null) {
                    status = "APPROVED"; text = "已审批"; type = "success";
                } else {
                    applyOrderProgress(target, order);
                    return;
                }
            }
        } else if (("FAMILY_ORDER".equals(notification.getBizType())
                || "RECEIPT_EXCEPTION".equals(notification.getBizType())) && notification.getBizId() != null) {
            FamilyPurchaseOrder order = orderMapper.selectById(notification.getBizId());
            if (order != null) {
                applyOrderProgress(target, order);
                return;
            }
        }
        target.put("bizStatus", status);
        target.put("bizStatusText", text);
        target.put("bizStatusType", type);
    }

    private void applyOrderProgress(Map<String, Object> target, FamilyPurchaseOrder order) {
        String status;
        String text;
        String type;
        if ("VERIFIED".equals(order.getReceiptStatus())) {
            status = "COMPLETED"; text = "已完成"; type = "success";
        } else if ("EXCEPTION".equals(order.getReceiptStatus())) {
            status = "RECEIPT_EXCEPTION"; text = "收货异常"; type = "danger";
        } else if ("DELIVERED".equals(order.getLogisticsStatus())) {
            status = "AWAITING_RECEIPT"; text = "待收货核验"; type = "warning";
        } else if ("IN_TRANSIT".equals(order.getLogisticsStatus())
                || "OUT_FOR_DELIVERY".equals(order.getLogisticsStatus())
                || "PICKED_UP".equals(order.getLogisticsStatus())) {
            status = "IN_TRANSIT"; text = "配送中"; type = "primary";
        } else {
            status = "ORDERED"; text = "已购买"; type = "primary";
        }
        target.put("bizStatus", status);
        target.put("bizStatusText", text);
        target.put("bizStatusType", type);
    }

    public long unreadCount(Long recipientId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<UserNotification>().eq(UserNotification::getRecipientId, recipientId).eq(UserNotification::getReadStatus, 0));
    }

    public void markRead(Long notificationId, Long recipientId) {
        UserNotification n = notificationMapper.selectById(notificationId);
        if (n == null || !recipientId.equals(n.getRecipientId())) throw new BusinessException("通知不存在");
        n.setReadStatus(1); n.setReadTime(LocalDateTime.now()); notificationMapper.updateById(n);
    }

    public void sendCheckIn(Long elderId) {
        SysUser elder = userMapper.selectById(elderId);
        if (elder == null || !"ELDER".equals(elder.getRole()) || elder.getBindParentId() == null) {
            throw new BusinessException("尚未绑定子女账号");
        }
        notificationService.notify(elder.getBindParentId(), elder.getRealName() + "发来报平安", "老人刚刚点击了“我今天很好”，无需立即回电。", "CHECK_IN", elderId);
    }

    private void addLogisticsEvent(Long orderId, String code, String text, String detail, LocalDateTime time, String source) {
        LogisticsEvent event = new LogisticsEvent();
        event.setOrderId(orderId); event.setStatusCode(code); event.setStatusText(text); event.setDetail(detail);
        event.setOccurredTime(time == null ? LocalDateTime.now() : time); event.setSource(source); logisticsMapper.insert(event);
    }

    private String buildTrackingUrl(FamilyPurchaseOrder order) {
        if (order.getTrackingNo() == null || order.getTrackingNo().trim().isEmpty() || logisticsUrlTemplate == null || logisticsUrlTemplate.isEmpty()) return null;
        try {
            return String.format(logisticsUrlTemplate, URLEncoder.encode(order.getCarrierCode() == null ? "" : order.getCarrierCode(), "UTF-8"), URLEncoder.encode(order.getTrackingNo(), "UTF-8"));
        } catch (Exception e) { return null; }
    }

    private String normalizeApprovalNumber(String value) {
        if (value == null) return "";
        return value.replaceAll("\\s+", "")
                .replaceFirst("^(?i:国药准字)", "")
                .toUpperCase(Locale.ROOT);
    }

    private Long fileIdFromContentUrl(String value) {
        if (value == null || !value.matches("^/api/files/\\d+/content$")) return null;
        try {
            return Long.valueOf(value.substring("/api/files/".length(), value.length() - "/content".length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String reasonLabel(String reason) {
        if ("LOW_STOCK".equals(reason)) return "药品快用完";
        if ("LOST".equals(reason)) return "药品丢失或损坏";
        if ("NEW_PRESCRIPTION".equals(reason)) return "新增或更换药品";
        if ("TRAVEL".equals(reason)) return "外出前备药";
        return "其他购药需求";
    }
}
