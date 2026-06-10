package com.dy.liveauction.service.auction.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dy.liveauction.dao.entity.AuctionItem;
import com.dy.liveauction.dao.mapper.AuctionItemMapper;
import com.dy.liveauction.service.auction.impl.AuctionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 竞拍结束扫描 —— 每秒扫描一次，找出到期的竞拍并结束
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionItemMapper auctionItemMapper;
    private final AuctionServiceImpl auctionService;

    @Scheduled(fixedRate = 1000)
    public void scanEndingAuctions() {
        List<AuctionItem> endingItems = auctionItemMapper.selectList(
                new LambdaQueryWrapper<AuctionItem>()
                        .eq(AuctionItem::getStatus, 2)  // 竞拍中
                        .le(AuctionItem::getActualEndTime, LocalDateTime.now())  // 到期的
        );

        for (AuctionItem item : endingItems) {
            try {
                auctionService.endAuction(item.getId());
                log.info("定时结束竞拍: itemId={}, 赢家={}", item.getId(), item.getWinnerId());
            } catch (Exception e) {
                log.error("结束竞拍失败: itemId={}", item.getId(), e);
            }
        }
    }
}
