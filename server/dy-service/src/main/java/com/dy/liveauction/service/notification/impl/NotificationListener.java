package com.dy.liveauction.service.notification.impl;

import com.dy.liveauction.dao.entity.AuctionItem;
import com.dy.liveauction.dao.mapper.AuctionItemMapper;
import com.dy.liveauction.service.event.AuctionStatusChangeEvent;
import com.dy.liveauction.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 通知监听器 —— 竞拍状态变更时自动持久化通知
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final NotificationService notificationService;
    private final AuctionItemMapper auctionItemMapper;

    @EventListener
    public void onAuctionEnd(AuctionStatusChangeEvent event) {
        if (event.getNewStatus() == 4 || event.getNewStatus() == 3) {
            AuctionItem item = auctionItemMapper.selectById(event.getItemId());
            if (item != null) {
                if (event.getNewStatus() == 4) {
                    notificationService.notifyAuctionEnd(
                            event.getItemId(), item.getName(),
                            event.getWinnerId(), item.getCurrentPrice());
                } else {
                    notificationService.notifyAuctionEnd(
                            event.getItemId(), item.getName(), null, item.getCurrentPrice());
                }
            }
        }
        if (event.getNewStatus() == 5) {
            AuctionItem item = auctionItemMapper.selectById(event.getItemId());
            if (item != null) {
                notificationService.notifyAuctionCancelled(event.getItemId(), item.getName());
            }
        }
    }
}
