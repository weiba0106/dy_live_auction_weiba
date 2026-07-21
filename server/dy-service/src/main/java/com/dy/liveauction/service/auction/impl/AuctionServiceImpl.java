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
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
        runAfterCommit(() -> invalidateAuctionCaches(item));
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
        runAfterCommit(() -> invalidateAuctionCaches(item));
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
        int oldStatus = item.getStatus();
        stateMachine.start(item);
        int updated = auctionItemMapper.startIfPending(
                item.getId(), item.getStartTime(), item.getPlannedEndTime(), item.getActualEndTime());
        if (updated != 1) throw new BizException(409, "竞拍状态已变化，请刷新后重试");

        runAfterCommit(() -> {
            invalidateAuctionCaches(item);
            eventPublisher.publishEvent(new AuctionStatusChangeEvent(this, item.getId(), oldStatus, item.getStatus(), null));
            broadcastAuctionEventSafely(item, "STARTED");
        });
    }

    @Override
    @Transactional
    public void cancelAuction(Long itemId, String reason) {
        RLock lock = lockAuctionItem(itemId);
        try {
            AuctionItem item = mustGet(itemId);
            int oldStatus = item.getStatus();
            stateMachine.cancel(item, reason);

            int updated = auctionItemMapper.cancelIfOpen(item.getId(), reason);
            if (updated != 1) throw new BizException(409, "竞拍状态已变化，请刷新后重试");

            if (oldStatus == 2 && item.getCurrentBidderId() != null) {
                releaseFrozenFunds(item.getCurrentBidderId(), item.getCurrentPrice());
            }

            runAfterCommit(() -> {
                invalidateAuctionCaches(item);
                eventPublisher.publishEvent(new AuctionStatusChangeEvent(this, item.getId(), oldStatus, 5, null));
                broadcastAuctionEventSafely(item, "CANCELLED");
            });
        } finally {
            unlockSafely(lock);
        }
    }

    @Override
    @Transactional
    public void endAuction(Long itemId) {
        RLock lock = lockAuctionItem(itemId);
        try {
            doEndAuction(mustGet(itemId));
        } finally {
            unlockSafely(lock);
        }
    }

    private boolean doEndAuction(AuctionItem item) {
        int oldStatus = item.getStatus();
        stateMachine.end(item);
        int updated = auctionItemMapper.finishIfActive(item.getId(), item.getStatus(), item.getWinnerId());
        if (updated != 1) {
            log.info("竞拍结束跳过，状态已变化: itemId={}", item.getId());
            return false;
        }

        runAfterCommit(() -> {
            invalidateAuctionCaches(item);
            eventPublisher.publishEvent(new AuctionStatusChangeEvent(
                    this, item.getId(), oldStatus, item.getStatus(), item.getWinnerId()));
            broadcastAuctionEventSafely(item, item.getStatus() == 4 ? "SOLD" : "ENDED");
        });
        return true;
    }

    @Override
    @Transactional
    public AuctionItem placeBid(Long itemId, Long userId, BigDecimal amount) {
        RLock lock = lockAuctionItem(itemId);
        try {
            AuctionItem item = mustGet(itemId);
            LocalDateTime now = LocalDateTime.now();

            validateAuctionActive(item);
            validateAuctionNotExpired(item, now);
            validateNotCurrentWinner(item, userId);
            validateIncrement(item.getCurrentPrice(), amount, item.getIncrementAmount());
            validateMaxPrice(amount, item.getMaxPrice());

            BigDecimal oldPrice = item.getCurrentPrice();
            Long oldBidderId = item.getCurrentBidderId();
            LocalDateTime newEndTime = calculateEndTimeAfterBid(item, now);
            boolean delayed = !Objects.equals(newEndTime, item.getActualEndTime());

            freezeBidFunds(userId, amount);
            int updated = auctionItemMapper.updateBidIfCurrent(
                    itemId, oldPrice, amount, userId, newEndTime, now);
            if (updated != 1) throw new BizException(409, "当前价格已变化，请重新出价");

            if (oldBidderId != null) {
                releaseFrozenFunds(oldBidderId, oldPrice);
            }

            bidMapper.invalidateActiveBids(itemId);
            Bid bid = new Bid();
            bid.setItemId(itemId);
            bid.setUserId(userId);
            bid.setBidAmount(amount);
            bid.setBidTime(now);
            bid.setIsValid(1);
            bidMapper.insert(bid);

            item.setCurrentPrice(amount);
            item.setCurrentBidderId(userId);
            item.setBidCount(item.getBidCount() + 1);
            item.setActualEndTime(newEndTime);

            runAfterCommit(() -> afterBidCommitted(item, userId, amount, oldBidderId, delayed));

            if (item.getMaxPrice() != null && amount.compareTo(item.getMaxPrice()) >= 0) {
                log.info("达到封顶价，自动成交: itemId={}, amount={}, maxPrice={}", itemId, amount, item.getMaxPrice());
                doEndAuction(item);
            }

            return item;
        } finally {
            unlockSafely(lock);
        }
    }

    void validateAuctionActive(AuctionItem item) {
        if (!stateMachine.canBid(item)) throw new BizException(ErrorCode.AUCTION_NOT_STARTED);
    }

    void validateIncrement(BigDecimal currentPrice, BigDecimal bidAmount, BigDecimal increment) {
        if (bidAmount.compareTo(currentPrice.add(increment)) < 0) {
            throw new BizException(ErrorCode.BID_INCREMENT_INVALID.getCode(),
                    ErrorCode.BID_INCREMENT_INVALID.getMessage()
                            + "（当前价: " + currentPrice + ", 最低出价: " + currentPrice.add(increment) + "）");
        }
    }

    void validateMaxPrice(BigDecimal amount, BigDecimal maxPrice) {
        if (maxPrice != null && amount.compareTo(maxPrice) > 0) {
            throw new BizException(ErrorCode.BID_TOO_LOW.getCode(), "出价超过封顶价 " + maxPrice + "，不可超出");
        }
    }

    boolean handleDelay(AuctionItem item) {
        LocalDateTime nextEndTime = calculateEndTimeAfterBid(item, LocalDateTime.now());
        boolean delayed = !Objects.equals(nextEndTime, item.getActualEndTime());
        item.setActualEndTime(nextEndTime);
        return delayed;
    }

    private void validateAuctionNotExpired(AuctionItem item, LocalDateTime now) {
        if (item.getActualEndTime() != null && !item.getActualEndTime().isAfter(now)) {
            throw new BizException(ErrorCode.AUCTION_ALREADY_ENDED);
        }
    }

    private void validateNotCurrentWinner(AuctionItem item, Long userId) {
        if (userId.equals(item.getCurrentBidderId())) {
            throw new BizException(400, "您已是当前最高出价者，无需重复出价");
        }
    }

    private LocalDateTime calculateEndTimeAfterBid(AuctionItem item, LocalDateTime now) {
        if (item.getActualEndTime() == null) return null;
        long remainSeconds = Duration.between(now, item.getActualEndTime()).getSeconds();
        if (remainSeconds > 0 && remainSeconds <= item.getDelaySeconds()) {
            LocalDateTime newEndTime = item.getActualEndTime().plusSeconds(item.getDelaySeconds());
            log.info("竞拍延时: itemId={}, newEndTime={}", item.getId(), newEndTime);
            return newEndTime;
        }
        return item.getActualEndTime();
    }

    private void freezeBidFunds(Long userId, BigDecimal amount) {
        int updated = userMapper.freezeBidFunds(userId, amount);
        if (updated != 1) throw new BizException(ErrorCode.BALANCE_INSUFFICIENT);
    }

    private void releaseFrozenFunds(Long userId, BigDecimal amount) {
        int updated = userMapper.releaseBidFunds(userId, amount);
        if (updated != 1) throw new BizException(500, "释放冻结资金失败，请稍后重试");
    }

    private RLock lockAuctionItem(Long itemId) {
        RLock lock = redissonClient.getLock("lock:bid:" + itemId);
        boolean locked;
        try {
            locked = lock.tryLock(1, 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("获取拍品锁失败: itemId={}", itemId, e);
            throw new BizException(ErrorCode.BID_LOCK_FAILED);
        }
        if (!locked) throw new BizException(ErrorCode.BID_LOCK_FAILED);
        return lock;
    }

    private void unlockSafely(RLock lock) {
        try {
            if (lock != null && lock.isHeldByCurrentThread()) lock.unlock();
        } catch (Exception e) {
            log.warn("释放拍品锁失败", e);
        }
    }

    private void afterBidCommitted(AuctionItem item, Long userId, BigDecimal amount, Long oldBidderId, boolean delayed) {
        try {
            redissonClient.getScoredSortedSet("auction:" + item.getId() + ":ranking")
                    .add(amount.doubleValue(), userId.toString());
        } catch (Exception e) {
            log.error("排行榜更新失败: itemId={}", item.getId(), e);
        }

        try {
            String bidderNick = getUserNickSafely(userId);
            broadcaster.broadcastBidUpdate(item.getRoomId(),
                    new BidUpdateMessage("BID", amount, bidderNick, item.getBidCount(), item.getId()));
            if (delayed) broadcastAuctionEventSafely(item, "DELAYED");
            if (oldBidderId != null && !oldBidderId.equals(userId)) {
                broadcaster.sendOutbidNotification(String.valueOf(oldBidderId),
                        new OutbidMessage("OUTBID", amount, item.getId(), item.getName()));
            }
        } catch (Exception e) {
            log.error("WS 广播失败: itemId={}", item.getId(), e);
        }

        invalidateAuctionCaches(item);
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
        } catch (Exception e) {
            log.error("WS 事件广播失败: itemId={}, event={}", item.getId(), event, e);
        }
    }

    private void invalidateAuctionCaches(AuctionItem item) {
        cacheService.invalidate("cache:item:" + item.getId());
        cacheService.invalidate("cache:room:" + item.getRoomId() + ":auctions");
        cacheService.invalidate("cache:rooms:live");
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
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
