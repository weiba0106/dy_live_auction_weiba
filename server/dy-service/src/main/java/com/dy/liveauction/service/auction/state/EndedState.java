package com.dy.liveauction.service.auction.state;

/**
 * 已结束状态（流拍）—— 竞拍结束但无人出价，终态
 */
public class EndedState implements AuctionState {

    static final EndedState INSTANCE = new EndedState();

    @Override
    public int code() {
        return 3;
    }
}
