package com.dy.liveauction.api.websocket;

import com.dy.liveauction.common.exception.BizException;
import com.dy.liveauction.common.exception.ErrorCode;
import com.dy.liveauction.service.auction.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketBidController {

    private final AuctionService auctionService;

    @MessageMapping("/bid")
    public void handleBid(@Payload Map<String, Object> payload, Principal principal) {
        if (principal == null) throw new BizException(ErrorCode.UNAUTHORIZED);

        Long itemId = Long.valueOf(payload.get("itemId").toString());
        Long userId = Long.valueOf(principal.getName());
        BigDecimal amount = new BigDecimal(payload.get("amount").toString());

        log.info("WS 出价: itemId={}, userId={}, amount={}", itemId, userId, amount);
        auctionService.placeBid(itemId, userId, amount);
    }
}
