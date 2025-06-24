package com.playgami.challenge.memdb.eviction;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Interface for sorted set eviction policies.
 * Defines how to select and remove entries from sorted sets when memory is constrained.
 */
public interface SortedSetEvictionStrategy {
    /**
     * Selects and removes an entry from a sorted set based on the policy.
     * @param sortedSets Map of sorted sets to choose from
     * @return true if an entry was removed, false if no entries could be removed
     */
    boolean evictFromSortedSets(Map<String, ConcurrentSkipListMap<Double, String>> sortedSets);
} 