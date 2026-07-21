package com.dy.liveauction.service.auction.redis;

import com.dy.liveauction.common.exception.BizException;
import com.dy.liveauction.common.exception.ErrorCode;
import com.dy.liveauction.dao.entity.AuctionItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisBidAdjudicator {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private static final String BID_SCRIPT = """
            local stateKey = KEYS[1]
            local rankingKey = KEYS[2]
            local biddersKey = KEYS[3]
            local streamKey = KEYS[4]
            local endZSetKey = KEYS[5]

            local itemId = ARGV[1]
            local userId = ARGV[2]
            local amount = tonumber(ARGV[3])
            local nowMs = tonumber(ARGV[4])
            local requestId = ARGV[5]

            local status = redis.call('HGET', stateKey, 'status')
            if not status then
                return {'MISSING'}
            end
            if status ~= '2' then
                return {'NOT_ACTIVE', status}
            end

            local currentPrice = tonumber(redis.call('HGET', stateKey, 'currentPriceCents') or '0')
            local increment = tonumber(redis.call('HGET', stateKey, 'incrementCents') or '0')
            local maxPrice = tonumber(redis.call('HGET', stateKey, 'maxPriceCents') or '-1')
            local endTime = tonumber(redis.call('HGET', stateKey, 'actualEndTimeMs') or '0')
            local delaySeconds = tonumber(redis.call('HGET', stateKey, 'delaySeconds') or '0')
            local oldBidder = redis.call('HGET', stateKey, 'currentBidderId') or ''

            if endTime > 0 and endTime <= nowMs then
                return {'ENDED'}
            end
            if oldBidder == userId then
                return {'CURRENT_WINNER'}
            end
            if maxPrice >= 0 and amount > maxPrice then
                return {'MAX_EXCEEDED', tostring(maxPrice)}
            end

            local minBid = currentPrice + increment
            if amount < minBid then
                return {'INCREMENT_INVALID', tostring(currentPrice), tostring(minBid)}
            end

            local newEndTime = endTime
            local delayed = 0
            if endTime > 0 then
                local remainMs = endTime - nowMs
                if remainMs > 0 and remainMs <= delaySeconds * 1000 then
                    newEndTime = endTime + delaySeconds * 1000
                    delayed = 1
                    redis.call('ZADD', endZSetKey, newEndTime, itemId)
                end
            end

            local version = redis.call('HINCRBY', stateKey, 'version', 1)
            local bidCount = redis.call('HINCRBY', stateKey, 'bidCount', 1)
            redis.call('HSET', stateKey,
                'currentPriceCents', amount,
                'currentBidderId', userId,
                'actualEndTimeMs', newEndTime)
            redis.call('ZADD', rankingKey, amount, userId)
            redis.call('SADD', biddersKey, userId)

            local sold = 0
            if maxPrice >= 0 and amount >= maxPrice then
                sold = 1
                redis.call('HSET', stateKey, 'status', '4')
                redis.call('ZREM', endZSetKey, itemId)
            end

            redis.call('XADD', streamKey, '*',
                'event', 'BID_ACCEPTED',
                'requestId', requestId,
                'itemId', itemId,
                'userId', userId,
                'amountCents', tostring(amount),
                'oldPriceCents', tostring(currentPrice),
                'oldBidderId', oldBidder,
                'bidCount', tostring(bidCount),
                'actualEndTimeMs', tostring(newEndTime),
                'delayed', tostring(delayed),
                'sold', tostring(sold),
                'version', tostring(version))

            return {
                'OK',
                tostring(currentPrice),
                oldBidder,
                tostring(bidCount),
                tostring(newEndTime),
                tostring(delayed),
                tostring(sold),
                tostring(version)
            }
            """;

    private final RedissonClient redissonClient;

    @Value("${auction.bid.redis-lua-enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void initializeAuction(AuctionItem item) {
        Map<String, String> state = new HashMap<>();
        state.put("itemId", String.valueOf(item.getId()));
        state.put("status", String.valueOf(item.getStatus()));
        state.put("currentPriceCents", String.valueOf(toCents(item.getCurrentPrice())));
        state.put("currentBidderId", item.getCurrentBidderId() == null ? "" : String.valueOf(item.getCurrentBidderId()));
        state.put("bidCount", String.valueOf(item.getBidCount() == null ? 0 : item.getBidCount()));
        state.put("incrementCents", String.valueOf(toCents(item.getIncrementAmount())));
        state.put("maxPriceCents", item.getMaxPrice() == null ? "-1" : String.valueOf(toCents(item.getMaxPrice())));
        state.put("actualEndTimeMs", String.valueOf(toMillis(item.getActualEndTime())));
        state.put("delaySeconds", String.valueOf(item.getDelaySeconds() == null ? 0 : item.getDelaySeconds()));
        state.put("version", "0");

        RMap<String, String> map = redissonClient.getMap(stateKey(item.getId()), StringCodec.INSTANCE);
        map.putAll(state);
        if (item.getStatus() != null && item.getStatus() == 2 && item.getActualEndTime() != null) {
            redissonClient.getScoredSortedSet(endZSetKey(), StringCodec.INSTANCE)
                    .add((double) toMillis(item.getActualEndTime()), String.valueOf(item.getId()));
        }
    }

    public void clearAuction(Long itemId) {
        redissonClient.getBucket(stateKey(itemId), StringCodec.INSTANCE).delete();
        redissonClient.getScoredSortedSet(rankingKey(itemId), StringCodec.INSTANCE).delete();
        redissonClient.getSet(biddersKey(itemId), StringCodec.INSTANCE).delete();
        redissonClient.getScoredSortedSet(endZSetKey(), StringCodec.INSTANCE).remove(String.valueOf(itemId));
    }

    public void markClosed(Long itemId, int status) {
        RMap<String, String> map = redissonClient.getMap(stateKey(itemId), StringCodec.INSTANCE);
        if (map.isExists()) {
            map.put("status", String.valueOf(status));
        }
        redissonClient.getScoredSortedSet(endZSetKey(), StringCodec.INSTANCE).remove(String.valueOf(itemId));
    }

    public RedisBidAdjudicationResult adjudicate(AuctionItem item, Long userId, BigDecimal amount, LocalDateTime now) {
        initializeIfMissing(item);

        long amountCents = toCents(amount);
        List<Object> raw = redissonClient.getScript(StringCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                BID_SCRIPT,
                RScript.ReturnType.MULTI,
                Arrays.asList(
                        stateKey(item.getId()),
                        rankingKey(item.getId()),
                        biddersKey(item.getId()),
                        bidStreamKey(),
                        endZSetKey()),
                String.valueOf(item.getId()),
                String.valueOf(userId),
                String.valueOf(amountCents),
                String.valueOf(toMillis(now)),
                UUID.randomUUID().toString());

        return parseResult(item.getId(), userId, amount, raw);
    }

    public boolean isCurrentVersion(Long itemId, Long version) {
        Object current = redissonClient.getMap(stateKey(itemId), StringCodec.INSTANCE).get("version");
        return current != null && Long.parseLong(current.toString()) == version;
    }

    private void initializeIfMissing(AuctionItem item) {
        RMap<String, String> map = redissonClient.getMap(stateKey(item.getId()), StringCodec.INSTANCE);
        if (!map.isExists() || map.get("status") == null) {
            initializeAuction(item);
        }
    }

    private RedisBidAdjudicationResult parseResult(Long itemId, Long userId, BigDecimal amount, List<Object> raw) {
        String code = value(raw, 0);
        switch (code) {
            case "OK":
                RedisBidAdjudicationResult result = new RedisBidAdjudicationResult();
                result.setItemId(itemId);
                result.setUserId(userId);
                result.setAmount(amount);
                result.setOldPrice(fromCents(Long.parseLong(value(raw, 1))));
                String oldBidder = value(raw, 2);
                result.setOldBidderId(oldBidder == null || oldBidder.isBlank() ? null : Long.valueOf(oldBidder));
                result.setBidCount(Integer.valueOf(value(raw, 3)));
                result.setActualEndTime(fromMillis(Long.parseLong(value(raw, 4))));
                result.setDelayed("1".equals(value(raw, 5)));
                result.setSold("1".equals(value(raw, 6)));
                result.setVersion(Long.valueOf(value(raw, 7)));
                return result;
            case "MISSING":
                throw new BizException(409, "竞拍实时状态未初始化，请稍后重试");
            case "NOT_ACTIVE":
                throw new BizException(ErrorCode.AUCTION_NOT_STARTED);
            case "ENDED":
                throw new BizException(ErrorCode.AUCTION_ALREADY_ENDED);
            case "CURRENT_WINNER":
                throw new BizException(400, "您已是当前最高出价者，无需重复出价");
            case "MAX_EXCEEDED":
                throw new BizException(ErrorCode.BID_TOO_LOW.getCode(), "出价超过封顶价 " + fromCents(Long.parseLong(value(raw, 1))));
            case "INCREMENT_INVALID":
                throw new BizException(ErrorCode.BID_INCREMENT_INVALID.getCode(),
                        ErrorCode.BID_INCREMENT_INVALID.getMessage()
                                + "（当前价: " + fromCents(Long.parseLong(value(raw, 1)))
                                + ", 最低出价: " + fromCents(Long.parseLong(value(raw, 2))) + "）");
            default:
                throw new BizException(500, "未知 Redis 出价裁决结果: " + code);
        }
    }

    private String value(List<Object> raw, int index) {
        Object value = raw == null || raw.size() <= index ? null : raw.get(index);
        return value == null ? null : value.toString();
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) return 0L;
        return amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    private BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    private long toMillis(LocalDateTime time) {
        if (time == null) return 0L;
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }

    private LocalDateTime fromMillis(long millis) {
        if (millis <= 0) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONE);
    }

    private String stateKey(Long itemId) {
        return "auction:" + itemId + ":state";
    }

    private String rankingKey(Long itemId) {
        return "auction:" + itemId + ":ranking";
    }

    private String biddersKey(Long itemId) {
        return "auction:" + itemId + ":bidders";
    }

    private String bidStreamKey() {
        return "auction:bid:stream";
    }

    private String endZSetKey() {
        return "auction:end:zset";
    }
}
