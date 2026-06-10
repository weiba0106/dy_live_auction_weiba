package com.dy.liveauction.service.order.impl;

import com.dy.liveauction.common.exception.BizException;
import com.dy.liveauction.common.exception.ErrorCode;
import com.dy.liveauction.dao.entity.AuctionItem;
import com.dy.liveauction.dao.entity.Order;
import com.dy.liveauction.dao.entity.User;
import com.dy.liveauction.dao.mapper.AuctionItemMapper;
import com.dy.liveauction.dao.mapper.OrderMapper;
import com.dy.liveauction.dao.mapper.UserMapper;
import com.dy.liveauction.service.order.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final AuctionItemMapper auctionItemMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Order createOrder(Long itemId, Long buyerId) {
        AuctionItem item = auctionItemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        }

        Order order = new Order();
        order.setItemId(itemId);
        order.setBuyerId(buyerId);
        order.setSellerId(item.getMerchantId());
        order.setFinalPrice(item.getCurrentPrice());
        order.setStatus(1);  // 待付款
        orderMapper.insert(order);

        log.info("订单已生成: orderId={}, itemId={}, buyerId={}, price={}",
                order.getId(), itemId, buyerId, item.getCurrentPrice());
        return order;
    }

    @Override
    @Transactional
    public Order payOrder(Long orderId, Long buyerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        if (!order.getBuyerId().equals(buyerId)) throw new BizException(403, "无权操作此订单");
        if (order.getStatus() != 1) throw new BizException(ErrorCode.ORDER_ALREADY_PAID);

        User buyer = userMapper.selectById(buyerId);
        if (buyer.getBalance().compareTo(order.getFinalPrice()) < 0) {
            throw new BizException(ErrorCode.BALANCE_INSUFFICIENT);
        }

        buyer.setBalance(buyer.getBalance().subtract(order.getFinalPrice()));
        userMapper.updateById(buyer);

        order.setStatus(2);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        log.info("订单已支付: orderId={}, buyerId={}, amount={}", orderId, buyerId, order.getFinalPrice());
        return order;
    }

    @Override
    public List<Order> listByBuyer(Long buyerId) {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getBuyerId, buyerId)
                        .orderByDesc(Order::getId));
    }
}
