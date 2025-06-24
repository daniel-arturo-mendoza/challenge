package com.playgami.challenge.controller;

import com.playgami.challenge.memdb.StorageEngine;
import com.playgami.challenge.service.MemDBCommandService;
import com.playgami.challenge.util.ValidationUtil;
import com.playgami.challenge.service.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MemDBController {
    private static final Logger logger = LoggerFactory.getLogger(MemDBController.class);
    private static final String OK_RESPONSE  = "OK";
    private static final String NIL_RESPONSE = "(nil)";
    private static final String KEY_FIELD    = "Key";
    private static final String VALUE_FIELD  = "Value";
    private final StorageEngine storageEngine;
    private final MemDBCommandService memDBCommandService;

    public MemDBController(StorageEngine storageEngine, MemDBCommandService memDBCommandService) {
        this.storageEngine = storageEngine;
        this.memDBCommandService = memDBCommandService;
    }

    // Redis-style command endpoint at root level
    @GetMapping("/")
    public ResponseEntity<String> executeCommand(@RequestParam String cmd) {
        CommandResult result = memDBCommandService.executeCommand(cmd);
        if (result.isError()) {
            return ResponseEntity.badRequest().body(result.getResponse());
        }
        return ResponseEntity.ok(result.getResponse());
    }

    // REST API endpoints
    @PostMapping("/set")
    public ResponseEntity<String> set(@RequestParam String key, @RequestParam String value) {
        logger.info("Received set request for key: {} and value: {}", key, value);
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        ValidationUtil.validateKeyOrValue(value, VALUE_FIELD);
        storageEngine.set(key, value);
        logger.info("Set operation completed successfully for key: {}", key);
        return ResponseEntity.ok(OK_RESPONSE);
    }

    @GetMapping("/get")
    public ResponseEntity<String> get(@RequestParam String key) {
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        String value = storageEngine.get(key);
        return ResponseEntity.ok(value != null ? value : NIL_RESPONSE);
    }

    @DeleteMapping("/del")
    public ResponseEntity<String> del(@RequestParam String key) {
        logger.info("Received del request for key: {}", key);
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        boolean deleted = storageEngine.del(key);
        logger.info("Del operation completed for key: {}, result: {}", key, deleted);
        return ResponseEntity.ok(deleted ? OK_RESPONSE : NIL_RESPONSE);
    }

    @GetMapping("/dbsize")
    public ResponseEntity<String> dbSize() {
        return ResponseEntity.ok(String.valueOf(storageEngine.dbSize()));
    }

    @PostMapping("/incr")
    public ResponseEntity<String> incr(@RequestParam String key) {
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        long value = storageEngine.incr(key);
        return ResponseEntity.ok(String.valueOf(value));
    }

    @PostMapping("/zadd")
    public ResponseEntity<String> zadd(
            @RequestParam String key,
            @RequestParam double score,
            @RequestParam String value) {
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        ValidationUtil.validateKeyOrValue(value, VALUE_FIELD);
        boolean added = storageEngine.zadd(key, score, value);
        return ResponseEntity.ok(added ? OK_RESPONSE : NIL_RESPONSE);
    }

    @GetMapping("/zcard")
    public ResponseEntity<String> zcard(@RequestParam String key) {
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        long size = storageEngine.zcard(key);
        return ResponseEntity.ok(String.valueOf(size));
    }

    @GetMapping("/zrank")
    public ResponseEntity<String> zrank(
            @RequestParam String key,
            @RequestParam String value) {
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        ValidationUtil.validateKeyOrValue(value, VALUE_FIELD);
        Long rank = storageEngine.zrank(key, value);
        return ResponseEntity.ok(rank != null ? String.valueOf(rank) : NIL_RESPONSE);
    }

    @GetMapping("/zrange")
    public ResponseEntity<List<String>> zrange(
            @RequestParam String key,
            @RequestParam long start,
            @RequestParam long end) {
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        List<String> range = storageEngine.zrange(key, start, end);
        return ResponseEntity.ok(range);
    }

    @PostMapping("/setex")
    public ResponseEntity<String> setEx(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam long seconds) {
        ValidationUtil.validateKeyOrValue(key, KEY_FIELD);
        ValidationUtil.validateKeyOrValue(value, VALUE_FIELD);
        storageEngine.setEx(key, value, seconds);
        return ResponseEntity.ok(OK_RESPONSE);
    }
} 