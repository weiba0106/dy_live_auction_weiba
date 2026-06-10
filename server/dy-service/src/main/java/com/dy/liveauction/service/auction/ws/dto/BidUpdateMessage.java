package com.dy.liveauction.service.auction.ws.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 出价更新消息 —— 每次出价成功后推送给直播间所有人
 *
 * 客户端 JS 示例:
 *   stompClient.subscribe('/topic/auction/{roomId}', function(msg) {
 *       var bid = JSON.parse(msg.body);
 *       if (bid.type === 'BID') updateUI(bid.price, bid.bidder, bid.count);
 *   });
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BidUpdateMessage {
    /** 消息类型标识: BID */
    private String type = "BID";
    /** 当前最高出价 */
    private BigDecimal price;
    /** 出价者昵称 */
    private String bidder;
    /** 总出价次数 */
    private int count;
    /** 拍品ID */
    private Long itemId;
}
