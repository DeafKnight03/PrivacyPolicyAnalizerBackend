package com.dto;

public record SignupRequest(
        String username,
        String password,
        String role // optional; if null/blank we’ll default to USER
) {}
