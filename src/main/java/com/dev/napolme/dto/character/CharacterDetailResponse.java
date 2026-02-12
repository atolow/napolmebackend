package com.dev.napolme.dto.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.time.Instant;

public record CharacterDetailResponse(
    String server,
    String name,
    Integer level,
    String characterClass,
    Long combatPower,
    String guildName,
    String avatarUrl,
    Instant lastUpdated,
    CachePolicyDto cache
) {}
