package com.dy.liveauction.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bids")
public class Bid {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long itemId;
    private Long userId;
    private BigDecimal bidAmount;
    private LocalDateTime bidTime;
    private Integer isValid;
    private String clientIp;
}
