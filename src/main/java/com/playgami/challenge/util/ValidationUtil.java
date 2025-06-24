package com.playgami.challenge.util;

public class ValidationUtil {
    private static final String ALLOWED_CHARS = "[a-zA-Z0-9-_]+";

    public static void validateKeyOrValue(String input, String fieldName) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
        if (!input.matches(ALLOWED_CHARS)) {
            throw new IllegalArgumentException(fieldName + " can only contain letters, numbers, hyphens, and underscores");
        }
    }
} 