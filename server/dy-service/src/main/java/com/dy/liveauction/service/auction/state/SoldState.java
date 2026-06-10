package com.dy.liveauction.service.auction.state;

/**
 * 已成交状态 —— 竞拍成功，有人出价并胜出，终态
 */
public class SoldState implements AuctionState {

    static final SoldState INSTANCE = new SoldState();

    @Override
    public int code() {
        return 4;
    }
}
