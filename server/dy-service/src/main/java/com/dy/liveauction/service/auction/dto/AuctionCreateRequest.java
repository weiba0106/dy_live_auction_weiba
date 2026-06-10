package com.dy.liveauction.service.auction.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AuctionCreateRequest {
    private Long roomId;
    private String name;
    private String description;
    private String images;
    private BigDecimal startPrice;
    private BigDecimal incrementAmount;
    private BigDecimal maxPrice;
    private Integer durationMinutes;
    private Integer delaySeconds;
}
