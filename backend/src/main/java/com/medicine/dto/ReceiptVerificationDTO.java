package com.medicine.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.util.List;

@Data
public class ReceiptVerificationDTO {
    @NotBlank(message = "请先上传收到的药品照片")
    @Size(max = 500)
    private String photoUrl;

    @Valid
    @NotEmpty(message = "请逐项核对收到的药品")
    private List<ReceiptItemCheckDTO> items;

    @Size(max = 500, message = "说明不能超过500字")
    private String note;
}
