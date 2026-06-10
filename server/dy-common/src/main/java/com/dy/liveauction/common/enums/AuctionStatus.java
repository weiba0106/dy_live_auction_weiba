package com.dy.liveauction.common.enums;

import lombok.Getter;

/**
 * 竞拍状态枚举
 */
@Getter
public enum AuctionStatus {

    PENDING(1, "待开始"),
    ACTIVE(2, "竞拍中"),
    ENDED(3, "已结束(流拍)"),
    SOLD(4, "已成交"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String desc;

    AuctionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 从 code 获得枚举，用于数据库 TINYINT -> Java 枚举的映射
     */
    public static AuctionStatus fromCode(int code) {
        for (AuctionStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }

    /**
     * 是否允许出价
     */
    public boolean canBid() {
        return this == ACTIVE;
    }
}
