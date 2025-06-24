package com.playgami.challenge.memdb.eviction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Eviction policy that randomly selects a sorted set and removes its lowest score.
 */
public class RandomLowestScoreEvictionPolicy implements SortedSetEvictionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(RandomLowestScoreEvictionPolicy.class);
    private final Random random = new Random();
    private final AtomicLong currentMemory;
    
    public RandomLowestScoreEvictionPolicy(AtomicLong currentMemory) {
        this.currentMemory = currentMemory;
    }
    
    /**
     * Calculates memory delta for a sorted set operation.
     */
    private long calculateSortedSetMemoryDelta(String key, String value) {
        long valueSize = value != null ? value.length() : 0;
        long keySize = key.length();
        return valueSize + keySize + 8; // Add 8 bytes for the score (Double)
    }
    
    @Override
    public boolean evictFromSortedSets(Map<String, ConcurrentSkipListMap<Double, String>> sortedSets) {
        if (sortedSets.isEmpty()) {
            return false;
        }

        // Keep track of sets we've tried
        Set<String> triedSets = new HashSet<>();
        String[] setKeys = sortedSets.keySet().toArray(new String[0]);
        
        // Keep trying until we've tried all sets
        while (triedSets.size() < setKeys.length) {
            // Get a random set we haven't tried yet
            String randomSetKey;
            do {
                randomSetKey = setKeys[random.nextInt(setKeys.length)];
            } while (triedSets.contains(randomSetKey));
            
            triedSets.add(randomSetKey);
            ConcurrentSkipListMap<Double, String> set = sortedSets.get(randomSetKey);
            
            if (!set.isEmpty()) {
                // Remove the lowest score (first entry in the sorted map)
                Entry<Double, String> lowestScore = set.firstEntry();
                logger.debug("Evicting lowest score from sorted set {}: score={}, value={}", 
                           randomSetKey, lowestScore.getKey(), lowestScore.getValue());
                
                // Update memory usage (negative delta for removal)
                long memoryDelta = -calculateSortedSetMemoryDelta(randomSetKey, lowestScore.getValue());
                currentMemory.addAndGet(memoryDelta);
                
                set.remove(lowestScore.getKey());
                return true;
            }
        }

        return false;
    }
} 