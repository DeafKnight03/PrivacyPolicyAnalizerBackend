package com.example.myapp.dto;


public record AuthResponse(String accessToken, String refreshToken, String username, String role) {}
