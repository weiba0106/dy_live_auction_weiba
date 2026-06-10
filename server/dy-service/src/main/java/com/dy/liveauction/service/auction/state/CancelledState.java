package com.dy.liveauction.service.auction.state;

/**
 * 已取消状态 —— 主播手动取消，终态
 */
public class CancelledState implements AuctionState {

    static final CancelledState INSTANCE = new CancelledState();

    @Override
    public int code() {
        return 5;
    }
}
