package com.dev.napolme.dto.combat;

import java.util.List;

/**
 * 캐릭터 전투력 비교 응답
 */
public record CombatCompareResponse(
    List<CharacterCombatSummary> characters
) {}
