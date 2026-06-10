package com.dy.liveauction.common.enums;

import lombok.Getter;

@Getter
public enum LiveRoomStatus {

    OFFLINE(1, "未开播"),
    LIVE(2, "直播中"),
    ENDED(3, "已结束");

    private final int code;
    private final String desc;

    LiveRoomStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static LiveRoomStatus fromCode(int code) {
        for (LiveRoomStatus s : values()) {
            if (s.code == code) return s;
        }
        return null;
    }
}
