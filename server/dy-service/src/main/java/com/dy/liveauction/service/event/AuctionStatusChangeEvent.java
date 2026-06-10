package com.dy.liveauction.service.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

/**
 * 竞拍状态变更事件 —— 基于 Spring 事件机制，解耦竞拍核心逻辑和副作用
 *
 * 使用方式：竞拍状态变更时发布事件，通知服务、排行榜服务等监听者自动响应
 *
 * 扩展方式：新增监听器只需 @EventListener 注解，无需修改竞拍核心代码
 */
@Getter
@ToString
public class AuctionStatusChangeEvent extends ApplicationEvent {

    private final Long itemId;
    private final int oldStatus;
    private final int newStatus;
    private final Long winnerId;

    public AuctionStatusChangeEvent(Object source, Long itemId, int oldStatus, int newStatus, Long winnerId) {
        super(source);
        this.itemId = itemId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.winnerId = winnerId;
    }
}
