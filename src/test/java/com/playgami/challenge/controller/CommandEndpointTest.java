package com.playgami.challenge.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CommandEndpointTest {

    private static final Logger logger = LoggerFactory.getLogger(CommandEndpointTest.class);

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testAllCommands() throws InterruptedException {
        // SET command
        String setResponse = restTemplate.getForObject("/?cmd=SET testkey testvalue", String.class);
        assertEquals("OK", setResponse);

        // GET command
        String getResponse = restTemplate.getForObject("/?cmd=GET testkey", String.class);
        assertEquals("testvalue", getResponse);

        // DEL command
        String delResponse = restTemplate.getForObject("/?cmd=DEL testkey", String.class);
        assertEquals("OK", delResponse);

        // Set up data for INCR
        restTemplate.getForObject("/?cmd=SET counter 0", String.class);

        // INCR command
        String incrResponse = restTemplate.getForObject("/?cmd=INCR counter", String.class);
        assertEquals("1", incrResponse);

        // Set up data for ZADD
        restTemplate.getForObject("/?cmd=ZADD scores 100 player1", String.class);

        // ZADD command
        String zaddResponse = restTemplate.getForObject("/?cmd=ZADD scores 200 player2", String.class);
        logger.debug("ZADD Response: {}", zaddResponse);
        assertEquals("OK", zaddResponse);

        // ZCARD command
        String zcardResponse = restTemplate.getForObject("/?cmd=ZCARD scores", String.class);
        assertEquals("2", zcardResponse);

        // ZRANK command
        String zrankResponse = restTemplate.getForObject("/?cmd=ZRANK scores player1", String.class);
        assertEquals("0", zrankResponse);

        // ZRANGE command
        String zrangeResponse = restTemplate.getForObject("/?cmd=ZRANGE scores 0 1", String.class);
        assertTrue(zrangeResponse.contains("player1"));
        assertTrue(zrangeResponse.contains("player2"));

        // SETEX command
        String setexResponse = restTemplate.getForObject("/?cmd=SET expirekey expirevalue EX 1", String.class);
        assertEquals("OK", setexResponse);
        // Wait for 2 seconds to allow the key to expire
        Thread.sleep(2000);
        // Try to access the expired key
        String expiredResponse = restTemplate.getForObject("/?cmd=GET expirekey", String.class);
        assertEquals("(nil)", expiredResponse);

        // Invalid command
        ResponseEntity<String> invalidResponse = restTemplate.getForEntity("/?cmd=INVALID", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, invalidResponse.getStatusCode());
    }
} 