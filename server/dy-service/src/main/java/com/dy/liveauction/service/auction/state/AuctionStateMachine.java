package com.dy.liveauction.service.auction.state;

import com.dy.liveauction.dao.entity.AuctionItem;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 竞拍状态机 —— 根据商品当前状态码，找到对应的状态对象，委托执行操作
 *
 * 核心思路：不存 currentState 字段，每次操作都从 item.status 重新解析状态，
 * 这样天然保证多线程并发安全（每次拿到最新状态）
 */
@Slf4j
public class AuctionStateMachine {

    private static final Map<Integer, AuctionState> STATE_MAP = Map.of(
            1, PendingState.INSTANCE,
            2, ActiveState.INSTANCE,
            3, EndedState.INSTANCE,
            4, SoldState.INSTANCE,
            5, CancelledState.INSTANCE
    );

    /**
     * 根据 code 解析状态对象
     */
    public static AuctionState resolve(int statusCode) {
        AuctionState state = STATE_MAP.get(statusCode);
        if (state == null) {
            throw new IllegalArgumentException("未知竞拍状态: " + statusCode);
        }
        return state;
    }

    /**
     * 开始竞拍
     */
    public void start(AuctionItem item) {
        AuctionState current = resolve(item.getStatus());
        AuctionState next = current.start(item);
        log.info("竞拍[{}] 状态变更: {} -> {}", item.getId(), current.code(), next.code());
    }

    /**
     * 取消竞拍
     */
    public void cancel(AuctionItem item, String reason) {
        AuctionState current = resolve(item.getStatus());
        AuctionState next = current.cancel(item, reason);
        log.info("竞拍[{}] 状态变更: {} -> {}, 原因: {}",
                item.getId(), current.code(), next.code(), reason);
    }

    /**
     * 结束竞拍
     */
    public void end(AuctionItem item) {
        AuctionState current = resolve(item.getStatus());
        AuctionState next = current.end(item);
        log.info("竞拍[{}] 状态变更: {} -> {}, 赢家: {}",
                item.getId(), current.code(), next.code(), item.getWinnerId());
    }

    /**
     * 判断是否可出价（只有 ACTIVE 状态可出价）
     */
    public boolean canBid(AuctionItem item) {
        return item.getStatus() == ActiveState.INSTANCE.code();
    }
}
