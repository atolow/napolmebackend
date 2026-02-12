package com.dev.napolme.dto.character;

import com.dev.napolme.dto.cache.CachePolicyDto;
import java.util.List;

public record CharacterEquipmentItemResponse(
    Long id,
    String name,
    String grade,
    String gradeName,
    String icon,
    Integer level,
    Integer levelValue,
    Integer enchantLevel,
    Integer maxEnchantLevel,
    Integer maxExceedEnchantLevel,
    String raceName,
    List<String> classNames,
    String categoryName,
    Integer equipLevel,
    Integer magicStoneSlotCount,
    Integer godStoneSlotCount,
    List<String> costumes,
    Integer subStatCount,
    Integer subSkillCountMax,
    Boolean subStatRandom,
    List<MainStat> mainStats,
    String soulBindRate,
    List<SubStat> subStats,
    List<SubSkill> subSkills,
    List<MagicStoneStat> magicStoneStat,
    List<GodStoneStat> godStoneStat,
    List<String> sources,
    CachePolicyDto cache
) {
    public record MainStat(
        String name,
        String minValue,
        String value,
        String extra,
        Boolean exceed
    ) {}

    public record SubStat(
        String name,
        String value
    ) {}

    public record SubSkill(
        Integer id,
        Integer level,
        String icon,
        String name
    ) {}

    public record MagicStoneStat(
        String icon,
        String value,
        String name,
        String grade,
        Integer slotPos
    ) {}

    public record GodStoneStat(
        String icon,
        String name,
        String desc,
        String grade,
        Integer slotPos
    ) {}
}
