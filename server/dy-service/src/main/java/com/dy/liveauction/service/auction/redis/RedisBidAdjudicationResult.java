package com.dy.liveauction.service.auction.redis;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RedisBidAdjudicationResult {
    private Long itemId;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal oldPrice;
    private Long oldBidderId;
    private Integer bidCount;
    private LocalDateTime actualEndTime;
    private boolean delayed;
    private boolean sold;
    private Long version;
}
