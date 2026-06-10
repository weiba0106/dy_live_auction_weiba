package com.dy.liveauction.service.auction.state;

import com.dy.liveauction.dao.entity.AuctionItem;
import java.time.LocalDateTime;

/**
 * 待开始状态 —— 主播刚发布，尚未启动
 *
 * 允许操作：start()     -> ACTIVE
 * 允许操作：cancel()    -> CANCELLED
 * 禁止操作：end()
 */
public class PendingState implements AuctionState {

    static final PendingState INSTANCE = new PendingState();

    @Override
    public AuctionState start(AuctionItem item) {
        item.setStartTime(LocalDateTime.now());
        item.setPlannedEndTime(LocalDateTime.now().plusMinutes(item.getDurationMinutes()));
        item.setActualEndTime(item.getPlannedEndTime());
        item.setStatus(ActiveState.INSTANCE.code());
        return ActiveState.INSTANCE;
    }

    @Override
    public AuctionState cancel(AuctionItem item, String reason) {
        item.setCancelReason(reason);
        item.setStatus(CancelledState.INSTANCE.code());
        return CancelledState.INSTANCE;
    }

    @Override
    public int code() {
        return 1;
    }
}
