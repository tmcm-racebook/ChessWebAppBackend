package com.chess.backend.security.dto;

public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private String username;

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
} 