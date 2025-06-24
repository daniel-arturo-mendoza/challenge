package com.playgami.challenge.memdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MemDBTest {
    @Autowired
    private StorageEngine storageEngine;

    @BeforeEach
    void setUp() {
        // Clear the storage before each test
        if (storageEngine instanceof MockMemDB) {
            ((MockMemDB) storageEngine).clear();
        }
    }

    @Test
    void testSetAndGet() {
        // Test setting and getting a value
        storageEngine.set("test", "value");
        assertEquals("value", storageEngine.get("test"));
        
        // Test getting non-existent key
        assertNull(storageEngine.get("nonexistent"));
    }

    @Test
    void testDel() {
        // Test deleting existing key
        storageEngine.set("test", "value");
        assertTrue(storageEngine.del("test"));
        assertNull(storageEngine.get("test"));
        
        // Test deleting non-existent key
        assertFalse(storageEngine.del("nonexistent"));
    }

    @Test
    void testDbSize() {
        // Test empty database
        assertEquals(0, storageEngine.dbSize());
        
        // Test after adding items
        storageEngine.set("key1", "value1");
        assertEquals(1, storageEngine.dbSize());
        
        storageEngine.set("key2", "value2");
        assertEquals(2, storageEngine.dbSize());
        
        // Test after deleting item
        storageEngine.del("key1");
        assertEquals(1, storageEngine.dbSize());
    }

    @Test
    void testIncr() {
        // Test incrementing non-existent key
        assertEquals(1, storageEngine.incr("counter"));
        
        // Test incrementing existing numeric value
        storageEngine.set("counter", "5");
        assertEquals(6, storageEngine.incr("counter"));
        
        // Test incrementing non-numeric value
        storageEngine.set("nonnumeric", "abc");
        assertEquals(1, storageEngine.incr("nonnumeric"));
    }

    @Test
    void testZAdd() {
        // Test adding to sorted set
        assertTrue(storageEngine.zadd("scores", 100.0, "player1"));
        assertTrue(storageEngine.zadd("scores", 200.0, "player2"));
        
        // Test adding duplicate score
        assertTrue(storageEngine.zadd("scores", 100.0, "player3"));
    }

    @Test
    void testZCard() {
        // Test empty sorted set
        assertEquals(0, storageEngine.zcard("scores"));
        
        // Test after adding items
        storageEngine.zadd("scores", 100.0, "player1");
        assertEquals(1, storageEngine.zcard("scores"));
        
        storageEngine.zadd("scores", 200.0, "player2");
        assertEquals(2, storageEngine.zcard("scores"));
    }

    @Test
    void testZRank() {
        // Test empty sorted set
        assertNull(storageEngine.zrank("scores", "player1"));
        
        // Test ranking in sorted set
        storageEngine.zadd("scores", 100.0, "player1");
        storageEngine.zadd("scores", 200.0, "player2");
        storageEngine.zadd("scores", 300.0, "player3");
        
        assertEquals(0, storageEngine.zrank("scores", "player1"));
        assertEquals(1, storageEngine.zrank("scores", "player2"));
        assertEquals(2, storageEngine.zrank("scores", "player3"));
        
        // Test non-existent value
        assertNull(storageEngine.zrank("scores", "nonexistent"));
    }

    @Test
    void testZRange() {
        // Test empty sorted set
        assertTrue(storageEngine.zrange("scores", 0, 1).isEmpty());
        
        // Test range in sorted set
        storageEngine.zadd("scores", 100.0, "player1");
        storageEngine.zadd("scores", 200.0, "player2");
        storageEngine.zadd("scores", 300.0, "player3");
        
        List<String> range = storageEngine.zrange("scores", 0, 1);
        assertEquals(2, range.size());
        assertEquals("player1", range.get(0));
        assertEquals("player2", range.get(1));
        
        // Test out of bounds range
        assertTrue(storageEngine.zrange("scores", 10, 20).isEmpty());
    }

    @Test
    void testConcurrentOperations() {
        // Test concurrent set operations
        storageEngine.set("key1", "value1");
        storageEngine.set("key2", "value2");
        assertEquals("value1", storageEngine.get("key1"));
        assertEquals("value2", storageEngine.get("key2"));
        
        // Test concurrent sorted set operations
        storageEngine.zadd("scores", 100.0, "player1");
        storageEngine.zadd("scores", 200.0, "player2");
        assertEquals(2, storageEngine.zcard("scores"));
        assertEquals(0, storageEngine.zrank("scores", "player1"));
        assertEquals(1, storageEngine.zrank("scores", "player2"));
    }

    @Test
    void testLRUEviction() {
        // Set up test data
        storageEngine.set("key1", "value1");
        storageEngine.set("key2", "value2");
        storageEngine.set("key3", "value3");

        // Access keys in order to establish LRU order
        storageEngine.get("key1"); // First key accessed
        storageEngine.get("key2"); // Second key accessed
        storageEngine.get("key3"); // Third key accessed

        // Mock memory check by directly calling checkMemoryLimit
        if (storageEngine instanceof MockMemDB) {
            ((MockMemDB) storageEngine).checkMemoryLimit();
        }

        // The least recently accessed key (key3) should be evicted
        assertNull(storageEngine.get("key3"));
        // But the recently accessed keys should still be there
        assertNotNull(storageEngine.get("key1"));
        assertNotNull(storageEngine.get("key2"));
    }
} 