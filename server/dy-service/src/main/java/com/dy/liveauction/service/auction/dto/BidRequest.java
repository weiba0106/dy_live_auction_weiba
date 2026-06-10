package com.dy.liveauction.service.auction.dto;

import lombok.Data;

@Data
public class BidRequest {
    private Long itemId;
    private Long userId;
    private java.math.BigDecimal amount;
}
