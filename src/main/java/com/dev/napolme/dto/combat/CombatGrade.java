package com.dev.napolme.dto.combat;

/**
 * 전투력 등급
 */
public enum CombatGrade {
    SSS, SS, S, A, B, C, D;

    public static CombatGrade fromTotalPower(long totalPower) {
        if (totalPower >= 100_000) return SSS;
        if (totalPower >= 80_000) return SS;
        if (totalPower >= 60_000) return S;
        if (totalPower >= 40_000) return A;
        if (totalPower >= 25_000) return B;
        if (totalPower >= 10_000) return C;
        return D;
    }
}
