package com.dev.napolme.dto.stat;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.time.Instant;
import java.util.List;

public record PopularStatResponse(
    String type,
    Instant asOf,
    List<PopularStatItemDto> items,
    CachePolicyDto cache
) {}
