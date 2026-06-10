package com.dy.liveauction.service.auction.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dy.liveauction.common.exception.BizException;
import com.dy.liveauction.common.exception.ErrorCode;
import com.dy.liveauction.dao.entity.AuctionItem;
import com.dy.liveauction.dao.entity.Bid;
import com.dy.liveauction.dao.entity.LiveRoom;
import com.dy.liveauction.dao.entity.User;
import com.dy.liveauction.dao.mapper.AuctionItemMapper;
import com.dy.liveauction.dao.mapper.BidMapper;
import com.dy.liveauction.dao.mapper.LiveRoomMapper;
import com.dy.liveauction.dao.mapper.UserMapper;
import com.dy.liveauction.service.auction.AuctionService;
import com.dy.liveauction.service.auction.dto.AuctionCreateRequest;
import com.dy.liveauction.service.auction.dto.AuctionDetailVO;
import com.dy.liveauction.service.auction.state.AuctionStateMachine;
import com.dy.liveauction.service.auction.ws.AuctionWsBroadcaster;
import com.dy.liveauction.service.auction.ws.dto.AuctionEventMessage;
import com.dy.liveauction.service.auction.ws.dto.BidUpdateMessage;
import com.dy.liveauction.service.auction.ws.dto.OutbidMessage;
import com.dy.liveauction.service.cache.CacheService;
import com.dy.liveauction.service.event.AuctionStatusChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private final AuctionItemMapper auctionItemMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final UserMapper userMapper;
    private final BidMapper bidMapper;
    private final AuctionWsBroadcaster broadcaster;
    private final RedissonClient redissonClient;
    private final CacheService cacheService;
    private final AuctionStateMachine stateMachine = new AuctionStateMachine();
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AuctionItem createAuction(Long merchantId, AuctionCreateRequest req) {
        LiveRoom room = liveRoomMapper.selectById(req.getRoomId());
        if (room == null) throw new BizException(404, "直播间不存在");
        if (!room.getMerchantId().equals(merchantId)) throw new BizException(403, "无权在此直播间发布竞拍");
        if (room.getStatus() != 2) throw new BizException(400, "直播间未开播，无法发布竞拍");

        AuctionItem item = new AuctionItem();
        item.setRoomId(req.getRoomId());
        item.setMerchantId(merchantId);
        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setImages(toJsonArray(req.getImages()));
        item.setStartPrice(req.getStartPrice() != null ? req.getStartPrice() : BigDecimal.ZERO);
        item.setIncrementAmount(req.getIncrementAmount());
        item.setMaxPrice(req.getMaxPrice());
        item.setDurationMinutes(req.getDurationMinutes());
        item.setDelaySeconds(req.getDelaySeconds() != null ? req.getDelaySeconds() : 10);
        item.setCurrentPrice(item.getStartPrice());
        item.setStatus(1);
        auctionItemMapper.insert(item);
        cacheService.invalidate("cache:room:" + req.getRoomId() + ":auctions");
        cacheService.invalidate("cache:rooms:live");
        return item;
    }

    @Override
    @Transactional
    public void updateAuction(Long itemId, AuctionCreateRequest req) {
        AuctionItem item = mustGet(itemId);
        if (item.getStatus() != 1) throw new BizException(400, "只有待开始的竞拍可以修改规则");
        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setImages(toJsonArray(req.getImages()));
        item.setStartPrice(req.getStartPrice() != null ? req.getStartPrice() : item.getStartPrice());
        item.setIncrementAmount(req.getIncrementAmount());
        item.setMaxPrice(req.getMaxPrice());
        item.setDurationMinutes(req.getDurationMinutes());
        item.setDelaySeconds(req.getDelaySeconds() != null ? req.getDelaySeconds() : item.getDelaySeconds());
        auctionItemMapper.updateById(item);
        cacheService.invalidate("cache:room:" + item.getRoomId() + ":auctions");
        cacheService.invalidate("cache:rooms:live");
    }

    @Override
    public AuctionDetailVO getDetail(Long itemId) {
        return cacheService.getOrLoad("cache:item:" + itemId,
                AuctionDetailVO.class, Duration.ofSeconds(2),
                () -> AuctionDetailVO.from(mustGet(itemId)));
    }

    @Override
    public List<AuctionDetailVO> listByRoom(Long roomId) {
        return cacheService.getOrLoad("cache:room:" + roomId + ":auctions",
                (Class<List<AuctionDetailVO>>) (Class<?>) List.class,
                Duration.ofSeconds(2),
                () -> auctionItemMapper.selectList(
                        new LambdaQueryWrapper<AuctionItem>()
                                .eq(AuctionItem::getRoomId, roomId)
                                .orderByDesc(AuctionItem::getId))
                        .stream().map(AuctionDetailVO::from).collect(Collectors.toList()));
    }

    @Override
    public IPage<AuctionDetailVO> pageMyAuctions(Long merchantId, int page, int size) {
        return auctionItemMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<AuctionItem>()
                        .eq(AuctionItem::getMerchantId, merchantId)
                        .orderByDesc(AuctionItem::getId))
                .convert(AuctionDetailVO::from);
    }

    @Override
    @Transactional
    public void startAuction(Long itemId) {
        AuctionItem item = mustGet(itemId);
        stateMachine.start(item);
        auctionItemMapper.updateById(item);
        cacheService.invalidate("cache:item:" + itemId);
        cacheService.invalidate("cache:room:" + item.getRoomId() + ":auctions");
        cacheService.invalidate("cache:rooms:live");
        eventPublisher.publishEvent(new AuctionStatusChangeEvent(this, item.getId(), 1, item.getStatus(), null));
        broadcastAuctionEventSafely(item, "STARTED");
    }

    @Override
    @Transactional
    public void cancelAuction(Long itemId, String reason) {
        AuctionItem item = mustGet(itemId);
        int oldStatus = item.getStatus();
        stateMachine.cancel(item, reason);
        auctionItemMapper.updateById(item);
        cacheService.invalidate("cache:item:" + item.getId());
        cacheService.invalidate("cache:room:" + item.getRoomId() + ":auctions");
        cacheService.invalidate("cache:rooms:live");
        eventPublisher.publishEvent(new AuctionStatusChangeEvent(this, item.getId(), oldStatus, item.getStatus(), null));
        broadcastAuctionEventSafely(item, "CANCELLED");
    }

    @Override
    @Transactional
    public void endAuction(Long itemId) {
        doEndAuction(mustGet(itemId));
    }

    /** 结束竞拍（不重查库，直接用传入的 item 对象，避免 MyBatis 一级缓存回写旧数据） */
    private void doEndAuction(AuctionItem item) {
        int oldStatus = item.getStatus();
        stateMachine.end(item);
        auctionItemMapper.updateById(item);
        cacheService.invalidate("cache:item:" + item.getId());
        cacheService.invalidate("cache:room:" + item.getRoomId() + ":auctions");
        cacheService.invalidate("cache:rooms:live");
        eventPublisher.publishEvent(new AuctionStatusChangeEvent(this, item.getId(), oldStatus, item.getStatus(), item.getWinnerId()));
        broadcastAuctionEventSafely(item, item.getStatus() == 4 ? "SOLD" : "ENDED");
    }

    @Override
    @Transactional
    public AuctionItem placeBid(Long itemId, Long userId, BigDecimal amount) {
        AuctionItem item = mustGet(itemId);

        validateAuctionActive(item);
        validateIncrement(item.getCurrentPrice(), amount, item.getIncrementAmount());
        validateMaxPrice(amount, item.getMaxPrice());
        validateBalance(userId, amount);

        // -- 分布式锁 --
        RLock lock = redissonClient.getLock("lock:bid:" + itemId + ":" + userId);
        boolean locked;
        try { locked = lock.tryLock(0, 3, TimeUnit.SECONDS); }
        catch (Exception e) { log.error("获取锁失败, itemId={}, userId={}", itemId, userId, e); throw new BizException(ErrorCode.BID_LOCK_FAILED); }
        if (!locked) throw new BizException(ErrorCode.BID_LOCK_FAILED);

        try {
            boolean delayed = handleDelay(item);

            Long oldBidderId = item.getCurrentBidderId();
            item.setCurrentPrice(amount);
            item.setCurrentBidderId(userId);
            item.setBidCount(item.getBidCount() + 1);
            auctionItemMapper.updateById(item);

            Bid bid = new Bid();
            bid.setItemId(itemId);
            bid.setUserId(userId);
            bid.setBidAmount(amount);
            bid.setBidTime(java.time.LocalDateTime.now());
            bid.setIsValid(1);
            bidMapper.insert(bid);

            // 副作用：更新排行榜
            try {
                redissonClient.getScoredSortedSet("auction:" + itemId + ":ranking")
                        .add(amount.doubleValue(), userId.toString());
            } catch (Exception e) { log.error("排行榜更新失败, itemId={}", itemId, e); }

            try {
                String bidderNick = getUserNickSafely(userId);
                broadcaster.broadcastBidUpdate(item.getRoomId(),
                        new BidUpdateMessage("BID", amount, bidderNick, item.getBidCount(), itemId));
                if (delayed) broadcastAuctionEventSafely(item, "DELAYED");
                if (oldBidderId != null && !oldBidderId.equals(userId)) {
                    broadcaster.sendOutbidNotification(String.valueOf(oldBidderId),
                            new OutbidMessage("OUTBID", amount, itemId, item.getName()));
                }
            } catch (Exception e) { log.error("WS广播失败, itemId={}", itemId, e); }

            if (item.getMaxPrice() != null && amount.compareTo(item.getMaxPrice()) >= 0) {
                log.info("达到封顶价,自动成交,itemId={},amount={},maxPrice={}",itemId,amount,item.getMaxPrice());
                doEndAuction(item);
            }

            return item;
        } finally {
            try { lock.unlock(); } catch (Exception ignored) {}
            cacheService.invalidate("cache:item:" + itemId);
            cacheService.invalidate("cache:room:" + item.getRoomId() + ":auctions");
        }
    }

    void validateAuctionActive(AuctionItem item) {
        if (!stateMachine.canBid(item)) throw new BizException(ErrorCode.AUCTION_NOT_STARTED);
    }

    void validateIncrement(BigDecimal currentPrice, BigDecimal bidAmount, BigDecimal increment) {
        if (bidAmount.compareTo(currentPrice.add(increment)) < 0)
            throw new BizException(ErrorCode.BID_INCREMENT_INVALID.getCode(),
                    ErrorCode.BID_INCREMENT_INVALID.getMessage() + "（当前价: " + currentPrice + ", 最低出价: " + currentPrice.add(increment) + "）");
    }

    void validateMaxPrice(BigDecimal amount, BigDecimal maxPrice) {
        if (maxPrice != null && amount.compareTo(maxPrice) > 0)
            throw new BizException(ErrorCode.BID_TOO_LOW.getCode(), "出价超过封顶价" + maxPrice + "，不可超出");
    }

    void validateBalance(Long userId, BigDecimal amount) {
        User user = userMapper.selectById(userId);
        if (user.getBalance().compareTo(amount) < 0) throw new BizException(ErrorCode.BALANCE_INSUFFICIENT);
    }

    boolean handleDelay(AuctionItem item) {
        if (item.getActualEndTime() == null) return false;
        long remainSeconds = java.time.Duration.between(java.time.LocalDateTime.now(), item.getActualEndTime()).getSeconds();
        if (remainSeconds > 0 && remainSeconds <= item.getDelaySeconds()) {
            item.setActualEndTime(item.getActualEndTime().plusSeconds(item.getDelaySeconds()));
            log.info("竞拍延时, itemId={}, 新结束时间={}", item.getId(), item.getActualEndTime());
            return true;
        }
        return false;
    }

    private String getUserNickSafely(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getNickname() : "用户" + userId;
    }

    private void broadcastAuctionEventSafely(AuctionItem item, String event) {
        try {
            AuctionEventMessage msg = new AuctionEventMessage();
            msg.setEvent(event);
            msg.setItemId(item.getId());
            if (item.getActualEndTime() != null) msg.setNewEndTime(item.getActualEndTime().toString());
            if (item.getStatus() == 4 && item.getWinnerId() != null) {
                msg.setWinner(getUserNickSafely(item.getWinnerId()));
                msg.setFinalPrice(item.getCurrentPrice());
            }
            broadcaster.broadcastAuctionEvent(item.getRoomId(), msg);
        } catch (Exception e) { log.error("WS事件广播失败, itemId={}, event={}", item.getId(), event, e); }
    }

    private String toJsonArray(String images) {
        if (images == null || images.isBlank()) return "[]";
        String trimmed = images.trim();
        if (trimmed.startsWith("[")) return trimmed;
        return "[\"" + trimmed + "\"]";
    }

    private AuctionItem mustGet(Long itemId) {
        AuctionItem item = auctionItemMapper.selectById(itemId);
        if (item == null) throw new BizException(ErrorCode.AUCTION_NOT_FOUND);
        return item;
    }
}
