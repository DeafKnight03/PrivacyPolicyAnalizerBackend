package com.example.myapp.dto;

import java.time.OffsetDateTime;

public record ListItem( String text, int good, Long id, OffsetDateTime createdAt) {}
