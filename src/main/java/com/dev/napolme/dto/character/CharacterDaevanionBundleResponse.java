package com.dev.napolme.dto.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.util.List;

public record CharacterDaevanionBundleResponse(
    List<CharacterDaevanionDetailResponse> boards,
    CachePolicyDto cache
) {}
