package com.dy.liveauction.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 通用 Redis 缓存服务 —— Cache-Aside 模式，Redis 异常不回抛
 *
 * 读：Redis miss（或不可用） → 查 DB → 返回
 * 写：Redis 不可用时只记日志，不影响主流程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheService {

    private final RedissonClient redissonClient;

    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Class<T> type, Duration ttl, Supplier<T> dbLoader) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            T cached = bucket.get();
            if (cached != null) return cached;
        } catch (Exception e) {
            log.warn("Redis 读缓存失败 key={}, 回退DB", key, e);
        }

        T data = dbLoader.get();
        if (data != null) {
            try {
                redissonClient.getBucket(key).set(data, ttl);
            } catch (Exception e) {
                log.warn("Redis 写缓存失败 key={}", key, e);
            }
        }
        return data;
    }

    public void invalidate(String key) {
        try {
            redissonClient.getBucket(key).delete();
        } catch (Exception e) {
            log.warn("Redis 清缓存失败 key={}", key, e);
        }
    }

    public void invalidatePrefix(String prefix) {
        try {
            redissonClient.getKeys().deleteByPattern(prefix + "*");
        } catch (Exception e) {
            log.warn("Redis 批量清缓存失败 prefix={}", prefix, e);
        }
    }
}
