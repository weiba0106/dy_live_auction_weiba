package com.dy.liveauction.service.auction.state;

import com.dy.liveauction.dao.entity.AuctionItem;

/**
 * 竞拍中状态 —— 用户正在出价
 *
 * 允许操作：cancel()    -> CANCELLED
 * 允许操作：end()       -> SOLD / ENDED
 * 禁止操作：start()
 */
public class ActiveState implements AuctionState {

    static final ActiveState INSTANCE = new ActiveState();

    @Override
    public AuctionState cancel(AuctionItem item, String reason) {
        item.setCancelReason(reason);
        item.setStatus(CancelledState.INSTANCE.code());
        return CancelledState.INSTANCE;
    }

    @Override
    public AuctionState end(AuctionItem item) {
        if (item.getCurrentBidderId() != null && item.getCurrentBidderId() > 0) {
            item.setWinnerId(item.getCurrentBidderId());
            item.setStatus(SoldState.INSTANCE.code());
            return SoldState.INSTANCE;
        }
        item.setStatus(EndedState.INSTANCE.code());
        return EndedState.INSTANCE;
    }

    @Override
    public int code() {
        return 2;
    }
}
