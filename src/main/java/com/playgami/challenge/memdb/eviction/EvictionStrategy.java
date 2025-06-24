package com.playgami.challenge.memdb.eviction;

import com.playgami.challenge.memdb.ValueWrapper;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public interface EvictionStrategy {
    String selectKeyToEvict(Map<String, AtomicReference<ValueWrapper>> keyValueStore, Map<String, Long> lastAccessTime);
} 