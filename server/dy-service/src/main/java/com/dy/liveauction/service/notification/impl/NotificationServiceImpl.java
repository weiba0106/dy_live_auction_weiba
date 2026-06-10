package com.dy.liveauction.service.notification.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dy.liveauction.dao.entity.Bid;
import com.dy.liveauction.dao.entity.Notification;
import com.dy.liveauction.dao.entity.User;
import com.dy.liveauction.dao.mapper.BidMapper;
import com.dy.liveauction.dao.mapper.NotificationMapper;
import com.dy.liveauction.dao.mapper.UserMapper;
import com.dy.liveauction.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final BidMapper bidMapper;

    @Override
    @Transactional
    public void create(Long userId, Long itemId, int type, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setItemId(itemId);
        n.setType(type);
        n.setContent(content);
        n.setIsRead(0);
        notificationMapper.insert(n);
    }

    @Override
    public void notifyOutbid(Long outbidUserId, Long itemId, String itemName, BigDecimal newPrice) {
        create(outbidUserId, itemId, 1,
                "您在『" + itemName + "』的出价被超越！当前最高价 ¥" + newPrice);
    }

    @Override
    public void notifyAuctionEnd(Long itemId, String itemName, Long winnerId, BigDecimal finalPrice) {
        if (winnerId != null) {
            User winner = userMapper.selectById(winnerId);
            create(winnerId, itemId, 4,
                    "恭喜！您以 ¥" + finalPrice + " 拍得『" + itemName + "』");
        }
        // 通知所有出价过的用户
        List<Bid> bids = bidMapper.selectList(
                new LambdaQueryWrapper<Bid>().eq(Bid::getItemId, itemId));
        Set<Long> userIds = bids.stream().map(Bid::getUserId).collect(Collectors.toSet());
        for (Long uid : userIds) {
            if (!uid.equals(winnerId)) {
                create(uid, itemId, 3, "竞拍『" + itemName + "』已结束，成交价 ¥" + finalPrice);
            }
        }
    }

    @Override
    public void notifyAuctionCancelled(Long itemId, String itemName) {
        List<Bid> bids = bidMapper.selectList(
                new LambdaQueryWrapper<Bid>().eq(Bid::getItemId, itemId));
        Set<Long> userIds = bids.stream().map(Bid::getUserId).collect(Collectors.toSet());
        for (Long uid : userIds) {
            create(uid, itemId, 3, "竞拍『" + itemName + "』已被主播取消");
        }
    }
}
