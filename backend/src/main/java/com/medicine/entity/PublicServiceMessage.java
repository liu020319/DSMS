package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("public_service_message")
public class PublicServiceMessage {
    @TableId(type = IdType.AUTO)
    private Long messageId;
    private Long inquiryId;
    private String senderType;
    private Long senderUserId;
    private String messageText;
    private Integer visibleToVisitor;
    private LocalDateTime createTime;
    @TableLogic
    private Integer deleted;
}
