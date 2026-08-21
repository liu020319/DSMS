package com.medicine.service;

import com.medicine.common.BusinessException;
import com.medicine.entity.FileAsset;
import com.medicine.entity.SysUser;
import com.medicine.mapper.FileAssetMapper;
import com.medicine.mapper.SysUserMapper;
import com.medicine.storage.FileStorage;
import com.medicine.storage.FileStorageRouter;
import com.medicine.storage.ObsFileStorage;
import com.medicine.vo.FileUploadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileAssetServiceTest {
    private FileAssetMapper fileAssetMapper;
    private SysUserMapper userMapper;
    private FileStorageRouter storageRouter;
    private FileStorage storage;
    private FileAssetService service;

    @BeforeEach
    void setUp() {
        fileAssetMapper = mock(FileAssetMapper.class);
        userMapper = mock(SysUserMapper.class);
        storageRouter = mock(FileStorageRouter.class);
        storage = mock(FileStorage.class);
        ObsFileStorage obsFileStorage = mock(ObsFileStorage.class);
        service = new FileAssetService(fileAssetMapper, userMapper, storageRouter,
                obsFileStorage, 5 * 1024 * 1024L, "dsms/test");

        SysUser guardian = user(10L, "GUARDIAN", null);
        when(userMapper.selectById(10L)).thenReturn(guardian);
        when(storageRouter.active()).thenReturn(storage);
        when(storage.provider()).thenReturn("LOCAL");
    }

    @Test
    void uploadCreatesProtectedFileLedger() throws Exception {
        when(fileAssetMapper.insert(any(FileAsset.class))).thenAnswer(invocation -> {
            FileAsset asset = invocation.getArgument(0);
            asset.setFileId(88L);
            return 1;
        });

        FileUploadVO result = service.uploadImage(png(), "PAYMENT", null, null, 10L, "GUARDIAN");

        assertEquals(88L, result.getFileId());
        assertEquals("/api/files/88/content", result.getUrl());
        assertEquals("PAYMENT", result.getCategory());
        verify(storage).put(anyString(), any(byte[].class));
        verify(fileAssetMapper).insert(any(FileAsset.class));
    }

    @Test
    void databaseFailureCompensatesUploadedObject() throws Exception {
        doThrow(new RuntimeException("database unavailable"))
                .when(fileAssetMapper).insert(any(FileAsset.class));

        assertThrows(RuntimeException.class,
                () -> service.uploadImage(png(), "INVOICE", null, null, 10L, "GUARDIAN"));

        verify(storage).put(anyString(), any(byte[].class));
        verify(storage).delete(anyString());
    }

    @Test
    void userCannotReadAnotherFamilyFile() {
        FileAsset asset = new FileAsset();
        asset.setFileId(88L);
        asset.setOwnerUserId(10L);
        asset.setFamilyId(10L);
        asset.setAccessScope("FAMILY");
        asset.setStatus("ACTIVE");
        asset.setDeleted(0);
        when(fileAssetMapper.selectById(88L)).thenReturn(asset);
        when(userMapper.selectById(21L)).thenReturn(user(21L, "ELDER", 20L));

        assertThrows(BusinessException.class, () -> service.open(88L, 21L, "ELDER"));
        verify(storageRouter, never()).byProvider(anyString());
    }

    private SysUser user(Long id, String role, Long parentId) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setRole(role);
        user.setBindParentId(parentId);
        user.setStatus(1);
        return user;
    }

    private MockMultipartFile png() {
        byte[] bytes = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0, 0, 0, 0, 0, 0, 0, 0};
        return new MockMultipartFile("file", "payment.png", "image/png", bytes);
    }
}
