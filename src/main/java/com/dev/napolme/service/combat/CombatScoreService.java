package com.dev.napolme.service.combat;

import com.dev.napolme.dto.character.CharacterInfoResponse;
import com.dev.napolme.dto.combat.CombatGrade;
import com.dev.napolme.dto.combat.CombatCompareRequest;
import com.dev.napolme.dto.combat.CombatCompareResponse;
import com.dev.napolme.dto.combat.CombatScoreRequest;
import com.dev.napolme.dto.combat.CombatScoreResponse;
import com.dev.napolme.dto.combat.Role;
import com.dev.napolme.dto.combat.StatsInput;
import com.dev.napolme.dto.combat.SynergyDetails;
import com.dev.napolme.service.character.CharacterAnalysisService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * 전투력 계산 서비스.
 * - 스탯 직접 입력 또는 serverId+characterId로 캐릭터 정보 조회 후 계산.
 */
@Service
public class CombatScoreService {

    private static final Map<String, Role> CLASS_NAME_TO_ROLE = Map.ofEntries(
        Map.entry("수호성", Role.TANK),
        Map.entry("Guardian", Role.TANK),
        Map.entry("검성", Role.MELEE_DPS),
        Map.entry("Slayer", Role.MELEE_DPS),
        Map.entry("마도성", Role.MAGIC_DPS),
        Map.entry("Sorcerer", Role.MAGIC_DPS),
        Map.entry("호법성", Role.SUPPORT),
        Map.entry("Paladin", Role.SUPPORT),
        Map.entry("궁성", Role.RANGED_DPS),
        Map.entry("Ranger", Role.RANGED_DPS),
        Map.entry("살성", Role.ASSASSIN),
        Map.entry("Assassin", Role.ASSASSIN),
        Map.entry("치유성", Role.HEALER),
        Map.entry("Cleric", Role.HEALER),
        Map.entry("정령성", Role.SUMMONER),
        Map.entry("Spiritmaster", Role.SUMMONER)
    );

    private final CombatPowerCalculator combatPowerCalculator;
    private final CharacterAnalysisService characterAnalysisService;

    public CombatScoreService(
        CombatPowerCalculator combatPowerCalculator,
        CharacterAnalysisService characterAnalysisService
    ) {
        this.combatPowerCalculator = combatPowerCalculator;
        this.characterAnalysisService = characterAnalysisService;
    }

    /**
     * 요청에 따라 전투력 계산.
     * - serverId + characterId 있으면 캐릭터 정보 조회 후 스탯 매핑하여 계산.
     * - stats 있으면 해당 스탯으로 계산.
     */
    public CombatScoreResponse calculate(CombatScoreRequest request) {
        if (request.serverId() != null && !request.serverId().isBlank()
            && request.characterId() != null && !request.characterId().isBlank()) {
            return calculateFromCharacter(request.serverId(), request.characterId(), request.role(), request.partyBuffs());
        }
        if (request.stats() != null) {
            StatsInput stats = request.stats();
            Role role = request.role();
            SynergyDetails buffs = request.partyBuffs();
            if (buffs != null || role != null) {
                return combatPowerCalculator.calculateWithBuffs(
                    stats,
                    role,
                    buffs != null ? buffs : SynergyDetails.zero()
                );
            }
            return combatPowerCalculator.calculate(stats);
        }
        throw new IllegalArgumentException("Either (serverId + characterId) or stats must be provided");
    }

    /** 스탯 미매칭 시 점수가 이 값보다 작으면 아이템 레벨 기반 폴백 적용 */
    private static final long ITEM_LEVEL_FALLBACK_THRESHOLD = 500;

    /** 아이템 레벨 → 나폴미 점수 환산 계수 (실제 스탯 없을 때 사용) */
    private static final int ITEM_LEVEL_TO_SCORE_FACTOR = 15;

    /**
     * 캐릭터 정보 API로 조회한 뒤 스탯을 매핑하여 전투력 계산.
     * 상세 스탯이 없거나 점수가 비정상적으로 낮으면 아이템 레벨 기반 점수로 대체.
     */
    public CombatScoreResponse calculateFromCharacter(
        String serverId,
        String characterId,
        Role roleOverride,
        SynergyDetails partyBuffs
    ) {
        CharacterInfoResponse info = characterAnalysisService.fetchCharacterInfo(serverId, characterId, "ko");
        StatsInput stats = buildStatsFromCharacterInfo(info);
        Role role = roleOverride != null ? roleOverride : inferRole(info != null ? info.className() : null);
        CombatScoreResponse result = partyBuffs != null
            ? combatPowerCalculator.calculateWithBuffs(stats, role, partyBuffs)
            : combatPowerCalculator.calculateWithBuffs(stats, role, SynergyDetails.zero());

        Integer itemLevel = info != null && info.statsSummary() != null ? info.statsSummary().itemLevel() : null;
        if (result.totalCombatPower() < ITEM_LEVEL_FALLBACK_THRESHOLD && itemLevel != null && itemLevel > 0) {
            long fallbackScore = (long) itemLevel * ITEM_LEVEL_TO_SCORE_FACTOR;
            result = new CombatScoreResponse(
                result.dpsScore(),
                result.defenseScore(),
                result.survivalScore(),
                result.utilityScore(),
                fallbackScore,
                result.breakdown(),
                result.buffedStats(),
                CombatGrade.fromTotalPower(fallbackScore)
            );
        }
        return result;
    }

