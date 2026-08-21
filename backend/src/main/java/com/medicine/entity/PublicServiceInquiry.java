package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("public_service_inquiry")
public class PublicServiceInquiry {
    @TableId(type = IdType.AUTO)
    private Long inquiryId;
    private String inquiryNo;
    private String contactName;
    private String contactValue;
    private String serviceType;
    private String projectType;
    private String inquiryText;
    private String sourcePath;
    private String status;
    private String publicAccessHash;
    private String clientFingerprint;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
