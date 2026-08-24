package com.medicine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medicine.common.BusinessException;
import com.medicine.dto.PublicInquiryMessageDTO;
import com.medicine.dto.PublicServiceInquiryDTO;
import com.medicine.entity.PublicServiceInquiry;
import com.medicine.entity.PublicServiceMessage;
import com.medicine.entity.SysUser;
import com.medicine.mapper.PublicServiceInquiryMapper;
import com.medicine.mapper.PublicServiceMessageMapper;
import com.medicine.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PublicServiceInquiryService {
    private static final String NOTIFICATION_BIZ_TYPE = "SOFTWARE_SERVICE_INQUIRY";
    private static final List<String> SERVICE_TYPES = Arrays.asList(
            "GUIDANCE", "DEVELOPMENT", "DEBUG", "DEPLOYMENT", "CONSULTING");
    private static final List<String> INQUIRY_STATUSES = Arrays.asList("NEW", "CONTACTED", "CLOSED");
    private static final int MAX_SUBMITS_PER_HOUR = 5;
    private static final int MAX_MESSAGES_PER_HOUR = 20;
    private static final int MAX_QUERIES_PER_HOUR = 60;

    private final PublicServiceInquiryMapper inquiryMapper;
    private final PublicServiceMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;
    private final HumanVerificationService verificationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, RateWindow> submitRates = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> messageRates = new ConcurrentHashMap<>();
    private final Map<String, RateWindow> queryRates = new ConcurrentHashMap<>();

    public PublicServiceInquiryService(PublicServiceInquiryMapper inquiryMapper,
                                       PublicServiceMessageMapper messageMapper,
                                       SysUserMapper userMapper,
                                       NotificationService notificationService,
                                       HumanVerificationService verificationService) {
        this.inquiryMapper = inquiryMapper;
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
        this.verificationService = verificationService;
    }

    @Transactional
    public Map<String, String> submit(PublicServiceInquiryDTO dto, String clientAddress) {
        verificationService.consume(dto.getHumanToken(), clientAddress);
        enforceRateLimit(submitRates, clientAddress, MAX_SUBMITS_PER_HOUR, "提交过于频繁，请一小时后再试");
        String serviceType = normalize(dto.getServiceType());
        if (!SERVICE_TYPES.contains(serviceType)) throw new BusinessException(400, "服务类型不正确");

        String accessCode = createAccessCode();
        PublicServiceInquiry inquiry = new PublicServiceInquiry();
        inquiry.setInquiryNo(nextInquiryNo());
        inquiry.setContactName(clean(dto.getContactName()));
        inquiry.setContactValue(clean(dto.getContactValue()));
        inquiry.setServiceType(serviceType);
        inquiry.setProjectType(cleanNullable(dto.getProjectType()));
        inquiry.setInquiryText(clean(dto.getInquiryText()));
        inquiry.setSourcePath(cleanNullable(dto.getSourcePath()) == null ? "/cloud-hub/" : clean(dto.getSourcePath()));
        inquiry.setStatus("NEW");
        inquiry.setPublicAccessHash(sha256(accessCode));
        inquiry.setClientFingerprint(sha256(clientAddress));
        inquiryMapper.insert(inquiry);

        notifyAdmins("收到新的软件服务咨询",
                "咨询编号：" + inquiry.getInquiryNo() + "\n称呼：" + inquiry.getContactName()
                        + "\n服务类型：" + inquiry.getServiceType() + "\n请登录服务工作台查看并联系咨询人。",
                inquiry.getInquiryId());

        Map<String, String> result = new LinkedHashMap<>();
        result.put("inquiryNo", inquiry.getInquiryNo());
        result.put("accessCode", accessCode);
        result.put("status", "RECEIVED");
        return result;
    }

    public Map<String, Object> publicDetail(String inquiryNo, String accessCode, String clientAddress) {
        enforceRateLimit(queryRates, clientAddress, MAX_QUERIES_PER_HOUR, "查询过于频繁，请一小时后再试");
        PublicServiceInquiry inquiry = requireAccess(inquiryNo, accessCode);
        Map<String, Object> result = publicView(inquiry);
        List<PublicServiceMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<PublicServiceMessage>()
                .eq(PublicServiceMessage::getInquiryId, inquiry.getInquiryId())
                .eq(PublicServiceMessage::getVisibleToVisitor, 1)
                .orderByAsc(PublicServiceMessage::getCreateTime)
                .orderByAsc(PublicServiceMessage::getMessageId));
        List<Map<String, Object>> publicMessages = new ArrayList<>();
        for (PublicServiceMessage message : messages) {
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("messageId", message.getMessageId());
            safe.put("senderType", message.getSenderType());
            safe.put("messageText", message.getMessageText());
            safe.put("createTime", message.getCreateTime());
            publicMessages.add(safe);
        }
        result.put("messages", publicMessages);
        return result;
    }

    @Transactional
    public void addVisitorMessage(PublicInquiryMessageDTO dto, String clientAddress) {
        verificationService.consume(dto.getHumanToken(), clientAddress);
        enforceRateLimit(messageRates, clientAddress, MAX_MESSAGES_PER_HOUR, "留言过于频繁，请一小时后再试");
        PublicServiceInquiry inquiry = requireAccess(dto.getInquiryNo(), dto.getAccessCode());
        if ("CLOSED".equals(inquiry.getStatus())) throw new BusinessException(409, "该咨询已经关闭，暂时不能继续留言");
        insertMessage(inquiry.getInquiryId(), "VISITOR", null, dto.getMessageText(), 1);
        notifyAdmins("软件服务咨询有新留言",
                "咨询编号：" + inquiry.getInquiryNo() + "\n咨询人：" + inquiry.getContactName()
                        + "\n请登录服务工作台查看新留言。",
                inquiry.getInquiryId());
    }

    public List<Map<String, Object>> adminList(String role, String status) {
        requireAdmin(role);
        List<PublicServiceInquiry> rows = inquiryMapper.selectList(new LambdaQueryWrapper<PublicServiceInquiry>()
                .eq(hasText(status), PublicServiceInquiry::getStatus, normalize(status))
                .orderByDesc(PublicServiceInquiry::getCreateTime)
                .orderByDesc(PublicServiceInquiry::getInquiryId)
                .last("LIMIT 300"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (PublicServiceInquiry row : rows) result.add(adminView(row));
        return result;
    }

    public Map<String, Object> adminDetail(Long inquiryId, String role) {
        requireAdmin(role);
        PublicServiceInquiry inquiry = requireInquiry(inquiryId);
        Map<String, Object> result = adminView(inquiry);
        result.put("messages", messageMapper.selectList(new LambdaQueryWrapper<PublicServiceMessage>()
                .eq(PublicServiceMessage::getInquiryId, inquiryId)
                .orderByAsc(PublicServiceMessage::getCreateTime)
                .orderByAsc(PublicServiceMessage::getMessageId)));
        return result;
    }

    @Transactional
    public void updateStatus(Long inquiryId, String status, String role) {
        requireAdmin(role);
        String normalized = normalize(status);
        if (!INQUIRY_STATUSES.contains(normalized)) throw new BusinessException(400, "咨询状态不正确");
        PublicServiceInquiry inquiry = requireInquiry(inquiryId);
        if ("CLOSED".equals(inquiry.getStatus()) && !"CLOSED".equals(normalized)) {
            throw new BusinessException(409, "已关闭的咨询不能重新开启");
        }
        inquiry.setStatus(normalized);
        inquiryMapper.updateById(inquiry);
    }

    @Transactional
    public void adminReply(Long inquiryId, Long adminUserId, String messageText, String role) {
        requireAdmin(role);
        PublicServiceInquiry inquiry = requireInquiry(inquiryId);
        if ("CLOSED".equals(inquiry.getStatus())) throw new BusinessException(409, "已关闭的咨询不能继续回复");
        insertMessage(inquiryId, "ADMIN", adminUserId, messageText, 1);
        if ("NEW".equals(inquiry.getStatus())) {
            inquiry.setStatus("CONTACTED");
            inquiryMapper.updateById(inquiry);
        }
    }

    @Transactional
    public void adminDelete(Long inquiryId, String role) {
        requireAdmin(role);
        requireInquiry(inquiryId);
        messageMapper.delete(new LambdaQueryWrapper<PublicServiceMessage>()
                .eq(PublicServiceMessage::getInquiryId, inquiryId));
        inquiryMapper.deleteById(inquiryId);
    }

    private void insertMessage(Long inquiryId, String senderType, Long senderUserId,
                               String messageText, int visibleToVisitor) {
        PublicServiceMessage message = new PublicServiceMessage();
        message.setInquiryId(inquiryId);
        message.setSenderType(senderType);
        message.setSenderUserId(senderUserId);
        message.setMessageText(clean(messageText));
        message.setVisibleToVisitor(visibleToVisitor);
        messageMapper.insert(message);
    }

    private PublicServiceInquiry requireAccess(String inquiryNo, String accessCode) {
        PublicServiceInquiry inquiry = inquiryMapper.selectOne(new LambdaQueryWrapper<PublicServiceInquiry>()
                .eq(PublicServiceInquiry::getInquiryNo, clean(inquiryNo)));
        byte[] supplied = sha256Bytes(accessCode);
        byte[] stored = inquiry == null ? new byte[32] : hexToBytes(inquiry.getPublicAccessHash());
        boolean accessMatches = MessageDigest.isEqual(stored, supplied);
        if (inquiry == null || !accessMatches) {
            throw new BusinessException(404, "咨询编号或访问码不正确");
        }
        return inquiry;
    }

    private PublicServiceInquiry requireInquiry(Long inquiryId) {
        PublicServiceInquiry inquiry = inquiryMapper.selectById(inquiryId);
        if (inquiry == null) throw new BusinessException(404, "咨询不存在");
        return inquiry;
    }

    private Map<String, Object> publicView(PublicServiceInquiry inquiry) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inquiryNo", inquiry.getInquiryNo());
        result.put("serviceType", inquiry.getServiceType());
        result.put("projectType", inquiry.getProjectType());
        result.put("inquiryText", inquiry.getInquiryText());
        result.put("status", inquiry.getStatus());
        result.put("createTime", inquiry.getCreateTime());
        result.put("updateTime", inquiry.getUpdateTime());
        return result;
    }

    private Map<String, Object> adminView(PublicServiceInquiry inquiry) {
        Map<String, Object> result = publicView(inquiry);
        result.put("inquiryId", inquiry.getInquiryId());
        result.put("contactName", inquiry.getContactName());
        result.put("contactValue", inquiry.getContactValue());
        result.put("sourcePath", inquiry.getSourcePath());
        return result;
    }

    private void notifyAdmins(String title, String content, Long inquiryId) {
        List<SysUser> admins = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, "ADMIN")
                .eq(SysUser::getStatus, 1));
        for (SysUser admin : admins) {
            notificationService.notify(admin.getUserId(), title, content,
                    NOTIFICATION_BIZ_TYPE, inquiryId);
        }
    }

    private synchronized void enforceRateLimit(Map<String, RateWindow> rates, String clientAddress,
                                               int maximum, String message) {
        long now = System.currentTimeMillis();
        rates.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= 3_600_000L);
        RateWindow window = rates.get(clientAddress);
        if (window == null || now - window.startedAt >= 3_600_000L) {
            rates.put(clientAddress, new RateWindow(now));
            return;
        }
        if (window.count.incrementAndGet() > maximum) throw new BusinessException(429, message);
    }

    private String createAccessCode() {
        byte[] bytes = new byte[18];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String nextInquiryNo() {
        byte[] randomPart = new byte[9];
        secureRandom.nextBytes(randomPart);
        return "LX" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + Base64.getUrlEncoder().withoutPadding().encodeToString(randomPart).toUpperCase();
    }

    private String sha256(String value) {
        StringBuilder out = new StringBuilder();
        for (byte item : sha256Bytes(value)) out.append(String.format("%02x", item));
        return out.toString();
    }

    private byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成安全摘要", e);
        }
    }

    private byte[] hexToBytes(String value) {
        if (value == null || value.length() != 64) return new byte[32];
        byte[] bytes = new byte[32];
        try {
            for (int index = 0; index < bytes.length; index++) {
                bytes[index] = (byte) Integer.parseInt(value.substring(index * 2, index * 2 + 2), 16);
            }
            return bytes;
        } catch (Exception e) {
            return new byte[32];
        }
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new BusinessException(403, "仅平台管理员可处理游客咨询");
    }
    private String normalize(String value) { return value == null ? "" : value.trim().toUpperCase(); }
    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String clean(String value) { return value == null ? "" : value.trim(); }
    private String cleanNullable(String value) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static class RateWindow {
        private final long startedAt;
        private final AtomicInteger count = new AtomicInteger(1);
        private RateWindow(long startedAt) { this.startedAt = startedAt; }
    }
}
