package com.dy.liveauction.service.auction.state;

import com.dy.liveauction.dao.entity.AuctionItem;

/**
 * 竞拍状态接口 —— 每个具体状态实现此接口，
 * 只编写自己允许的操作，不允许的操作直接抛异常
 */
public interface AuctionState {

    /** 开始竞拍：PENDING -> ACTIVE */
    default AuctionState start(AuctionItem item) {
        throw new IllegalStateException("当前状态不允许开始竞拍");
    }

    /** 取消竞拍：PENDING/ACTIVE -> CANCELLED */
    default AuctionState cancel(AuctionItem item, String reason) {
        throw new IllegalStateException("当前状态不允许取消竞拍");
    }

    /** 结束竞拍：ACTIVE -> SOLD / ENDED */
    default AuctionState end(AuctionItem item) {
        throw new IllegalStateException("当前状态不允许结束竞拍");
    }

    /** 数据库对应的 TINYINT 值 */
    int code();
}
