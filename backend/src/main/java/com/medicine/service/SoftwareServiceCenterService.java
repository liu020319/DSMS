package com.medicine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.common.BusinessException;
import com.medicine.dto.SoftwareMilestoneDTO;
import com.medicine.dto.SoftwareServiceRequestDTO;
import com.medicine.dto.SoftwareServiceStatusDTO;
import com.medicine.dto.SoftwareWorkOrderDTO;
import com.medicine.dto.SoftwareWorkOrderStatusDTO;
import com.medicine.entity.SoftwareServiceMilestone;
import com.medicine.entity.SoftwareServiceRequest;
import com.medicine.entity.SoftwareServiceWorkOrder;
import com.medicine.mapper.SoftwareServiceMilestoneMapper;
import com.medicine.mapper.SoftwareServiceRequestMapper;
import com.medicine.mapper.SoftwareServiceWorkOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SoftwareServiceCenterService {
    private static final List<String> SERVICE_TYPES = Arrays.asList(
            "DEVELOPMENT", "GUIDANCE", "DEBUG", "DEPLOYMENT", "CONSULTING");
    private static final List<String> REQUEST_STATUSES = Arrays.asList(
            "SUBMITTED", "ASSESSING", "QUOTED", "IN_PROGRESS", "DELIVERED", "CLOSED", "CANCELLED");
    private static final List<String> MILESTONE_STATUSES = Arrays.asList(
            "PENDING", "IN_PROGRESS", "COMPLETED", "BLOCKED");
    private static final List<String> WORK_ORDER_TYPES = Arrays.asList(
            "QUESTION", "BUG", "DEPLOYMENT", "AFTER_SALES");
    private static final List<String> WORK_ORDER_STATUSES = Arrays.asList(
            "OPEN", "PROCESSING", "RESOLVED", "CLOSED");
    private static final Map<String, java.util.Set<String>> REQUEST_TRANSITIONS = buildRequestTransitions();
    private static final Map<String, java.util.Set<String>> WORK_ORDER_TRANSITIONS = buildWorkOrderTransitions();

    private final SoftwareServiceRequestMapper requestMapper;
    private final SoftwareServiceMilestoneMapper milestoneMapper;
    private final SoftwareServiceWorkOrderMapper workOrderMapper;
    private final NotificationService notificationService;

    public SoftwareServiceCenterService(SoftwareServiceRequestMapper requestMapper,
                                        SoftwareServiceMilestoneMapper milestoneMapper,
                                        SoftwareServiceWorkOrderMapper workOrderMapper,
                                        NotificationService notificationService) {
        this.requestMapper = requestMapper;
        this.milestoneMapper = milestoneMapper;
        this.workOrderMapper = workOrderMapper;
        this.notificationService = notificationService;
    }

    public List<SoftwareServiceRequest> listRequests(Long userId, String role, String status) {
        LambdaQueryWrapper<SoftwareServiceRequest> wrapper = new LambdaQueryWrapper<SoftwareServiceRequest>()
                .eq(!isAdmin(role), SoftwareServiceRequest::getRequesterUserId, userId)
                .eq(hasText(status), SoftwareServiceRequest::getStatus, normalize(status))
                .orderByDesc(SoftwareServiceRequest::getUpdateTime)
                .orderByDesc(SoftwareServiceRequest::getRequestId)
                .last("LIMIT 300");
        return requestMapper.selectList(wrapper);
    }

    public Map<String, Object> detail(Long requestId, Long userId, String role) {
        SoftwareServiceRequest request = requireVisibleRequest(requestId, userId, role);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("request", request);
        result.put("milestones", milestoneMapper.selectList(new LambdaQueryWrapper<SoftwareServiceMilestone>()
                .eq(SoftwareServiceMilestone::getRequestId, requestId)
                .orderByAsc(SoftwareServiceMilestone::getSortNo)
                .orderByAsc(SoftwareServiceMilestone::getMilestoneId)));
        result.put("workOrders", workOrderMapper.selectList(new LambdaQueryWrapper<SoftwareServiceWorkOrder>()
                .eq(SoftwareServiceWorkOrder::getRequestId, requestId)
                .orderByDesc(SoftwareServiceWorkOrder::getUpdateTime)
                .orderByDesc(SoftwareServiceWorkOrder::getWorkOrderId)));
        return result;
    }

    @Transactional
    public SoftwareServiceRequest createRequest(Long userId, SoftwareServiceRequestDTO dto) {
        String type = normalize(dto.getServiceType());
        if (!SERVICE_TYPES.contains(type)) throw new BusinessException(400, "服务类型不正确");
        if (dto.getExpectedDate() != null && dto.getExpectedDate().isBefore(java.time.LocalDate.now())) {
            throw new BusinessException(400, "期望日期不能早于今天");
        }

        SoftwareServiceRequest request = new SoftwareServiceRequest();
        request.setRequesterUserId(userId);
        request.setServiceType(type);
        request.setTitle(clean(dto.getTitle()));
        request.setRequirementText(clean(dto.getRequirementText()));
        request.setTechnologyStack(cleanNullable(dto.getTechnologyStack()));
        request.setBudgetRange(cleanNullable(dto.getBudgetRange()));
        request.setExpectedDate(dto.getExpectedDate());
        request.setContactChannel(cleanNullable(dto.getContactChannel()));
        request.setStatus("SUBMITTED");
        requestMapper.insert(request);
        return request;
    }

    @Transactional
    public SoftwareServiceRequest updateStatus(Long requestId, SoftwareServiceStatusDTO dto, String role) {
        requireAdmin(role);
        SoftwareServiceRequest request = requireRequest(requestId);
        String status = normalize(dto.getStatus());
        if (!REQUEST_STATUSES.contains(status)) throw new BusinessException(400, "需求状态不正确");
        requireTransition(REQUEST_TRANSITIONS, request.getStatus(), status, "需求");
        request.setStatus(status);
        request.setQuoteAmount(dto.getQuoteAmount());
        request.setManagerNote(cleanNullable(dto.getManagerNote()));
        requestMapper.updateById(request);
        notificationService.notify(request.getRequesterUserId(), "软件服务进度已更新",
                "需求《" + request.getTitle() + "》状态已更新为 " + status,
                "SOFTWARE_SERVICE_REQUEST", requestId);
        return request;
    }

    @Transactional
    public SoftwareServiceRequest cancelRequest(Long requestId, Long userId, String role) {
        SoftwareServiceRequest request = requireVisibleRequest(requestId, userId, role);
        if (!isAdmin(role) && !userId.equals(request.getRequesterUserId())) {
            throw new BusinessException(403, "无权取消该需求");
        }
        if (!Arrays.asList("SUBMITTED", "ASSESSING", "QUOTED").contains(request.getStatus())) {
            throw new BusinessException(409, "当前状态不能取消");
        }
        request.setStatus("CANCELLED");
        requestMapper.updateById(request);
        return request;
    }

    @Transactional
    public SoftwareServiceMilestone saveMilestone(Long requestId, Long milestoneId,
                                                   SoftwareMilestoneDTO dto, String role) {
        requireAdmin(role);
        requireRequest(requestId);
        String status = normalize(dto.getStatus());
        if (!MILESTONE_STATUSES.contains(status)) throw new BusinessException(400, "里程碑状态不正确");

        SoftwareServiceMilestone milestone;
        if (milestoneId == null) {
            milestone = new SoftwareServiceMilestone();
            milestone.setRequestId(requestId);
        } else {
            milestone = milestoneMapper.selectById(milestoneId);
            if (milestone == null || !requestId.equals(milestone.getRequestId())) {
                throw new BusinessException(404, "里程碑不存在");
            }
        }
        milestone.setMilestoneName(clean(dto.getMilestoneName()));
        milestone.setMilestoneDescription(cleanNullable(dto.getMilestoneDescription()));
        milestone.setPlannedDate(dto.getPlannedDate());
        milestone.setStatus(status);
        milestone.setSortNo(dto.getSortNo() == null ? 0 : dto.getSortNo());
        milestone.setCompletedTime("COMPLETED".equals(status) ? LocalDateTime.now() : null);
        if (milestone.getMilestoneId() == null) milestoneMapper.insert(milestone);
        else milestoneMapper.updateById(milestone);
        return milestone;
    }

    @Transactional
    public SoftwareServiceWorkOrder createWorkOrder(Long userId, String role, SoftwareWorkOrderDTO dto) {
        SoftwareServiceRequest request = requireVisibleRequest(dto.getRequestId(), userId, role);
        if (!isAdmin(role) && !userId.equals(request.getRequesterUserId())) {
            throw new BusinessException(403, "无权为该需求创建工单");
        }
        String type = normalize(dto.getWorkOrderType());
        if (!WORK_ORDER_TYPES.contains(type)) throw new BusinessException(400, "工单类型不正确");

        SoftwareServiceWorkOrder order = new SoftwareServiceWorkOrder();
        order.setRequestId(dto.getRequestId());
        order.setRequesterUserId(request.getRequesterUserId());
        order.setWorkOrderType(type);
        order.setSubject(clean(dto.getSubject()));
        order.setDescription(clean(dto.getDescription()));
        order.setStatus("OPEN");
        workOrderMapper.insert(order);
        return order;
    }

    public List<SoftwareServiceWorkOrder> listWorkOrders(Long userId, String role, String status) {
        return workOrderMapper.selectList(new LambdaQueryWrapper<SoftwareServiceWorkOrder>()
                .eq(!isAdmin(role), SoftwareServiceWorkOrder::getRequesterUserId, userId)
                .eq(hasText(status), SoftwareServiceWorkOrder::getStatus, normalize(status))
                .orderByDesc(SoftwareServiceWorkOrder::getUpdateTime)
                .orderByDesc(SoftwareServiceWorkOrder::getWorkOrderId)
                .last("LIMIT 300"));
    }

    @Transactional
    public SoftwareServiceWorkOrder updateWorkOrder(Long workOrderId, SoftwareWorkOrderStatusDTO dto,
                                                     Long handlerUserId, String role) {
        requireAdmin(role);
        SoftwareServiceWorkOrder order = workOrderMapper.selectById(workOrderId);
        if (order == null) throw new BusinessException(404, "工单不存在");
        String status = normalize(dto.getStatus());
        if (!WORK_ORDER_STATUSES.contains(status)) throw new BusinessException(400, "工单状态不正确");
        requireTransition(WORK_ORDER_TRANSITIONS, order.getStatus(), status, "工单");
        order.setStatus(status);
        order.setHandlerUserId(handlerUserId);
        order.setResolutionText(cleanNullable(dto.getResolutionText()));
        order.setResolvedTime(Arrays.asList("RESOLVED", "CLOSED").contains(status) ? LocalDateTime.now() : null);
        workOrderMapper.updateById(order);
        notificationService.notify(order.getRequesterUserId(), "软件服务工单已更新",
                "工单《" + order.getSubject() + "》状态已更新为 " + status,
                "SOFTWARE_SERVICE_WORK_ORDER", workOrderId);
        return order;
    }

    private SoftwareServiceRequest requireVisibleRequest(Long requestId, Long userId, String role) {
        SoftwareServiceRequest request = requireRequest(requestId);
        if (!isAdmin(role) && !userId.equals(request.getRequesterUserId())) {
            throw new BusinessException(403, "无权查看该服务需求");
        }
        return request;
    }

    private SoftwareServiceRequest requireRequest(Long requestId) {
        SoftwareServiceRequest request = requestMapper.selectById(requestId);
        if (request == null) throw new BusinessException(404, "服务需求不存在");
        return request;
    }

    private void requireAdmin(String role) {
        if (!isAdmin(role)) throw new BusinessException(403, "仅平台管理员可执行此操作");
    }

    private boolean isAdmin(String role) {
        return "ADMIN".equals(role);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanNullable(String value) {
        if (value == null) return null;
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private void requireTransition(Map<String, java.util.Set<String>> transitions,
                                   String current, String target, String businessName) {
        if (target.equals(current)) return;
        java.util.Set<String> allowed = transitions.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw new BusinessException(409, businessName + "不能从 " + current + " 直接变更为 " + target);
        }
    }

    private static Map<String, java.util.Set<String>> buildRequestTransitions() {
        Map<String, java.util.Set<String>> map = new HashMap<>();
        map.put("SUBMITTED", setOf("ASSESSING", "CANCELLED"));
        map.put("ASSESSING", setOf("QUOTED", "IN_PROGRESS", "CANCELLED"));
        map.put("QUOTED", setOf("IN_PROGRESS", "CANCELLED"));
        map.put("IN_PROGRESS", setOf("DELIVERED"));
        map.put("DELIVERED", setOf("CLOSED", "IN_PROGRESS"));
        map.put("CLOSED", Collections.emptySet());
        map.put("CANCELLED", Collections.emptySet());
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, java.util.Set<String>> buildWorkOrderTransitions() {
        Map<String, java.util.Set<String>> map = new HashMap<>();
        map.put("OPEN", setOf("PROCESSING", "RESOLVED", "CLOSED"));
        map.put("PROCESSING", setOf("RESOLVED", "CLOSED"));
        map.put("RESOLVED", setOf("PROCESSING", "CLOSED"));
        map.put("CLOSED", Collections.emptySet());
        return Collections.unmodifiableMap(map);
    }

    private static java.util.Set<String> setOf(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }
}
