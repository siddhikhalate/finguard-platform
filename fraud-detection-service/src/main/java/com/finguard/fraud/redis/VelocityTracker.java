package com.finguard.fraud.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class VelocityTracker {

    private final StringRedisTemplate redisTemplate;

    // In-memory fallback if Redis is unreachable in local lightweight mode
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> inMemoryFallback = new ConcurrentHashMap<>();

    /**
     * Records a transaction and calculates the sliding window count for an account.
     *
     * @param accountId Account ID
     * @param transactionId Transaction ID
     * @param windowSeconds Rolling window size in seconds
     * @return Number of transactions recorded within the window
     */
    public long recordAndGetTransactionCount(UUID accountId, UUID transactionId, int windowSeconds) {
        String key = "velocity:account:" + accountId + ":txns";
        return recordSlidingWindow(key, transactionId.toString(), windowSeconds);
    }

    /**
     * Records a failed transaction attempt and returns the sliding window failure count.
     */
    public long recordAndGetFailedAttemptCount(UUID accountId, UUID transactionId, int windowSeconds) {
        String key = "velocity:account:" + accountId + ":failures";
        return recordSlidingWindow(key, transactionId.toString(), windowSeconds);
    }

    private long recordSlidingWindow(String key, String member, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMillis = (long) windowSeconds * 1000L;
        long cutoff = now - windowMillis;

        try {
            if (redisTemplate != null && redisTemplate.getConnectionFactory() != null) {
                // 1. Remove events older than the sliding window threshold
                redisTemplate.opsForZSet().removeRangeByScore(key, 0, cutoff);
                // 2. Add current event
                redisTemplate.opsForZSet().add(key, member, now);
                // 3. Set expiration so keys automatically expire
                redisTemplate.expire(key, windowSeconds * 2L, TimeUnit.SECONDS);
                // 4. Return current count
                Long count = redisTemplate.opsForZSet().zCard(key);
                return count != null ? count : 1L;
            }
        } catch (Exception ex) {
            log.warn("Redis unreachable for velocity tracking. Using local in-memory fallback: {}", ex.getMessage());
        }

        // In-Memory Fallback
        ConcurrentLinkedDeque<Long> timestamps = inMemoryFallback.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        timestamps.removeIf(ts -> ts < cutoff);
        timestamps.add(now);
        return timestamps.size();
    }
}
