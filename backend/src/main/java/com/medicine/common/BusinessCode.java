package com.medicine.common;

import lombok.Getter;

@Getter
public enum BusinessCode {
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    BUSINESS_ERROR(500, "业务异常"),
    STOCK_NEGATIVE(5001, "库存不足，禁止负库存"),
    APPROVAL_PENDING(5002, "该申请待审批中，请勿重复提交"),
    MEDICINE_EXISTS(5003, "国药准字号已存在"),
    USER_EXISTS(5004, "用户名已存在");

    private final Integer code;
    private final String message;

    BusinessCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
