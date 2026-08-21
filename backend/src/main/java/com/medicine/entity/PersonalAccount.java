package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("personal_account")
public class PersonalAccount {
    @TableId(type = IdType.AUTO)
    private Long accountId;
    private Long ledgerId;
    private Long ownerUserId;
    private String accountName;
    private String accountType;
    private BigDecimal initialBalance;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
