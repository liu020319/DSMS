package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("software_service_milestone")
public class SoftwareServiceMilestone {
    @TableId(type = IdType.AUTO)
    private Long milestoneId;
    private Long requestId;
    private String milestoneName;
    private String milestoneDescription;
    private LocalDate plannedDate;
    private String status;
    private Integer sortNo;
    private LocalDateTime completedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
