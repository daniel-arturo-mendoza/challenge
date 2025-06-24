package com.playgami.challenge.memdb;

import com.playgami.challenge.memdb.eviction.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Component
public class MemDB implements StorageEngine {
    private static final Logger logger = LoggerFactory.getLogger(MemDB.class);
    private static final int MAX_MEMORY = 100 * 1024 * 1024; // 100MB

    // Main key-value store using AtomicReference for thread-safe value updates
    protected final Map<String, AtomicReference<ValueWrapper>> keyValueStore;
    // Sorted sets store: key -> ConcurrentSkipListMap<score, value>
    protected final Map<String, ConcurrentSkipListMap<Double, String>> sortedSets;
    // Counter for database size
    protected final AtomicLong dbSize;
    // Memory limit in bytes
    private final long maxMemory;
    // Current memory usage in bytes
    protected final AtomicLong currentMemoryUsed;
    // Eviction strategy
    protected final EvictionStrategy evictionStrategy;
    // Sorted set eviction policy
    private final SortedSetEvictionStrategy sortedSetEvictionPolicy;
    // Expiration times
    protected final Map<String, Long> expirationTimes;

    @Autowired
    public MemDB(EvictionStrategy evictionStrategy) {
        this.maxMemory = MAX_MEMORY;
        this.keyValueStore = new ConcurrentHashMap<>();
        this.sortedSets = new ConcurrentHashMap<>();
        this.dbSize = new AtomicLong(0);
        this.currentMemoryUsed = new AtomicLong(0);
        this.evictionStrategy = evictionStrategy;
        this.sortedSetEvictionPolicy = new RandomLowestScoreEvictionPolicy(currentMemoryUsed);
        this.expirationTimes = new ConcurrentHashMap<>();
    }

    // ===== Memory Management Methods =====

    /**
     * Checks if current memory usage exceeds the limit and evicts keys if necessary.
     */
    protected void checkMemoryLimit() {
        logger.debug("Checking memory limit. Current memory: {}, Max memory: {}", currentMemoryUsed.get(), maxMemory);
        while (isMemoryLimitExceeded()) {
            if (!evictKey()) {
                break; // No more keys to evict
            }
        }
    }

    /**
     * Checks if current memory usage exceeds the maximum limit.
     */
    protected boolean isMemoryLimitExceeded() {
        return currentMemoryUsed.get() >= maxMemory;
    }

    /**
     * Evicts a key based on the eviction strategy.
     */
    private boolean evictKey() {
        // First try to evict from keyValueStore
        String keyToEvict = evictionStrategy.selectKeyToEvict(keyValueStore, null);
        if (keyToEvict != null) {
            logger.debug("Evicting key from keyValueStore: {}", keyToEvict);
            del(keyToEvict);
            return true;
        }

        // If no keys in keyValueStore, try to evict from sorted sets
        return sortedSetEvictionPolicy.evictFromSortedSets(sortedSets);
    }

    /**
     * Calculates memory delta for a key-value operation.
     */
    private long calculateMemoryDelta(String key, String oldValue, String newValue) {
        long oldSize = oldValue != null ? oldValue.length() : 0;
        long newSize = newValue != null ? newValue.length() : 0;
        long keySize = key.length();
        return newSize - oldSize + keySize;
    }

    /**
     * Calculates memory delta for a sorted set operation.
     */
    private long calculateSortedSetMemoryDelta(String key, String oldValue, String newValue) {
        return calculateMemoryDelta(key, oldValue, newValue) + 8; // Add 8 bytes for the score (Double)
    }

    /**
     * Updates memory usage based on old and new values.
     */
    private void updateMemoryUsage(String key, String oldValue, String newValue) {
        currentMemoryUsed.addAndGet(calculateMemoryDelta(key, oldValue, newValue));
    }

    // ===== Key-Value Store Methods =====

    /**
     * Sets a key-value pair with optional expiry.
     */
    public void set(String key, String value, long expirySeconds) {
        AtomicReference<ValueWrapper> oldRef = keyValueStore.get(key);
        String oldValue = oldRef != null ? oldRef.get().value : null;
        long memoryDelta = calculateMemoryDelta(key, oldValue, value);

        logger.debug("Setting key: {}, Current memory: {}, Delta: {}, Max: {}", 
                    key, currentMemoryUsed.get(), memoryDelta, maxMemory);

        if (currentMemoryUsed.get() + memoryDelta > maxMemory) {
            checkMemoryLimit();
        }
        
        ValueWrapper wrapper = expirySeconds > 0 
            ? new ValueWrapper(value, System.currentTimeMillis() + (expirySeconds * 1000))
            : new ValueWrapper(value);
            
        keyValueStore.computeIfAbsent(key, k -> new AtomicReference<>()).set(wrapper);
        
        currentMemoryUsed.addAndGet(memoryDelta);
        updateAccessTime(key);
        
        if (oldValue == null) {
            dbSize.incrementAndGet();
        }
    }

