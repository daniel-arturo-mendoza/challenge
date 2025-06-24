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
class MemDBValidationIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testSetWithInvalidKey() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8080/set?key=&value=value", 
            null, 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testSetWithInvalidValue() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8080/set?key=test&value=", 
            null, 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testGetWithInvalidKey() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8080/get?key=", 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testZAddWithInvalidParameters() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8080/zadd?key=&score=100.0&value=player1", 
            null, 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testZRankWithInvalidParameters() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8080/zrank?key=&value=player1", 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testSetExWithInvalidParameters() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8080/setex?key=&value=value&seconds=60", 
            null, 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testIncrWithInvalidKey() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8080/incr?key=", 
            null, 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testZCardWithInvalidKey() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8080/zcard?key=", 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testZRangeWithInvalidKey() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8080/zrange?key=&start=0&end=1", 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testDelWithInvalidKey() {
        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:8080/del?key=", 
            org.springframework.http.HttpMethod.DELETE,
            null,
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testZAddWithInvalidValue() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8080/zadd?key=scores&score=100.0&value=", 
            null, 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testZRankWithInvalidValue() {
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:8080/zrank?key=scores&value=", 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }

    @Test
    void testSetExWithInvalidValue() {
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:8080/setex?key=test&value=&seconds=60", 
            null, 
            String.class
        );
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().startsWith("ERROR:"));
    }
} 