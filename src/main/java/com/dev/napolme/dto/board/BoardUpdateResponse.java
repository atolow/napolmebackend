package com.dev.napolme.dto.board;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.time.Instant;
import java.util.List;

public record BoardUpdateResponse(
    List<BoardUpdateItemDto> items,
    Instant lastUpdated,
    CachePolicyDto cache
) {}
