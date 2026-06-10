package com.dy.liveauction.service.order;

import com.dy.liveauction.dao.entity.Order;
import java.util.List;

public interface OrderService {

    Order createOrder(Long itemId, Long buyerId);

    Order payOrder(Long orderId, Long buyerId);

    List<Order> listByBuyer(Long buyerId);
}
