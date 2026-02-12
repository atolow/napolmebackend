package com.dev.napolme.dto.combat;

import java.math.BigDecimal;

/**
 * 전투력 계산 상세
 */
public record CombatScoreBreakdown(
    int baseAttack,
    BigDecimal critMultiplier,
    BigDecimal speedMultiplier,
    BigDecimal penetrationBonus,
    BigDecimal effectiveDps
) {}
