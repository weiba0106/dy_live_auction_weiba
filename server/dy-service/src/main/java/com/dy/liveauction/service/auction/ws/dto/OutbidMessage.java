package com.dy.liveauction.service.auction.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 被超越通知 —— 用户定向推送
 *
 * 通过 STOMP 点对点推送:
 *   messagingTemplate.convertAndSendToUser(userId, "/queue/outbid", msg)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutbidMessage {
    private String type = "OUTBID";
    /** 新价格 */
    private BigDecimal newPrice;
    /** 拍品ID */
    private Long itemId;
    /** 拍品名称 */
    private String itemName;
}
