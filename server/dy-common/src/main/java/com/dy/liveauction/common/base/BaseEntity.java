package com.dy.liveauction.common.base;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实体基类 —— 不引入 ORM 依赖，只定义公共字段约定
 *
 * 约定：
 *   - id: 自增主键
 *   - createdAt: 插入时自动填充时间
 *   - updatedAt: 插入/更新时自动填充时间
 *
 * 子类通过 MyBatis-Plus @TableField(fill=...) 实现自动填充
 */
@Data
public abstract class BaseEntity {

    private Long id;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
