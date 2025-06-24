package com.playgami.challenge.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class MemDBConcurrentIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(MemDBConcurrentIntegrationTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testConcurrentSetAndGet() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> {
                String key = "concurrentKey" + index;
                String value = "value" + index;
                String setResponse = restTemplate.postForObject("http://localhost:8080/set?key=" + key + "&value=" + value, null, String.class);
                if ("OK".equals(setResponse)) {
                    String getResponse = restTemplate.getForObject("http://localhost:8080/get?key=" + key, String.class);
                    if (value.equals(getResponse)) {
                        successCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(100, successCount.get());
    }

    @Test
    void testConcurrentIncr() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                String incrResponse = restTemplate.postForObject("http://localhost:8080/incr?key=concurrentCounter", null, String.class);
                if (incrResponse != null) {
                    successCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(100, successCount.get());
    }

    @Test
    void testConcurrentZAdd() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 100; i++) {
            final int index = i;
            executor.submit(() -> {
                String zaddResponse = restTemplate.postForObject("http://localhost:8080/zadd?key=concurrentScores&score=" + index + "&value=player" + index, null, String.class);
                logger.debug("ZADD operation {} response: {}", index, zaddResponse);
                if ("OK".equals(zaddResponse)) {
                    successCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        logger.debug("testConcurrentZAdd() - Success count: {}", successCount.get());
        assertEquals(100, successCount.get());
    }

    @Test
    void testConcurrentMixedOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        int operations = 100;

        // Test concurrent mixed operations (key-value and sorted sets)
        for (int i = 0; i < operations; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Key-value operations
                    String key = "key" + index;
                    String value = "value" + index;
                    String setResponse = restTemplate.postForObject("http://localhost:8080/set?key=" + key + "&value=" + value, null, String.class);
                    if ("OK".equals(setResponse)) {
                        String getResponse = restTemplate.getForObject("http://localhost:8080/get?key=" + key, String.class);
                        if (value.equals(getResponse)) {
                            successCount.incrementAndGet();
                        }
                    }

                    // Sorted set operations
                    String zsetKey = "zset" + (index % 10); // Use 10 different keys in the sortedSets map
                    double score = index;
                    String zsetValue = "value" + index;
                    String zaddResponse = restTemplate.postForObject("http://localhost:8080/zadd?key=" + zsetKey + "&score=" + score + "&value=" + zsetValue, null, String.class);
                    if ("OK".equals(zaddResponse)) {
                        String zcardResponse = restTemplate.getForObject("http://localhost:8080/zcard?key=" + zsetKey, String.class);
                        if (Integer.parseInt(zcardResponse) > 0) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    // Log error but don't fail the test
                    System.err.println("Error in concurrent operation: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        assertTrue(successCount.get() >= operations, "Expected at least " + operations + " successful operations, but got " + successCount.get());
    }

    @Test
    void testConcurrentSortedSetOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        int operations = 100;
        String zsetKey = "concurrentZSet"; // Single key in the sortedSets map

        // Test concurrent operations on a single TreeMap in the sortedSets map
        for (int i = 0; i < operations; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    // Add to the TreeMap
                    String zaddResponse = restTemplate.postForObject(
                        "http://localhost:8080/zadd?key=" + zsetKey + "&score=" + index + "&value=value" + index, 
                        null, 
                        String.class
                    );
                    if ("OK".equals(zaddResponse)) {
                        // Get rank from the TreeMap
                        String zrankResponse = restTemplate.getForObject(
                            "http://localhost:8080/zrank?key=" + zsetKey + "&value=value" + index, 
                            String.class
                        );
                        if (!"(nil)".equals(zrankResponse)) {
                            // Get range from the TreeMap
                            String zrangeResponse = restTemplate.getForObject(
                                "http://localhost:8080/zrange?key=" + zsetKey + "&start=0&end=" + (index % 10), 
                                String.class
                            );
                            if (zrangeResponse != null && !zrangeResponse.isEmpty()) {
                                successCount.incrementAndGet();
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error in concurrent sorted set operation: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        assertTrue(successCount.get() >= operations, "Expected at least " + operations + " successful sorted set operations, but got " + successCount.get());
    }

    @Test
    void testConcurrentIncrAndSortedSet() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        int operations = 100;
        String counterKey = "concurrentCounter";
        String zsetKey = "concurrentZSet"; // Single key in the sortedSets map

        // Test concurrent increment and operations on a single TreeMap
        for (int i = 0; i < operations; i++) {
            executor.submit(() -> {
                try {
                    // Increment counter
                    String incrResponse = restTemplate.postForObject(
                        "http://localhost:8080/incr?key=" + counterKey, 
                        null, 
                        String.class
                    );
                    if (incrResponse != null) {
                        // Add to the TreeMap with the counter value as score
                        String zaddResponse = restTemplate.postForObject(
                            "http://localhost:8080/zadd?key=" + zsetKey + "&score=" + incrResponse + "&value=value" + incrResponse, 
                            null, 
                            String.class
                        );
                        if ("OK".equals(zaddResponse)) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error in concurrent incr and sorted set operation: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        assertTrue(successCount.get() >= operations, "Expected at least " + operations + " successful operations, but got " + successCount.get());
    }
} 