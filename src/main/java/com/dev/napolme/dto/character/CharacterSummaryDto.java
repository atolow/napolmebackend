package com.dev.napolme.dto.character;

public record CharacterSummaryDto(
    String characterId,
    String name,
    Integer level,
    Integer serverId,
    String serverName,
    Integer race,
    Integer classId,
    String profileImageUrl,
    Integer combatPower
) {}
