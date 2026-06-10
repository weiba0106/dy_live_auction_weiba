package com.dy.liveauction.common.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {

    PENDING_PAY(1, "待付款"),
    PAID(2, "已付款"),
    CANCELLED(3, "已取消");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus fromCode(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
