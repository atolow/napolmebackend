package com.dev.napolme.service.combat;

import com.dev.napolme.dto.combat.BuffedStatsInfo;
import com.dev.napolme.dto.combat.CombatGrade;
import com.dev.napolme.dto.combat.CombatScoreBreakdown;
import com.dev.napolme.dto.combat.CombatScoreResponse;
import com.dev.napolme.dto.combat.Role;
import com.dev.napolme.dto.combat.StatsInput;
import com.dev.napolme.dto.combat.SynergyDetails;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 전투력 계산기 (CALCULATION.md / testbackend CombatPowerCalculator 기준)
 * - 역할별 가중치, 치명타·속도·관통 보정, DPS/방어/생존/유틸 점수 → 종합 전투력
 */
@Component
public class CombatPowerCalculator {

    private static final int ATTACK_PERCENT_CAP = 100;

    private static final Map<Role, RoleWeight> ROLE_WEIGHTS = Map.of(
        Role.TANK, new RoleWeight(0.6, 0.4, 1.5, 1.3, 0.5, 0.8),
        Role.MELEE_DPS, new RoleWeight(1.2, 0.3, 0.8, 0.8, 1.3, 1.2),
        Role.RANGED_DPS, new RoleWeight(1.2, 0.3, 0.6, 0.7, 1.2, 1.1),
        Role.MAGIC_DPS, new RoleWeight(0.3, 1.3, 0.5, 0.6, 1.2, 1.2),
        Role.HEALER, new RoleWeight(0.2, 1.0, 0.7, 1.0, 0.5, 1.0),
        Role.SUPPORT, new RoleWeight(0.8, 0.8, 0.9, 1.0, 0.8, 0.9),
        Role.ASSASSIN, new RoleWeight(1.3, 0.2, 0.4, 0.5, 1.5, 1.3),
        Role.SUMMONER, new RoleWeight(0.4, 1.1, 0.6, 0.7, 1.0, 1.0)
    );

    private static final RoleWeight DEFAULT_WEIGHT = new RoleWeight(1.0, 1.0, 1.0, 1.0, 1.0, 1.0);

    public CombatScoreResponse calculateWithBuffs(
        StatsInput stats,
        Role role,
        SynergyDetails partyBuffs
    ) {
        BuffedStatsInfo buffedStats = partyBuffs != null
            ? applyBuffsToStats(stats, partyBuffs)
            : null;
        StatsInput effectiveStats = buffedStats != null
            ? stats.copyWithBuffed(buffedStats)
            : stats;
        return calculateInternal(effectiveStats, role, buffedStats);
    }

    public CombatScoreResponse calculate(StatsInput stats) {
        return calculateInternal(stats, null, null);
    }

    private CombatScoreResponse calculateInternal(
        StatsInput stats,
        Role role,
        BuffedStatsInfo buffedStats
    ) {
        RoleWeight weight = role != null && ROLE_WEIGHTS.containsKey(role)
            ? ROLE_WEIGHTS.get(role)
            : DEFAULT_WEIGHT;

        int baseAttack = selectPrimaryAttack(stats, role);

        BigDecimal effectiveCritRate = stats.criticalRate().min(BigDecimal.valueOf(100));
        BigDecimal critRate = effectiveCritRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal critDamageBonus = stats.criticalDamage()
            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
            .subtract(BigDecimal.ONE);
        BigDecimal critMultiplier = BigDecimal.ONE.add(critRate.multiply(critDamageBonus));

        BigDecimal speedMultiplier = calculateSpeedMultiplier(stats, role);

        double penetrationRatio = Math.min(stats.penetration() / 10000.0, 0.3);
        BigDecimal penetrationBonus = BigDecimal.ONE.add(BigDecimal.valueOf(penetrationRatio));

        BigDecimal effectiveDps = BigDecimal.valueOf(baseAttack)
            .multiply(critMultiplier)
            .multiply(speedMultiplier)
            .multiply(penetrationBonus);
        long dpsScore = effectiveDps.longValue();

        long defenseScore = (long) (
            (stats.defense() + stats.magicDefense()) * 0.5 * weight.defense + stats.resilience()
        );
        long survivalScore = (long) (
            stats.hp() * 0.01 * weight.hp + stats.evasion() * 0.5
        );
        long utilityScore = (long) (
            stats.accuracy() * 0.3
                + stats.attackSpeed().add(stats.castingSpeed()).multiply(BigDecimal.valueOf(100)).doubleValue()
        );

        double attackWeight = (weight.attack + weight.magicAttack) / 2;
        long totalCombatPower = (long) (
            dpsScore * 0.5 * attackWeight
                + defenseScore * 0.2 * weight.defense
                + survivalScore * 0.2 * weight.hp
                + utilityScore * 0.1
        );

        CombatGrade grade = calculateGrade(totalCombatPower);
        CombatScoreBreakdown breakdown = new CombatScoreBreakdown(
            baseAttack,
            critMultiplier.setScale(4, RoundingMode.HALF_UP),
            speedMultiplier.setScale(4, RoundingMode.HALF_UP),
            penetrationBonus.setScale(4, RoundingMode.HALF_UP),
            effectiveDps.setScale(2, RoundingMode.HALF_UP)
        );

        return new CombatScoreResponse(
            dpsScore,
            defenseScore,
            survivalScore,
            utilityScore,
            totalCombatPower,
            breakdown,
            buffedStats,
            grade
        );
    }

