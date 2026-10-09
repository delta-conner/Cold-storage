package com.coldstorage.oms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Redis 缓存封装；Redis 不可用时自动降级为本地内存，并在一段时间内跳过 Redis，避免每次请求空等超时。
 */
@Service
public class AppCacheService {

    private static final String PREFIX = "oms:";
    /** Redis 连续失败后，冷却期内不再尝试，直接走本地 */
    private static final long COOLDOWN_MS = 60_000L;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${app.redis-enabled:true}")
    private boolean redisEnabled;

    private final Map<String, CacheItem> local = new ConcurrentHashMap<>();
    private final AtomicLong redisCooldownUntil = new AtomicLong(0);

    public void set(String key, String value, long seconds) {
        String k = PREFIX + key;
        if (useRedis()) {
            try {
                redisTemplate.opsForValue().set(k, value, seconds, TimeUnit.SECONDS);
                markRedisOk();
                return;
            } catch (Exception e) {
                markRedisDown();
            }
        }
        local.put(k, new CacheItem(value, System.currentTimeMillis() + seconds * 1000));
    }

    public String get(String key) {
        String k = PREFIX + key;
        if (useRedis()) {
            try {
                String v = redisTemplate.opsForValue().get(k);
                markRedisOk();
                return v;
            } catch (Exception e) {
                markRedisDown();
            }
        }
        CacheItem item = local.get(k);
        if (item == null) {
            return null;
        }
        if (item.expireAt < System.currentTimeMillis()) {
            local.remove(k);
            return null;
        }
        return item.value;
    }

    public void delete(String key) {
        String k = PREFIX + key;
        if (useRedis()) {
            try {
                redisTemplate.delete(k);
                markRedisOk();
            } catch (Exception e) {
                markRedisDown();
            }
        }
        local.remove(k);
    }

    public boolean has(String key) {
        return get(key) != null;
    }

    public String backend() {
        if (!redisEnabled || redisTemplate == null) {
            return "LOCAL";
        }
        if (System.currentTimeMillis() < redisCooldownUntil.get()) {
            return "LOCAL_FALLBACK";
        }
        try {
            redisTemplate.hasKey(PREFIX + "ping");
            markRedisOk();
            return "REDIS";
        } catch (Exception e) {
            markRedisDown();
            return "LOCAL_FALLBACK";
        }
    }

    private boolean useRedis() {
        return redisEnabled
                && redisTemplate != null
                && System.currentTimeMillis() >= redisCooldownUntil.get();
    }

    private void markRedisDown() {
        redisCooldownUntil.set(System.currentTimeMillis() + COOLDOWN_MS);
    }

    private void markRedisOk() {
        redisCooldownUntil.set(0);
    }

    private static class CacheItem {
        final String value;
        final long expireAt;

        CacheItem(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }
}
