package com.dev.napolme.dto.combat;

import java.math.BigDecimal;

/**
 * 전투력 계산용 스탯 입력
 */
public record StatsInput(
    int attack,
    int magicAttack,
    int defense,
    int magicDefense,
    int hp,
    int mp,
    BigDecimal criticalRate,
    BigDecimal criticalDamage,
    int accuracy,
    int evasion,
    BigDecimal attackSpeed,
    BigDecimal castingSpeed,
    int penetration,
    int resilience
) {
    public static StatsInput defaults() {
        return new StatsInput(
            0, 0, 0, 0, 0, 0,
            BigDecimal.ZERO,
            new BigDecimal("150"),
            0, 0,
            BigDecimal.ONE,
            BigDecimal.ONE,
            0, 0
        );
    }

    /** 버프 적용된 스탯으로 새 StatsInput 생성 (전투력 계산용) */
    public StatsInput copyWithBuffed(BuffedStatsInfo buffed) {
        return new StatsInput(
            buffed.attack(),
            buffed.magicAttack(),
            buffed.defense(),
            buffed.magicDefense(),
            hp,
            mp,
            buffed.criticalRate(),
            buffed.criticalDamage(),
            buffed.accuracy(),
            evasion,
            buffed.attackSpeed(),
            castingSpeed,
            penetration,
            resilience
        );
    }
}
