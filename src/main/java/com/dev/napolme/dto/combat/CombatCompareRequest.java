package com.dev.napolme.dto.combat;

import java.util.List;

/**
 * 캐릭터 전투력 비교 요청
 */
public record CombatCompareRequest(
    List<Long> characterIds
) {}
