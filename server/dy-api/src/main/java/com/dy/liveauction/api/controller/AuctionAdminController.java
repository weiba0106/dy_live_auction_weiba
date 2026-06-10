package com.dy.liveauction.api.controller;

import com.dy.liveauction.common.result.Result;
import com.dy.liveauction.dao.entity.AuctionItem;
import com.dy.liveauction.dao.entity.Order;
import com.dy.liveauction.dao.mapper.OrderMapper;
import com.dy.liveauction.service.auction.AuctionService;
import com.dy.liveauction.service.auction.dto.AuctionCreateRequest;
import com.dy.liveauction.service.auction.dto.AuctionDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 主播端 PC 后台接口
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuctionAdminController {

    private final AuctionService auctionService;
    private final OrderMapper orderMapper;

    /** 发布竞拍 */
    @PostMapping("/auction")
    public Result<AuctionItem> create(@RequestBody AuctionCreateRequest req, HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        return Result.ok(auctionService.createAuction(merchantId, req));
    }

    /** 修改未开始的竞拍规则 */
    @PutMapping("/auction/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AuctionCreateRequest req) {
        auctionService.updateAuction(id, req);
        return Result.ok();
    }

    /** 我发布的竞拍列表（分页） */
    @GetMapping("/auctions")
    public Result<?> myAuctions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        return Result.ok(auctionService.pageMyAuctions(merchantId, page, size));
    }

    /** 竞拍详情 */
    @GetMapping("/auction/{id}")
    public Result<AuctionDetailVO> detail(@PathVariable Long id) {
        return Result.ok(auctionService.getDetail(id));
    }

    /** 开始竞拍 */
    @PostMapping("/auction/{id}/start")
    public Result<Void> start(@PathVariable Long id) {
        auctionService.startAuction(id);
        return Result.ok();
    }

    /** 取消竞拍 */
    @PostMapping("/auction/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam String reason) {
        auctionService.cancelAuction(id, reason);
        return Result.ok();
    }

    /** 订单列表（我卖出的） */
    @GetMapping("/orders")
    public Result<List<Order>> myOrders(HttpServletRequest request) {
        Long merchantId = (Long) request.getAttribute("userId");
        return Result.ok(orderMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, merchantId)
                        .orderByDesc(Order::getId)));
    }
}
