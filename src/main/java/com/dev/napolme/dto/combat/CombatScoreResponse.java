package com.dev.napolme.dto.combat;

/**
 * 전투력 계산 응답
 */
public record CombatScoreResponse(
    long dpsScore,
    long defenseScore,
    long survivalScore,
    long utilityScore,
    long totalCombatPower,
    CombatScoreBreakdown breakdown,
    BuffedStatsInfo buffedStats,
    CombatGrade grade
) {}
