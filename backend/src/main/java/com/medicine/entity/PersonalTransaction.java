package com.medicine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("personal_transaction")
public class PersonalTransaction {
    @TableId(type = IdType.AUTO)
    private Long transactionId;
    private Long ledgerId;
    private Long accountId;
    private Long ownerUserId;
    private String transactionType;
    private String categoryName;
    private BigDecimal amount;
    private LocalDateTime transactionTime;
    private String counterparty;
    private String note;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
