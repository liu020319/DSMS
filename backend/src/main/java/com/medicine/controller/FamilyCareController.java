package com.medicine.controller;

import com.medicine.common.BusinessException;
import com.medicine.common.Result;
import com.medicine.dto.*;
import com.medicine.entity.*;
import com.medicine.service.FamilyCareService;
import com.medicine.util.AccessControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/family")
public class FamilyCareController {
    @Autowired private FamilyCareService familyCareService;
    @Autowired private AccessControl accessControl;

    @PostMapping("/request")
    public Result<ApprovalTask> submitRequest(@Valid @RequestBody PurchaseRequestDTO dto, HttpServletRequest request) {
        accessControl.requireElder(request);
        accessControl.requireOwnerOrAdmin(request, dto.getElderId());
        return Result.success(familyCareService.submitPurchaseRequest(dto));
    }

    @PostMapping("/order")
    public Result<FamilyPurchaseOrder> createOrder(@Valid @RequestBody FamilyOrderDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        return Result.success(familyCareService.createOrder(dto, userId(request)));
    }

    @GetMapping("/orders")
    public Result<List<FamilyPurchaseOrder>> orders(@RequestParam(required = false) Long elderId, HttpServletRequest request) {
        if ("ELDER".equals(request.getAttribute("role"))) {
            accessControl.requireOwnerOrAdmin(request, elderId);
            return Result.success(familyCareService.listOrders(elderId, null));
        }
        accessControl.requireAdmin(request);
        return Result.success(familyCareService.listOrders(elderId, userId(request)));
    }

    @GetMapping("/order/{id}")
    public Result<Map<String, Object>> orderDetail(@PathVariable("id") Long id, HttpServletRequest request) {
        Map<String, Object> result = familyCareService.orderDetail(id);
        FamilyPurchaseOrder order = (FamilyPurchaseOrder) result.get("order");
        Long current = userId(request);
        if (!current.equals(order.getElderId()) && !current.equals(order.getParentId())) throw new BusinessException(403, "无权查看该订单");
        return Result.success(result);
    }

    @PostMapping("/order/{id}/logistics")
    public Result<Void> updateLogistics(@PathVariable("id") Long id, @Valid @RequestBody LogisticsUpdateDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        familyCareService.updateLogistics(id, dto, userId(request));
        return Result.success();
    }

    @PostMapping("/order/{id}/receipt-check")
    public Result<OrderReceiptVerification> verifyReceipt(@PathVariable("id") Long id,
                                                           @Valid @RequestBody ReceiptVerificationDTO dto,
                                                           HttpServletRequest request) {
        accessControl.requireElder(request);
        return Result.success(familyCareService.verifyReceipt(id, dto, userId(request)));
    }

    @PutMapping("/order/{id}/receipt-reopen")
    public Result<Void> reopenReceipt(@PathVariable("id") Long id, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        familyCareService.reopenReceiptVerification(id, userId(request));
        return Result.success();
    }

    @GetMapping("/fund/{elderId}")
    public Result<Map<String, Object>> fund(@PathVariable Long elderId, HttpServletRequest request) {
        accessControl.requireOwnerOrAdmin(request, elderId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("balance", familyCareService.getBalance(elderId));
        result.put("records", familyCareService.listFunds(elderId));
        return Result.success(result);
    }

    @PostMapping("/fund")
    public Result<FamilyFundTransaction> addFund(@Valid @RequestBody FundTransactionDTO dto, HttpServletRequest request) {
        accessControl.requireAdmin(request);
        if (dto.getAmount().compareTo(BigDecimal.ZERO) == 0) throw new BusinessException("金额不能为0");
        return Result.success(familyCareService.addFund(dto, userId(request)));
    }

    @GetMapping("/notifications")
    public Result<List<UserNotification>> notifications(HttpServletRequest request) {
        return Result.success(familyCareService.listNotifications(userId(request)));
    }

    @GetMapping("/notifications/unread-count")
    public Result<Long> unreadCount(HttpServletRequest request) {
        return Result.success(familyCareService.unreadCount(userId(request)));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        familyCareService.markRead(id, userId(request));
        return Result.success();
    }

    @PostMapping("/check-in")
    public Result<Void> checkIn(HttpServletRequest request) {
        accessControl.requireElder(request);
        familyCareService.sendCheckIn(userId(request));
        return Result.success();
    }

    private Long userId(HttpServletRequest request) { return (Long) request.getAttribute("userId"); }
}
