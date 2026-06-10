package com.dy.liveauction.service.auction.ws;

import com.dy.liveauction.service.auction.ws.dto.AuctionEventMessage;
import com.dy.liveauction.service.auction.ws.dto.BidUpdateMessage;
import com.dy.liveauction.service.auction.ws.dto.OutbidMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 竞拍 WebSocket 广播器 —— 所有消息推送的统一入口
 *
 * 房间隔离方案：
 *   直播间内的 N 个用户订阅同一个 topic，收到相同的消息
 *   被超越通知是点对点的（只推给被超越的那个人）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionWsBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 出价更新 → 推给直播间所有人
     */
    public void broadcastBidUpdate(Long roomId, BidUpdateMessage msg) {
        String destination = "/topic/auction/" + roomId;
        messagingTemplate.convertAndSend(destination, msg);
        log.debug("WS 出价广播: roomId={}, price={}", roomId, msg.getPrice());
    }

    /**
     * 竞拍事件（开始/结束/取消/延时）→ 推给直播间所有人
     */
    public void broadcastAuctionEvent(Long roomId, AuctionEventMessage msg) {
        String destination = "/topic/auction/" + roomId;
        messagingTemplate.convertAndSend(destination, msg);
        log.info("WS 竞拍事件: roomId={}, event={}", roomId, msg.getEvent());
    }

    /**
     * 被超越通知 → 定向推给被超越的用户
     */
    public void sendOutbidNotification(String outbidUserId, OutbidMessage msg) {
        messagingTemplate.convertAndSendToUser(outbidUserId, "/queue/outbid", msg);
        log.debug("WS 被超越通知: userId={}, newPrice={}", outbidUserId, msg.getNewPrice());
    }
}
