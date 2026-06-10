package com.dy.liveauction.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("auction_items")
public class AuctionItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long roomId;
    private Long merchantId;
    private String name;
    private String description;
    private String images;
    private BigDecimal startPrice;
    private BigDecimal incrementAmount;
    private BigDecimal maxPrice;
    private Integer durationMinutes;
    private Integer delaySeconds;
    private BigDecimal currentPrice;
    private Long currentBidderId;
    private Integer bidCount;
    private LocalDateTime startTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualEndTime;
    private Integer status;
    private Long winnerId;
    private String cancelReason;
}
