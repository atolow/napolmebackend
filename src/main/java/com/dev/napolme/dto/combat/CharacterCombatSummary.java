package com.dev.napolme.dto.combat;

/**
 * 비교용 캐릭터 전투력 요약
 */
public record CharacterCombatSummary(
    long characterId,
    String nickname,
    String className,
    long combatPower,
    long dpsScore,
    int rank,
    CombatGrade grade
) {}
