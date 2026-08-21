package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.entity.FileAsset;
import com.medicine.entity.SysUser;
import com.medicine.mapper.FileAssetMapper;
import com.medicine.mapper.SysUserMapper;
import com.medicine.storage.FileStorage;
import com.medicine.storage.FileStorageRouter;
import com.medicine.storage.ObsFileStorage;
import com.medicine.storage.StoredContent;
import com.medicine.vo.FileUploadVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileAssetService {
    private static final Set<String> CATEGORIES = new HashSet<>(Arrays.asList(
            "GENERAL_IMAGE", "AVATAR", "MEDICINE_IMAGE", "ORDER_SCREENSHOT",
            "CONSULTATION", "PAYMENT", "INVOICE", "FUND_TRANSFER_PROOF", "RECEIPT_PHOTO"));

    private final FileAssetMapper fileAssetMapper;
    private final SysUserMapper userMapper;
    private final FileStorageRouter storageRouter;
    private final ObsFileStorage obsFileStorage;
    private final long maxImageBytes;
    private final String objectPrefix;

    public FileAssetService(FileAssetMapper fileAssetMapper,
                            SysUserMapper userMapper,
                            FileStorageRouter storageRouter,
                            ObsFileStorage obsFileStorage,
                            @Value("${storage.image.max-bytes:5242880}") long maxImageBytes,
                            @Value("${storage.object-prefix:dsms/prod}") String objectPrefix) {
        this.fileAssetMapper = fileAssetMapper;
        this.userMapper = userMapper;
        this.storageRouter = storageRouter;
        this.obsFileStorage = obsFileStorage;
        this.maxImageBytes = maxImageBytes;
        this.objectPrefix = trimSlashes(objectPrefix);
    }

    @Transactional(rollbackFor = Exception.class)
    public FileUploadVO uploadImage(MultipartFile file, String category, String businessType,
                                    Long businessId, Long currentUserId, String role) throws IOException {
        if (currentUserId == null) throw new BusinessException(401, "请先登录");
        if (file == null || file.isEmpty()) throw new BusinessException(400, "请选择图片");
        if (file.getSize() > maxImageBytes) throw new BusinessException(400, "图片大小不能超过5MB");

        String normalizedCategory = normalizeCategory(category);
        byte[] content = file.getBytes();
        DetectedImage image = detectImage(content);
        if (image == null) throw new BusinessException(400, "仅支持内容真实的 JPG、PNG、GIF、WEBP 图片");

        SysUser user = userMapper.selectById(currentUserId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(401, "账号不可用");
        Long familyId = resolveFamilyId(user);
        String accessScope = "MEDICINE_IMAGE".equals(normalizedCategory) ? "AUTHENTICATED" : "FAMILY";
        String objectKey = buildObjectKey(familyId, normalizedCategory, image.extension);
        FileStorage storage = storageRouter.active();

        // 先写对象，再写台账；数据库失败时补偿删除对象，避免产生无人管理的孤儿文件。
        storage.put(objectKey, content);
        try {
            FileAsset asset = new FileAsset();
            asset.setStorageProvider(storage.provider());
            asset.setBucketName("OBS".equals(storage.provider()) ? obsFileStorage.bucket() : null);
            asset.setObjectKey(objectKey);
            asset.setOriginalName(safeOriginalName(file.getOriginalFilename(), image.extension));
            asset.setContentType(image.contentType);
            asset.setFileSize((long) content.length);
            asset.setSha256(sha256(content));
            asset.setFileCategory(normalizedCategory);
            asset.setAccessScope(accessScope);
            asset.setBusinessType(blankToNull(businessType));
            asset.setBusinessId(businessId);
            asset.setOwnerUserId(currentUserId);
            asset.setFamilyId(familyId);
            asset.setStatus("ACTIVE");
            fileAssetMapper.insert(asset);
            return new FileUploadVO(asset.getFileId(), contentUrl(asset.getFileId()), normalizedCategory,
                    asset.getContentType(), asset.getFileSize(), asset.getSha256());
        } catch (RuntimeException ex) {
            try { storage.delete(objectKey); } catch (Exception ignored) { }
            throw ex;
        }
    }

    public DownloadableFile open(Long fileId, Long currentUserId, String role) throws IOException {
        FileAsset asset = activeAsset(fileId);
        requireReadAccess(asset, currentUserId, role);
        StoredContent content = storageRouter.byProvider(asset.getStorageProvider()).get(asset.getObjectKey());
        return new DownloadableFile(asset, content);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long fileId, Long currentUserId, String role) throws IOException {
        FileAsset asset = activeAsset(fileId);
        if (!"ADMIN".equals(role) && !currentUserId.equals(asset.getOwnerUserId())) {
            throw new BusinessException(403, "只有上传人或平台管理员可以删除文件");
        }
        storageRouter.byProvider(asset.getStorageProvider()).delete(asset.getObjectKey());
        asset.setStatus("DELETED");
        asset.setDeleteTime(LocalDateTime.now());
        asset.setDeleted(1);
        fileAssetMapper.updateById(asset);
    }

    public FileAsset requireUsable(Long fileId, String expectedCategory, Long currentUserId, String role) {
        FileAsset asset = activeAsset(fileId);
        requireReadAccess(asset, currentUserId, role);
        if (expectedCategory != null && !expectedCategory.equals(asset.getFileCategory())) {
            throw new BusinessException(400, "文件用途不正确，请重新上传");
        }
        return asset;
    }

    @Transactional(rollbackFor = Exception.class)
    public void attachBusiness(Long fileId, String expectedCategory, String businessType,
                               Long businessId, Long familyId, Long currentUserId, String role) {
        FileAsset asset = requireUsable(fileId, expectedCategory, currentUserId, role);
        if (asset.getBusinessType() != null && (!asset.getBusinessType().equals(businessType)
                || !businessId.equals(asset.getBusinessId()))) {
            throw new BusinessException(400, "该文件已经关联其他业务，请重新上传");
        }
        if (familyId != null && asset.getFamilyId() == null && "ADMIN".equals(role)) {
            asset.setFamilyId(familyId);
        } else if (familyId != null && !familyId.equals(asset.getFamilyId())) {
            throw new BusinessException(403, "文件不属于当前家庭");
        }
        asset.setBusinessType(businessType);
        asset.setBusinessId(businessId);
        fileAssetMapper.updateById(asset);
    }

    private FileAsset activeAsset(Long fileId) {
        FileAsset asset = fileId == null ? null : fileAssetMapper.selectById(fileId);
        if (asset == null || !"ACTIVE".equals(asset.getStatus()) || Integer.valueOf(1).equals(asset.getDeleted())) {
            throw new BusinessException(404, "文件不存在或已删除");
        }
        return asset;
    }

    private void requireReadAccess(FileAsset asset, Long currentUserId, String role) {
        if (currentUserId == null) throw new BusinessException(401, "请先登录");
        if ("AUTHENTICATED".equals(asset.getAccessScope()) || "ADMIN".equals(role)
                || currentUserId.equals(asset.getOwnerUserId())) return;
        SysUser user = userMapper.selectById(currentUserId);
        Long familyId = user == null ? null : resolveFamilyId(user);
        if (familyId != null && familyId.equals(asset.getFamilyId())) return;
        throw new BusinessException(403, "无权查看其他家庭的文件");
    }

    private Long resolveFamilyId(SysUser user) {
        if ("ELDER".equals(user.getRole())) return user.getBindParentId();
        if ("GUARDIAN".equals(user.getRole())) return user.getUserId();
        return null;
    }

    private String buildObjectKey(Long familyId, String category, String extension) {
        String familyPart = familyId == null ? "platform" : "family-" + familyId;
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        return objectPrefix + "/" + familyPart + "/" + category.toLowerCase(Locale.ROOT)
                + "/" + month + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    private String normalizeCategory(String category) {
        String value = category == null ? "GENERAL_IMAGE" : category.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(value)) throw new BusinessException(400, "不支持的文件用途");
        return value;
    }

    private DetectedImage detectImage(byte[] data) {
        if (data == null || data.length < 12) return null;
        if ((data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff)
            return new DetectedImage(".jpg", "image/jpeg");
        if ((data[0] & 0xff) == 0x89 && data[1] == 0x50 && data[2] == 0x4e && data[3] == 0x47)
            return new DetectedImage(".png", "image/png");
        if (data[0] == 'G' && data[1] == 'I' && data[2] == 'F')
            return new DetectedImage(".gif", "image/gif");
        if (data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P')
            return new DetectedImage(".webp", "image/webp");
        return null;
    }

    private String sha256(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            StringBuilder value = new StringBuilder(64);
            for (byte item : digest) value.append(String.format("%02x", item & 0xff));
            return value.toString();
        } catch (Exception e) {
            throw new IllegalStateException("无法计算文件摘要", e);
        }
    }

    private String safeOriginalName(String original, String extension) {
        if (original == null || original.trim().isEmpty()) return "image" + extension;
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_");
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String contentUrl(Long fileId) { return "/api/files/" + fileId + "/content"; }
    private String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private String trimSlashes(String value) { return value == null ? "dsms/prod" : value.replaceAll("^/+|/+$", ""); }

    private static class DetectedImage {
        private final String extension;
        private final String contentType;
        private DetectedImage(String extension, String contentType) { this.extension = extension; this.contentType = contentType; }
    }

    public static class DownloadableFile implements AutoCloseable {
        private final FileAsset asset;
        private final StoredContent content;
        public DownloadableFile(FileAsset asset, StoredContent content) { this.asset = asset; this.content = content; }
        public FileAsset getAsset() { return asset; }
        public StoredContent getContent() { return content; }
        @Override public void close() throws IOException { content.close(); }
    }
}
