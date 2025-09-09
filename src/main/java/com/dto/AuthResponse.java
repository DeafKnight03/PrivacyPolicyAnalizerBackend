package com.dto;


public record AuthResponse(String accessToken, String refreshToken, String username, String role) {}
