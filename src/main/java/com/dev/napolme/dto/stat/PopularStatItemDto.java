package com.dev.napolme.dto.stat;

public record PopularStatItemDto(
    String key,
    String label,
    long count
) {}
