package com.dy.liveauction.service.notification;

import com.dy.liveauction.dao.entity.Notification;

public interface NotificationService {

    void create(Long userId, Long itemId, int type, String content);

    void notifyOutbid(Long outbidUserId, Long itemId, String itemName, java.math.BigDecimal newPrice);

    void notifyAuctionEnd(Long itemId, String itemName, Long winnerId, java.math.BigDecimal finalPrice);

    void notifyAuctionCancelled(Long itemId, String itemName);
}
