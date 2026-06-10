package com.dy.liveauction.common.enums;

import lombok.Getter;

@Getter
public enum UserRole {

    MERCHANT(1, "主播/商家"),
    USER(2, "普通用户");

    private final int code;
    private final String desc;

    UserRole(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
