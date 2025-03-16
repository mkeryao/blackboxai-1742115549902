package com.jobflow.lock;

/**
 * Distributed Lock Interface
 * 
 * Provides methods for distributed locking to ensure exclusive access to resources
 * across multiple application instances.
 */
public interface DistributedLock {
    
    /**
     * Acquire a lock with default timeout
     * @param lockKey The key to lock
     * @return true if lock acquired successfully, false otherwise
     */
    boolean acquire(String lockKey);

    /**
     * Acquire a lock with specified timeout
     * @param lockKey The key to lock
     * @param timeout Timeout in milliseconds
     * @return true if lock acquired successfully, false otherwise
     */
    boolean acquire(String lockKey, long timeout);

    /**
     * Release a lock
     * @param lockKey The key to unlock
     * @return true if lock released successfully, false otherwise
     */
    boolean release(String lockKey);

    /**
     * Check if a lock is currently held
     * @param lockKey The key to check
     * @return true if lock is held, false otherwise
     */
    boolean isLocked(String lockKey);

    /**
     * Force release a lock (use with caution)
     * @param lockKey The key to force unlock
     * @return true if lock was forcibly released, false otherwise
     */
    boolean forceRelease(String lockKey);

    /**
     * Get the remaining time to live for a lock
     * @param lockKey The key to check
     * @return Time to live in milliseconds, -1 if lock doesn't exist
     */
    long getTimeToLive(String lockKey);

    /**
     * Extend the lock timeout
     * @param lockKey The key to extend
     * @param timeout Additional time in milliseconds
     * @return true if timeout was extended, false otherwise
     */
    boolean extend(String lockKey, long timeout);

    /**
     * Execute a task with a lock
     * @param lockKey The key to lock
     * @param timeout Lock timeout in milliseconds
     * @param task The task to execute
     * @return true if task was executed successfully, false if lock couldn't be acquired
     */
    default boolean executeWithLock(String lockKey, long timeout, Runnable task) {
        try {
            if (!acquire(lockKey, timeout)) {
                return false;
            }
            task.run();
            return true;
        } finally {
            release(lockKey);
        }
    }

    /**
     * Execute a task with a lock and return a result
     * @param lockKey The key to lock
     * @param timeout Lock timeout in milliseconds
     * @param task The task to execute
     * @return The task result, or null if lock couldn't be acquired
     */
    default <T> T executeWithLock(String lockKey, long timeout, LockTask<T> task) {
        try {
            if (!acquire(lockKey, timeout)) {
                return null;
            }
            return task.execute();
        } finally {
            release(lockKey);
        }
    }

    /**
     * Functional interface for tasks that return a result
     */
    @FunctionalInterface
    interface LockTask<T> {
        T execute();
    }
}
