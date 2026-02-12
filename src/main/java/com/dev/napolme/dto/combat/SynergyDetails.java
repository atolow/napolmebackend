package com.dev.napolme.dto.combat;

import java.math.BigDecimal;

/**
 * 파티 시너지 합산 (버프 적용용, 선택)
 */
public record SynergyDetails(
    BigDecimal attackBonus,
    BigDecimal magicAttackBonus,
    BigDecimal defenseBonus,
    BigDecimal magicDefenseBonus,
    BigDecimal criticalRateBonus,
    BigDecimal criticalDamageBonus,
    BigDecimal attackSpeedBonus,
    BigDecimal accuracyBonus
) {
    public static SynergyDetails zero() {
        return new SynergyDetails(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}
