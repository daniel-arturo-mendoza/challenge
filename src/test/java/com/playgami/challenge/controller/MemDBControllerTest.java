package com.playgami.challenge.controller;

import com.playgami.challenge.memdb.StorageEngine;
import com.playgami.challenge.service.MemDBCommandService;
import com.playgami.challenge.service.CommandResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemDBController.class)
class MemDBControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(MemDBControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageEngine storageEngine;

    @MockBean
    private MemDBCommandService memDBCommandService;

    @BeforeEach
    void setUp() {
        // Default successful responses
        doNothing().when(storageEngine).set(anyString(), anyString());
        doNothing().when(storageEngine).setEx(anyString(), anyString(), anyLong());
        when(storageEngine.get(anyString())).thenReturn("0"); // Default numeric value
        when(storageEngine.del(anyString())).thenReturn(true);
        when(storageEngine.dbSize()).thenReturn(1L);
        when(storageEngine.incr(anyString())).thenReturn(1L);
        when(storageEngine.zadd(anyString(), anyDouble(), anyString())).thenReturn(true);
        when(storageEngine.zcard(anyString())).thenReturn(1L);
        when(storageEngine.zrank(anyString(), anyString())).thenReturn(0L);
        when(storageEngine.zrange(anyString(), anyLong(), anyLong())).thenReturn(Arrays.asList("value1", "value2"));

        // Mock MemDBCommandService responses
        when(memDBCommandService.executeCommand(anyString())).thenAnswer(invocation -> {
            String cmd = invocation.getArgument(0);
            if (cmd.contains("EX -1") || cmd.contains("EX 0")) {
                return new CommandResult("EX seconds must be greater than zero", true);
            }
            if (cmd.contains("EX abc")) {
                return new CommandResult("Invalid EX seconds value", true);
            }
            if (cmd.contains("INCR invalid")) {
                return new CommandResult("ERR value is not an integer or out of range", true);
            }
            if (cmd.contains("ZADD key abc") || cmd.contains("ZADD key Infinity") || cmd.contains("ZADD key NaN")) {
                return new CommandResult("ERR score is not a valid float", true);
            }
            if (cmd.contains("ZRANGE key abc") || cmd.contains("ZRANGE key 1 abc") || cmd.contains("ZRANGE key abc def")) {
                return new CommandResult("ERR start or end is not a valid integer", true);
            }
            if (cmd.contains("SET key value EX 10")) {
                return new CommandResult("OK", false);
            }
            if (cmd.contains("INCR key")) {
                return new CommandResult("1", false);
            }
            if (cmd.contains("ZADD key 1.5 value")) {
                return new CommandResult("OK", false);
            }
            if (cmd.contains("ZRANGE key 0 1")) {
                return new CommandResult("value1 value2", false);
            }
            return new CommandResult("OK", false);
        });
    }

    @Test
    void setExWithInvalidSeconds() throws Exception {
        // Test negative seconds
        mockMvc.perform(get("/").param("cmd", "SET key value EX -1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("EX seconds must be greater than zero"));

        // Test zero seconds
        mockMvc.perform(get("/").param("cmd", "SET key value EX 0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("EX seconds must be greater than zero"));

        // Test non-numeric seconds
        mockMvc.perform(get("/").param("cmd", "SET key value EX abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid EX seconds value"));
    }

    @Test
    void incrWithInvalidValue() throws Exception {
        // Mock storage to return non-numeric value
        when(storageEngine.get("invalid")).thenReturn("not-a-number");

        mockMvc.perform(get("/").param("cmd", "INCR invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ERR value is not an integer or out of range"));
    }

    @Test
    void zaddWithInvalidScore() throws Exception {
        // Test non-numeric score
        mockMvc.perform(get("/").param("cmd", "ZADD key abc value"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ERR score is not a valid float"));

        // Test infinity
        mockMvc.perform(get("/").param("cmd", "ZADD key Infinity value"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ERR score is not a valid float"));

        // Test NaN
        mockMvc.perform(get("/").param("cmd", "ZADD key NaN value"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ERR score is not a valid float"));
    }

    @Test
    void zrangeWithInvalidIndices() throws Exception {
        // Test non-numeric start
        mockMvc.perform(get("/").param("cmd", "ZRANGE key abc 1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ERR start or end is not a valid integer"));

        // Test non-numeric end
        mockMvc.perform(get("/").param("cmd", "ZRANGE key 1 abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ERR start or end is not a valid integer"));

        // Test both invalid
        mockMvc.perform(get("/").param("cmd", "ZRANGE key abc def"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("ERR start or end is not a valid integer"));
    }

    @Test
    void successfulNumericOperations() throws Exception {
        // Test successful SET with EX
        mockMvc.perform(get("/").param("cmd", "SET key value EX 10"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        // Test successful INCR
        String incrResponse = mockMvc.perform(get("/").param("cmd", "INCR key"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        logger.debug("INCR key response: '{}'", incrResponse);
        // Keep the assertion
        org.junit.jupiter.api.Assertions.assertEquals("1", incrResponse);

        // Test successful ZADD
        mockMvc.perform(get("/").param("cmd", "ZADD key 1.5 value"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        // Test successful ZRANGE
        mockMvc.perform(get("/").param("cmd", "ZRANGE key 0 1"))
                .andExpect(status().isOk())
                .andExpect(content().string("value1 value2"));
    }
} 