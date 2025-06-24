package com.playgami.challenge.memdb;

import com.playgami.challenge.memdb.eviction.EvictionStrategy;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;

@Component
@Primary
public class MockMemDB extends MemDB {
    public MockMemDB(EvictionStrategy evictionStrategy) {
        super(evictionStrategy);
    }

    @Override
    protected boolean isMemoryLimitExceeded() {
        return true; // Always return true for testing eviction
    }

    public void clear() {
        super.clear();
    }
} 