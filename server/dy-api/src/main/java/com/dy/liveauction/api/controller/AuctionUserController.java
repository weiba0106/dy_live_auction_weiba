package com.dy.liveauction.api.controller;

import com.dy.liveauction.common.exception.BizException;
import com.dy.liveauction.common.exception.ErrorCode;
import com.dy.liveauction.common.result.Result;
import com.dy.liveauction.dao.entity.AuctionItem;
import com.dy.liveauction.dao.entity.Bid;
import com.dy.liveauction.dao.entity.Order;
import com.dy.liveauction.dao.mapper.BidMapper;
import com.dy.liveauction.service.auction.AuctionService;
import com.dy.liveauction.service.auction.dto.AuctionDetailVO;
import com.dy.liveauction.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

/**
 * 用户端 移动端接口
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuctionUserController {

    private final AuctionService auctionService;
    private final BidMapper bidMapper;
    private final OrderService orderService;
    private final RedissonClient redissonClient;

    /** 直播间竞拍商品列表 */
    @GetMapping("/room/{roomId}/auctions")
    public Result<List<AuctionDetailVO>> roomAuctions(@PathVariable Long roomId) {
        return Result.ok(auctionService.listByRoom(roomId));
    }

    /** 竞拍详情 */
    @GetMapping("/auction/{id}")
    public Result<AuctionDetailVO> detail(@PathVariable Long id) {
        return Result.ok(auctionService.getDetail(id));
    }

    /** 出价 */
    @PostMapping("/auction/{id}/bid")
    public Result<AuctionItem> bid(@PathVariable Long id,
                                    @RequestParam BigDecimal amount,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        return Result.ok(auctionService.placeBid(id, userId, amount));
    }

    /** 我的出价记录 */
    @GetMapping("/user/bids")
    public Result<List<Bid>> myBids(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        return Result.ok(bidMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Bid>()
                        .eq(Bid::getUserId, userId)
                        .orderByDesc(Bid::getId)));
    }

    /** 我的订单（买家） */
    @GetMapping("/user/orders")
    public Result<List<Order>> myOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        return Result.ok(orderService.listByBuyer(userId));
    }

    /** 支付订单 */
    @PostMapping("/order/{id}/pay")
    public Result<Order> payOrder(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) throw new BizException(ErrorCode.UNAUTHORIZED);
        return Result.ok(orderService.payOrder(id, userId));
    }

    /** 实时排行榜 Top 20 */
    @GetMapping("/auction/{id}/ranking")
    public Result<List<Map<String, Object>>> ranking(@PathVariable Long id) {
        RScoredSortedSet<String> set = redissonClient.getScoredSortedSet("auction:" + id + ":ranking");
        Collection<String> topIds = set.valueRangeReversed(0, 19);
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (String userId : topIds) {
            Double score = set.getScore(userId);
            if (score == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("userId", Long.valueOf(userId));
            m.put("amount", BigDecimal.valueOf(score));
            list.add(m);
        }
        return Result.ok(list);
    }
}
