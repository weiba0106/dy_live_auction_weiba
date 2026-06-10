package com.dy.liveauction.api.websocket;

import com.dy.liveauction.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.Map;

/**
 * WebSocket 出价接口 —— 接收用户通过 STOMP 发送的出价请求
 *
 * 前端发送:
 *   stompClient.send("/app/bid", {}, JSON.stringify({itemId:10, amount:200}))
 *
 * 与 REST 接口 POST /api/auction/{id}/bid 功能等价，双通道并存
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketBidController {

    private final AuctionService auctionService;

    @MessageMapping("/bid")
    public void handleBid(@Payload Map<String, Object> payload) {
        Long itemId = Long.valueOf(payload.get("itemId").toString());
        Long userId = Long.valueOf(payload.get("userId").toString());
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        log.info("WS 出价: itemId={}, userId={}, amount={}", itemId, userId, amount);
        auctionService.placeBid(itemId, userId, amount);
    }
}
