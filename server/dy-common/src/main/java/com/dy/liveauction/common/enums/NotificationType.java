package com.dy.liveauction.common.enums;

import lombok.Getter;

@Getter
public enum NotificationType {

    OUTBID(1, "被超越"),
    AUCTION_DELAY(2, "竞拍延时"),
    AUCTION_END(3, "竞拍结束"),
    AUCTION_WIN(4, "竞拍获胜"),
    ITEM_ON_SHELF(5, "商品上架提醒");

    private final int code;
    private final String desc;

    NotificationType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
