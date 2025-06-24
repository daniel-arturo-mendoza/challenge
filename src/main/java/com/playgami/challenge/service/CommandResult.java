package com.playgami.challenge.service;

public class CommandResult {
    private final String response;
    private final boolean isError;

    public CommandResult(String response, boolean isError) {
        this.response = response;
        this.isError = isError;
    }

    public String getResponse() {
        return response;
    }

    public boolean isError() {
        return isError;
    }
} 