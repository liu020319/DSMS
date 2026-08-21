package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("personal_budget")
public class PersonalBudget {
    @TableId(type = IdType.AUTO)
    private Long budgetId;
    private Long ledgerId;
    private Long ownerUserId;
    private String budgetMonth;
    private String categoryName;
    private BigDecimal budgetAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
