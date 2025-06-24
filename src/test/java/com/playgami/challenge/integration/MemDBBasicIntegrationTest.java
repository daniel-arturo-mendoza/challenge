package com.playgami.challenge.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class MemDBBasicIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        assertNotNull(restTemplate);
    }

    @Test
    void testSetAndGet() {
        String setResponse = restTemplate.postForObject("http://localhost:8080/set?key=test&value=value", null, String.class);
        assertEquals("OK", setResponse);

        String getResponse = restTemplate.getForObject("http://localhost:8080/get?key=test", String.class);
        assertEquals("value", getResponse);
    }

    @Test
    void testDel() {
        restTemplate.postForObject("http://localhost:8080/set?key=delkey&value=toDelete", null, String.class);
        restTemplate.delete("http://localhost:8080/del?key=delkey");
        String getResponse = restTemplate.getForObject("http://localhost:8080/get?key=delkey", String.class);
        assertEquals("(nil)", getResponse);
    }

    @Test
    void testIncr() {
        String incrResponse = restTemplate.postForObject("http://localhost:8080/incr?key=counter", null, String.class);
        assertEquals("1", incrResponse);

        incrResponse = restTemplate.postForObject("http://localhost:8080/incr?key=counter", null, String.class);
        assertEquals("2", incrResponse);

        // Edge case: Incrementing a non-numeric value
        restTemplate.postForObject("http://localhost:8080/set?key=nonnumeric&value=abc", null, String.class);
        incrResponse = restTemplate.postForObject("http://localhost:8080/incr?key=nonnumeric", null, String.class);
        assertEquals("1", incrResponse);
    }

    @Test
    void testZAdd() {
        String zaddResponse = restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=100.0&value=player1", null, String.class);
        assertEquals("OK", zaddResponse);

        zaddResponse = restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=200.0&value=player2", null, String.class);
        assertEquals("OK", zaddResponse);

        // Edge case: Adding a duplicate score
        zaddResponse = restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=100.0&value=player3", null, String.class);
        assertEquals("OK", zaddResponse);
    }

    @Test
    void testZCard() {
        restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=100.0&value=player1", null, String.class);
        restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=200.0&value=player2", null, String.class);

        String zcardResponse = restTemplate.getForObject("http://localhost:8080/zcard?key=scores", String.class);
        assertEquals("2", zcardResponse);

        // Edge case: Empty sorted set
        zcardResponse = restTemplate.getForObject("http://localhost:8080/zcard?key=empty", String.class);
        assertEquals("0", zcardResponse);
    }

    @Test
    void testZRank() {
        restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=100.0&value=player1", null, String.class);
        restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=200.0&value=player2", null, String.class);

        String zrankResponse = restTemplate.getForObject("http://localhost:8080/zrank?key=scores&value=player1", String.class);
        assertEquals("0", zrankResponse);

        zrankResponse = restTemplate.getForObject("http://localhost:8080/zrank?key=scores&value=player2", String.class);
        assertEquals("1", zrankResponse);

        // Edge case: Non-existent value
        zrankResponse = restTemplate.getForObject("http://localhost:8080/zrank?key=scores&value=nonexistent", String.class);
        assertEquals("(nil)", zrankResponse);
    }

    @Test
    void testZRange() {
        restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=100.0&value=player1", null, String.class);
        restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=200.0&value=player2", null, String.class);
        restTemplate.postForObject("http://localhost:8080/zadd?key=scores&score=300.0&value=player3", null, String.class);

        String zrangeResponse = restTemplate.getForObject("http://localhost:8080/zrange?key=scores&start=0&end=1", String.class);
        assertTrue(zrangeResponse.contains("player1") && zrangeResponse.contains("player2"));

        // Edge case: Out of bounds range
        zrangeResponse = restTemplate.getForObject("http://localhost:8080/zrange?key=scores&start=10&end=20", String.class);
        assertEquals("[]", zrangeResponse);
    }
} 