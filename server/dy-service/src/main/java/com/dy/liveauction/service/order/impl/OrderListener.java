package com.dy.liveauction.service.order.impl;

import com.dy.liveauction.service.auction.AuctionService;
import com.dy.liveauction.service.event.AuctionStatusChangeEvent;
import com.dy.liveauction.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 订单监听器 —— 监听竞拍成交事件，自动生成订单
 *
 * 为什么不用 RabbitMQ：
 *   订单生成是一条 INSERT，毫秒级完成。
 *   Spring Event 进程内同步调用，无需引入消息队列的额外依赖和部署复杂度。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderListener {

    private final OrderService orderService;

    /**
     * 竞拍成交 → 自动生成订单
     */
    @EventListener
    public void onAuctionSold(AuctionStatusChangeEvent event) {
        if (event.getNewStatus() != 4) return;  // 只处理成交事件
        if (event.getWinnerId() == null) return;

        log.info("收到成交事件, 生成订单: itemId={}, winnerId={}",
                event.getItemId(), event.getWinnerId());
        orderService.createOrder(event.getItemId(), event.getWinnerId());
    }
}