    /**
     * 여러 캐릭터 비교. 각각 serverId+characterId로 조회 후 전투력 계산해 순위 반환.
     */
    public CombatCompareResponse compare(CombatCompareRequest request) {
        if (request.characterIds() == null || request.characterIds().isEmpty()) {
            throw new IllegalArgumentException("characterIds must not be empty");
        }
        // 비교는 (serverId, characterId) 쌍이 필요. 현재 요청은 saved character id만 있음.
        // 별도 DTO로 List<ServerCharacterRef> 받거나, 여기서는 미지원.
        return new CombatCompareResponse(List.of());
    }

    /**
     * CharacterInfoResponse의 primaryStats + specialStats를 StatsInput으로 변환.
     * PlayNC 실제 응답: type(영문 코드) + name(표시명, 한글 등). type 우선 매칭 후 name으로 보완.
     */
    StatsInput buildStatsFromCharacterInfo(CharacterInfoResponse info) {
        List<CharacterInfoResponse.StatItem> all = new ArrayList<>();
        if (info != null && info.statsSummary() != null) {
            if (info.statsSummary().primaryStats() != null) {
                all.addAll(info.statsSummary().primaryStats());
            }
            if (info.statsSummary().specialStats() != null) {
                all.addAll(info.statsSummary().specialStats());
            }
        }

        // type(영문) 우선, 없으면 name(한글/영문)으로 매칭. PlayNC 응답에 맞춘 키 목록.
        int attack = intFromStat(all, "PhysicalAttack", "Attack", "물리 공격력", "물리공격력", "공격력");
        int magicAttack = intFromStat(all, "MagicAttack", "마법 공격력", "마법공격력", "마법 공격");
        int defense = intFromStat(all, "PhysicalDefense", "Defense", "물리 방어력", "물리방어력", "방어력");
        int magicDefense = intFromStat(all, "MagicDefense", "마법 방어력", "마법방어력", "마법 방어");
        int hp = intFromStat(all, "MaxHp", "MaxHP", "Hp", "HP", "최대 HP", "최대HP", "생명력");
        int mp = intFromStat(all, "MaxMp", "MaxMP", "Mp", "MP", "최대 MP", "최대MP", "정신력");
        BigDecimal criticalRate = decimalFromStat(all, "CriticalRate", "Critical", "치명타 확률", "치명타확률", "치명타");
        BigDecimal criticalDamage = decimalFromStat(all, "CriticalDamage", "치명타 피해", "치명타피해", "치명 피해");
        if (criticalDamage != null) criticalDamage = criticalDamage.max(new BigDecimal("150"));
        int accuracy = intFromStat(all, "Accuracy", "Hit", "명중", "적중");
        int evasion = intFromStat(all, "Evasion", "회피", "회피율");
        BigDecimal attackSpeedRaw = decimalFromStat(all, "AttackSpeed", "공격 속도", "공격속도", "공속");
        BigDecimal castingSpeedRaw = decimalFromStat(all, "CastingSpeed", "시전 속도", "시전속도", "시전");
        int penetration = intFromStat(all, "Penetration", "관통", "관통력");
        int resilience = intFromStat(all, "Resilience", "강인", "강인함");

        if (criticalRate == null) criticalRate = BigDecimal.ZERO;
        if (criticalDamage == null) criticalDamage = new BigDecimal("150");
        BigDecimal attackSpeed = (attackSpeedRaw != null && attackSpeedRaw.compareTo(BigDecimal.ONE) >= 0)
            ? attackSpeedRaw : BigDecimal.ONE;
        BigDecimal castingSpeed = (castingSpeedRaw != null && castingSpeedRaw.compareTo(BigDecimal.ONE) >= 0)
            ? castingSpeedRaw : BigDecimal.ONE;

        return new StatsInput(
            attack, magicAttack, defense, magicDefense, hp, mp,
            criticalRate, criticalDamage,
            accuracy, evasion,
            attackSpeed, castingSpeed,
            penetration, resilience
        );
    }

    /** type(완전 일치) 또는 name(포함)으로 정수 스탯 추출. PlayNC 응답 type/name 모두 대응. */
    private static int intFromStat(List<CharacterInfoResponse.StatItem> items, String... typeOrNameKeys) {
        for (String key : typeOrNameKeys) {
            Optional<Integer> v = items.stream()
                .filter(s -> matchStat(s, key))
                .map(CharacterInfoResponse.StatItem::value)
                .filter(java.util.Objects::nonNull)
                .findFirst();
            if (v.isPresent()) return v.get();
        }
        return 0;
    }

    /** type(완전 일치) 또는 name(포함)으로 소수 스탯 추출. */
    private static BigDecimal decimalFromStat(List<CharacterInfoResponse.StatItem> items, String... typeOrNameKeys) {
        for (String key : typeOrNameKeys) {
            Optional<Integer> v = items.stream()
                .filter(s -> matchStat(s, key))
                .map(CharacterInfoResponse.StatItem::value)
                .filter(java.util.Objects::nonNull)
                .findFirst();
            if (v.isPresent()) return BigDecimal.valueOf(v.get());
        }
        return null;
    }

    /** PlayNC StatItem: type(영문 코드) 완전 일치 또는 name(표시명) 포함 여부 */
    private static boolean matchStat(CharacterInfoResponse.StatItem s, String key) {
        if (key == null) return false;
        if (s.type() != null && s.type().equalsIgnoreCase(key)) return true;
        if (s.name() != null && s.name().contains(key)) return true;
        return false;
    }

    private static Role inferRole(String className) {
        if (className == null || className.isBlank()) return null;
        return CLASS_NAME_TO_ROLE.get(className.trim());
    }
}
