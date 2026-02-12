package com.dev.napolme.dto.combat;

import java.math.BigDecimal;

/**
 * 버프 적용 후 스탯 (선택 반환)
 */
public record BuffedStatsInfo(
    int attack,
    int magicAttack,
    int defense,
    int magicDefense,
    BigDecimal criticalRate,
    BigDecimal criticalDamage,
    BigDecimal attackSpeed,
    int accuracy
) {}
