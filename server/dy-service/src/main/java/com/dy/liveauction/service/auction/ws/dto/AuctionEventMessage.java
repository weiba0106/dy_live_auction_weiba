package com.dy.liveauction.service.auction.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 竞拍事件消息 —— 状态变更时推送给直播间所有人
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuctionEventMessage {
    /** 事件类型: STARTED / ENDED / CANCELLED / DELAYED */
    private String type = "EVENT";
    /** 具体事件 */
    private String event;
    /** 新结束时间（延时事件专用） */
    private String newEndTime;
    /** 赢家信息（竞拍结束时） */
    private String winner;
    /** 成交价 */
    private java.math.BigDecimal finalPrice;
    /** 拍品ID */
    private Long itemId;
}
