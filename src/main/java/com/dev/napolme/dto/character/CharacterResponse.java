package com.dev.napolme.dto.character;

import java.time.Instant;

/**
 * 저장된 캐릭터 정보 응답 (POST /fetch, GET /{id}, POST /{id}/refresh).
 */
public record CharacterResponse(
    Long id,
    String characterId,
    String serverId,
    String nickname,
    Integer level,
    String serverName,
    String className,
    String tribe,
    String guild,
    String profileImage,
    Integer itemLevel,
    Integer napolmePoint,
    Instant lastSyncedAt,
    Instant createdAt,
    Instant updatedAt
) {}