    private BuffedStatsInfo applyBuffsToStats(StatsInput stats, SynergyDetails buffs) {
        BigDecimal attackPercentBonus = buffs.attackBonus().min(BigDecimal.valueOf(ATTACK_PERCENT_CAP));
        BigDecimal magicAttackPercentBonus = buffs.magicAttackBonus().min(BigDecimal.valueOf(ATTACK_PERCENT_CAP));

        int buffedAttack = BigDecimal.valueOf(stats.attack())
            .multiply(BigDecimal.ONE.add(attackPercentBonus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
            .setScale(0, RoundingMode.HALF_UP).intValue();
        int buffedMagicAttack = BigDecimal.valueOf(stats.magicAttack())
            .multiply(BigDecimal.ONE.add(magicAttackPercentBonus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
            .setScale(0, RoundingMode.HALF_UP).intValue();

        int buffedDefense = BigDecimal.valueOf(stats.defense())
            .multiply(BigDecimal.ONE.add(buffs.defenseBonus().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
            .setScale(0, RoundingMode.HALF_UP).intValue();
        int buffedMagicDefense = BigDecimal.valueOf(stats.magicDefense())
            .multiply(BigDecimal.ONE.add(buffs.magicDefenseBonus().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
            .setScale(0, RoundingMode.HALF_UP).intValue();

        BigDecimal buffedCritRate = stats.criticalRate().add(buffs.criticalRateBonus());
        BigDecimal buffedCritDamage = stats.criticalDamage().add(buffs.criticalDamageBonus());
        BigDecimal buffedAttackSpeed = stats.attackSpeed()
            .multiply(BigDecimal.ONE.add(buffs.attackSpeedBonus().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
            .setScale(4, RoundingMode.HALF_UP);
        int buffedAccuracy = stats.accuracy() + buffs.accuracyBonus().intValue();

        return new BuffedStatsInfo(
            buffedAttack, buffedMagicAttack, buffedDefense, buffedMagicDefense,
            buffedCritRate, buffedCritDamage, buffedAttackSpeed, buffedAccuracy
        );
    }

    private int selectPrimaryAttack(StatsInput stats, Role role) {
        if (role == null) {
            return Math.max(stats.attack(), stats.magicAttack());
        }
        return switch (role) {
            case MAGIC_DPS, HEALER, SUMMONER -> stats.magicAttack();
            case MELEE_DPS, RANGED_DPS, ASSASSIN -> stats.attack();
            default -> Math.max(stats.attack(), stats.magicAttack());
        };
    }

    private BigDecimal calculateSpeedMultiplier(StatsInput stats, Role role) {
        if (role == null) {
            return stats.attackSpeed().add(stats.castingSpeed()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        }
        return switch (role) {
            case MAGIC_DPS, HEALER, SUMMONER -> stats.castingSpeed();
            case MELEE_DPS, RANGED_DPS, ASSASSIN -> stats.attackSpeed();
            default -> stats.attackSpeed().add(stats.castingSpeed()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP);
        };
    }

    private static CombatGrade calculateGrade(long totalPower) {
        if (totalPower >= 100_000) return CombatGrade.SSS;
        if (totalPower >= 80_000) return CombatGrade.SS;
        if (totalPower >= 60_000) return CombatGrade.S;
        if (totalPower >= 40_000) return CombatGrade.A;
        if (totalPower >= 25_000) return CombatGrade.B;
        if (totalPower >= 10_000) return CombatGrade.C;
        return CombatGrade.D;
    }

    private record RoleWeight(
        double attack, double magicAttack, double defense, double hp, double critical, double speed
    ) {}
}
