package com.playgami.challenge.service;

import com.playgami.challenge.memdb.StorageEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static com.playgami.challenge.service.MemDBCommandService.NIL_RESPONSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemDBCommandServiceTest {

    @Mock
    private StorageEngine storageEngine;

    private MemDBCommandService memDBCommandService;

    @BeforeEach
    void setUp() {
        memDBCommandService = new MemDBCommandService(storageEngine);
    }

    @Test
    void handleSet_Success() {
        doNothing().when(storageEngine).set(anyString(), anyString());
        CommandResult result = memDBCommandService.executeCommand("SET key value");
        assertEquals("OK", result.getResponse());
    }

    @Test
    void handleSet_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("SET key");
        assertEquals("Invalid SET command", result.getResponse());
    }

    @Test
    void handleSetEx_Success() {
        doNothing().when(storageEngine).setEx(anyString(), anyString(), anyLong());
        CommandResult result = memDBCommandService.executeCommand("SET key value EX 10");
        assertEquals("OK", result.getResponse());
    }

    @Test
    void handleSetEx_InvalidSeconds() {
        CommandResult result = memDBCommandService.executeCommand("SET key value EX -1");
        assertEquals("EX seconds must be greater than zero", result.getResponse());
    }

    @Test
    void handleSetEx_NonNumericSeconds() {
        CommandResult result = memDBCommandService.executeCommand("SET key value EX abc");
        assertEquals("Invalid EX seconds value", result.getResponse());
    }

    @Test
    void handleGet_Success() {
        when(storageEngine.get("key")).thenReturn("value");
        CommandResult result = memDBCommandService.executeCommand("GET key");
        assertEquals("value", result.getResponse());
    }

    @Test
    void handleGet_NonExistentKey() {
        when(storageEngine.get("key")).thenReturn(null);
        CommandResult result = memDBCommandService.executeCommand("GET key");
        assertEquals(NIL_RESPONSE, result.getResponse());
    }

    @Test
    void handleGet_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("GET");
        assertEquals("Invalid GET command", result.getResponse());
    }

    @Test
    void handleDel_Success() {
        when(storageEngine.del("key")).thenReturn(true);
        CommandResult result = memDBCommandService.executeCommand("DEL key");
        assertEquals("OK", result.getResponse());
    }

    @Test
    void handleDel_NonExistentKey() {
        when(storageEngine.del("key")).thenReturn(false);
        CommandResult result = memDBCommandService.executeCommand("DEL key");
        assertEquals(NIL_RESPONSE, result.getResponse());
    }

    @Test
    void handleDel_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("DEL");
        assertEquals("Invalid DEL command", result.getResponse());
    }

    @Test
    void handleDbSize_Success() {
        when(storageEngine.dbSize()).thenReturn(5L);
        CommandResult result = memDBCommandService.executeCommand("DBSIZE");
        assertEquals("5", result.getResponse());
    }

    @Test
    void handleDbSize_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("DBSIZE extra");
        assertEquals("Invalid DBSIZE command", result.getResponse());
    }

    @Test
    void handleIncr_Success() {
        when(storageEngine.get("key")).thenReturn("1");
        when(storageEngine.incr("key")).thenReturn(2L);
        CommandResult result = memDBCommandService.executeCommand("INCR key");
        assertEquals("2", result.getResponse());
    }

    @Test
    void handleIncr_NonNumericValue() {
        when(storageEngine.get("key")).thenReturn("not-a-number");
        CommandResult result = memDBCommandService.executeCommand("INCR key");
        assertEquals("ERR value is not an integer or out of range", result.getResponse());
    }

    @Test
    void handleIncr_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("INCR");
        assertEquals("Invalid INCR command", result.getResponse());
    }

    @Test
    void handleZAdd_Success() {
        when(storageEngine.zadd(anyString(), anyDouble(), anyString())).thenReturn(true);
        CommandResult result = memDBCommandService.executeCommand("ZADD key 1.5 value");
        assertEquals("OK", result.getResponse());
    }

    @Test
    void handleZAdd_InvalidScore() {
        CommandResult result = memDBCommandService.executeCommand("ZADD key abc value");
        assertEquals("ERR score is not a valid float", result.getResponse());
    }

    @Test
    void handleZAdd_InfinityScore() {
        CommandResult result = memDBCommandService.executeCommand("ZADD key Infinity value");
        assertEquals("ERR score is not a valid float", result.getResponse());
    }

    @Test
    void handleZAdd_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("ZADD key");
        assertEquals("Invalid ZADD command", result.getResponse());
    }

    @Test
    void handleZAdd_NonExistentKey() {
        when(storageEngine.zadd(anyString(), anyDouble(), anyString())).thenReturn(false);
        CommandResult result = memDBCommandService.executeCommand("ZADD key 1.5 value");
        assertEquals(NIL_RESPONSE, result.getResponse());
    }

    @Test
    void handleZCard_Success() {
        when(storageEngine.zcard("key")).thenReturn(3L);
        CommandResult result = memDBCommandService.executeCommand("ZCARD key");
        assertEquals("3", result.getResponse());
    }

    @Test
    void handleZCard_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("ZCARD");
        assertEquals("Invalid ZCARD command", result.getResponse());
    }

    @Test
    void handleZRank_Success() {
        when(storageEngine.zrank("key", "value")).thenReturn(1L);
        CommandResult result = memDBCommandService.executeCommand("ZRANK key value");
        assertEquals("1", result.getResponse());
    }

    @Test
    void handleZRank_NonExistentValue() {
        when(storageEngine.zrank("key", "value")).thenReturn(null);
        CommandResult result = memDBCommandService.executeCommand("ZRANK key value");
        assertEquals(NIL_RESPONSE, result.getResponse());
    }

    @Test
    void handleZRank_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("ZRANK key");
        assertEquals("Invalid ZRANK command", result.getResponse());
    }

    @Test
    void handleZRange_Success() {
        List<String> range = Arrays.asList("value1", "value2");
        when(storageEngine.zrange(anyString(), anyLong(), anyLong())).thenReturn(range);
        CommandResult result = memDBCommandService.executeCommand("ZRANGE key 0 1");
        assertEquals("value1 value2", result.getResponse());
    }

    @Test
    void handleZRange_InvalidStart() {
        CommandResult result = memDBCommandService.executeCommand("ZRANGE key abc 1");
        assertEquals("ERR start or end is not a valid integer", result.getResponse());
    }

    @Test
    void handleZRange_InvalidEnd() {
        CommandResult result = memDBCommandService.executeCommand("ZRANGE key 0 abc");
        assertEquals("ERR start or end is not a valid integer", result.getResponse());
    }

    @Test
    void handleZRange_InvalidCommand() {
        CommandResult result = memDBCommandService.executeCommand("ZRANGE key");
        assertEquals("Invalid ZRANGE command", result.getResponse());
    }

    @Test
    void executeCommand_UnknownCommand() {
        CommandResult result = memDBCommandService.executeCommand("UNKNOWN command");
        assertEquals("Unknown command: UNKNOWN", result.getResponse());
    }

    @Test
    void executeCommand_EmptyCommand() {
        CommandResult result = memDBCommandService.executeCommand("");
        assertEquals("Invalid command", result.getResponse());
    }

    @Test
    void executeCommand_WhitespaceCommand() {
        CommandResult result = memDBCommandService.executeCommand("   ");
        assertEquals("Invalid command", result.getResponse());
    }
} 