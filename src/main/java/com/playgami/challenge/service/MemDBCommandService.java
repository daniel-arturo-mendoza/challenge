package com.playgami.challenge.service;

import com.playgami.challenge.memdb.StorageEngine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
public class MemDBCommandService {
    public static final String NIL_RESPONSE = "(nil)";
    private final StorageEngine storageEngine;
    // Maps command names to their handler functions
    private final Map<String, Function<String[], String>> commandHandlers;

    public MemDBCommandService(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
        this.commandHandlers = Map.of(
            "SET", this::handleSet,
            "GET", this::handleGet,
            "DEL", this::handleDel,
            "DBSIZE", this::handleDbSize,
            "INCR", this::handleIncr,
            "ZADD", this::handleZAdd,
            "ZCARD", this::handleZCard,
            "ZRANK", this::handleZRank,
            "ZRANGE", this::handleZRange
        );
    }

    public CommandResult executeCommand(String cmd) {
        // Handle empty or whitespace-only commands
        if (cmd == null || cmd.trim().isEmpty()) {
            return new CommandResult("Invalid command", true);
        }

        // Split command into parts by whitespace
        String[] parts = cmd.split("\\s+");
        if (parts.length == 0) {
            return new CommandResult("Invalid command", true);
        }

        try {
            String command = parts[0].toUpperCase();
            Function<String[], String> handler = commandHandlers.get(command);
            if (handler == null) {
                return new CommandResult("Unknown command: " + command, true);
            }
            String response = handler.apply(parts);
            if (response.startsWith("ERR") || response.startsWith("Invalid") || response.startsWith("Unknown") || response.contains("EX seconds must be greater than zero") || response.contains("Invalid EX seconds value")) {
                return new CommandResult(response, true);
            }
            return new CommandResult(response, false);
        } catch (IllegalArgumentException e) {
            return new CommandResult(e.getMessage(), true);
        } catch (Exception e) {
            return new CommandResult("Error executing command: " + e.getMessage(), true);
        }
    }

    /**
     * Handles both SET and SETEX commands:
     * - SET key value
     * - SET key value EX seconds
     */
    private String handleSet(String[] parts) {
        if (parts.length < 3) {
            return "Invalid SET command";
        }
        // Check if this is a SETEX command (5 parts: SET key value EX seconds)
        if (parts.length == 5 && parts[3].equalsIgnoreCase("EX")) {
            return handleSetEx(parts);
        }
        // Regular SET command
        storageEngine.set(parts[1], parts[2]);
        return "OK";
    }

    /**
     * Handles SETEX command: SET key value EX seconds
     * parts[0] = "SET"
     * parts[1] = key
     * parts[2] = value
     * parts[3] = "EX"
     * parts[4] = seconds
     */
    private String handleSetEx(String[] parts) {
        try {
            long seconds = Long.parseLong(parts[4]);
            if (seconds <= 0) {
                return "EX seconds must be greater than zero";
            }
            storageEngine.setEx(parts[1], parts[2], seconds);
            return "OK";
        } catch (NumberFormatException e) {
            return "Invalid EX seconds value";
        }
    }

    /**
     * Handles GET command: GET key
     * Returns the value or (nil) if key doesn't exist
     */
    private String handleGet(String[] parts) {
        if (parts.length != 2) {
            return "Invalid GET command";
        }
        String value = storageEngine.get(parts[1]);
        return value != null ? value : NIL_RESPONSE;
    }

    /**
     * Handles DEL command: DEL key
     * Returns OK if key was deleted, (nil) if key didn't exist
     */
    private String handleDel(String[] parts) {
        if (parts.length != 2) {
            return "Invalid DEL command";
        }
        boolean deleted = storageEngine.del(parts[1]);
        return deleted ? "OK" : NIL_RESPONSE;
    }

    /**
     * Handles DBSIZE command: DBSIZE
     * Returns the number of keys in the database
     */
    private String handleDbSize(String[] parts) {
        if (parts.length != 1) {
            return "Invalid DBSIZE command";
        }
        return String.valueOf(storageEngine.dbSize());
    }

    /**
     * Handles INCR command: INCR key
     * Increments the numeric value by 1
     * Returns error if value is not numeric
     */
    private String handleIncr(String[] parts) {
        if (parts.length != 2) {
            return "Invalid INCR command";
        }
        try {
            String currentValue = storageEngine.get(parts[1]);
            if (currentValue != null) {
                Long.parseLong(currentValue);
            }
            return String.valueOf(storageEngine.incr(parts[1]));
        } catch (NumberFormatException e) {
            return "ERR value is not an integer or out of range";
        }
    }

    /**
     * Handles ZADD command: ZADD key score value
     * Adds a member with score to a sorted set
     * Returns OK if added, (nil) if already exists
     */
    private String handleZAdd(String[] parts) {
        if (parts.length != 4) {
            return "Invalid ZADD command";
        }
        try {
            double score = Double.parseDouble(parts[2]);
            if (Double.isInfinite(score) || Double.isNaN(score)) {
                return "ERR score is not a valid float";
            }
            boolean added = storageEngine.zadd(parts[1], score, parts[3]);
            return added ? "OK" : NIL_RESPONSE;
        } catch (NumberFormatException e) {
            return "ERR score is not a valid float";
        }
    }

    /**
     * Handles ZCARD command: ZCARD key
     * Returns the number of members in a sorted set
     */
    private String handleZCard(String[] parts) {
        if (parts.length != 2) {
            return "Invalid ZCARD command";
        }
        return String.valueOf(storageEngine.zcard(parts[1]));
    }

    /**
     * Handles ZRANK command: ZRANK key member
     * Returns the rank of member in the sorted set
     * Returns (nil) if member doesn't exist
     */
    private String handleZRank(String[] parts) {
        if (parts.length != 3) {
            return "Invalid ZRANK command";
        }
        Long rank = storageEngine.zrank(parts[1], parts[2]);
        return rank != null ? String.valueOf(rank) : NIL_RESPONSE;
    }

    /**
     * Handles ZRANGE command: ZRANGE key start end
     * Returns members in the sorted set within the range
     * start and end must be valid integers
     */
    private String handleZRange(String[] parts) {
        if (parts.length != 4) {
            return "Invalid ZRANGE command";
        }
        try {
            long start = Long.parseLong(parts[2]);
            long end = Long.parseLong(parts[3]);
            List<String> range = storageEngine.zrange(parts[1], start, end);
            return String.join(" ", range);
        } catch (NumberFormatException e) {
            return "ERR start or end is not a valid integer";
        }
    }
} 