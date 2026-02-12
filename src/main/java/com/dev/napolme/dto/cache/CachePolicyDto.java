package com.dev.napolme.dto.cache;

public record CachePolicyDto(
    boolean cacheHit,
    boolean canRefresh
) {}
