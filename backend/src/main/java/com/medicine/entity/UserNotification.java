package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_notification")
public class UserNotification {
    @TableId(type = IdType.AUTO)
    private Long notificationId;
    private Long recipientId;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer readStatus;
    private String emailStatus;
    private String emailError;
    private LocalDateTime createTime;
    private LocalDateTime readTime;
    @TableLogic
    private Integer deleted;
}
