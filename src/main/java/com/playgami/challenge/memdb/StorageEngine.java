package com.playgami.challenge.memdb;

import java.util.List;

public interface StorageEngine {
    void set(String key, String value);
    void setEx(String key, String value, long seconds);
    String get(String key);
    boolean del(String key);
    long dbSize();
    long incr(String key);
    boolean zadd(String key, double score, String value);
    long zcard(String key);
    Long zrank(String key, String value);
    List<String> zrange(String key, long start, long end);
} 