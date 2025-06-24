package com.playgami.challenge.memdb;

import com.playgami.challenge.memdb.eviction.LRUEvictionStrategy;
import com.playgami.challenge.memdb.eviction.EvictionStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public EvictionStrategy evictionStrategy() {
        return new LRUEvictionStrategy();
    }
} 