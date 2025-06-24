package com.playgami.challenge.memdb.eviction;

import com.playgami.challenge.memdb.ValueWrapper;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class LRUEvictionStrategy implements EvictionStrategy {
    // Using ConcurrentSkipListMap for thread-safe sorted map operations
    private final ConcurrentSkipListMap<Long, String> accessTimeMap = new ConcurrentSkipListMap<>();

    @Override
    public String selectKeyToEvict(Map<String, AtomicReference<ValueWrapper>> keyValueStore, Map<String, Long> lastAccessTime) {
        // Get the oldest entry (first entry in TreeMap)
        Map.Entry<Long, String> oldestEntry = accessTimeMap.firstEntry();
        if (oldestEntry == null) {
            return null;
        }

        String oldestKey = oldestEntry.getValue();
        // Verify the key still exists and is not expired
        AtomicReference<ValueWrapper> wrapperRef = keyValueStore.get(oldestKey);
        if (wrapperRef != null && !wrapperRef.get().isExpired()) {
            return oldestKey;
        }

        // If the oldest key is invalid, remove it and try the next one
        accessTimeMap.remove(oldestEntry.getKey());
        return selectKeyToEvict(keyValueStore, lastAccessTime);
    }

    // Method to update access time for a key
    public void updateAccessTime(String key, long accessTime) {
        // First remove any existing entries for this key
        accessTimeMap.entrySet().removeIf(entry -> entry.getValue().equals(key));
        // Then add the new access time
        accessTimeMap.put(accessTime, key);
    }

    // Method to remove a key from tracking
    public void removeKey(String key) {
        // Remove all entries with this key
        accessTimeMap.entrySet().removeIf(entry -> entry.getValue().equals(key));
    }
} 