package com.jobflow.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Database-based implementation of DistributedLock
 * 
 * Uses a dedicated table for lock management with row-level locking.
 * Provides a fallback mechanism when Redis is not available.
 */
@Slf4j
@Component
public class DatabaseLock implements DistributedLock {

    private final JdbcTemplate jdbcTemplate;
    private final ThreadLocal<String> lockOwner = new ThreadLocal<>();

    @Value("${lock.default.timeout}")
    private long defaultTimeout;

    @Value("${lock.default.retry-interval}")
    private long retryInterval;

    private static final String CREATE_LOCK_TABLE = """
        CREATE TABLE IF NOT EXISTS fj_lock (
            lock_key VARCHAR(255) PRIMARY KEY,
            lock_token VARCHAR(36) NOT NULL,
            owner VARCHAR(255),
            acquired_time TIMESTAMP NOT NULL,
            timeout_time TIMESTAMP NOT NULL,
            created_by VARCHAR(50),
            created_time TIMESTAMP,
            updated_by VARCHAR(50),
            updated_time TIMESTAMP
        )
    """;

    private static final String ACQUIRE_LOCK = """
        INSERT INTO fj_lock (
            lock_key, lock_token, owner, acquired_time, timeout_time,
            created_by, created_time, updated_by, updated_time
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            lock_token = IF(timeout_time < NOW(), VALUES(lock_token), lock_token),
            owner = IF(timeout_time < NOW(), VALUES(owner), owner),
            acquired_time = IF(timeout_time < NOW(), VALUES(acquired_time), acquired_time),
            timeout_time = IF(timeout_time < NOW(), VALUES(timeout_time), timeout_time),
            updated_by = VALUES(updated_by),
            updated_time = VALUES(updated_time)
    """;

    private static final String RELEASE_LOCK = """
        DELETE FROM fj_lock 
        WHERE lock_key = ? AND lock_token = ?
    """;

    private static final String CHECK_LOCK = """
        SELECT COUNT(*) 
        FROM fj_lock 
        WHERE lock_key = ? AND timeout_time > NOW()
    """;

    private static final String GET_LOCK_INFO = """
        SELECT lock_token, timeout_time 
        FROM fj_lock 
        WHERE lock_key = ?
    """;

    private static final String EXTEND_LOCK = """
        UPDATE fj_lock 
        SET timeout_time = ?, 
            updated_by = ?, 
            updated_time = NOW() 
        WHERE lock_key = ? AND lock_token = ?
    """;

    @Autowired
    public DatabaseLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeLockTable();
    }

    private void initializeLockTable() {
        try {
            jdbcTemplate.execute(CREATE_LOCK_TABLE);
        } catch (DataAccessException e) {
            log.error("Failed to initialize lock table", e);
            throw new RuntimeException("Failed to initialize lock table", e);
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean acquire(String lockKey) {
        return acquire(lockKey, defaultTimeout);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean acquire(String lockKey, long timeout) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime timeoutTime = now.plusNanos(timeout * 1000000); // Convert milliseconds to nanos

        try {
            int updated = jdbcTemplate.update(
                ACQUIRE_LOCK,
                lockKey,
                token,
                Thread.currentThread().getName(),
                now,
                timeoutTime,
                "system",
                now,
                "system",
                now
            );

            if (updated > 0) {
                lockOwner.set(token);
                log.debug("Lock acquired: {}", lockKey);
                return true;
            }

            return false;
        } catch (DataAccessException e) {
            log.error("Failed to acquire lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean release(String lockKey) {
        String token = lockOwner.get();
        if (token == null) {
            log.warn("Attempt to release lock without ownership: {}", lockKey);
            return false;
        }

        try {
            int updated = jdbcTemplate.update(RELEASE_LOCK, lockKey, token);
            if (updated > 0) {
                lockOwner.remove();
                log.debug("Lock released: {}", lockKey);
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            log.error("Failed to release lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        try {
            Integer count = jdbcTemplate.queryForObject(CHECK_LOCK, Integer.class, lockKey);
            return count != null && count > 0;
        } catch (DataAccessException e) {
            log.error("Failed to check lock status: {}", lockKey, e);
            return false;
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean forceRelease(String lockKey) {
        try {
            int updated = jdbcTemplate.update("DELETE FROM fj_lock WHERE lock_key = ?", lockKey);
            if (updated > 0) {
                log.warn("Lock forcibly released: {}", lockKey);
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            log.error("Failed to force release lock: {}", lockKey, e);
            return false;
        }
    }

    @Override
    public long getTimeToLive(String lockKey) {
        try {
            return jdbcTemplate.query(
                GET_LOCK_INFO,
                rs -> {
                    if (rs.next()) {
                        LocalDateTime timeoutTime = rs.getTimestamp("timeout_time").toLocalDateTime();
                        return java.time.Duration.between(LocalDateTime.now(), timeoutTime).toMillis();
                    }
                    return -1L;
                },
                lockKey
            );
        } catch (DataAccessException e) {
            log.error("Failed to get lock TTL: {}", lockKey, e);
            return -1;
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean extend(String lockKey, long timeout) {
        String token = lockOwner.get();
        if (token == null) {
            log.warn("Attempt to extend lock without ownership: {}", lockKey);
            return false;
        }

        LocalDateTime newTimeoutTime = LocalDateTime.now().plusNanos(timeout * 1000000);

        try {
            int updated = jdbcTemplate.update(
                EXTEND_LOCK,
                newTimeoutTime,
                "system",
                lockKey,
                token
            );

            if (updated > 0) {
                log.debug("Lock extended: {}", lockKey);
                return true;
            }
            return false;
        } catch (DataAccessException e) {
            log.error("Failed to extend lock: {}", lockKey, e);
            return false;
        }
    }

    /**
     * Clean up thread local variables
     */
    public void cleanup() {
        lockOwner.remove();
    }

    /**
     * Get the current lock owner
     * @param lockKey The key to check
     * @return The lock owner thread name, or null if not locked
     */
    public String getLockOwner(String lockKey) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT owner FROM fj_lock WHERE lock_key = ? AND timeout_time > NOW()",
                String.class,
                lockKey
            );
        } catch (DataAccessException e) {
            return null;
        }
    }

    /**
     * Check if the current thread owns the lock
     * @param lockKey The key to check
     * @return true if the current thread owns the lock, false otherwise
     */
    public boolean isOwnedByCurrentThread(String lockKey) {
        String token = lockOwner.get();
        if (token == null) {
            return false;
        }

        try {
            String currentToken = jdbcTemplate.queryForObject(
                "SELECT lock_token FROM fj_lock WHERE lock_key = ? AND timeout_time > NOW()",
                String.class,
                lockKey
            );
            return token.equals(currentToken);
        } catch (DataAccessException e) {
            return false;
        }
    }
}
