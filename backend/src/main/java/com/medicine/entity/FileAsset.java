package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_asset")
public class FileAsset {
    @TableId(type = IdType.AUTO)
    private Long fileId;
    private String storageProvider;
    private String bucketName;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private String fileCategory;
    private String accessScope;
    private String businessType;
    private Long businessId;
    private Long ownerUserId;
    private Long familyId;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    private LocalDateTime deleteTime;
    @TableLogic
    private Integer deleted;
}
