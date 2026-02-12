package com.dev.napolme.dto.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.util.List;

public record CharacterSearchResponse(
    String query,
    String server,
    int total,
    List<CharacterSummaryDto> items,
    CachePolicyDto cache
) {}
