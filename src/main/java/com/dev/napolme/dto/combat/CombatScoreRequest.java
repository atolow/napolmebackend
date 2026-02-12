package com.dev.napolme.dto.combat;

import jakarta.validation.Valid;

/**
 * 전투력 계산 요청.
 * - serverId + characterId: 캐릭터 정보 조회 후 스탯 매핑하여 계산
 * - stats: 직접 스탯 입력으로 계산
 */
public record CombatScoreRequest(
    String serverId,
    String characterId,
    Long savedCharacterId,
    @Valid StatsInput stats,
    Role role,
    SynergyDetails partyBuffs
) {}
