package com.dy.liveauction.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_rooms")
public class LiveRoom {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long merchantId;
    private String title;
    private String coverImage;
    private Integer status;
    private String videoUrl;
    private String notice;
}
