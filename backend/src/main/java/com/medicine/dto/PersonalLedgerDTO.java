package com.medicine.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class PersonalLedgerDTO {
    @NotBlank(message = "账本名称不能为空")
    @Size(max = 80, message = "账本名称最多80个字符")
    private String ledgerName;
}
