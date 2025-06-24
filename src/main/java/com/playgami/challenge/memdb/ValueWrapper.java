package com.playgami.challenge.memdb;

public class ValueWrapper {
    final String value;
    final Long expiryTime; // null means no expiry

    public ValueWrapper(String value) {
        this.value = value;
        this.expiryTime = null;
    }

    public ValueWrapper(String value, long expiryTime) {
        this.value = value;
        this.expiryTime = expiryTime;
    }

    public boolean isExpired() {
        return expiryTime != null && expiryTime <= System.currentTimeMillis();
    }
} 