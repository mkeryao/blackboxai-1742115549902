package com.jobflow.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of DistributedLock
 * 
 * Uses Redis SET command with NX and PX options for atomic lock acquisition.
 * Implements lock owner tracking to prevent unauthorized releases.
 */
@Slf4j
@Component
public class RedisLock implements DistributedLock {

    private final RedisTemplate<String, String> redisTemplate;
    private final ThreadLocal<String> lockOwner = new ThreadLocal<>();
    private static final String LOCK_PREFIX = "lock:";

    @Value("${lock.default.timeout}")
    private long defaultTimeout;

    @Value("${lock.default.retry-interval}")
    private long retryInterval;

    @Autowired
    public RedisLock(@Qualifier("lockRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquire(String lockKey) {
        return acquire(lockKey, defaultTimeout);
    }

    @Override
    public boolean acquire(String lockKey, long timeout) {
        String fullKey = LOCK_PREFIX + lockKey;
        String token = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeout) {
            Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(fullKey, token, timeout, TimeUnit.MILLISECONDS);

            if (Boolean.TRUE.equals(acquired)) {
                lockOwner.set(token);
                log.debug("Lock acquired: {}", lockKey);
                return true;
            }

            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.warn("Failed to acquire lock: {}", lockKey);
        return false;
    }

    @Override
    public boolean release(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        String token = lockOwner.get();

        if (token == null) {
            log.warn("Attempt to release lock without ownership: {}", lockKey);
            return false;
        }

        String currentToken = redisTemplate.opsForValue().get(fullKey);
        if (currentToken == null || !currentToken.equals(token)) {
            log.warn("Lock {} is held by another owner", lockKey);
            return false;
        }

        Boolean deleted = redisTemplate.delete(fullKey);
        if (Boolean.TRUE.equals(deleted)) {
            lockOwner.remove();
            log.debug("Lock released: {}", lockKey);
            return true;
        }

        log.warn("Failed to release lock: {}", lockKey);
        return false;
    }

    @Override
    public boolean isLocked(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    @Override
    public boolean forceRelease(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        Boolean deleted = redisTemplate.delete(fullKey);
        if (Boolean.TRUE.equals(deleted)) {
            log.warn("Lock forcibly released: {}", lockKey);
            return true;
        }
        return false;
    }

    @Override
    public long getTimeToLive(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        Long ttl = redisTemplate.getExpire(fullKey, TimeUnit.MILLISECONDS);
        return ttl != null ? ttl : -1;
    }

    @Override
    public boolean extend(String lockKey, long timeout) {
        String fullKey = LOCK_PREFIX + lockKey;
        String token = lockOwner.get();

        if (token == null) {
            log.warn("Attempt to extend lock without ownership: {}", lockKey);
            return false;
        }

        String currentToken = redisTemplate.opsForValue().get(fullKey);
        if (currentToken == null || !currentToken.equals(token)) {
            log.warn("Lock {} is held by another owner", lockKey);
            return false;
        }

        Boolean extended = redisTemplate.expire(fullKey, timeout, TimeUnit.MILLISECONDS);
        if (Boolean.TRUE.equals(extended)) {
            log.debug("Lock extended: {}", lockKey);
            return true;
        }

        log.warn("Failed to extend lock: {}", lockKey);
        return false;
    }

    /**
     * Clean up thread local variables
     */
    public void cleanup() {
        lockOwner.remove();
    }

    /**
     * Get the current lock owner token
     * @param lockKey The key to check
     * @return The lock owner token, or null if not locked
     */
    public String getLockOwner(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        return redisTemplate.opsForValue().get(fullKey);
    }

    /**
     * Check if the current thread owns the lock
     * @param lockKey The key to check
     * @return true if the current thread owns the lock, false otherwise
     */
    public boolean isOwnedByCurrentThread(String lockKey) {
        String fullKey = LOCK_PREFIX + lockKey;
        String token = lockOwner.get();
        String currentToken = redisTemplate.opsForValue().get(fullKey);
        return token != null && token.equals(currentToken);
    }
}
