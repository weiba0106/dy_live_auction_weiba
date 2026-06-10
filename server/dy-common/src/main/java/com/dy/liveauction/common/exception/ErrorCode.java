package com.dy.liveauction.common.exception;

import lombok.Getter;

/**
 * 错误码枚举 —— 所有已知错误在此统一管理，
 * 新增错误只需加一个枚举值
 */
@Getter
public enum ErrorCode {

    // 通用错误
    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统繁忙"),

    // 用户相关 1xxx
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    BALANCE_INSUFFICIENT(1003, "余额不足"),

    // 竞拍相关 2xxx
    AUCTION_NOT_FOUND(2001, "竞拍不存在"),
    AUCTION_NOT_STARTED(2002, "竞拍尚未开始"),
    AUCTION_ALREADY_ENDED(2003, "竞拍已结束"),
    AUCTION_CANCELLED(2004, "竞拍已取消"),
    BID_TOO_LOW(2005, "出价低于当前价"),
    BID_INCREMENT_INVALID(2006, "加价幅度不符合规则"),
    BID_LOCK_FAILED(2007, "操作太频繁，请稍后再试"),
    MAX_PRICE_REACHED(2008, "已达到封顶价"),

    // 订单相关 3xxx
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_ALREADY_PAID(3002, "订单已支付");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