    /**
     * Sets a key-value pair with no expiry.
     */
    public void set(String key, String value) {
        set(key, value, 0); // 0 means no expiry
    }

    /**
     * Gets the value for a key, updating access time if found.
     */
    public String get(String key) {
        AtomicReference<ValueWrapper> ref = keyValueStore.get(key);
        if (ref != null) {
            ValueWrapper wrapper = ref.get();
            if (wrapper.isExpired()) {
                del(key);
                return null;
            }
            updateAccessTime(key);
            return wrapper.value;
        }
        return null;
    }

    /**
     * Deletes a key and updates memory usage.
     */
    public boolean del(String key) {
        AtomicReference<ValueWrapper> ref = keyValueStore.remove(key);
        if (ref != null) {
            String value = ref.get().value;
            updateMemoryUsage(key, value, null);
            if (evictionStrategy instanceof LRUEvictionStrategy) {
                ((LRUEvictionStrategy)evictionStrategy).removeKey(key);
            }
            dbSize.decrementAndGet();
            return true;
        }
        return false;
    }

    /**
     * Increments the value of a key by 1.
     */
    public long incr(String key) {
        AtomicReference<ValueWrapper> ref = keyValueStore.compute(key, (k, oldRef) -> {
            String oldValue = oldRef != null ? oldRef.get().value : "0";
            long newValue;
            try {
                newValue = Long.parseLong(oldValue) + 1;
            } catch (NumberFormatException e) {
                newValue = 1;
            }
            return new AtomicReference<>(new ValueWrapper(String.valueOf(newValue)));
        });
        updateAccessTime(key);
        return Long.parseLong(ref.get().value);
    }

    // ===== Sorted Set Methods =====

    /**
     * Adds a value to a sorted set with a score.
     */
    public boolean zadd(String key, double score, String value) {
        ConcurrentSkipListMap<Double, String> set = sortedSets.computeIfAbsent(key, k -> new ConcurrentSkipListMap<>());
        String oldValue = set.get(score);
        
        // Calculate memory delta
        long memoryDelta = calculateSortedSetMemoryDelta(key, oldValue, value);
        
        logger.debug("ZADD key: {}, Current memory: {}, Delta: {}, Max: {}", 
                    key, currentMemoryUsed.get(), memoryDelta, maxMemory);

        if (currentMemoryUsed.get() + memoryDelta > maxMemory) {
            checkMemoryLimit();
        }
        
        // Update memory usage and add to sorted set
        currentMemoryUsed.addAndGet(memoryDelta);
        set.put(score, value);
        return true;  // Always return true to match Redis behavior
    }

    /**
     * Returns the number of elements in a sorted set.
     */
    public long zcard(String key) {
        ConcurrentSkipListMap<Double, String> set = sortedSets.get(key);
        return set != null ? set.size() : 0;
    }

    /**
     * Returns the rank of a value in a sorted set.
     */
    public Long zrank(String key, String value) {
        ConcurrentSkipListMap<Double, String> set = sortedSets.get(key);
        if (set != null) {
            int rank = 0;
            for (Map.Entry<Double, String> entry : set.entrySet()) {
                if (entry.getValue().equals(value)) {
                    return (long) rank;
                }
                rank++;
            }
        }
        return null;
    }

    /**
     * Returns a range of values from a sorted set.
     */
    public List<String> zrange(String key, long start, long end) {
        ConcurrentSkipListMap<Double, String> set = sortedSets.get(key);
        if (set != null) {
            return set.values().stream()
                    .skip(start)
                    .limit(end - start + 1)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    // ===== Utility Methods =====

    /**
     * Updates the access time for a key.
     */
    private void updateAccessTime(String key) {
        if (evictionStrategy instanceof LRUEvictionStrategy) {
            ((LRUEvictionStrategy)evictionStrategy).updateAccessTime(key, System.currentTimeMillis());
        }
    }

    /**
     * Returns the current number of keys in the database.
     */
    public long dbSize() {
        return dbSize.get();
    }

    /**
     * Returns the current memory usage in bytes.
     */
    public long getCurrentMemoryUsed() {
        return currentMemoryUsed.get();
    }

    /**
     * Returns the maximum memory limit in bytes.
     */
    public long getMaxMemory() {
        return maxMemory;
    }

    /**
     * Returns the current eviction policy.
     */
    public EvictionPolicy getEvictionPolicy() {
        return EvictionPolicy.LRU;
    }

    /**
     * Clears all data from the database.
     */
    protected void clear() {
        keyValueStore.clear();
        sortedSets.clear();
        dbSize.set(0);
        currentMemoryUsed.set(0);
    }

    /**
     * Sets a key-value pair with an expiry time in seconds.
     */
    public void setEx(String key, String value, long seconds) {
        set(key, value, seconds);
    }
} 